package com.payment.processing.exception;

import lombok.Getter;

@Getter
public class SubscriptionNotFoundException extends RuntimeException {
    private final String errorCode = "SUBSCRIPTION_NOT_FOUND";

    public SubscriptionNotFoundException(String subscriptionId) {
        super("Subscription not found: " + subscriptionId);
    }
}

