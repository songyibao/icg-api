package com.yb.icgapi.icpic.domain.picture.entity;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.*;
import com.yb.icgapi.icpic.domain.picture.constant.PictureConstant;
import com.yb.icgapi.icpic.infrastructure.exception.ErrorCode;
import com.yb.icgapi.icpic.infrastructure.exception.ThrowUtils;
import com.yb.icgapi.icpic.interfaces.vo.picture.PictureVO;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 图片
 * @TableName picture
 */
@TableName(value ="picture")
@Data
public class Picture implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 图片 url
     */
    private String url;

    /**
     * 图片缩略图 url
     */
    private String thumbnailUrl;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签（JSON 数组）
     */
    private String tags;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 图片主色调
     */
    private String picColor;


    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 空间 id
     */
    // 防止shardingsphere因为更新分片键而报错
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Long spaceId;

    /**
     * 状态：0-待审核; 1-通过; 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 审核人 id
     */
    private Long reviewerId;

    /**
     * 审核时间
     */
    private Date reviewTime;


    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    public static void validPicture(Picture picture) {
        ThrowUtils.ThrowIf(picture == null, ErrorCode.PARAM_BLANK);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String name = picture.getName();
        String introduction = picture.getIntroduction();
        String category = picture.getCategory();
        String tags = picture.getTags();
        Long picSize = picture.getPicSize();
        Integer picWidth = picture.getPicWidth();
        Integer picHeight = picture.getPicHeight();
        Double picScale = picture.getPicScale();
        String picFormat = picture.getPicFormat();
        Long userId = picture.getUserId();
        Date createTime = picture.getCreateTime();
        Date editTime = picture.getEditTime();
        Date updateTime = picture.getUpdateTime();

        ThrowUtils.ThrowIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.ThrowIf(url.length() > PictureConstant.PICTURE_URL_MAX_LENGTH,
                    ErrorCode.PARAMS_ERROR, "图片url过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.ThrowIf(introduction.length() > PictureConstant.PICTURE_INTRODUCTION_MAX_LENGTH,
                    ErrorCode.PARAMS_ERROR, "图片简介过长");
        }
    }

    /**
     * 封装类转对象
     */
    public static Picture fromVO(PictureVO pictureVO) {
        if (pictureVO == null) {
            return null;
        }
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureVO, picture);
        // 类型不同，需要转换
        picture.setTags(JSONUtil.toJsonStr(pictureVO.getTags()));
        return picture;
    }

    /**
     * 对象转封装类
     */
    public static PictureVO toVO(Picture picture) {
        if (picture == null) {
            return null;
        }
        PictureVO pictureVO = new PictureVO();
        BeanUtils.copyProperties(picture, pictureVO);
        // 类型不同，需要转换
        pictureVO.setTags(JSONUtil.toList(picture.getTags(), String.class));
        return pictureVO;
    }
}