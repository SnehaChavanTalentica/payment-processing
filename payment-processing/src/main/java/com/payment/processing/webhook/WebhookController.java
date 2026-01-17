package com.payment.processing.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.processing.domain.entity.WebhookEvent;
import com.payment.processing.domain.enums.WebhookEventType;
import com.payment.processing.gateway.PaymentGateway;
import com.payment.processing.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WebhookEventRepository webhookEventRepository;
    private final PaymentGateway paymentGateway;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @PostMapping("/authorize-net")
    public ResponseEntity<String> handleAuthorizeNetWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-ANET-Signature", required = false) String signature) {

        log.info("Received webhook from Authorize.Net");

        try {
            if (!paymentGateway.validateWebhookSignature(payload, signature)) {
                log.warn("Invalid webhook signature");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }

            JsonNode jsonNode = objectMapper.readTree(payload);
            String eventId = jsonNode.has("notificationId") ? jsonNode.get("notificationId").asText() : UUID.randomUUID().toString();
            String eventTypeRaw = jsonNode.has("eventType") ? jsonNode.get("eventType").asText() : "UNKNOWN";

            if (webhookEventRepository.existsByEventId(eventId)) {
                log.info("Duplicate webhook event received: {}", eventId);
                return ResponseEntity.ok("Already processed");
            }

            WebhookEventType eventType = parseEventType(eventTypeRaw);

            WebhookEvent webhookEvent = WebhookEvent.builder()
                    .eventId(eventId)
                    .eventType(eventType)
                    .eventTypeRaw(eventTypeRaw)
                    .payload(payload)
                    .signature(signature)
                    .processed(false)
                    .correlationId(UUID.randomUUID().toString())
                    .build();

            if (jsonNode.has("payload")) {
                JsonNode payloadNode = jsonNode.get("payload");
                if (payloadNode.has("id")) webhookEvent.setTransactionId(payloadNode.get("id").asText());
                if (payloadNode.has("subscriptionId")) webhookEvent.setSubscriptionId(payloadNode.get("subscriptionId").asText());
            }

            webhookEventRepository.save(webhookEvent);
            rabbitTemplate.convertAndSend("payment.exchange", "webhook.event", webhookEvent.getId().toString());

            log.info("Webhook event queued for processing: {}", eventId);
            return ResponseEntity.ok("Accepted");

        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Processing error");
        }
    }

    private WebhookEventType parseEventType(String eventTypeRaw) {
        if (eventTypeRaw == null) return WebhookEventType.UNKNOWN;
        return switch (eventTypeRaw.toUpperCase()) {
            case "NET.AUTHORIZE.PAYMENT.AUTHCAPTURE.CREATED" -> WebhookEventType.PAYMENT_CREATED;
            case "NET.AUTHORIZE.PAYMENT.AUTHORIZATION.CREATED" -> WebhookEventType.PAYMENT_AUTHORIZED;
            case "NET.AUTHORIZE.PAYMENT.CAPTURE.CREATED" -> WebhookEventType.PAYMENT_CAPTURED;
            case "NET.AUTHORIZE.PAYMENT.REFUND.CREATED" -> WebhookEventType.REFUND_CREATED;
            case "NET.AUTHORIZE.PAYMENT.VOID.CREATED" -> WebhookEventType.PAYMENT_VOIDED;
            case "NET.AUTHORIZE.PAYMENT.FRAUD.HELD" -> WebhookEventType.FRAUD_HELD;
            case "NET.AUTHORIZE.PAYMENT.FRAUD.APPROVED" -> WebhookEventType.FRAUD_APPROVED;
            case "NET.AUTHORIZE.PAYMENT.FRAUD.DECLINED" -> WebhookEventType.FRAUD_DECLINED;
            case "NET.AUTHORIZE.CUSTOMER.SUBSCRIPTION.CREATED" -> WebhookEventType.SUBSCRIPTION_CREATED;
            case "NET.AUTHORIZE.CUSTOMER.SUBSCRIPTION.UPDATED" -> WebhookEventType.SUBSCRIPTION_UPDATED;
            case "NET.AUTHORIZE.CUSTOMER.SUBSCRIPTION.CANCELLED" -> WebhookEventType.SUBSCRIPTION_CANCELLED;
            case "NET.AUTHORIZE.CUSTOMER.SUBSCRIPTION.SUSPENDED" -> WebhookEventType.SUBSCRIPTION_SUSPENDED;
            case "NET.AUTHORIZE.CUSTOMER.SUBSCRIPTION.TERMINATED" -> WebhookEventType.SUBSCRIPTION_TERMINATED;
            case "NET.AUTHORIZE.CUSTOMER.SUBSCRIPTION.EXPIRING" -> WebhookEventType.SUBSCRIPTION_EXPIRING;
            default -> WebhookEventType.UNKNOWN;
        };
    }
}

