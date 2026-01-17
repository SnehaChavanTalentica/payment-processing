package com.payment.processing.service.impl;

import com.payment.processing.audit.AuditService;
import com.payment.processing.domain.entity.Subscription;
import com.payment.processing.domain.enums.SubscriptionStatus;
import com.payment.processing.dto.request.SubscriptionRequest;
import com.payment.processing.dto.request.SubscriptionUpdateRequest;
import com.payment.processing.dto.response.SubscriptionResponse;
import com.payment.processing.exception.*;
import com.payment.processing.gateway.GatewayResponse;
import com.payment.processing.gateway.PaymentGateway;
import com.payment.processing.repository.SubscriptionRepository;
import com.payment.processing.service.SubscriptionService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentGateway paymentGateway;
    private final AuditService auditService;
    private final Counter subscriptionCreatedCounter;
    private final Counter subscriptionCanceledCounter;

    public SubscriptionServiceImpl(SubscriptionRepository subscriptionRepository, PaymentGateway paymentGateway,
                                   AuditService auditService, MeterRegistry meterRegistry) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentGateway = paymentGateway;
        this.auditService = auditService;
        this.subscriptionCreatedCounter = Counter.builder("subscription.operations").tag("type", "created").register(meterRegistry);
        this.subscriptionCanceledCounter = Counter.builder("subscription.operations").tag("type", "canceled").register(meterRegistry);
    }

    @Override
    public SubscriptionResponse createSubscription(SubscriptionRequest request, String idempotencyKey, String correlationId) {
        log.info("Creating subscription for customer: {}", request.getCustomerId());
        subscriptionCreatedCounter.increment();

        if (idempotencyKey != null && subscriptionRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new DuplicateRequestException(idempotencyKey);
        }

        Subscription subscription = createSubscriptionEntity(request, idempotencyKey, correlationId);
        subscription = subscriptionRepository.save(subscription);

        try {
            GatewayResponse gatewayResponse = paymentGateway.createSubscription(request);

            if (gatewayResponse.isSuccess()) {
                subscription.setStatus(SubscriptionStatus.ACTIVE);
                subscription.setGatewaySubscriptionId(gatewayResponse.getSubscriptionId());
                subscription.setGatewayCustomerProfileId(gatewayResponse.getCustomerProfileId());
                subscription.setGatewayPaymentProfileId(gatewayResponse.getPaymentProfileId());
                calculateNextBillingDate(subscription);
                auditService.logSubscription(subscription, "SUBSCRIPTION_CREATED", correlationId);
            } else {
                subscription.setStatus(SubscriptionStatus.FAILED);
                throw new GatewayException(gatewayResponse.getErrorCode(), gatewayResponse.getErrorMessage());
            }

            subscription = subscriptionRepository.save(subscription);
            return mapToResponse(subscription);

        } catch (GatewayException e) {
            log.error("Subscription creation failed for customer: {}", request.getCustomerId(), e);
            subscription.setStatus(SubscriptionStatus.FAILED);
            subscriptionRepository.save(subscription);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscription(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId.toString()));
        return mapToResponse(subscription);
    }

    @Override
    public SubscriptionResponse updateSubscription(UUID subscriptionId, SubscriptionUpdateRequest request, String correlationId) {
        log.info("Updating subscription: {}", subscriptionId);

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId.toString()));

        if (!canUpdateSubscription(subscription)) {
            throw new IllegalStateException("Subscription cannot be updated in status: " + subscription.getStatus());
        }

        try {
            GatewayResponse gatewayResponse = paymentGateway.updateSubscription(subscription.getGatewaySubscriptionId(), request);

            if (gatewayResponse.isSuccess()) {
                updateSubscriptionFields(subscription, request);
                auditService.logSubscription(subscription, "SUBSCRIPTION_UPDATED", correlationId);
            } else {
                throw new GatewayException(gatewayResponse.getErrorCode(), gatewayResponse.getErrorMessage());
            }

            subscription = subscriptionRepository.save(subscription);
            return mapToResponse(subscription);

        } catch (GatewayException e) {
            log.error("Subscription update failed: {}", subscriptionId, e);
            throw e;
        }
    }

    @Override
    public SubscriptionResponse cancelSubscription(UUID subscriptionId, String correlationId) {
        log.info("Canceling subscription: {}", subscriptionId);
        subscriptionCanceledCounter.increment();

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId.toString()));

        if (!canCancelSubscription(subscription)) {
            throw new IllegalStateException("Subscription cannot be canceled in status: " + subscription.getStatus());
        }

        try {
            GatewayResponse gatewayResponse = paymentGateway.cancelSubscription(subscription.getGatewaySubscriptionId());

            if (gatewayResponse.isSuccess()) {
                subscription.setStatus(SubscriptionStatus.CANCELED);
                subscription.setEndDate(LocalDate.now());
                auditService.logSubscription(subscription, "SUBSCRIPTION_CANCELED", correlationId);
            } else {
                throw new GatewayException(gatewayResponse.getErrorCode(), gatewayResponse.getErrorMessage());
            }

            subscription = subscriptionRepository.save(subscription);
            return mapToResponse(subscription);

        } catch (GatewayException e) {
            log.error("Subscription cancellation failed: {}", subscriptionId, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> getSubscriptionsByCustomer(String customerId, Pageable pageable) {
        Page<Subscription> subscriptions = subscriptionRepository.findByCustomerId(customerId, pageable);
        List<SubscriptionResponse> responses = subscriptions.getContent().stream()
                .map(this::mapToResponse).collect(Collectors.toList());
        return new PageImpl<>(responses, pageable, subscriptions.getTotalElements());
    }

    private Subscription createSubscriptionEntity(SubscriptionRequest request, String idempotencyKey, String correlationId) {
        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now().plusDays(1);
        return Subscription.builder()
                .name(request.getName())
                .description(request.getDescription())
                .customerId(request.getCustomerId())
                .customerEmail(request.getCustomerEmail())
                .status(SubscriptionStatus.PENDING)
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .billingInterval(request.getBillingInterval())
                .intervalCount(request.getIntervalCount())
                .trialDays(request.getTrialDays())
                .trialAmount(request.getTrialAmount())
                .startDate(startDate)
                .endDate(request.getEndDate())
                .totalCycles(request.getTotalCycles())
                .cardLastFour(request.getCardNumber().substring(request.getCardNumber().length() - 4))
                .cardBrand(detectCardBrand(request.getCardNumber()))
                .cardExpMonth(request.getExpMonth())
                .cardExpYear(request.getExpYear())
                .billingFirstName(request.getBillingFirstName())
                .billingLastName(request.getBillingLastName())
                .billingAddress(request.getBillingAddress())
                .billingCity(request.getBillingCity())
                .billingState(request.getBillingState())
                .billingZip(request.getBillingZip())
                .billingCountry(request.getBillingCountry())
                .idempotencyKey(idempotencyKey)
                .correlationId(correlationId)
                .build();
    }

    private void calculateNextBillingDate(Subscription subscription) {
        LocalDate startDate = subscription.getStartDate();
        if (subscription.getTrialDays() != null && subscription.getTrialDays() > 0) {
            subscription.setTrialEndDate(startDate.plusDays(subscription.getTrialDays()));
            subscription.setNextBillingDate(subscription.getTrialEndDate());
        } else {
            subscription.setNextBillingDate(startDate);
        }
    }

    private void updateSubscriptionFields(Subscription subscription, SubscriptionUpdateRequest request) {
        if (request.getName() != null) subscription.setName(request.getName());
        if (request.getDescription() != null) subscription.setDescription(request.getDescription());
        if (request.getAmount() != null) subscription.setAmount(request.getAmount());
        if (request.getBillingInterval() != null) subscription.setBillingInterval(request.getBillingInterval());
        if (request.getIntervalCount() != null) subscription.setIntervalCount(request.getIntervalCount());
        if (request.getEndDate() != null) subscription.setEndDate(request.getEndDate());
        if (request.getTotalCycles() != null) subscription.setTotalCycles(request.getTotalCycles());
        if (request.getCardNumber() != null) {
            subscription.setCardLastFour(request.getCardNumber().substring(request.getCardNumber().length() - 4));
            subscription.setCardBrand(detectCardBrand(request.getCardNumber()));
        }
        if (request.getExpMonth() != null) subscription.setCardExpMonth(request.getExpMonth());
        if (request.getExpYear() != null) subscription.setCardExpYear(request.getExpYear());
        if (request.getBillingFirstName() != null) subscription.setBillingFirstName(request.getBillingFirstName());
        if (request.getBillingLastName() != null) subscription.setBillingLastName(request.getBillingLastName());
        if (request.getBillingAddress() != null) subscription.setBillingAddress(request.getBillingAddress());
        if (request.getBillingCity() != null) subscription.setBillingCity(request.getBillingCity());
        if (request.getBillingState() != null) subscription.setBillingState(request.getBillingState());
        if (request.getBillingZip() != null) subscription.setBillingZip(request.getBillingZip());
        if (request.getBillingCountry() != null) subscription.setBillingCountry(request.getBillingCountry());
    }

    private boolean canUpdateSubscription(Subscription subscription) {
        return subscription.getStatus() == SubscriptionStatus.ACTIVE ||
               subscription.getStatus() == SubscriptionStatus.SUSPENDED;
    }

    private boolean canCancelSubscription(Subscription subscription) {
        return subscription.getStatus() == SubscriptionStatus.ACTIVE ||
               subscription.getStatus() == SubscriptionStatus.SUSPENDED ||
               subscription.getStatus() == SubscriptionStatus.TRIAL;
    }

    private String detectCardBrand(String cardNumber) {
        if (cardNumber.startsWith("4")) return "VISA";
        else if (cardNumber.startsWith("5") || cardNumber.startsWith("2")) return "MASTERCARD";
        else if (cardNumber.startsWith("34") || cardNumber.startsWith("37")) return "AMEX";
        else if (cardNumber.startsWith("6011") || cardNumber.startsWith("65")) return "DISCOVER";
        else return "UNKNOWN";
    }

    private SubscriptionResponse mapToResponse(Subscription subscription) {
        Integer remainingCycles = null;
        if (subscription.getTotalCycles() != null) {
            remainingCycles = subscription.getTotalCycles() - subscription.getCompletedCycles();
        }

        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .name(subscription.getName())
                .description(subscription.getDescription())
                .customerId(subscription.getCustomerId())
                .customerEmail(subscription.getCustomerEmail())
                .status(subscription.getStatus())
                .amount(subscription.getAmount())
                .currency(subscription.getCurrency())
                .billingInterval(subscription.getBillingInterval())
                .intervalCount(subscription.getIntervalCount())
                .trialDays(subscription.getTrialDays())
                .trialAmount(subscription.getTrialAmount())
                .trialEndDate(subscription.getTrialEndDate())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .nextBillingDate(subscription.getNextBillingDate())
                .lastBillingDate(subscription.getLastBillingDate())
                .totalCycles(subscription.getTotalCycles())
                .completedCycles(subscription.getCompletedCycles())
                .failedCycles(subscription.getFailedCycles())
                .remainingCycles(remainingCycles)
                .gatewaySubscriptionId(subscription.getGatewaySubscriptionId())
                .cardLastFour(subscription.getCardLastFour())
                .cardBrand(subscription.getCardBrand())
                .cardExpMonth(subscription.getCardExpMonth())
                .cardExpYear(subscription.getCardExpYear())
                .billingFirstName(subscription.getBillingFirstName())
                .billingLastName(subscription.getBillingLastName())
                .billingCity(subscription.getBillingCity())
                .billingState(subscription.getBillingState())
                .billingZip(subscription.getBillingZip())
                .billingCountry(subscription.getBillingCountry())
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .correlationId(subscription.getCorrelationId())
                .canUpdate(canUpdateSubscription(subscription))
                .canCancel(canCancelSubscription(subscription))
                .canReactivate(subscription.getStatus() == SubscriptionStatus.SUSPENDED)
                .build();
    }
}

