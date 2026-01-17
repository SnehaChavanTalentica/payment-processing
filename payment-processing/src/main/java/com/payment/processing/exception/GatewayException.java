package com.payment.processing.exception;

import lombok.Getter;

@Getter
public class GatewayException extends RuntimeException {
    private final String errorCode;
    private final String gatewayCode;
    private final String gatewayMessage;

    public GatewayException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.gatewayCode = errorCode;
        this.gatewayMessage = message;
    }

    public GatewayException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.gatewayCode = errorCode;
        this.gatewayMessage = message;
    }
}

