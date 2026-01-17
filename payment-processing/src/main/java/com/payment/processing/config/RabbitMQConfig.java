package com.payment.processing.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${queue.webhook.name:payment.webhook.events}")
    private String webhookQueueName;

    @Value("${queue.webhook.dlq-name:payment.webhook.events.dlq}")
    private String webhookDlqName;

    @Value("${queue.webhook.exchange:payment.exchange}")
    private String exchangeName;

    @Bean
    public Queue webhookQueue() {
        return QueueBuilder.durable(webhookQueueName)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", webhookDlqName)
                .build();
    }

    @Bean
    public Queue webhookDlq() {
        return QueueBuilder.durable(webhookDlqName).build();
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(exchangeName);
    }

    @Bean
    public Binding webhookBinding(Queue webhookQueue, DirectExchange exchange) {
        return BindingBuilder.bind(webhookQueue).to(exchange).with("webhook.event");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}

