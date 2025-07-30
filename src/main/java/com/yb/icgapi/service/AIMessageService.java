package com.yb.icgapi.service;

import com.yb.icgapi.model.dto.ai.AIProcessingMessage;
import com.yb.icgapi.model.dto.ai.BaseAIMessage;
import com.yb.icgapi.model.dto.ai.BatchReprocessMessage;
import com.yb.icgapi.model.dto.ai.SingleProcessMessage;

/**
 * AI处理消息发送服务
 *
 * @author songyibao
 */
public interface AIMessageService {

    /**
     * 发送AI处理消息（旧版本，保持向下兼容）
     *
     * @param pictureId 图片ID
     * @param imageUrl 图片URL
     * @param userId 用户ID
     * @param forceReprocess 是否强制重新处理
     */
    @Deprecated
    void sendAIProcessingMessage(String pictureId, String imageUrl, String userId, Boolean forceReprocess);

    /**
     * 发送AI处理消息（旧版本，保持向下兼容）
     *
     * @param message AI处理消息对象
     */
    @Deprecated
    void sendAIProcessingMessage(AIProcessingMessage message);
    
    /**
     * 发送单图处理消息
     *
     * @param pictureId 图片ID
     * @param forceReprocess 是否强制重新处理
     */
    void sendSingleProcessMessage(Long pictureId, Boolean forceReprocess);
    
    /**
     * 发送单图处理消息
     *
     * @param message 单图处理消息对象
     */
    void sendSingleProcessMessage(SingleProcessMessage message);
    
    /**
     * 发送批量重新处理消息
     *
     * @param userId 用户ID
     * @param spaceId 空间ID（可选）
     * @param options 处理选项（可选）
     */
    void sendBatchReprocessMessage(Long userId, Long spaceId, BatchReprocessMessage.ProcessingOptions options);
    
    /**
     * 发送批量重新处理消息
     *
     * @param message 批量重新处理消息对象
     */
    void sendBatchReprocessMessage(BatchReprocessMessage message);
    
    /**
     * 发送基础AI消息
     *
     * @param message 基础AI消息对象
     */
    void sendAIMessage(BaseAIMessage message);
}
