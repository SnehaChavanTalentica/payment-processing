package com.payment.processing.domain.entity;

import com.payment.processing.domain.enums.WebhookEventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "webhook_events", indexes = {
    @Index(name = "idx_webhook_event_id", columnList = "event_id", unique = true),
    @Index(name = "idx_webhook_event_type", columnList = "event_type"),
    @Index(name = "idx_webhook_processed", columnList = "processed")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEvent extends BaseEntity {

    @Column(name = "event_id", nullable = false, unique = true, length = 100)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 100)
    private WebhookEventType eventType;

    @Column(name = "event_type_raw", length = 100)
    private String eventTypeRaw;

    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(name = "signature", length = 500)
    private String signature;

    @Column(name = "processed", nullable = false)
    @Builder.Default
    private Boolean processed = false;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "processing_attempts")
    @Builder.Default
    private Integer processingAttempts = 0;

    @Column(name = "processing_error", length = 2000)
    private String processingError;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "subscription_id", length = 100)
    private String subscriptionId;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    public void markProcessed() {
        this.processed = true;
        this.processedAt = Instant.now();
    }

    public void recordFailure(String error) {
        this.processingAttempts++;
        this.processingError = error != null && error.length() > 2000 ? error.substring(0, 2000) : error;
    }
}

