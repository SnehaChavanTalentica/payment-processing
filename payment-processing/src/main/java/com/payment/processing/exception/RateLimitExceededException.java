package com.payment.processing.exception;

import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {
    private final String errorCode = "RATE_LIMIT_EXCEEDED";

    public RateLimitExceededException(String message) {
        super(message);
    }
}

