package com.yb.icgapi.manager;

import cn.hutool.core.io.FileUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.yb.icgapi.config.CosClientConfig;
import com.yb.icgapi.constant.PictureConstant;
import com.yb.icgapi.exception.BusinessException;
import com.yb.icgapi.exception.ErrorCode;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class CosManager {  
  
    @Resource
    private CosClientConfig cosClientConfig;
  
    @Resource  
    private COSClient cosClient;

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        return cosClient.putObject(putObjectRequest);
    }
    /**
     * 上传图片对象（附带图片信息）
     *
     * @param key bucket内的唯一文件路径，且带有真实的图片格式后缀
     * @param file 文件
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        // 对图片进行处理（获取基本信息也被视作为一种处理）
        PicOperations picOperations = new PicOperations();
        // 1 表示返回原图信息
        picOperations.setIsPicInfo(1);
        // 图片处理规则
        List<PicOperations.Rule> rules = new ArrayList<>();
        // 图片压缩（转成 webp 格式）
        // 禁用，最好保留用户上传的原图
//        String webpKey = FileUtil.mainName(key) + ".webp";
//        PicOperations.Rule compressRule = new PicOperations.Rule();
//        compressRule.setRule("imageMogr2/format/webp");
//        compressRule.setBucket(cosClientConfig.getBucket());
//        compressRule.setFileId(webpKey);
//        rules.add(compressRule);

        // 缩略图处理
        String thumbnailKey = FileUtil.mainName(key) + "_thumbnail."+FileUtil.getSuffix(key);
        PicOperations.Rule thumbnailRule = new PicOperations.Rule();
        // thumbnail/<width>x<height>>
        thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", PictureConstant.THUMBNAIL_MAX_WIDTH, PictureConstant.THUMBNAIL_MAX_HEIGHT));
        thumbnailRule.setBucket(cosClientConfig.getBucket());
        thumbnailRule.setFileId(thumbnailKey);
        rules.add(thumbnailRule);

        // 构造处理参数
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

    public void deleteObject(String key){
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }


}
