package com.yb.icgapi.service.impl;

import com.yb.icgapi.model.dto.ai.AIProcessingMessage;
import com.yb.icgapi.model.dto.ai.BaseAIMessage;
import com.yb.icgapi.model.dto.ai.BatchReprocessMessage;
import com.yb.icgapi.model.dto.ai.SingleProcessMessage;
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
    @Deprecated
    public void sendAIProcessingMessage(String pictureId, String imageUrl, String userId, Boolean forceReprocess) {
        AIProcessingMessage message = new AIProcessingMessage();
        message.setPictureId(pictureId);
        message.setImageUrl(imageUrl);
        message.setUserId(userId);
        message.setForceReprocess(forceReprocess != null ? forceReprocess : false);

        sendAIProcessingMessage(message);
    }

    @Override
    @Deprecated
    public void sendAIProcessingMessage(AIProcessingMessage message) {
        try {
            log.info("发送AI处理消息(旧版本): pictureId={}, imageUrl={}, userId={}, forceReprocess={}",
                    message.getPictureId(), message.getImageUrl(), message.getUserId(), message.getForceReprocess());

            rabbitTemplate.convertAndSend(aiProcessingQueueName, message);

            log.info("AI处理消息发送成功(旧版本): pictureId={}", message.getPictureId());
        } catch (Exception e) {
            log.error("发送AI处理消息失败(旧版本): pictureId={}, error={}", message.getPictureId(), e.getMessage(), e);
        }
    }

    @Override
    public void sendSingleProcessMessage(Long pictureId, Boolean forceReprocess) {
        SingleProcessMessage message = new SingleProcessMessage();
        SingleProcessMessage.SingleProcessData data = new SingleProcessMessage.SingleProcessData();
        data.setPictureId(pictureId);
        data.setForceReprocess(forceReprocess != null ? forceReprocess : false);
        message.setData(data);

        sendSingleProcessMessage(message);
    }

    @Override
    public void sendSingleProcessMessage(SingleProcessMessage message) {
        try {
            log.info("发送单图处理消息: type={}, pictureId={}, forceReprocess={}",
                    message.getType(),
                    message.getData().getPictureId(),
                    message.getData().getForceReprocess());

            sendAIMessage(message);

            log.info("单图处理消息发送成功: pictureId={}", message.getData().getPictureId());
        } catch (Exception e) {
            log.error("发送单图处理消息失败: pictureId={}, error={}",
                    message.getData().getPictureId(), e.getMessage(), e);
        }
    }

    @Override
    public void sendBatchReprocessMessage(Long userId, Long spaceId, BatchReprocessMessage.ProcessingOptions options) {
        BatchReprocessMessage message = new BatchReprocessMessage();
        BatchReprocessMessage.BatchReprocessData data = new BatchReprocessMessage.BatchReprocessData();
        data.setUserId(userId);
        data.setSpaceId(spaceId);
        data.setOptions(options != null ? options : new BatchReprocessMessage.ProcessingOptions());
        message.setData(data);

        sendBatchReprocessMessage(message);
    }

    @Override
    public void sendBatchReprocessMessage(BatchReprocessMessage message) {
        try {
            log.info("发送批量重新处理消息: type={}, userId={}, spaceId={}, options={}",
                    message.getType(),
                    message.getData().getUserId(),
                    message.getData().getSpaceId(),
                    message.getData().getOptions());

            sendAIMessage(message);

            log.info("批量重新处理消息发送成功: userId={}, spaceId={}",
                    message.getData().getUserId(), message.getData().getSpaceId());
        } catch (Exception e) {
            log.error("发送批量重新处理消息失败: userId={}, spaceId={}, error={}",
                    message.getData().getUserId(), message.getData().getSpaceId(), e.getMessage(), e);
        }
    }

    @Override
    public void sendAIMessage(BaseAIMessage message) {
        try {
            log.info("发送AI消息: type={}", message.getType());

            rabbitTemplate.convertAndSend(aiProcessingQueueName, message);

            log.info("AI消息发送成功: type={}", message.getType());
        } catch (Exception e) {
            log.error("发送AI消息失败: type={}, error={}", message.getType(), e.getMessage(), e);
            // 这里可以根据业务需求决定是否抛出异常或进行其他处理
            // 为了不影响业务流程，这里只记录日志，不抛出异常
        }
    }
}
