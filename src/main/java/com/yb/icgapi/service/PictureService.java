package com.yb.icgapi.service;

import com.yb.icgapi.model.dto.picture.PictureUploadRequest;
import com.yb.icgapi.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yb.icgapi.model.entity.User;
import com.yb.icgapi.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

/**
* @author songyibao
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-06-10 09:52:50
*/
public interface PictureService extends IService<Picture> {


    PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser);
}
