package com.yb.icgapi.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yb.icgapi.common.DeleteRequest;
import com.yb.icgapi.model.dto.picture.*;
import com.yb.icgapi.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yb.icgapi.model.entity.User;
import com.yb.icgapi.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author songyibao
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-06-10 09:52:50
*/
public interface PictureService extends IService<Picture> {


    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);
    public PictureVO getPictureVO(Picture picture);

    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage);
    public Page<PictureVO> listPictureVOByPage(PictureQueryRequest pictureQueryRequest);
    public void validPicture(Picture picture);
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
}
