package com.payment.processing.queue;

import com.payment.processing.webhook.WebhookProcessor;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebhookEventConsumer {

    private final WebhookProcessor webhookProcessor;

    @RabbitListener(queues = "${queue.webhook.name:payment.webhook.events}")
    public void processWebhookEvent(String webhookEventId, Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("Received webhook event from queue: {}", webhookEventId);

        try {
            webhookProcessor.processWebhookEvent(webhookEventId);
            channel.basicAck(deliveryTag, false);
            log.info("Webhook event processed and acknowledged: {}", webhookEventId);
        } catch (Exception e) {
            log.error("Error processing webhook event: {}", webhookEventId, e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ex) {
                log.error("Error rejecting message", ex);
            }
        }
    }
}

