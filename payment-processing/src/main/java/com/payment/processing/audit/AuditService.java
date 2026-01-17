package com.payment.processing.audit;

import com.payment.processing.domain.entity.Subscription;
import com.payment.processing.domain.entity.Transaction;

public interface AuditService {
    void logTransaction(Transaction transaction, String action, String correlationId);
    void logSubscription(Subscription subscription, String action, String correlationId);
    void log(String entityType, String entityId, String action, String oldValue, String newValue, String userId, String correlationId);
    void logSecurityEvent(String action, String userId, String ipAddress, String userAgent, boolean success, String details);
}

