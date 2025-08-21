package com.yb.icgapi.icpic.application.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yb.icgapi.constant.SpaceUserPermissionConstant;
import com.yb.icgapi.icpic.application.service.PictureApplicationService;
import com.yb.icgapi.icpic.application.service.UserApplicationService;
import com.yb.icgapi.icpic.domain.picture.entity.Picture;
import com.yb.icgapi.icpic.domain.picture.service.PictureDomainService;
import com.yb.icgapi.icpic.domain.user.entity.User;
import com.yb.icgapi.icpic.infrastructure.annotation.MultiLevelCache;
import com.yb.icgapi.icpic.infrastructure.api.CosManager;
import com.yb.icgapi.icpic.infrastructure.api.aliYunAi.AliYunAiApi;
import com.yb.icgapi.icpic.infrastructure.api.aliYunAi.model.CreateOutPaintingTaskResponse;
import com.yb.icgapi.icpic.infrastructure.api.aliYunAi.model.GetOutPaintingTaskResponse;
import com.yb.icgapi.icpic.infrastructure.api.imagesearch.ImageSearchApiFacade;
import com.yb.icgapi.icpic.infrastructure.api.imagesearch.model.ImageSearchResult;
import com.yb.icgapi.icpic.infrastructure.exception.BusinessException;
import com.yb.icgapi.icpic.infrastructure.exception.ErrorCode;
import com.yb.icgapi.icpic.infrastructure.exception.ThrowUtils;
import com.yb.icgapi.icpic.interfaces.dto.picture.*;
import com.yb.icgapi.icpic.interfaces.vo.picture.PictureVO;
import com.yb.icgapi.manager.FilePictureUpload;
import com.yb.icgapi.manager.UrlPictureUpload;
import com.yb.icgapi.manager.auth.SpaceUserAuthManager;
import com.yb.icgapi.manager.auth.StpKit;
import com.yb.icgapi.manager.upload.PictureUploadTemplate;
import com.yb.icgapi.model.dto.analyze.SpaceTagAnalyzeRequest;
import com.yb.icgapi.model.dto.file.UploadPictureResult;
import com.yb.icgapi.model.entity.Space;
import com.yb.icgapi.model.enums.PictureReviewStatusEnum;
import com.yb.icgapi.model.vo.PictureTagCategory;
import com.yb.icgapi.model.vo.analyze.SpaceTagAnalyzeResponse;
import com.yb.icgapi.service.AIMessageService;
import com.yb.icgapi.service.impl.SpaceServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author songyibao
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-06-10 09:52:50
 */
@Slf4j
@Service
public class PictureApplicationServiceImpl implements PictureApplicationService {

    @Resource
    SpaceUserAuthManager spaceUserAuthManager;
    @Resource
    PictureDomainService pictureDomainService;
    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    FilePictureUpload filePictureUpload;

    @Resource
    UrlPictureUpload urlPictureUpload;

    @Resource
    private CosManager cosManager;

    @Resource
    private SpaceServiceImpl spaceService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private AIMessageService aiMessageService;

    @Resource
    private AliYunAiApi aliYunAiApi;


    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.ThrowIf(inputSource == null, ErrorCode.PARAMS_ERROR, "上传的文件不能为空");
        ThrowUtils.ThrowIf(loginUser == null, ErrorCode.NO_AUTHORIZED);
        ThrowUtils.ThrowIf(pictureUploadRequest == null, ErrorCode.PARAM_BLANK, "图片上传请求不能为空");
        Picture oldPicture;
        Long pictureId = pictureUploadRequest.getId();
        Long spaceId = pictureUploadRequest.getSpaceId();
        if(pictureId ==null){
            oldPicture = null;
            // 上传图片
            if(spaceId == null){
                // 上传到公共空间，暂时不需要任何校验
            }else{
                // 上传到私有空间，需要进行空间权限校验和空间额度校验
                Space space = spaceService.getById(spaceId);
                ThrowUtils.ThrowIf(space == null, ErrorCode.NOT_FOUND, "空间不存在");
                ThrowUtils.ThrowIf(!space.getUserId().equals(loginUser.getId()),ErrorCode.NO_AUTHORIZED, "没有权限访问该空间");
                // 校验空间额度，不进行复杂的大小额度校验，只要还没满，就允许
                if(space.getTotalCount()>=space.getMaxCount()){
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "已达到数量限制，无法上传图片");
                }
                if(space.getTotalSize() >= space.getMaxSize()){
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "已达到容量大小限制，无法上传图片");
                }
            }
        }else{
            // 更新图片，不进行空间额度校验
            oldPicture = pictureDomainService.getById(pictureId);
            ThrowUtils.ThrowIf(oldPicture == null, ErrorCode.NOT_FOUND, "图片不存在");
            // 校验图片操作权限,已经更改为注解权限校验
//            this.checkPictureAuth(loginUser, oldPicture);
            // 防止更新时空间id被篡改(如果有)
            if(spaceId!=null && !spaceId.equals(oldPicture.getSpaceId())){
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }
        }
        // 通过后再执行实际上传
        String uploadPathPrefix;
        if(spaceId == null){
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        }else{
            // 如果有空间id，则使用空间id作为前缀
            uploadPathPrefix = String.format("space/%s", spaceId);
        }
        // 根据 inputSource 类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        Picture picture = new Picture();
        picture.setId(pictureId);
        picture.setSpaceId(spaceId);
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        String picName = uploadPictureResult.getPicName();
        if (StrUtil.isNotBlank(pictureUploadRequest.getName())) {
            // 如果有传入图片名称，则使用传入的名称
            picName = pictureUploadRequest.getName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setPicColor(uploadPictureResult.getPicColor());
        picture.setUserId(loginUser.getId());
        if(picture.getId()!= null){
            // 如果是更新图片，则设置编辑时间
            picture.setEditTime(new Date());
        }

        // 补充审核参数，这步约定放在最后，即图片信息被填充完整之后
        this.fillReviewParams(picture, loginUser);

        if (oldPicture != null) {
            // 更新图片
            Boolean execute_res = transactionTemplate.execute(status -> {
                // 插入或更新
                boolean res = pictureDomainService.saveOrUpdate(picture);
                ThrowUtils.ThrowIf(!res, ErrorCode.SERVER_ERROR, "图片上传失败");
                if(spaceId != null){
                    // 更新空间使用额度
                    res = spaceService.lambdaUpdate()
                            .eq(Space::getId, spaceId)
                            .setSql("totalSize = totalSize + " + (picture.getPicSize()-oldPicture.getPicSize()))
                            .update();
                }
                return res;
            });
            ThrowUtils.ThrowIf(!Boolean.TRUE.equals(execute_res), ErrorCode.SERVER_ERROR, "图片上传失败");
            // 清理旧图片文件
            this.clearPictureFile(oldPicture);

            // 发送AI处理消息 - 更新图片时也需要重新处理
            try {
                aiMessageService.sendSingleProcessMessage(
                    picture.getId(),
                    true // 更新图片时强制重新处理
                );
                log.info("图片更新后AI处理消息发送成功，图片ID: {}", picture.getId());
            } catch (Exception e) {
                log.error("图片更新后AI处理消息发送失败，图片ID: {}, 错误: {}", picture.getId(), e.getMessage(), e);
            }
        }else{
            // 上传图片
            Boolean execute_res = transactionTemplate.execute(status -> {
                boolean res = pictureDomainService.saveOrUpdate(picture);
                ThrowUtils.ThrowIf(!res, ErrorCode.SERVER_ERROR, "图片上传失败");
                if(spaceId != null){
                    // 更新空间使用额度
                    res = spaceService.lambdaUpdate()
                            .eq(Space::getId, spaceId)
                            .setSql("totalSize = totalSize + " + picture.getPicSize())
                            .setSql("totalCount = totalCount + 1")
                            .update();
                }

                return res;
            });
            ThrowUtils.ThrowIf(!Boolean.TRUE.equals(execute_res), ErrorCode.SERVER_ERROR, "图片上传失败");

            // 发送AI处理消息 - 新上传的图片
            try {
                aiMessageService.sendSingleProcessMessage(
                    picture.getId(),
                    false // 新图片不需要强制重新处理
                );
                log.info("新图片上传后AI处理消息发送成功，图片ID: {}", picture.getId());
            } catch (Exception e) {
                log.error("新图片上传后AI处理消息发送失败，图片ID: {}, 错误: {}", picture.getId(), e.getMessage(), e);
            }
        }
        return Picture.toVO(picture);
    }


    /**
     * 获取图片VO，会查询用户信息
     * @param picture
     * @return
     */
    @Override
    public PictureVO getPictureVO(Picture picture) {
        if (picture == null) {
            return null;
        }
        // 对象转封装
        PictureVO pictureVO = Picture.toVO(picture);
        // 查询并填充用户信息，不一定有
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userApplicationService.getUserById(userId);
            if (user != null) {
                pictureVO.setUser(User.toVO(user));
            }
        }
        return pictureVO;
    }

    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装列表
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(Picture::toVO)
                .collect(Collectors.toList());
        // 查询并填充用户信息
        // 这里作一下优化，通过一次查询查出所需要的所有用户信息
        Set<Long> userIdSet = pictureList.stream()
                .map(Picture::getUserId)
                .filter(ObjUtil::isNotEmpty)
                .collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userApplicationService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
        // 填充用户信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(User.toVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    @MultiLevelCache(KeyPrefix = "listPictureVOByPage", expireTime = 300, randomRange = 300)
    public Page<PictureVO> listPictureVOByPage(PictureQueryRequest pictureQueryRequest) {
        Page<Picture> picturePage =
                this.listPictureByPage(pictureQueryRequest);
        return this.getPictureVOPage(picturePage);
    }

    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser){
        Space space = spaceService.getById(spaceId);
        ThrowUtils.ThrowIf(space == null, ErrorCode.NOT_FOUND, "空间不存在");
        ThrowUtils.ThrowIf(!space.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTHORIZED, "没有权限访问该空间");
        return pictureDomainService.searchPictureByColor(spaceId,picColor,loginUser);
    }


    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        pictureDomainService.doPictureReview(pictureReviewRequest,loginUser);
    }

    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        Long spaceId = picture.getSpaceId();
        if(spaceId != null && spaceService.getById(spaceId).getUserId().equals(loginUser.getId())){
            // 如果指定了上传空间且是用户自己的空间，则不需要审核
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("用户空间自动过审");
            picture.setReviewTime(new Date());
            return;
        }
        if(spaceId == null){
            // 上传到公共空间
            if (loginUser.isAdmin()) {
                // 管理员自动过审
                picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
                picture.setReviewerId(loginUser.getId());
                picture.setReviewMessage("管理员自动过审");
                picture.setReviewTime(new Date());
            } else {
                // 非管理员，创建或编辑都要改为待审核
                picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
            }
        }
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = StrUtil.isNotBlank(searchText) ? searchText + "_" : "default_";
        }
        ThrowUtils.ThrowIf(count < 0, ErrorCode.PARAMS_ERROR, "上传数量不能小于0");
        ThrowUtils.ThrowIf(count > 30, ErrorCode.PARAMS_ERROR, "最多支持批量抓取30张图片");
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败，错误信息：{}", e.getMessage());
            throw new BusinessException(ErrorCode.SERVER_ERROR, "获取页面失败");
        }
        Element div = document.getElementsByClass("dgControl").first();
        ThrowUtils.ThrowIf(div == null, ErrorCode.SERVER_ERROR, "获取页面元素失败");
        Elements elements = div.select("img.mimg");
        // 遍历元素，依次处理上传图片
        int uploadedCount = 0;
        for (Element element : elements) {
            String fileUrl = element.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已经跳过：{}", fileUrl);
                continue; // 跳过没有src的元素
            }
            // 处理图片地址，防止特殊字符转义、对象存储冲突等
            int index = fileUrl.indexOf("?");
            if (index != -1) {
                fileUrl = fileUrl.substring(0, index);
            }
            // 复用已有服务，上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            pictureUploadRequest.setName(namePrefix + (uploadedCount + 1));
            try {
                this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("上传图片成功，图片地址：{}", fileUrl);
                uploadedCount++;
            } catch (Exception e) {
                log.error("上传图片失败，图片地址：{}，错误信息：{}", fileUrl, e.getMessage());
                continue;
            }
            if (uploadedCount >= count) {
                log.info("已上传{}张图片，达到批量上传限制", uploadedCount);
                break; // 达到上传数量限制，退出循环
            }
        }
        return uploadedCount;
    }


    /**
     * 编辑图片信息
     *
     * @param pictureEditRequest 图片编辑请求
     * @param loginUser 用户信息
     */
    @Override
    public void editPicture(PictureEditRequest pictureEditRequest,
                            User loginUser) {
        // 判断旧图片是否存在
        Picture oldPicture = pictureDomainService.getById(pictureEditRequest.getId());
        ThrowUtils.ThrowIf(oldPicture == null, ErrorCode.NOT_FOUND, "图片不存在");
        // 校验权限,已经更改为注解权限校验
//        this.checkPictureAuth(loginUser, oldPicture);

        // 校验通过，开始编辑
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 注意将tags转string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        Picture.validPicture(picture);
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 更新图片信息
        boolean res = pictureDomainService.updateById(picture);
        ThrowUtils.ThrowIf(!res, ErrorCode.SERVER_ERROR, "编辑图片失败");
    }
    /**
     * 删除图片
     *
     * @param id 图片ID
     * @param loginUser 登录用户信息
     */
    @Override
    public void deletePicture(long id, User loginUser) {
        Picture oldPicture = pictureDomainService.getById(id);
        // 判断是否存在
        ThrowUtils.ThrowIf(oldPicture == null, ErrorCode.NOT_FOUND, "图片不存在");
        // 校验权限,已经更改为注解权限校验
//        this.checkPictureAuth(loginUser, oldPicture);
        // 操作数据库，使用事务
        Boolean res = transactionTemplate.execute(status -> {
            // 删除图片
            boolean deleteResult = pictureDomainService.removeById(id);
            ThrowUtils.ThrowIf(!deleteResult, ErrorCode.SERVER_ERROR, "删除图片失败");
            // 更新额度信息
            Long spaceId = oldPicture.getSpaceId();
            if (spaceId != null) {
                boolean updateResult = spaceService.lambdaUpdate()
                        .eq(Space::getId, spaceId)
                        .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.ThrowIf(!updateResult, ErrorCode.SERVER_ERROR, "更新空间额度失败");
            }
            return true;
        });
        // 异步清理图片文件
        this.clearPictureFile(oldPicture);
    }

    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        pictureDomainService.clearPictureFile(oldPicture);
    }

    /**
     * 检查用户对于某个图片的操作权限
     *
     * @param user
     * @param picture
     */
    @Override
    public void checkPictureAuth(User user, Picture picture) {
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
            // 如果没有空间id，则认为是公共空间，只有图片所有者和管理员对其有操作权限
            ThrowUtils.ThrowIf(!Objects.equals(user.getId(), picture.getUserId()) && !user.isAdmin(),
                    ErrorCode.NO_AUTHORIZED, "您没有权限操作该图片");
        } else {
            // 判断用户是否有权限访问该空间
            spaceService.checkSpaceAuth(user, spaceId);
        }
    }


    /**
     * 创建AI扩图任务
     */
    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(
            CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
            User loginUser) {
        return pictureDomainService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest,loginUser);
    }
    /**
     * 查询AI扩图任务状态
     */
    @Override
    public GetOutPaintingTaskResponse getPictureOutPaintingTask(
            String taskId) {
        return pictureDomainService.getPictureOutPaintingTask(taskId);
    }

    @Override
    public boolean updatePicture(Picture picture,User loginUser) {

        // 数据校验
        Picture.validPicture(picture);
        // 判断旧图片是否存在
        Picture oldPicture = pictureDomainService.getById(picture.getId());
        ThrowUtils.ThrowIf(oldPicture == null, ErrorCode.NOT_FOUND, "图片不存在");
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 更新图片信息
        boolean res = pictureDomainService.updateById(picture);
        ThrowUtils.ThrowIf(!res, ErrorCode.SERVER_ERROR, "更新图片失败");
        return true;
    }

    @Override
    public Picture getPictureById(Long id) {
        return pictureDomainService.getById(id);
    }

    @Override
    public Page<Picture> listPictureByPage(PictureQueryRequest pictureQueryRequest) {
        return pictureDomainService.listPictureByPage(pictureQueryRequest);
    }

    @Override
    public PictureVO getPictureVOById(Long id, HttpServletRequest request) {
        Picture picture = this.getPictureById(id);
        User loginUser = userApplicationService.getLoginUser(request);
        ThrowUtils.ThrowIf(picture == null, ErrorCode.NOT_FOUND, "图片不存在");
        Long spaceId = picture.getSpaceId();
        Space space = null;
        if(spaceId != null){
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.ThrowIf(!hasPermission, ErrorCode.NO_AUTHORIZED);
            space = spaceService.getById(spaceId);
            ThrowUtils.ThrowIf(ObjUtil.isEmpty(space), ErrorCode.NOT_FOUND, "空间不存在");
        }
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        PictureVO pictureVO = this.getPictureVO(picture);
        // 设置权限列表
        pictureVO.setPermissionList(permissionList);
        return pictureVO;
    }

    @Override
    public PictureTagCategory listPIctureTagCategory() {
        return pictureDomainService.listPictureTagCategory();
    }

    @Override
    public List<ImageSearchResult> searchPictureByPicture(SearchPictureByPictureRequest searchPictureByPictureRequest) {
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        ThrowUtils.ThrowIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture oldPicture = this.getPictureById(pictureId);
        ThrowUtils.ThrowIf(oldPicture == null, ErrorCode.NOT_FOUND);
        return ImageSearchApiFacade.searchImage(oldPicture.getUrl());
    }

    @Override
    public List<Long> getPictureSizeList(Boolean isQueryPublic,Long spaceId) {
        if(spaceId == null){
            return pictureDomainService.getAllPictureSizeList(isQueryPublic);
        }else{
            return pictureDomainService.getSpacePictureSizeList(spaceId);
        }


    }

    @Override
    public List<Map<String, Object>> listMaps(QueryWrapper<Picture> queryWrapper) {
        return pictureDomainService.listMaps(queryWrapper);
    }

    @Override
    public Map<String, Long> getTagCountMap(boolean isQueryPublic, Long spaceId) {
        return pictureDomainService.getTagCountMap(isQueryPublic, spaceId);
    }

    @Override
    public List<Picture> getByIds(Set<Long> pictureIds) {
        return pictureDomainService.getByIds(pictureIds);
    }


}
