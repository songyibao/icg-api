package com.yb.icgapi.icpic.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yb.icgapi.icpic.domain.picture.entity.Picture;
import com.yb.icgapi.icpic.domain.user.entity.User;
import com.yb.icgapi.icpic.infrastructure.api.aliYunAi.model.CreateOutPaintingTaskResponse;
import com.yb.icgapi.icpic.infrastructure.api.aliYunAi.model.GetOutPaintingTaskResponse;
import com.yb.icgapi.icpic.infrastructure.api.imagesearch.model.ImageSearchResult;
import com.yb.icgapi.icpic.interfaces.dto.picture.*;
import com.yb.icgapi.icpic.interfaces.vo.picture.PictureVO;
import com.yb.icgapi.model.dto.analyze.SpaceTagAnalyzeRequest;
import com.yb.icgapi.model.vo.PictureTagCategory;
import com.yb.icgapi.model.vo.analyze.SpaceTagAnalyzeResponse;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
* @author songyibao
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-06-10 09:52:50
*/
public interface PictureApplicationService{


    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);
    PictureVO getPictureVO(Picture picture);

    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage);
    Page<PictureVO> listPictureVOByPage(PictureQueryRequest pictureQueryRequest);

    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);
    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    void fillReviewParams(Picture picture, User loginUser);

    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);


    void editPicture(PictureEditRequest pictureEditRequest,
                     User loginUser);

    void deletePicture(long id, User loginUser);

    void clearPictureFile(Picture oldPicture);

    void checkPictureAuth(User user, Picture picture);

    CreateOutPaintingTaskResponse createPictureOutPaintingTask(
            CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
            User loginUser);

    GetOutPaintingTaskResponse getPictureOutPaintingTask(
            String taskId);

    boolean updatePicture(Picture picture,User loginUser);

    Picture getPictureById(Long id);

    Page<Picture> listPictureByPage(PictureQueryRequest pictureQueryRequest);

    PictureVO getPictureVOById(Long id, HttpServletRequest request);

    PictureTagCategory listPIctureTagCategory();

    List<ImageSearchResult> searchPictureByPicture(SearchPictureByPictureRequest searchPictureByPictureRequest);

    List<Long> getPictureSizeList(Boolean isQueryPublic,Long spaceId);

    List<Map<String, Object>> listMaps(QueryWrapper<Picture> queryWrapper);

    Map<String, Long> getTagCountMap(boolean isQueryPublic, Long spaceId);

    List<Picture> getByIds(Set<Long> pictureIds);
}
