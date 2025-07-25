package com.yb.icgapi.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 *
 * @author songyibao
 */
@Configuration
public class RabbitMQConfig {

    @Value("${ai.queue.name}")
    private String aiProcessingQueueName;

    /**
     * 配置AI处理队列
     */
    @Bean
    public Queue aiProcessingQueue() {
        return QueueBuilder.durable(aiProcessingQueueName).build();
    }

    /**
     * 配置消息转换器，使用JSON格式
     */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 配置RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());

        // 配置发送确认回调
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                // 消息发送成功
                System.out.println("消息发送成功");
            } else {
                // 消息发送失败
                System.err.println("消息发送失败，原因：" + cause);
            }
        });

        // 配置返回回调（当消息无法路由到队列时触发）
        rabbitTemplate.setReturnsCallback(returned -> {
            System.err.println("消息返回：" + returned.getMessage() +
                             "，回复代码：" + returned.getReplyCode() +
                             "，回复文本：" + returned.getReplyText());
        });

        return rabbitTemplate;
    }

    /**
     * 配置监听器容器工厂
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        return factory;
    }
}
