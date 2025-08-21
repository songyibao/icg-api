package com.yb.icgapi.icpic.domain.picture.service.impl;

import java.awt.*;
import java.io.IOException;
import java.util.Date;

import java.util.*;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yb.icgapi.constant.SpaceUserPermissionConstant;
import com.yb.icgapi.icpic.application.service.UserApplicationService;
import com.yb.icgapi.icpic.domain.picture.repository.PictureRepository;
import com.yb.icgapi.icpic.infrastructure.annotation.MultiLevelCache;
import com.yb.icgapi.icpic.infrastructure.api.aliYunAi.AliYunAiApi;
import com.yb.icgapi.icpic.infrastructure.api.aliYunAi.model.CreateOutPaintingTaskRequest;
import com.yb.icgapi.icpic.infrastructure.api.aliYunAi.model.CreateOutPaintingTaskResponse;
import com.yb.icgapi.icpic.infrastructure.api.aliYunAi.model.GetOutPaintingTaskResponse;
import com.yb.icgapi.icpic.infrastructure.constant.DatabaseConstant;
import com.yb.icgapi.icpic.infrastructure.exception.BusinessException;
import com.yb.icgapi.icpic.infrastructure.exception.ErrorCode;
import com.yb.icgapi.icpic.infrastructure.exception.ThrowUtils;
import com.yb.icgapi.icpic.infrastructure.api.CosManager;
import com.yb.icgapi.manager.FilePictureUpload;
import com.yb.icgapi.manager.UrlPictureUpload;
import com.yb.icgapi.manager.auth.StpKit;
import com.yb.icgapi.manager.upload.PictureUploadTemplate;
import com.yb.icgapi.model.dto.analyze.SpaceTagAnalyzeRequest;
import com.yb.icgapi.model.dto.file.UploadPictureResult;
import com.yb.icgapi.icpic.interfaces.dto.picture.*;
import com.yb.icgapi.icpic.domain.picture.entity.Picture;
import com.yb.icgapi.model.entity.Space;
import com.yb.icgapi.icpic.domain.user.entity.User;
import com.yb.icgapi.model.enums.PictureReviewStatusEnum;
import com.yb.icgapi.icpic.interfaces.vo.picture.PictureVO;
import com.yb.icgapi.model.vo.PictureTagCategory;
import com.yb.icgapi.model.vo.analyze.SpaceTagAnalyzeResponse;
import com.yb.icgapi.service.AIMessageService;
import com.yb.icgapi.icpic.domain.picture.service.PictureDomainService;
import com.yb.icgapi.icpic.infrastructure.utils.ColorSimilarUtils;
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
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author songyibao
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-06-10 09:52:50
 */
@Slf4j
@Service
public class PictureDomainServiceImpl implements PictureDomainService {

    @Resource
    PictureRepository pictureRepository;


    @Resource
    FilePictureUpload filePictureUpload;

    @Resource
    UrlPictureUpload urlPictureUpload;

    @Resource
    private CosManager cosManager;


    @Resource
    private AliYunAiApi aliYunAiApi;

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
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();


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
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        queryWrapper.le(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);


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
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser){
        // 查询该空间下所有图片，必须有主色调
        List<Picture> pictureList = pictureRepository.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();
        // 如果没有图片，直接返回空列表
        if (CollUtil.isEmpty(pictureList)) {
            return Collections.emptyList();
        }
        // 将目标颜色转换为color对象
        Color targetColor = Color.decode(picColor);
        // 计算相似度并排序
        List<Picture> sortedPictures = pictureList.stream()
                .sorted(Comparator.comparingDouble(picture->{
                    String hexColor = picture.getPicColor();
                    // 没有主色调的图片放到最后
                    if (StrUtil.isBlank(hexColor)) {
                        return Double.MAX_VALUE; // 没有主色调的图片放到最后
                    }
                    Color pictureColor = Color.decode(hexColor);
                    // 计算颜色差异
                    return -ColorSimilarUtils.calculateSimilarity(targetColor,pictureColor);
                }))
                .limit(12)
                .collect(Collectors.toList());
        return sortedPictures.stream()
                .map(Picture::toVO)
                .collect(Collectors.toList());
    }


    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        boolean flag = id != null && reviewStatusEnum != null;
        ThrowUtils.ThrowIf(!flag, ErrorCode.PARAMS_ERROR);
        // 判断登录用户是否是管理员
        ThrowUtils.ThrowIf(!loginUser.isAdmin(), ErrorCode.NO_AUTHORIZED, "只有管理员可以审核图片");
        // 查询图片
        Picture picture = pictureRepository.getById(id);
        ThrowUtils.ThrowIf(picture == null, ErrorCode.NOT_FOUND, "图片不存在");
        // 防止重复审核
        ThrowUtils.ThrowIf(Objects.equals(picture.getReviewStatus(), reviewStatus), ErrorCode.PARAMS_ERROR, "图片已处于该状态，无需重复审核");
        // 更新图片状态
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean res = pictureRepository.updateById(updatePicture);
        ThrowUtils.ThrowIf(!res, ErrorCode.SERVER_ERROR, "图片审核失败");
    }


    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 如果需要，还应该先判断图片是否被多条记录关联,这里模拟一下判断
        String pictureUrl = oldPicture.getUrl();
        Long count = pictureRepository.lambdaQuery()
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
     * 创建AI扩图任务
     */
    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(
            CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
            User loginUser) {
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        Picture picture = pictureRepository.getById(pictureId);
        ThrowUtils.ThrowIf(picture == null, ErrorCode.NOT_FOUND, "图片不存在");
        // 校验权限,已经更改为注解权限校验
//        this.checkPictureAuth(loginUser, picture);
        // 创建请求对象
        CreateOutPaintingTaskRequest request = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        request.setInput(input);
        request.setParameters(createPictureOutPaintingTaskRequest.getParameters());
        // 发送请求
        return aliYunAiApi.createOutPaintingTask(request);
    }
    /**
     * 查询AI扩图任务状态
     */
    @Override
    public GetOutPaintingTaskResponse getPictureOutPaintingTask(
            String taskId) {
        // 参数校验
        ThrowUtils.ThrowIf(StrUtil.isBlank(taskId), ErrorCode.PARAM_BLANK, "任务ID不能为空");
        // 查询任务状态
        GetOutPaintingTaskResponse response = aliYunAiApi.getOutPaintingTask(taskId);
        // 检查任务状态
        if (response == null || response.getOutput() == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在或状态未知");
        }
        return response;
    }

    @Override
    public Picture getById(Long id) {
        return pictureRepository.getById(id);
    }

    @Override
    public boolean updateById(Picture picture) {
        return pictureRepository.updateById(picture);
    }

    @Override
    public boolean removeById(long id) {
        return pictureRepository.removeById(id);
    }

    @Override
    public boolean saveOrUpdate(Picture picture) {
        return pictureRepository.saveOrUpdate(picture);
    }



    @Override
    public Page<Picture> listPictureByPage(PictureQueryRequest pictureQueryRequest) {
        long currentPage = pictureQueryRequest.getCurrentPage();
        long pageSize = pictureQueryRequest.getPageSize();
        // 空间权限校验
        Long spaceId = pictureQueryRequest.getSpaceId();
        // 普通用户默认只能查看已经过审的数据(仅针对公共空间)
        if (spaceId == null) {
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        } else {
            // 私有空间，校验权限
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.ThrowIf(!hasPermission, ErrorCode.NO_AUTHORIZED );
        }
        // 普通用户也不会获得审核信息字段
        pictureQueryRequest.setReviewerId(null);
        pictureQueryRequest.setReviewMessage(null);

        return pictureRepository.page(
                new Page<>(currentPage, pageSize),
                this.getQueryWrapper(pictureQueryRequest)
        );
    }

    @Override
    public PictureTagCategory listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return pictureTagCategory;
    }


    @Override
    public List<Long> getAllPictureSizeList(Boolean isQueryPublic) {
        return pictureRepository.listObjs(
                new LambdaQueryWrapper<Picture>()
                        .isNull(isQueryPublic, Picture::getSpaceId)
                        .select(Picture::getPicSize)
                , obj -> (Long) obj);
    }

    @Override
    public List<Long> getSpacePictureSizeList(Long spaceId) {
        return pictureRepository.listObjs(
                new LambdaQueryWrapper<Picture>()
                        .eq(Picture::getSpaceId, spaceId)
                        .select(Picture::getPicSize)
                , obj -> (Long) obj);
    }

    @Override
    public List<Map<String, Object>> listMaps(QueryWrapper<Picture> queryWrapper) {
        return pictureRepository.listMaps(queryWrapper);
    }

    @Override
    public Map<String, Long> getTagCountMap(boolean isQueryPublic, Long spaceId) {
        // 创建查询条件
        LambdaQueryWrapper<Picture> lambdaQueryWrapper = new LambdaQueryWrapper<>();
//        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if(spaceId!=null){
            lambdaQueryWrapper.eq(Picture::getSpaceId,spaceId);
        }else{
            if(isQueryPublic){
                lambdaQueryWrapper.isNull(Picture::getSpaceId);
            }
        }

        // 每张图可能使用多个tag 保存形式为 json string ["tag1", "tag2", ...]
        lambdaQueryWrapper.select(Picture::getTags);
        lambdaQueryWrapper.isNotNull(Picture::getTags);
        // 查询所有图片的tags
        List<String> tagsList = pictureRepository.listObjs(lambdaQueryWrapper, obj -> (String) obj);
        // 统计每个tag的数量
        return tagsList.stream()
                .flatMap(tags -> {
                    // 把每个图片的tags字符串转换为List
                    return JSONUtil.toList(tags, String.class).stream();
                })
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
    }

    @Override
    public List<Picture> getByIds(Set<Long> pictureIds) {
        return pictureRepository.getBaseMapper().selectByIds(pictureIds);
    }


}
