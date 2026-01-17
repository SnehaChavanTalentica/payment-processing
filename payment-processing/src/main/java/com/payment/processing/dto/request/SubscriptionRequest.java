package com.payment.processing.dto.request;

import com.payment.processing.domain.enums.BillingInterval;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionRequest {

    @NotBlank(message = "Subscription name is required")
    private String name;

    private String description;

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    private String customerEmail;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;

    private String currency;

    @NotNull(message = "Billing interval is required")
    private BillingInterval billingInterval;

    @Builder.Default
    private Integer intervalCount = 1;

    private Integer trialDays;
    private BigDecimal trialAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalCycles;

    @NotBlank(message = "Card number is required")
    private String cardNumber;

    @NotBlank(message = "Expiration month is required")
    private String expMonth;

    @NotBlank(message = "Expiration year is required")
    private String expYear;

    @NotBlank(message = "CVV is required")
    private String cvv;

    private String billingFirstName;
    private String billingLastName;
    private String billingAddress;
    private String billingCity;
    private String billingState;
    private String billingZip;
    private String billingCountry;
}
