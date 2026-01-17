package com.payment.processing.domain.enums;

public enum TransactionStatus {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    VOIDED,
    REFUNDED,
    PARTIALLY_REFUNDED,
    FAILED,
    DECLINED,
    EXPIRED,
    SETTLED,
    PENDING_REVIEW
}

