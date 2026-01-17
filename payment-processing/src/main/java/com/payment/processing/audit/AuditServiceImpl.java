package com.payment.processing.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.processing.domain.entity.AuditLog;
import com.payment.processing.domain.entity.Subscription;
import com.payment.processing.domain.entity.Transaction;
import com.payment.processing.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Async("asyncExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logTransaction(Transaction transaction, String action, String correlationId) {
        try {
            String entityValue = objectMapper.writeValueAsString(transaction);

            AuditLog auditLog = AuditLog.builder()
                    .entityType("TRANSACTION")
                    .entityId(transaction.getId() != null ? transaction.getId().toString() : null)
                    .action(action)
                    .newValue(entityValue)
                    .correlationId(correlationId)
                    .success(transaction.getStatus() != null && !transaction.getStatus().name().contains("FAILED"))
                    .errorMessage(transaction.getErrorMessage())
                    .timestamp(Instant.now())
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Transaction audit logged: {} - {}", action, transaction.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize transaction for audit: {}", transaction.getId(), e);
        }
    }

    @Override
    @Async("asyncExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSubscription(Subscription subscription, String action, String correlationId) {
        try {
            String entityValue = objectMapper.writeValueAsString(subscription);

            AuditLog auditLog = AuditLog.builder()
                    .entityType("SUBSCRIPTION")
                    .entityId(subscription.getId() != null ? subscription.getId().toString() : null)
                    .action(action)
                    .newValue(entityValue)
                    .correlationId(correlationId)
                    .success(subscription.getStatus() != null && !subscription.getStatus().name().contains("FAILED"))
                    .timestamp(Instant.now())
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Subscription audit logged: {} - {}", action, subscription.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize subscription for audit: {}", subscription.getId(), e);
        }
    }

    @Override
    @Async("asyncExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String entityType, String entityId, String action, String oldValue, String newValue, String userId, String correlationId) {
        AuditLog auditLog = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .oldValue(oldValue)
                .newValue(newValue)
                .userId(userId)
                .correlationId(correlationId)
                .success(true)
                .timestamp(Instant.now())
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Audit logged: {} - {} - {}", entityType, action, entityId);
    }

    @Override
    @Async("asyncExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSecurityEvent(String action, String userId, String ipAddress, String userAgent, boolean success, String details) {
        AuditLog auditLog = AuditLog.builder()
                .entityType("SECURITY")
                .action(action)
                .userId(userId)
                .userIp(ipAddress)
                .userAgent(userAgent)
                .success(success)
                .additionalData(details)
                .timestamp(Instant.now())
                .build();

        auditLogRepository.save(auditLog);
        log.info("Security event logged: {} - {} - success: {}", action, userId, success);
    }
}

