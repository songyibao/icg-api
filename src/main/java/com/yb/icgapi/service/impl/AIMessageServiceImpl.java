package com.yb.icgapi.service.impl;

import com.yb.icgapi.model.dto.ai.AIProcessingMessage;
import com.yb.icgapi.service.AIMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * AI处理消息发送服务实现
 *
 * @author songyibao
 */
@Slf4j
@Service
public class AIMessageServiceImpl implements AIMessageService {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Value("${ai.queue.name}")
    private String aiProcessingQueueName;

    @Override
    public void sendAIProcessingMessage(String pictureId, String imageUrl, String userId, Boolean forceReprocess) {
        AIProcessingMessage message = new AIProcessingMessage();
        message.setPictureId(pictureId);
        message.setImageUrl(imageUrl);
        message.setUserId(userId);
        message.setForceReprocess(forceReprocess != null ? forceReprocess : false);

        sendAIProcessingMessage(message);
    }

    @Override
    public void sendAIProcessingMessage(AIProcessingMessage message) {
        try {
            log.info("发送AI处理消息: pictureId={}, imageUrl={}, userId={}, forceReprocess={}",
                    message.getPictureId(), message.getImageUrl(), message.getUserId(), message.getForceReprocess());

            rabbitTemplate.convertAndSend(aiProcessingQueueName, message);

            log.info("AI处理消息发送成功: pictureId={}", message.getPictureId());
        } catch (Exception e) {
            log.error("发送AI处理消息失败: pictureId={}, error={}", message.getPictureId(), e.getMessage(), e);
            // 这里可以根据业务需求决定是否抛出异常或进行其他处理
            // 为了不影响图片上传流程，这里只记录日志，不抛出异常
        }
    }
}
