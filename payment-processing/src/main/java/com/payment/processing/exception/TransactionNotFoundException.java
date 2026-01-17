package com.payment.processing.exception;

import lombok.Getter;

@Getter
public class TransactionNotFoundException extends RuntimeException {
    private final String errorCode = "TRANSACTION_NOT_FOUND";

    public TransactionNotFoundException(String transactionId) {
        super("Transaction not found: " + transactionId);
    }
}

