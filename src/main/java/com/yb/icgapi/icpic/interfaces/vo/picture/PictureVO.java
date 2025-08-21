package com.yb.icgapi.icpic.interfaces.vo.picture;

import cn.hutool.json.JSONUtil;
import com.yb.icgapi.icpic.interfaces.vo.user.UserVO;
import com.yb.icgapi.icpic.domain.picture.entity.Picture;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class PictureVO implements Serializable {
  
    /**  
     * id  
     */  
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
     * 标签  
     */  
    private List<String> tags;
  
    /**  
     * 分类  
     */  
    private String category;  
  
    /**  
     * 文件体积  
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
     * 图片比例  
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
     * 用户 id  
     */  
    private Long userId;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 权限列表
     */
    private List<String> permissionList = new ArrayList<>();


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
     * 创建用户信息  
     */  
    private UserVO user;
  
    private static final long serialVersionUID = 1L;
}
