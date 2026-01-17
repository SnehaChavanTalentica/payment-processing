package com.payment.processing.exception;

import lombok.Getter;

@Getter
public class DuplicateRequestException extends RuntimeException {
    private final String errorCode = "DUPLICATE_REQUEST";
    private final String idempotencyKey;

    public DuplicateRequestException(String idempotencyKey) {
        super("Duplicate request detected for idempotency key: " + idempotencyKey);
        this.idempotencyKey = idempotencyKey;
    }
}

