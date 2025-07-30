package com.yb.icgapi.model.vo;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.yb.icgapi.model.entity.AiPersonCluster;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 【AI】人物簇 (人脸聚类结果)
 * @TableName ai_person_cluster
 */
@Data
public class AiPersonClusterVO implements Serializable {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Long id;


    /**
     * 人物的显示名称 (由用户命名, 如“宝宝”,"妈妈")
     */
    private String displayName;

    /**
     * 该人物簇的封面人脸ID, 关联ai_detected_face.id
     */
    private String coverFacePictureUrl;

    /**
     * 人脸在照片中的区域
     */
    private JSONObject coverFaceArea;


    /**
     * 该人物簇下的人脸数量
     */
    private Integer faceCount;

    public static AiPersonClusterVO objToVo(AiPersonCluster cluster) {
        AiPersonClusterVO vo = new AiPersonClusterVO();
        BeanUtils.copyProperties(cluster, vo);
        return vo;
    }


    @TableField(exist = false)
    private static final long serialVersionUID = 4072881152018633196L;
}