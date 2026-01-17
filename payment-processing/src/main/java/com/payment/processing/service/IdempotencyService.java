package com.payment.processing.service;

import com.payment.processing.domain.entity.IdempotencyKey;

import java.util.Optional;

public interface IdempotencyService {
    Optional<IdempotencyKey> findByKey(String key);
    IdempotencyKey startProcessing(String key, String requestPath, String requestMethod, String requestHash, String correlationId);
    void completeProcessing(String key, int status, String responseBody, String responseHeaders);
    void cleanupExpiredKeys();
}

