package com.payment.processing.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "idempotency_keys", indexes = {
    @Index(name = "idx_idempotency_key", columnList = "idempotency_key", unique = true),
    @Index(name = "idx_idempotency_expires_at", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKey extends BaseEntity {

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String key;

    @Column(name = "request_path", length = 500)
    private String requestPath;

    @Column(name = "request_method", length = 10)
    private String requestMethod;

    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "processing", nullable = false)
    @Builder.Default
    private Boolean processing = false;

    @Column(name = "completed", nullable = false)
    @Builder.Default
    private Boolean completed = false;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    public void complete(int status, String body, String headers) {
        this.completed = true;
        this.processing = false;
        this.completedAt = Instant.now();
        this.responseStatus = status;
        this.responseBody = body;
    }
}

