package com.payment.processing.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.processing.domain.entity.Transaction;
import com.payment.processing.domain.entity.WebhookEvent;
import com.payment.processing.domain.enums.TransactionStatus;
import com.payment.processing.domain.enums.WebhookEventType;
import com.payment.processing.repository.TransactionRepository;
import com.payment.processing.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookProcessor {

    private final WebhookEventRepository webhookEventRepository;
    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processWebhookEvent(String webhookEventId) {
        log.info("Processing webhook event: {}", webhookEventId);

        WebhookEvent event = webhookEventRepository.findById(java.util.UUID.fromString(webhookEventId))
                .orElseThrow(() -> new IllegalArgumentException("Webhook event not found: " + webhookEventId));

        if (event.getProcessed()) {
            log.info("Webhook event already processed: {}", webhookEventId);
            return;
        }

        try {
            processEvent(event);
            event.markProcessed();
            log.info("Webhook event processed successfully: {}", webhookEventId);
        } catch (Exception e) {
            log.error("Error processing webhook event: {}", webhookEventId, e);
            event.recordFailure(e.getMessage());
        }

        webhookEventRepository.save(event);
    }

    private void processEvent(WebhookEvent event) throws Exception {
        WebhookEventType eventType = event.getEventType();
        JsonNode payload = objectMapper.readTree(event.getPayload());

        switch (eventType) {
            case PAYMENT_CREATED, PAYMENT_AUTHORIZED, PAYMENT_CAPTURED -> processPaymentEvent(event, payload);
            case REFUND_CREATED -> processRefundEvent(event, payload);
            case PAYMENT_VOIDED -> processVoidEvent(event, payload);
            case FRAUD_HELD, FRAUD_APPROVED, FRAUD_DECLINED -> processFraudEvent(event, payload);
            case SUBSCRIPTION_CREATED, SUBSCRIPTION_UPDATED, SUBSCRIPTION_CANCELLED, SUBSCRIPTION_SUSPENDED, SUBSCRIPTION_TERMINATED, SUBSCRIPTION_EXPIRING -> processSubscriptionEvent(event, payload);
            default -> log.warn("Unknown webhook event type: {}", eventType);
        }
    }

    private void processPaymentEvent(WebhookEvent event, JsonNode payload) {
        log.info("Processing payment event: {}", event.getEventType());
        if (!payload.has("payload")) return;

        JsonNode payloadData = payload.get("payload");
        String transactionId = payloadData.has("id") ? payloadData.get("id").asText() : null;

        if (transactionId != null) {
            Optional<Transaction> transactionOpt = transactionRepository.findByGatewayTransactionId(transactionId);
            if (transactionOpt.isPresent()) {
                Transaction transaction = transactionOpt.get();
                if (event.getEventType() == WebhookEventType.PAYMENT_CAPTURED && transaction.getStatus() == TransactionStatus.AUTHORIZED) {
                    transaction.setStatus(TransactionStatus.CAPTURED);
                    transaction.setCapturedAt(Instant.now());
                    transactionRepository.save(transaction);
                    log.info("Transaction status updated to CAPTURED: {}", transaction.getId());
                }
            }
        }
    }

    private void processRefundEvent(WebhookEvent event, JsonNode payload) {
        log.info("Processing refund event: {}", event.getEventId());
    }

    private void processVoidEvent(WebhookEvent event, JsonNode payload) {
        log.info("Processing void event: {}", event.getEventId());
        if (!payload.has("payload")) return;

        JsonNode payloadData = payload.get("payload");
        String transactionId = payloadData.has("id") ? payloadData.get("id").asText() : null;

        if (transactionId != null) {
            Optional<Transaction> transactionOpt = transactionRepository.findByGatewayTransactionId(transactionId);
            if (transactionOpt.isPresent()) {
                Transaction transaction = transactionOpt.get();
                if (transaction.getStatus() != TransactionStatus.VOIDED) {
                    transaction.setStatus(TransactionStatus.VOIDED);
                    transaction.setVoidedAt(Instant.now());
                    transactionRepository.save(transaction);
                    log.info("Transaction status updated to VOIDED via webhook: {}", transaction.getId());
                }
            }
        }
    }

    private void processFraudEvent(WebhookEvent event, JsonNode payload) {
        log.info("Processing fraud event: {} - {}", event.getEventType(), event.getEventId());
        if (!payload.has("payload")) return;

        JsonNode payloadData = payload.get("payload");
        String transactionId = payloadData.has("id") ? payloadData.get("id").asText() : null;

        if (transactionId != null) {
            Optional<Transaction> transactionOpt = transactionRepository.findByGatewayTransactionId(transactionId);
            if (transactionOpt.isPresent()) {
                Transaction transaction = transactionOpt.get();
                switch (event.getEventType()) {
                    case FRAUD_HELD -> transaction.setStatus(TransactionStatus.PENDING_REVIEW);
                    case FRAUD_DECLINED -> transaction.markFailed("FRAUD_DECLINED", "Transaction declined due to fraud detection");
                }
                transactionRepository.save(transaction);
                log.info("Fraud event processed for transaction: {}", transaction.getId());
            }
        }
    }

    private void processSubscriptionEvent(WebhookEvent event, JsonNode payload) {
        log.info("Processing subscription event: {} - {}", event.getEventType(), event.getEventId());
    }
}

