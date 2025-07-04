package com.yb.icgapi.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.yb.icgapi.config.CosClientConfig;
import com.yb.icgapi.exception.BusinessException;
import com.yb.icgapi.exception.ErrorCode;
import com.yb.icgapi.manager.CosManager;
import com.yb.icgapi.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;

@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    protected CosManager cosManager;

    @Resource
    protected CosClientConfig cosClientConfig;

    /**
     * 模板方法，定义上传流程
     */
    public final UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        // 1. 校验图片  
        validPicture(inputSource);

        // 2. 图片上传地址  
        String uuid = RandomUtil.randomString(16);
        String originFilename = getOriginFilename(inputSource);
        // 下面还需要拼接图片格式
        String uploadFilename = String.format("%s_%s.", DateUtil.formatDate(new Date()), uuid);
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);

        File file = null;
        try {
            // 3. 创建临时文件  
            file = File.createTempFile(uploadPath, null);
            // 处理文件来源（本地或 URL）  
            processFile(inputSource, file);
            // 获取图片格式
            String imageType = getImageType(file);
            // 拼接图片格式到上传路径
            uploadPath += imageType;
            // 4. 上传图片到对象存储  
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            List<CIObject> objectList = putObjectResult.getCiUploadResult().getProcessResults().getObjectList();
            if (CollUtil.isNotEmpty(objectList)) {
                CIObject compressedCiObject = objectList.get(0);
                // 缩略图默认等于压缩图
                CIObject thumbnailCiObject = compressedCiObject;
                // 有生成缩略图，才得到缩略图
                if (objectList.size() > 1) {
                    thumbnailCiObject = objectList.get(1);
                }
                // 封装压缩图返回结果
                return buildResult(originFilename, compressedCiObject, thumbnailCiObject);
            } else {
                return buildResult(originFilename, file, uploadPath, imageInfo);
            }
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SERVER_ERROR, "上传失败");
        } finally {
            // 6. 清理临时文件  
            deleteTempFile(file);
        }
    }

    private UploadPictureResult buildResult(String originFilename, CIObject compressedObject, CIObject thumbnailObject) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = compressedObject.getWidth();
        int picHeight = compressedObject.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(compressedObject.getFormat());
        uploadPictureResult.setPicSize(compressedObject.getSize().longValue());
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressedObject.getKey());
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailObject.getKey());
        return uploadPictureResult;
    }

    /**
     * 校验输入源（本地文件或 URL），返回图片格式（如果是）
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 获取输入源的原始文件名
     */
    protected abstract String getOriginFilename(Object inputSource);

    /**
     * 处理输入源并生成本地临时文件
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;

    private String getImageType(File file) throws IOException {
        try (InputStream is = Files.newInputStream(file.toPath())) {
            String mimeType = new Tika().detect(is); // 返回如 "image/png"
            return mimeType.split("/")[1]; // 提取 "png"
        }
    }

    /**
     * 封装返回结果
     */
    private UploadPictureResult buildResult(String originFilename, File file, String uploadPath, ImageInfo imageInfo) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setUrl(cosClientConfig.getHost() + uploadPath);
        return uploadPictureResult;
    }

    /**
     * 删除临时文件
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }
}
