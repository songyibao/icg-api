package com.yb.icgapi.api.imagesearch.model;

import lombok.Data;

@Data
public class ImageSearchResult {

    /**
     * 缩略图地址
     */
    private String thumbUrl;

    /**
     * 来源地址
     */
    private String fromUrl;

    /**
     * 图片来源网站的名称
     */
    private String fromSiteName;
}
