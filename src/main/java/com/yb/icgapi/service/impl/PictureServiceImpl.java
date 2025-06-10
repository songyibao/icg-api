package com.yb.icgapi.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yb.icgapi.exception.ErrorCode;
import com.yb.icgapi.exception.ThrowUtils;
import com.yb.icgapi.manager.FileManager;
import com.yb.icgapi.model.dto.file.UploadPictureResult;
import com.yb.icgapi.model.dto.picture.PictureUploadRequest;
import com.yb.icgapi.model.entity.Picture;
import com.yb.icgapi.model.entity.User;
import com.yb.icgapi.model.vo.PictureVO;
import com.yb.icgapi.service.PictureService;
import com.yb.icgapi.mapper.PictureMapper;
import net.bytebuddy.implementation.bytecode.Throw;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

/**
* @author songyibao
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-06-10 09:52:50
*/
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService {

    private final FileManager fileManager;

    public PictureServiceImpl(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.ThrowIf(loginUser == null, ErrorCode.NO_AUTHORIZED);
        // 判断是新增还是更新图片
        Long pictureId = null;
        if(pictureUploadRequest !=null){
            pictureId = pictureUploadRequest.getId();
        }
        // 如果更新图片，需要校验图片是否存在
        if(pictureId!=null){
            boolean exists = this.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .exists();
            ThrowUtils.ThrowIf(!exists, ErrorCode.NOT_FOUND, "图片不存在");
        }
        // 上传图片，得到信息
        // 按照用户id划分目录
        String uploadPathPrefix = String.format("public/%s",loginUser.getId());
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        if(pictureId != null) {
            // 更新图片
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        boolean res = this.saveOrUpdate(picture);
        ThrowUtils.ThrowIf(!res,ErrorCode.SERVER_ERROR, "图片上传失败");
        return PictureVO.objToVo(picture);
    }
}




