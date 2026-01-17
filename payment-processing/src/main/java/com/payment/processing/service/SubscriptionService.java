package com.payment.processing.service;

import com.payment.processing.dto.request.SubscriptionRequest;
import com.payment.processing.dto.request.SubscriptionUpdateRequest;
import com.payment.processing.dto.response.SubscriptionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SubscriptionService {
    SubscriptionResponse createSubscription(SubscriptionRequest request, String idempotencyKey, String correlationId);
    SubscriptionResponse getSubscription(UUID subscriptionId);
    SubscriptionResponse updateSubscription(UUID subscriptionId, SubscriptionUpdateRequest request, String correlationId);
    SubscriptionResponse cancelSubscription(UUID subscriptionId, String correlationId);
    Page<SubscriptionResponse> getSubscriptionsByCustomer(String customerId, Pageable pageable);
}

