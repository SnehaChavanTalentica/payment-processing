package com.payment.processing.dto.response;

import com.payment.processing.domain.enums.PaymentMethodType;
import com.payment.processing.domain.enums.TransactionStatus;
import com.payment.processing.domain.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    private UUID id;
    private String orderId;
    private String customerId;
    private String customerEmail;
    private TransactionType type;
    private TransactionStatus status;
    private BigDecimal amount;
    private String currency;
    private BigDecimal authorizedAmount;
    private BigDecimal capturedAmount;
    private BigDecimal refundedAmount;
    private BigDecimal refundableAmount;
    private String gatewayTransactionId;
    private String gatewayAuthCode;
    private String gatewayAvsResult;
    private String gatewayCvvResult;
    private String gatewayResponseCode;
    private String gatewayResponseMessage;
    private PaymentMethodType paymentMethodType;
    private String cardLastFour;
    private String cardBrand;
    private String cardExpMonth;
    private String cardExpYear;
    private String billingFirstName;
    private String billingLastName;
    private String billingCity;
    private String billingState;
    private String billingZip;
    private String billingCountry;
    private Instant createdAt;
    private Instant authorizedAt;
    private Instant capturedAt;
    private Instant voidedAt;
    private Instant refundedAt;
    private Instant failedAt;
    private UUID parentTransactionId;
    private UUID subscriptionId;
    private String errorCode;
    private String errorMessage;
    private String description;
    private String correlationId;
    private boolean canCapture;
    private boolean canVoid;
    private boolean canRefund;
}

