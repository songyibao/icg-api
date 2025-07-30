package com.yb.icgapi.model.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.yb.icgapi.model.dto.AiDetectedFace.Area;
import com.yb.icgapi.model.entity.AiDetectedFace;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;

/**
 * 【AI】图片中检测到的人脸信息
 * @TableName ai_detected_face
 */
@TableName(value ="ai_detected_face",autoResultMap = true)
@Data
public class AiDetectedFaceVO implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的图片 url
     */
    private String pictureUrl;

    /**
     * 图片缩略图
     */
    private String pictureThumbnailUrl;

    /**
     * 【关键】关联到的人物簇 id (ai_person_cluster.id)
     */
    private Long clusterId;



    public static AiDetectedFaceVO objToVo(AiDetectedFace face) {
        AiDetectedFaceVO vo = new AiDetectedFaceVO();
        BeanUtils.copyProperties(face, vo);
        return vo;
    }

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}