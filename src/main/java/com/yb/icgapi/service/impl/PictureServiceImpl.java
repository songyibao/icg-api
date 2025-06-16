package com.yb.icgapi.service.impl;

import java.io.IOException;
import java.util.Date;

import java.util.*;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yb.icgapi.annotation.MultiLevelCache;
import com.yb.icgapi.common.ResultUtils;
import com.yb.icgapi.constant.DatabaseConstant;
import com.yb.icgapi.constant.GlobalConstant;
import com.yb.icgapi.constant.PictureConstant;
import com.yb.icgapi.exception.BusinessException;
import com.yb.icgapi.exception.ErrorCode;
import com.yb.icgapi.exception.ThrowUtils;
import com.yb.icgapi.manager.FilePictureUpload;
import com.yb.icgapi.manager.UrlPictureUpload;
import com.yb.icgapi.manager.upload.PictureUploadTemplate;
import com.yb.icgapi.model.dto.file.UploadPictureResult;
import com.yb.icgapi.model.dto.picture.PictureQueryRequest;
import com.yb.icgapi.model.dto.picture.PictureReviewRequest;
import com.yb.icgapi.model.dto.picture.PictureUploadByBatchRequest;
import com.yb.icgapi.model.dto.picture.PictureUploadRequest;
import com.yb.icgapi.model.entity.Picture;
import com.yb.icgapi.model.entity.User;
import com.yb.icgapi.model.enums.PictureReviewStatusEnum;
import com.yb.icgapi.model.vo.PictureVO;
import com.yb.icgapi.model.vo.UserVO;
import com.yb.icgapi.service.PictureService;
import com.yb.icgapi.mapper.PictureMapper;
import com.yb.icgapi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.implementation.bytecode.Throw;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
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

    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.ThrowIf(inputSource == null, ErrorCode.PARAMS_ERROR, "上传的文件不能为空");
        ThrowUtils.ThrowIf(loginUser == null, ErrorCode.NO_AUTHORIZED);
        // 判断是新增还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果更新图片，需要校验图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.ThrowIf(oldPicture == null, ErrorCode.NOT_FOUND, "图片不存在");
            // 如果存在，校验是否是本人或者管理员
            boolean isAdmin = userService.isAdmin(loginUser);
            boolean isOwner = loginUser.getId().equals(oldPicture.getUserId());
            ThrowUtils.ThrowIf(!isAdmin && !isOwner, ErrorCode.NO_AUTHORIZED, "没有权限编辑该图片");
        }
        // 上传图片，得到信息
        // 按照用户id划分目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        // 根据 inputSource 类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        String picName = uploadPictureResult.getPicName();
        if(pictureUploadRequest!=null && StrUtil.isNotBlank(pictureUploadRequest.getName())) {
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
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        if (pictureId != null) {
            // 更新图片
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        boolean res = this.saveOrUpdate(picture);
        ThrowUtils.ThrowIf(!res, ErrorCode.SERVER_ERROR, "图片上传失败");
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
        // 添加查询条件
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(wrapper -> wrapper
                    .like("name", searchText)
                    .or()
                    .like("introduction", searchText));
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

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if(StrUtil.isBlank(namePrefix)){
            namePrefix = StrUtil.isNotBlank(searchText)?searchText+"_":"default_";
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
            try{
                this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("上传图片成功，图片地址：{}", fileUrl);
                uploadedCount++;
            }catch (Exception e) {
                log.error("上传图片失败，图片地址：{}，错误信息：{}", fileUrl, e.getMessage());
                continue;
            }
            if(uploadedCount>=count) {
                log.info("已上传{}张图片，达到批量上传限制", uploadedCount);
                break; // 达到上传数量限制，退出循环
            }
        }
        return uploadedCount;
    }


}




