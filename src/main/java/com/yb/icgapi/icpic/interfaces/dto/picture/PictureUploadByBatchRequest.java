package com.yb.icgapi.icpic.interfaces.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadByBatchRequest implements Serializable {

    /**
     * 抓取关键词
     */
    private String searchText;

    /**
     * 图片数量
     */
    private Integer count;

    /**
     * 图片名称前缀
     */
    private String namePrefix;

    private static final long serialVersionUID = 1L;
}
