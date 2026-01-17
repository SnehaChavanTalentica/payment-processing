package com.payment.processing.dto.request;

import com.payment.processing.domain.enums.BillingInterval;
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
public class SubscriptionUpdateRequest {
    private String name;
    private String description;
    private BigDecimal amount;
    private BillingInterval billingInterval;
    private Integer intervalCount;
    private LocalDate endDate;
    private Integer totalCycles;
    private String cardNumber;
    private String expMonth;
    private String expYear;
    private String cvv;
    private String billingFirstName;
    private String billingLastName;
    private String billingAddress;
    private String billingCity;
    private String billingState;
    private String billingZip;
    private String billingCountry;
}

