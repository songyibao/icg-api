package com.yb.icgapi.manager;

import com.yb.icgapi.model.dto.ai.AIProcessingMessage;
import com.yb.icgapi.service.AIMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * AI处理管理器
 * 用于统一管理AI处理相关的业务逻辑
 * 
 * @author songyibao
 */
@Slf4j
@Component
public class AIProcessingManager {
    
    @Resource
    private AIMessageService aiMessageService;
    
    /**
     * 发送图片AI处理消息
     * 
     * @param pictureId 图片ID
     * @param imageUrl 图片URL
     * @param userId 用户ID
     * @param isUpdate 是否为更新操作
     */
    public void sendPictureProcessingMessage(String pictureId, String imageUrl, String userId, boolean isUpdate) {
        try {
            // 根据是否为更新操作决定是否强制重新处理
            boolean forceReprocess = isUpdate;
            
            log.info("准备发送AI处理消息 - 图片ID: {}, 用户ID: {}, 是否更新: {}, 强制重处理: {}", 
                    pictureId, userId, isUpdate, forceReprocess);
            
            aiMessageService.sendAIProcessingMessage(pictureId, imageUrl, userId, forceReprocess);
            
            log.info("AI处理消息发送成功 - 图片ID: {}", pictureId);
            
        } catch (Exception e) {
            log.error("AI处理消息发送失败 - 图片ID: {}, 错误: {}", pictureId, e.getMessage(), e);
            // 这里不抛出异常，避免影响图片上传的主流程
        }
    }
    
    /**
     * 发送批量图片AI处理消息
     * 
     * @param pictures 图片信息列表
     * @param userId 用户ID
     */
    public void sendBatchProcessingMessages(java.util.List<AIProcessingMessage> pictures, String userId) {
        if (pictures == null || pictures.isEmpty()) {
            return;
        }
        
        for (AIProcessingMessage message : pictures) {
            sendPictureProcessingMessage(
                message.getPictureId(), 
                message.getImageUrl(), 
                userId, 
                false // 批量处理默认不强制重新处理
            );
        }
    }
}
