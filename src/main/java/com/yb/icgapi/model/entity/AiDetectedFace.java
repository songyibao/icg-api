package com.yb.icgapi.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;

/**
 * 【AI】图片中检测到的人脸信息
 * @TableName ai_detected_face
 */
@TableName(value ="ai_detected_face")
@Data
public class AiDetectedFace implements Serializable {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的图片 id
     */
    private Long pictureId;

    /**
     * 【关键】关联到的人物簇 id (ai_person_cluster.id)
     */
    private Long clusterId;

    /**
     * 人脸区域坐标 (x, y, w, h, left_eye, right_eye)
     */
    private Object area;

    /**
     * 人脸检测的置信度
     */
    private Double confidence;

    /**
     * 人脸特征向量 (JSON数组格式, 备份用)
     */
    private String embedding;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}