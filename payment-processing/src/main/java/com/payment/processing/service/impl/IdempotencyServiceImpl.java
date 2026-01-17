package com.payment.processing.service.impl;

import com.payment.processing.domain.entity.IdempotencyKey;
import com.payment.processing.repository.IdempotencyKeyRepository;
import com.payment.processing.service.IdempotencyService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Value("${idempotency.ttl-hours:24}")
    private int ttlHours;

    @Override
    @Transactional
    public Optional<IdempotencyKey> findByKey(String key) {
        return idempotencyKeyRepository.findByKey(key);
    }

    @Override
    @Transactional
    public IdempotencyKey startProcessing(String key, String requestPath, String requestMethod, String requestHash, String correlationId) {
        IdempotencyKey idempotencyKey = IdempotencyKey.builder()
                .key(key)
                .requestPath(requestPath)
                .requestMethod(requestMethod)
                .requestHash(requestHash)
                .correlationId(correlationId)
                .processing(true)
                .completed(false)
                .expiresAt(java.time.Instant.now().plus(ttlHours, java.time.temporal.ChronoUnit.HOURS))
                .build();

        return idempotencyKeyRepository.save(idempotencyKey);
    }

    @Override
    @Transactional
    public void completeProcessing(String key, int status, String responseBody, String responseHeaders) {
        idempotencyKeyRepository.findByKey(key).ifPresent(idempotencyKey -> {
            idempotencyKey.complete(status, responseBody, responseHeaders);
            idempotencyKeyRepository.save(idempotencyKey);
        });
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 * * * *") // Run every hour
    public void cleanupExpiredKeys() {
        log.info("Cleaning up expired idempotency keys");
        int deleted = idempotencyKeyRepository.deleteExpiredKeys(Instant.now());
        log.info("Deleted {} expired idempotency keys", deleted);
    }

}
