package com.yb.icgapi.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.yb.icgapi.config.CosClientConfig;
import com.yb.icgapi.exception.BusinessException;
import com.yb.icgapi.exception.ErrorCode;
import com.yb.icgapi.exception.ThrowUtils;
import com.yb.icgapi.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class FileManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;


    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix){
        // 校验图片
        validPicture(multipartFile);
        // 图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()),uuid,FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("/%s/%s",uploadPathPrefix, uploadFilename);
        File file = null;
        try{
            // 创建临时文件
            file = File.createTempFile(uploadPath,null);
            multipartFile.transferTo(file);
            // 上传图片
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth*1.0/picHeight,2).doubleValue();
            uploadPictureResult.setUrl(cosClientConfig.getHost()+uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            return uploadPictureResult;
        } catch (IOException e) {
            log.error("上传图片失败，文件名：{}，错误信息：{}", multipartFile.getOriginalFilename(), e.getMessage());
            throw new BusinessException(ErrorCode.SERVER_ERROR, "上传图片失败");
        }finally {
            this.deleteTempFile(file);
        }
    }

    public void validPicture(MultipartFile multipartFile){
        ThrowUtils.ThrowIf(multipartFile == null || multipartFile.isEmpty(), ErrorCode.PARAM_BLANK,"上传的图片不能为空");
        // 1.校验文件大小
        long fileSize = multipartFile.getSize();
        final long ONE_M = 1024 * 1024L;
        ThrowUtils.ThrowIf(fileSize > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "上传的图片不能超过2M");
        // 2. 校验文件类型
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        final List<String> allowedSuffixes = Arrays.asList("jpg", "jpeg", "png", "webp");
        ThrowUtils.ThrowIf(!allowedSuffixes.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "上传的图片格式不支持，仅支持jpg、jpeg、png、webp格式");
    }

    public void deleteTempFile(File file){
        if(file == null){
            return;
        }
        boolean deleteResult = file.delete();
        if(!deleteResult){
            log.error("临时文件删除失败，文件路径：{}", file.getAbsolutePath());
        }
    }


}


