package com.payment.processing.gateway;

import com.payment.processing.dto.request.PaymentRequest;
import com.payment.processing.dto.request.SubscriptionRequest;
import com.payment.processing.dto.request.SubscriptionUpdateRequest;
import com.payment.processing.dto.request.PaymentRequest;
import com.payment.processing.exception.GatewayException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.math.BigDecimal;

public interface PaymentGateway {
    GatewayResponse authorize(PaymentRequest request);

    @Retryable(value = GatewayException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    GatewayResponse purchase(com.payment.processing.dto.request.PaymentRequest request);

    GatewayResponse capture(String transactionId, BigDecimal amount);
    GatewayResponse voidTransaction(String transactionId);
    GatewayResponse refund(String transactionId, BigDecimal amount, String cardLastFour);
    GatewayResponse createSubscription(SubscriptionRequest request);
    GatewayResponse updateSubscription(String subscriptionId, SubscriptionUpdateRequest request);
    GatewayResponse cancelSubscription(String subscriptionId);
    GatewayResponse getSubscriptionStatus(String subscriptionId);
    boolean validateWebhookSignature(String payload, String signature);
}
