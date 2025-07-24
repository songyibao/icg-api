package com.yb.icgapi.manager;

import cn.hutool.core.io.FileUtil;
import com.yb.icgapi.constant.PictureConstant;
import com.yb.icgapi.exception.ErrorCode;
import com.yb.icgapi.exception.ThrowUtils;
import com.yb.icgapi.manager.upload.PictureUploadTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Service
public class FilePictureUpload extends PictureUploadTemplate {
    @Override
    protected void validPicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        ThrowUtils.ThrowIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        // 1. 校验文件大小  
        long fileSize = multipartFile.getSize();
        ThrowUtils.ThrowIf(fileSize > PictureConstant.PICTURE_SIZE_MAX, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
        // 2. 校验文件后缀，简单校验，不健壮
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        if(fileSuffix != null){
            fileSuffix = fileSuffix.toLowerCase(); // 转为小写，避免大小写问题
        }
        // 允许上传的文件后缀
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "jpg", "png", "webp");
        ThrowUtils.ThrowIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件类型错误");
    }

    @Override
    protected String getOriginFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        multipartFile.transferTo(file);
    }
}
