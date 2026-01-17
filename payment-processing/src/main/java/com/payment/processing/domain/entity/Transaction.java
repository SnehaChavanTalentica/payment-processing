package com.payment.processing.domain.entity;

import com.payment.processing.domain.enums.PaymentMethodType;
import com.payment.processing.domain.enums.TransactionStatus;
import com.payment.processing.domain.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transaction_order_id", columnList = "order_id"),
    @Index(name = "idx_transaction_customer_id", columnList = "customer_id"),
    @Index(name = "idx_transaction_status", columnList = "status"),
    @Index(name = "idx_transaction_gateway_id", columnList = "gateway_transaction_id"),
    @Index(name = "idx_transaction_idempotency_key", columnList = "idempotency_key", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {

    @Column(name = "order_id", nullable = false, length = 100)
    private String orderId;

    @Column(name = "customer_id", nullable = false, length = 100)
    private String customerId;

    @Column(name = "customer_email", length = 255)
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "authorized_amount", precision = 19, scale = 4)
    private BigDecimal authorizedAmount;

    @Column(name = "captured_amount", precision = 19, scale = 4)
    private BigDecimal capturedAmount;

    @Column(name = "refunded_amount", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Column(name = "gateway_transaction_id", length = 100)
    private String gatewayTransactionId;

    @Column(name = "gateway_auth_code", length = 50)
    private String gatewayAuthCode;

    @Column(name = "gateway_avs_result", length = 10)
    private String gatewayAvsResult;

    @Column(name = "gateway_cvv_result", length = 10)
    private String gatewayCvvResult;

    @Column(name = "gateway_response_code", length = 20)
    private String gatewayResponseCode;

    @Column(name = "gateway_response_message", length = 500)
    private String gatewayResponseMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method_type", length = 30)
    private PaymentMethodType paymentMethodType;

    @Column(name = "card_last_four", length = 4)
    private String cardLastFour;

    @Column(name = "card_brand", length = 30)
    private String cardBrand;

    @Column(name = "card_exp_month", length = 2)
    private String cardExpMonth;

    @Column(name = "card_exp_year", length = 4)
    private String cardExpYear;

    @Column(name = "billing_first_name", length = 100)
    private String billingFirstName;

    @Column(name = "billing_last_name", length = 100)
    private String billingLastName;

    @Column(name = "billing_address", length = 255)
    private String billingAddress;

    @Column(name = "billing_city", length = 100)
    private String billingCity;

    @Column(name = "billing_state", length = 50)
    private String billingState;

    @Column(name = "billing_zip", length = 20)
    private String billingZip;

    @Column(name = "billing_country", length = 3)
    @Builder.Default
    private String billingCountry = "US";

    @Column(name = "authorized_at")
    private Instant authorizedAt;

    @Column(name = "captured_at")
    private Instant capturedAt;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "parent_transaction_id")
    private UUID parentTransactionId;

    @Column(name = "subscription_id")
    private UUID subscriptionId;

    @Column(name = "idempotency_key", length = 100, unique = true)
    private String idempotencyKey;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    public boolean canCapture() {
        return status == TransactionStatus.AUTHORIZED && type == TransactionType.AUTHORIZE;
    }

    public boolean canVoid() {
        return status == TransactionStatus.AUTHORIZED || status == TransactionStatus.CAPTURED;
    }

    public boolean canRefund() {
        return status == TransactionStatus.CAPTURED || status == TransactionStatus.SETTLED;
    }

    public boolean canPartialRefund(BigDecimal refundAmount) {
        if (!canRefund()) return false;
        BigDecimal available = getRefundableAmount();
        return refundAmount.compareTo(available) <= 0;
    }

    public BigDecimal getRefundableAmount() {
        BigDecimal captured = capturedAmount != null ? capturedAmount : amount;
        BigDecimal refunded = refundedAmount != null ? refundedAmount : BigDecimal.ZERO;
        return captured.subtract(refunded);
    }

    public void markAuthorized(String gatewayTransId, String authCode) {
        this.status = TransactionStatus.AUTHORIZED;
        this.gatewayTransactionId = gatewayTransId;
        this.gatewayAuthCode = authCode;
        this.authorizedAt = Instant.now();
        this.authorizedAmount = this.amount;
    }

    public void markCaptured(BigDecimal captureAmount) {
        this.status = TransactionStatus.CAPTURED;
        this.capturedAmount = captureAmount;
        this.capturedAt = Instant.now();
    }

    public void markVoided() {
        this.status = TransactionStatus.VOIDED;
        this.voidedAt = Instant.now();
    }

    public void addRefund(BigDecimal refundAmount) {
        if (this.refundedAmount == null) {
            this.refundedAmount = BigDecimal.ZERO;
        }
        this.refundedAmount = this.refundedAmount.add(refundAmount);
        this.refundedAt = Instant.now();

        BigDecimal captured = capturedAmount != null ? capturedAmount : amount;
        if (this.refundedAmount.compareTo(captured) >= 0) {
            this.status = TransactionStatus.REFUNDED;
        } else {
            this.status = TransactionStatus.PARTIALLY_REFUNDED;
        }
    }

    public void markFailed(String errorCode, String errorMessage) {
        this.status = TransactionStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.failedAt = Instant.now();
    }
}

