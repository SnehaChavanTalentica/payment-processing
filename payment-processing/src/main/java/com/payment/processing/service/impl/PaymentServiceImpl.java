package com.payment.processing.service.impl;

import com.payment.processing.audit.AuditService;
import com.payment.processing.domain.entity.Transaction;
import com.payment.processing.domain.enums.PaymentMethodType;
import com.payment.processing.domain.enums.TransactionStatus;
import com.payment.processing.domain.enums.TransactionType;
import com.payment.processing.dto.request.*;
import com.payment.processing.dto.response.TransactionResponse;
import com.payment.processing.exception.*;
import com.payment.processing.gateway.GatewayResponse;
import com.payment.processing.gateway.PaymentGateway;
import com.payment.processing.repository.TransactionRepository;
import com.payment.processing.service.IdempotencyService;
import com.payment.processing.service.PaymentService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final TransactionRepository transactionRepository;
    private final PaymentGateway paymentGateway;
    private final IdempotencyService idempotencyService;
    private final AuditService auditService;
    private final Counter purchaseCounter;
    private final Counter authorizeCounter;

    public PaymentServiceImpl(TransactionRepository transactionRepository, PaymentGateway paymentGateway,
                              IdempotencyService idempotencyService, AuditService auditService,
                              MeterRegistry meterRegistry) {
        this.transactionRepository = transactionRepository;
        this.paymentGateway = paymentGateway;
        this.idempotencyService = idempotencyService;
        this.auditService = auditService;

        this.purchaseCounter = Counter.builder("payment.transactions").tag("type", "purchase").register(meterRegistry);
        this.authorizeCounter = Counter.builder("payment.transactions").tag("type", "authorize").register(meterRegistry);
    }

    @Override
    public TransactionResponse purchase(PaymentRequest request, String idempotencyKey, String correlationId) {
        log.info("Processing purchase for order: {}", request.getOrderId());
        purchaseCounter.increment();
        checkIdempotency(idempotencyKey);

        Transaction transaction = createTransaction(request, TransactionType.PURCHASE, idempotencyKey, correlationId);
        transaction = transactionRepository.save(transaction);

        try {
            GatewayResponse gatewayResponse = paymentGateway.purchase(request);

            if (gatewayResponse.isSuccess()) {
                transaction.markAuthorized(gatewayResponse.getTransactionId(), gatewayResponse.getAuthCode());
                transaction.markCaptured(request.getAmount());
                transaction.setGatewayAvsResult(gatewayResponse.getAvsResult());
                transaction.setGatewayCvvResult(gatewayResponse.getCvvResult());
                transaction.setGatewayResponseCode(gatewayResponse.getResponseCode());
                transaction.setGatewayResponseMessage(gatewayResponse.getResponseMessage());
                auditService.logTransaction(transaction, "PURCHASE_SUCCESS", correlationId);
            } else {
                transaction.markFailed(gatewayResponse.getErrorCode(), gatewayResponse.getErrorMessage());
                auditService.logTransaction(transaction, "PURCHASE_FAILED", correlationId);
            }

            transaction = transactionRepository.save(transaction);
            return mapToResponse(transaction);

        } catch (Exception e) {
            log.error("Purchase failed for order: {}", request.getOrderId(), e);
            transaction.markFailed("SYSTEM_ERROR", e.getMessage());
            transactionRepository.save(transaction);
            throw new PaymentException("PURCHASE_FAILED", "Purchase transaction failed: " + e.getMessage());
        }
    }

    @Override
    public TransactionResponse authorize(PaymentRequest request, String idempotencyKey, String correlationId) {
        log.info("Processing authorization for order: {}", request.getOrderId());
        authorizeCounter.increment();
        checkIdempotency(idempotencyKey);

        Transaction transaction = createTransaction(request, TransactionType.AUTHORIZE, idempotencyKey, correlationId);
        transaction = transactionRepository.save(transaction);

        try {
            GatewayResponse gatewayResponse = paymentGateway.authorize(request);

            if (gatewayResponse.isSuccess()) {
                transaction.markAuthorized(gatewayResponse.getTransactionId(), gatewayResponse.getAuthCode());
                transaction.setGatewayAvsResult(gatewayResponse.getAvsResult());
                transaction.setGatewayCvvResult(gatewayResponse.getCvvResult());
                auditService.logTransaction(transaction, "AUTHORIZE_SUCCESS", correlationId);
            } else {
                transaction.markFailed(gatewayResponse.getErrorCode(), gatewayResponse.getErrorMessage());
                auditService.logTransaction(transaction, "AUTHORIZE_FAILED", correlationId);
            }

            transaction = transactionRepository.save(transaction);
            return mapToResponse(transaction);

        } catch (Exception e) {
            log.error("Authorization failed for order: {}", request.getOrderId(), e);
            transaction.markFailed("SYSTEM_ERROR", e.getMessage());
            transactionRepository.save(transaction);
            throw new PaymentException("AUTHORIZE_FAILED", "Authorization failed: " + e.getMessage());
        }
    }

    @Override
    public TransactionResponse capture(CaptureRequest request, String idempotencyKey, String correlationId) {
        log.info("Processing capture for transaction: {}", request.getTransactionId());
        checkIdempotency(idempotencyKey);

        UUID transactionId = UUID.fromString(request.getTransactionId());
        Transaction originalTransaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(request.getTransactionId()));

        if (!originalTransaction.canCapture()) {
            throw new InvalidTransactionStateException("Transaction cannot be captured. Current status: " + originalTransaction.getStatus());
        }

        BigDecimal captureAmount = request.getAmount() != null ? request.getAmount() : originalTransaction.getAuthorizedAmount();

        try {
            GatewayResponse gatewayResponse = paymentGateway.capture(originalTransaction.getGatewayTransactionId(), captureAmount);

            if (gatewayResponse.isSuccess()) {
                originalTransaction.markCaptured(captureAmount);
                auditService.logTransaction(originalTransaction, "CAPTURE_SUCCESS", correlationId);
            } else {
                throw new GatewayException(gatewayResponse.getErrorCode(), gatewayResponse.getErrorMessage());
            }

            originalTransaction = transactionRepository.save(originalTransaction);
            return mapToResponse(originalTransaction);

        } catch (GatewayException e) {
            log.error("Capture failed for transaction: {}", request.getTransactionId(), e);
            throw e;
        }
    }

    @Override
    public TransactionResponse cancel(CancelRequest request, String idempotencyKey, String correlationId) {
        log.info("Processing void for transaction: {}", request.getTransactionId());
        checkIdempotency(idempotencyKey);

        UUID transactionId = UUID.fromString(request.getTransactionId());
        Transaction originalTransaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(request.getTransactionId()));

        if (!originalTransaction.canVoid()) {
            throw new InvalidTransactionStateException("Transaction cannot be voided. Current status: " + originalTransaction.getStatus());
        }

        try {
            GatewayResponse gatewayResponse = paymentGateway.voidTransaction(originalTransaction.getGatewayTransactionId());

            if (gatewayResponse.isSuccess()) {
                originalTransaction.markVoided();
                originalTransaction.setDescription(request.getReason());
                auditService.logTransaction(originalTransaction, "VOID_SUCCESS", correlationId);
            } else {
                throw new GatewayException(gatewayResponse.getErrorCode(), gatewayResponse.getErrorMessage());
            }

            originalTransaction = transactionRepository.save(originalTransaction);
            return mapToResponse(originalTransaction);

        } catch (GatewayException e) {
            log.error("Void failed for transaction: {}", request.getTransactionId(), e);
            throw e;
        }
    }

    @Override
    public TransactionResponse refund(RefundRequest request, String idempotencyKey, String correlationId) {
        log.info("Processing refund for transaction: {}", request.getTransactionId());
        checkIdempotency(idempotencyKey);

        UUID transactionId = UUID.fromString(request.getTransactionId());
        Transaction originalTransaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(request.getTransactionId()));

        BigDecimal refundAmount;
        if (request.isFullRefund()) {
            refundAmount = originalTransaction.getRefundableAmount();
        } else {
            refundAmount = request.getAmount();
            if (!originalTransaction.canPartialRefund(refundAmount)) {
                throw new InvalidTransactionStateException("Cannot refund amount: " + refundAmount + ". Available: " + originalTransaction.getRefundableAmount());
            }
        }

        if (!originalTransaction.canRefund()) {
            throw new InvalidTransactionStateException("Transaction cannot be refunded. Current status: " + originalTransaction.getStatus());
        }

        Transaction refundTransaction = Transaction.builder()
                .orderId(originalTransaction.getOrderId())
                .customerId(originalTransaction.getCustomerId())
                .customerEmail(originalTransaction.getCustomerEmail())
                .type(TransactionType.REFUND)
                .status(TransactionStatus.PENDING)
                .amount(refundAmount)
                .currency(originalTransaction.getCurrency())
                .parentTransactionId(originalTransaction.getId())
                .idempotencyKey(idempotencyKey)
                .correlationId(correlationId)
                .description(request.getReason())
                .cardLastFour(originalTransaction.getCardLastFour())
                .cardBrand(originalTransaction.getCardBrand())
                .build();
        refundTransaction = transactionRepository.save(refundTransaction);

        try {
            GatewayResponse gatewayResponse = paymentGateway.refund(
                    originalTransaction.getGatewayTransactionId(), refundAmount, originalTransaction.getCardLastFour());

            if (gatewayResponse.isSuccess()) {
                refundTransaction.setStatus(TransactionStatus.REFUNDED);
                refundTransaction.setGatewayTransactionId(gatewayResponse.getTransactionId());
                originalTransaction.addRefund(refundAmount);
                transactionRepository.save(originalTransaction);
                auditService.logTransaction(refundTransaction, "REFUND_SUCCESS", correlationId);
            } else {
                refundTransaction.markFailed(gatewayResponse.getErrorCode(), gatewayResponse.getErrorMessage());
            }

            refundTransaction = transactionRepository.save(refundTransaction);
            return mapToResponse(refundTransaction);

        } catch (Exception e) {
            log.error("Refund failed for transaction: {}", request.getTransactionId(), e);
            refundTransaction.markFailed("SYSTEM_ERROR", e.getMessage());
            transactionRepository.save(refundTransaction);
            throw new PaymentException("REFUND_FAILED", "Refund failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId.toString()));
        return mapToResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionsByCustomer(String customerId, Pageable pageable) {
        Page<Transaction> transactions = transactionRepository.findByCustomerId(customerId, pageable);
        List<TransactionResponse> responses = transactions.getContent().stream()
                .map(this::mapToResponse).collect(Collectors.toList());
        return new PageImpl<>(responses, pageable, transactions.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionsByOrder(String orderId, Pageable pageable) {
        List<Transaction> transactions = transactionRepository.findByOrderId(orderId);
        List<TransactionResponse> responses = transactions.stream().map(this::mapToResponse).collect(Collectors.toList());
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), responses.size());
        return new PageImpl<>(responses.subList(start, end), pageable, responses.size());
    }

    private void checkIdempotency(String idempotencyKey) {
        if (idempotencyKey != null && transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new DuplicateRequestException(idempotencyKey);
        }
    }

    private Transaction createTransaction(PaymentRequest request, TransactionType type, String idempotencyKey, String correlationId) {
        return Transaction.builder()
                .orderId(request.getOrderId())
                .customerId(request.getCustomerId())
                .customerEmail(request.getCustomerEmail())
                .type(type)
                .status(TransactionStatus.PENDING)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethodType(PaymentMethodType.CREDIT_CARD)
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
                .description(request.getDescription())
                .metadata(request.getMetadata())
                .idempotencyKey(idempotencyKey)
                .correlationId(correlationId)
                .build();
    }

    private String detectCardBrand(String cardNumber) {
        if (cardNumber.startsWith("4")) return "VISA";
        else if (cardNumber.startsWith("5") || cardNumber.startsWith("2")) return "MASTERCARD";
        else if (cardNumber.startsWith("34") || cardNumber.startsWith("37")) return "AMEX";
        else if (cardNumber.startsWith("6011") || cardNumber.startsWith("65")) return "DISCOVER";
        else return "UNKNOWN";
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .orderId(transaction.getOrderId())
                .customerId(transaction.getCustomerId())
                .customerEmail(transaction.getCustomerEmail())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .authorizedAmount(transaction.getAuthorizedAmount())
                .capturedAmount(transaction.getCapturedAmount())
                .refundedAmount(transaction.getRefundedAmount())
                .refundableAmount(transaction.getRefundableAmount())
                .gatewayTransactionId(transaction.getGatewayTransactionId())
                .gatewayAuthCode(transaction.getGatewayAuthCode())
                .gatewayAvsResult(transaction.getGatewayAvsResult())
                .gatewayCvvResult(transaction.getGatewayCvvResult())
                .gatewayResponseCode(transaction.getGatewayResponseCode())
                .gatewayResponseMessage(transaction.getGatewayResponseMessage())
                .paymentMethodType(transaction.getPaymentMethodType())
                .cardLastFour(transaction.getCardLastFour())
                .cardBrand(transaction.getCardBrand())
                .cardExpMonth(transaction.getCardExpMonth())
                .cardExpYear(transaction.getCardExpYear())
                .billingFirstName(transaction.getBillingFirstName())
                .billingLastName(transaction.getBillingLastName())
                .billingCity(transaction.getBillingCity())
                .billingState(transaction.getBillingState())
                .billingZip(transaction.getBillingZip())
                .billingCountry(transaction.getBillingCountry())
                .createdAt(transaction.getCreatedAt())
                .authorizedAt(transaction.getAuthorizedAt())
                .capturedAt(transaction.getCapturedAt())
                .voidedAt(transaction.getVoidedAt())
                .refundedAt(transaction.getRefundedAt())
                .failedAt(transaction.getFailedAt())
                .parentTransactionId(transaction.getParentTransactionId())
                .subscriptionId(transaction.getSubscriptionId())
                .errorCode(transaction.getErrorCode())
                .errorMessage(transaction.getErrorMessage())
                .description(transaction.getDescription())
                .correlationId(transaction.getCorrelationId())
                .canCapture(transaction.canCapture())
                .canVoid(transaction.canVoid())
                .canRefund(transaction.canRefund())
                .build();
    }
}

