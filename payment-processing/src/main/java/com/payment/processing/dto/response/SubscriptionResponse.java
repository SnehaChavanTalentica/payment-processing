package com.payment.processing.dto.response;

import com.payment.processing.domain.enums.BillingInterval;
import com.payment.processing.domain.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionResponse {
    private UUID id;
    private String name;
    private String description;
    private String customerId;
    private String customerEmail;
    private SubscriptionStatus status;
    private BigDecimal amount;
    private String currency;
    private BillingInterval billingInterval;
    private Integer intervalCount;
    private Integer trialDays;
    private BigDecimal trialAmount;
    private LocalDate trialEndDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate nextBillingDate;
    private LocalDate lastBillingDate;
    private Integer totalCycles;
    private Integer completedCycles;
    private Integer failedCycles;
    private Integer remainingCycles;
    private String gatewaySubscriptionId;
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
    private Instant updatedAt;
    private String correlationId;
    private boolean canUpdate;
    private boolean canCancel;
    private boolean canReactivate;
}
