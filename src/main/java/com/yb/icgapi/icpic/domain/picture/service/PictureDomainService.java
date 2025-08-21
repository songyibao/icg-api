package com.yb.icgapi.icpic.domain.picture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yb.icgapi.icpic.infrastructure.api.aliYunAi.model.CreateOutPaintingTaskResponse;
import com.yb.icgapi.icpic.infrastructure.api.aliYunAi.model.GetOutPaintingTaskResponse;
import com.yb.icgapi.icpic.interfaces.dto.picture.*;
import com.yb.icgapi.icpic.domain.picture.entity.Picture;
import com.yb.icgapi.icpic.domain.user.entity.User;
import com.yb.icgapi.icpic.interfaces.vo.picture.PictureVO;
import com.yb.icgapi.model.dto.analyze.SpaceTagAnalyzeRequest;
import com.yb.icgapi.model.vo.PictureTagCategory;
import com.yb.icgapi.model.vo.analyze.SpaceTagAnalyzeResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
* @author songyibao
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-06-10 09:52:50
*/
public interface PictureDomainService {

    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);
    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);
    
    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);





    void clearPictureFile(Picture oldPicture);


    CreateOutPaintingTaskResponse createPictureOutPaintingTask(
            CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
            User loginUser);

    GetOutPaintingTaskResponse getPictureOutPaintingTask(
            String taskId);

    Picture getById(Long id);

    boolean updateById(Picture picture);

    boolean removeById(long id);

    boolean saveOrUpdate(Picture picture);


    Page<Picture> listPictureByPage(PictureQueryRequest pictureQueryRequest);

    PictureTagCategory listPictureTagCategory();

    List<Long> getAllPictureSizeList(Boolean isQueryPublic);

    List<Long> getSpacePictureSizeList(Long spaceId);

    List<Map<String, Object>> listMaps(QueryWrapper<Picture> queryWrapper);

    Map<String, Long> getTagCountMap(boolean isQueryPublic, Long spaceId);

    List<Picture> getByIds(Set<Long> pictureIds);
}
