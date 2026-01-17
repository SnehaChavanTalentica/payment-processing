package com.payment.processing.controller;

import com.payment.processing.dto.request.*;
import com.payment.processing.dto.response.ApiResponse;
import com.payment.processing.dto.response.TransactionResponse;
import com.payment.processing.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
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

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Payment transaction operations")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/purchase")
    @Operation(summary = "Process a purchase", description = "Single-step authorization and capture")
    public ResponseEntity<ApiResponse<TransactionResponse>> purchase(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        log.info("Processing purchase request for order: {}", request.getOrderId());
        TransactionResponse response = paymentService.purchase(request, idempotencyKey, correlationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Purchase completed successfully"));
    }

    @PostMapping("/authorize")
    @Operation(summary = "Authorize a payment", description = "Hold funds without capturing")
    public ResponseEntity<ApiResponse<TransactionResponse>> authorize(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        log.info("Processing authorization request for order: {}", request.getOrderId());
        TransactionResponse response = paymentService.authorize(request, idempotencyKey, correlationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Authorization completed successfully"));
    }

    @PostMapping("/capture")
    @Operation(summary = "Capture an authorized payment")
    public ResponseEntity<ApiResponse<TransactionResponse>> capture(
            @Valid @RequestBody CaptureRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        log.info("Processing capture request for transaction: {}", request.getTransactionId());
        TransactionResponse response = paymentService.capture(request, idempotencyKey, correlationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Capture completed successfully"));
    }

    @PostMapping("/cancel")
    @Operation(summary = "Cancel/void a transaction")
    public ResponseEntity<ApiResponse<TransactionResponse>> cancel(
            @Valid @RequestBody CancelRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        log.info("Processing cancel request for transaction: {}", request.getTransactionId());
        TransactionResponse response = paymentService.cancel(request, idempotencyKey, correlationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Transaction voided successfully"));
    }

    @PostMapping("/refund")
    @Operation(summary = "Refund a transaction")
    public ResponseEntity<ApiResponse<TransactionResponse>> refund(
            @Valid @RequestBody RefundRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        log.info("Processing refund request for transaction: {}", request.getTransactionId());
        TransactionResponse response = paymentService.refund(request, idempotencyKey, correlationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Refund completed successfully"));
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(@PathVariable UUID transactionId) {
        log.info("Fetching transaction: {}", transactionId);
        TransactionResponse response = paymentService.getTransaction(transactionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get transactions by customer")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactionsByCustomer(
            @PathVariable String customerId, Pageable pageable) {
        log.info("Fetching transactions for customer: {}", customerId);
        Page<TransactionResponse> response = paymentService.getTransactionsByCustomer(customerId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get transactions by order")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactionsByOrder(
            @PathVariable String orderId, Pageable pageable) {
        log.info("Fetching transactions for order: {}", orderId);
        Page<TransactionResponse> response = paymentService.getTransactionsByOrder(orderId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

