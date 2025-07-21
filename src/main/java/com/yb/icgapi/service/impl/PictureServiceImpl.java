package com.yb.icgapi.service.impl;

import java.io.IOException;
import java.util.Date;

import java.util.*;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yb.icgapi.annotation.MultiLevelCache;
import com.yb.icgapi.constant.DatabaseConstant;
import com.yb.icgapi.constant.PictureConstant;
import com.yb.icgapi.exception.BusinessException;
import com.yb.icgapi.exception.ErrorCode;
import com.yb.icgapi.exception.ThrowUtils;
import com.yb.icgapi.manager.CosManager;
import com.yb.icgapi.manager.FilePictureUpload;
import com.yb.icgapi.manager.UrlPictureUpload;
import com.yb.icgapi.manager.upload.PictureUploadTemplate;
import com.yb.icgapi.model.dto.file.UploadPictureResult;
import com.yb.icgapi.model.dto.picture.*;
import com.yb.icgapi.model.entity.Picture;
import com.yb.icgapi.model.entity.Space;
import com.yb.icgapi.model.entity.User;
import com.yb.icgapi.model.enums.PictureReviewStatusEnum;
import com.yb.icgapi.model.vo.PictureVO;
import com.yb.icgapi.model.vo.UserVO;
import com.yb.icgapi.service.PictureService;
import com.yb.icgapi.mapper.PictureMapper;
import com.yb.icgapi.service.UserService;
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
import java.util.stream.Collectors;

/**
 * @author songyibao
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-06-10 09:52:50
 */
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

    @Resource
    private UserService userService;

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
            oldPicture = this.getById(pictureId);
            ThrowUtils.ThrowIf(oldPicture == null, ErrorCode.NOT_FOUND, "图片不存在");
            // 校验图片操作权限
            this.checkPictureAuth(loginUser, oldPicture);
            // 防止更新时空间id被篡改
            if(!spaceId.equals(oldPicture.getSpaceId())){
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
                boolean res = this.saveOrUpdate(picture);
                ThrowUtils.ThrowIf(!res, ErrorCode.SERVER_ERROR, "图片上传失败");
                // 更新空间使用额度
                res = spaceService.lambdaUpdate()
                        .eq(Space::getId, spaceId)
                        .setSql("totalSize = totalSize + " + (picture.getPicSize()-oldPicture.getPicSize()))
                        .update();
                return res;
            });
            ThrowUtils.ThrowIf(!Boolean.TRUE.equals(execute_res), ErrorCode.SERVER_ERROR, "图片上传失败");
            // 清理旧图片文件
            this.clearPictureFile(oldPicture);
        }else{
            // 上传图片
            Boolean execute_res = transactionTemplate.execute(status -> {
                // 插入或更新
                boolean res = this.saveOrUpdate(picture);
                ThrowUtils.ThrowIf(!res, ErrorCode.SERVER_ERROR, "图片上传失败");
                // 更新空间使用额度
                res = spaceService.lambdaUpdate()
                        .eq(Space::getId, spaceId)
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                return res;
            });
            ThrowUtils.ThrowIf(!Boolean.TRUE.equals(execute_res), ErrorCode.SERVER_ERROR, "图片上传失败");
        }
        return PictureVO.objToVo(picture);
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        int currentPage = pictureQueryRequest.getCurrentPage();
        int pageSize = pictureQueryRequest.getPageSize();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        // 添加查询条件
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(wrapper -> wrapper
                    .like("name", searchText)
                    .or()
                    .like("introduction", searchText));
        }
        if(spaceId == null){
            // 如果没有空间id，则查询公共空间的图片
            queryWrapper.isNull("spaceId");
        } else {
            // 如果有空间id，则查询该空间下的图片
            queryWrapper.eq("spaceId", spaceId);
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);

        // JSON数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals(DatabaseConstant.ASC), sortField);

        return queryWrapper;
    }

    @Override
    public PictureVO getPictureVO(Picture picture) {
        if (picture == null) {
            return null;
        }
        // 对象转封装
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 查询并填充用户信息，不一定有
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            if (user != null) {
                pictureVO.setUser(UserVO.objToVO(user));
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
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
        // 查询并填充用户信息
        // 这里作一下优化，通过一次查询查出所需要的所有用户信息
        Set<Long> userIdSet = pictureList.stream()
                .map(Picture::getUserId)
                .filter(ObjUtil::isNotEmpty)
                .collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
        // 填充用户信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(UserVO.objToVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    @MultiLevelCache(KeyPrefix = "listPictureVOByPage", expireTime = 300, randomRange = 300)
    public Page<PictureVO> listPictureVOByPage(PictureQueryRequest pictureQueryRequest) {
        long currentPage = pictureQueryRequest.getCurrentPage();
        long pageSize = pictureQueryRequest.getPageSize();
        // 普通用户默认只能查看已经过审的数据
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        // 普通用户也不会获得审核信息字段
        pictureQueryRequest.setReviewerId(null);
        pictureQueryRequest.setReviewMessage(null);

        Page<Picture> page = this.page(
                new Page<>(currentPage, pageSize),
                this.getQueryWrapper(pictureQueryRequest)
        );
        return this.getPictureVOPage(page);
    }

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.ThrowIf(picture == null, ErrorCode.PARAM_BLANK);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String name = picture.getName();
        String introduction = picture.getIntroduction();
        String category = picture.getCategory();
        String tags = picture.getTags();
        Long picSize = picture.getPicSize();
        Integer picWidth = picture.getPicWidth();
        Integer picHeight = picture.getPicHeight();
        Double picScale = picture.getPicScale();
        String picFormat = picture.getPicFormat();
        Long userId = picture.getUserId();
        Date createTime = picture.getCreateTime();
        Date editTime = picture.getEditTime();
        Date updateTime = picture.getUpdateTime();

        ThrowUtils.ThrowIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.ThrowIf(url.length() > PictureConstant.PICTURE_URL_MAX_LENGTH,
                    ErrorCode.PARAMS_ERROR, "图片url过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.ThrowIf(introduction.length() > PictureConstant.PICTURE_INTRODUCTION_MAX_LENGTH,
                    ErrorCode.PARAMS_ERROR, "图片简介过长");
        }
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        boolean flag = id != null && reviewStatusEnum != null;
        ThrowUtils.ThrowIf(!flag, ErrorCode.PARAMS_ERROR);
        // 判断登录用户是否是管理员
        boolean isAdmin = userService.isAdmin(loginUser);
        ThrowUtils.ThrowIf(!isAdmin, ErrorCode.NO_AUTHORIZED, "只有管理员可以审核图片");
        // 查询图片
        Picture picture = this.getById(id);
        ThrowUtils.ThrowIf(picture == null, ErrorCode.NOT_FOUND, "图片不存在");
        // 防止重复审核
        ThrowUtils.ThrowIf(Objects.equals(picture.getReviewStatus(), reviewStatus), ErrorCode.PARAMS_ERROR, "图片已处于该状态，无需重复审核");
        // 更新图片状态
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean res = this.updateById(updatePicture);
        ThrowUtils.ThrowIf(!res, ErrorCode.SERVER_ERROR, "图片审核失败");
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
            if (userService.isAdmin(loginUser)) {
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
        Picture oldPicture = this.getById(pictureEditRequest.getId());
        ThrowUtils.ThrowIf(oldPicture == null, ErrorCode.NOT_FOUND, "图片不存在");
        // 校验权限
        this.checkPictureAuth(loginUser, oldPicture);

        // 校验通过，开始编辑
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 注意将tags转string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        this.validPicture(picture);
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 更新图片信息
        boolean res = this.updateById(picture);
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
        Picture oldPicture = this.getById(id);
        // 判断是否存在
        ThrowUtils.ThrowIf(oldPicture == null, ErrorCode.NOT_FOUND, "图片不存在");
        // 校验权限
        this.checkPictureAuth(loginUser, oldPicture);
        // 操作数据库，使用事务
        Boolean res = transactionTemplate.execute(status -> {
            // 删除图片
            boolean deleteResult = this.removeById(id);
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
        // 如果需要，还应该先判断图片是否被多条记录关联,这里模拟一下判断
        String pictureUrl = oldPicture.getUrl();
        Long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        if (count > 1) {
            log.error("图片 {} 被多条记录关联，无法删除文件", pictureUrl);
            return; // 如果被多条记录关联，则不删除文件
        }
        // 删除图片
        cosManager.deleteObject(pictureUrl);
        // 删除缩略图
        if (StrUtil.isNotBlank(oldPicture.getThumbnailUrl())) {
            cosManager.deleteObject(oldPicture.getThumbnailUrl());
        }

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
            ThrowUtils.ThrowIf(!Objects.equals(user.getId(), picture.getUserId()) && !userService.isAdmin(user),
                    ErrorCode.NO_AUTHORIZED, "您没有权限操作该图片");
        } else {
            // 判断用户是否有权限访问该空间
            spaceService.checkSpaceAuth(user, spaceId);
        }
    }

}




