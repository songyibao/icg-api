package com.yb.icgapi.model.dto.ai;

import lombok.Data;

/**
 * AI处理请求消息
 *
 * @author songyibao
 */
@Data
public class AIProcessingMessage {

    /**
     * 图片ID
     */
    private String pictureId;

    /**
     * 图片URL地址
     */
    private String imageUrl;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 是否强制重新处理，默认false
     */
    private Boolean forceReprocess = false;
}
