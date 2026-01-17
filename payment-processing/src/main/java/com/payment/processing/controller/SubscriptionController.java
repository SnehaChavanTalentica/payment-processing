package com.payment.processing.controller;

import com.payment.processing.dto.request.SubscriptionRequest;
import com.payment.processing.dto.request.SubscriptionUpdateRequest;
import com.payment.processing.dto.response.ApiResponse;
import com.payment.processing.dto.response.SubscriptionResponse;
import com.payment.processing.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for subscription/recurring billing operations.
 */
@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Subscriptions", description = "Recurring billing subscription operations")
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/create")
    @Operation(summary = "Create a subscription", description = "Set up recurring billing for a customer")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> createSubscription(
            @Valid @RequestBody SubscriptionRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        log.info("Creating subscription for customer: {}", request.getCustomerId());
        SubscriptionResponse response = subscriptionService.createSubscription(request, idempotencyKey, correlationId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Subscription created successfully"));
    }

    @GetMapping("/{subscriptionId}")
    @Operation(summary = "Get subscription by ID", description = "Retrieve subscription details")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getSubscription(
            @Parameter(description = "Subscription ID") @PathVariable UUID subscriptionId) {

        log.info("Fetching subscription: {}", subscriptionId);
        SubscriptionResponse response = subscriptionService.getSubscription(subscriptionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{subscriptionId}")
    @Operation(summary = "Update a subscription", description = "Update subscription details")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> updateSubscription(
            @Parameter(description = "Subscription ID") @PathVariable UUID subscriptionId,
            @Valid @RequestBody SubscriptionUpdateRequest request,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        log.info("Updating subscription: {}", subscriptionId);
        SubscriptionResponse response = subscriptionService.updateSubscription(subscriptionId, request, correlationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Subscription updated successfully"));
    }

    @DeleteMapping("/{subscriptionId}")
    @Operation(summary = "Cancel a subscription", description = "Cancel an active subscription")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> cancelSubscription(
            @Parameter(description = "Subscription ID") @PathVariable UUID subscriptionId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {

        log.info("Canceling subscription: {}", subscriptionId);
        SubscriptionResponse response = subscriptionService.cancelSubscription(subscriptionId, correlationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Subscription canceled successfully"));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get subscriptions by customer", description = "Retrieve all subscriptions for a customer")
    public ResponseEntity<ApiResponse<Page<SubscriptionResponse>>> getSubscriptionsByCustomer(
            @Parameter(description = "Customer ID") @PathVariable String customerId,
            Pageable pageable) {

        log.info("Fetching subscriptions for customer: {}", customerId);
        Page<SubscriptionResponse> response = subscriptionService.getSubscriptionsByCustomer(customerId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

