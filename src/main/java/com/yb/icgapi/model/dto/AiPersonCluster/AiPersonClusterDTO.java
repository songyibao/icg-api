package com.yb.icgapi.model.dto.AiPersonCluster;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.yb.icgapi.model.dto.AiDetectedFace.Area;
import com.yb.icgapi.model.entity.AiPersonCluster;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 人物簇及其封面URL的数据传输对象
 * 用于接收Mapper自定义查询的结果
 */
@Data
@EqualsAndHashCode(callSuper = true) // 确保在比较对象时也考虑父类的字段
@TableName(autoResultMap = true)
public class AiPersonClusterDTO extends AiPersonCluster {

    /**
     * 封面人脸所在的图片URL
     * 这个字段名必须和SQL查询中的别名 "coverFacePictureUrl" 完全对应
     */
    private String coverFacePictureUrl;

    /**
     * 人脸在照片中的区域
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private JSONObject coverFaceArea;
}
