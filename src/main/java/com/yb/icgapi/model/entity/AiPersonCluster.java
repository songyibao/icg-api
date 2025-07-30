package com.yb.icgapi.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 【AI】人物簇 (人脸聚类结果)
 * @TableName ai_person_cluster
 */
@TableName(value ="ai_person_cluster")
@Data
public class AiPersonCluster implements Serializable {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户 id (实现数据隔离)
     */
    private Long userId;

    /**
     * 人物的显示名称 (由用户命名, 如“宝宝”,"妈妈")
     */
    private String displayName;

    /**
     * 该人物簇的封面人脸ID, 关联ai_detected_face.id
     */
    private Long coverFaceId;

    /**
     * 该人物簇下的人脸数量
     */
    private Integer faceCount;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}