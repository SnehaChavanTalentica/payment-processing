package com.payment.processing.exception;

import lombok.Getter;

@Getter
public class InvalidTransactionStateException extends RuntimeException {
    private final String errorCode = "INVALID_TRANSACTION_STATE";

    public InvalidTransactionStateException(String message) {
        super(message);
    }
}

