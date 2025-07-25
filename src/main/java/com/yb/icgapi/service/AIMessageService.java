package com.yb.icgapi.service;

import com.yb.icgapi.model.dto.ai.AIProcessingMessage;

/**
 * AI处理消息发送服务
 *
 * @author songyibao
 */
public interface AIMessageService {

    /**
     * 发送AI处理消息
     *
     * @param pictureId 图片ID
     * @param imageUrl 图片URL
     * @param userId 用户ID
     * @param forceReprocess 是否强制重新处理
     */
    void sendAIProcessingMessage(String pictureId, String imageUrl, String userId, Boolean forceReprocess);

    /**
     * 发送AI处理消息
     *
     * @param message AI处理消息对象
     */
    void sendAIProcessingMessage(AIProcessingMessage message);
}
