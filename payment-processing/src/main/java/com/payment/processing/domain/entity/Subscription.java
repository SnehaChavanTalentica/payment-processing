package com.payment.processing.domain.entity;

import com.payment.processing.domain.enums.BillingInterval;
import com.payment.processing.domain.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "subscriptions", indexes = {
    @Index(name = "idx_subscription_customer_id", columnList = "customer_id"),
    @Index(name = "idx_subscription_gateway_id", columnList = "gateway_subscription_id"),
    @Index(name = "idx_subscription_status", columnList = "status"),
    @Index(name = "idx_subscription_idempotency_key", columnList = "idempotency_key", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "customer_id", nullable = false, length = 100)
    private String customerId;

    @Column(name = "customer_email", length = 255)
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.PENDING;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval", nullable = false, length = 20)
    private BillingInterval billingInterval;

    @Column(name = "interval_count", nullable = false)
    @Builder.Default
    private Integer intervalCount = 1;

    @Column(name = "trial_days")
    private Integer trialDays;

    @Column(name = "trial_amount", precision = 19, scale = 4)
    private BigDecimal trialAmount;

    @Column(name = "trial_end_date")
    private LocalDate trialEndDate;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "next_billing_date")
    private LocalDate nextBillingDate;

    @Column(name = "last_billing_date")
    private LocalDate lastBillingDate;

    @Column(name = "total_cycles")
    private Integer totalCycles;

    @Column(name = "completed_cycles")
    @Builder.Default
    private Integer completedCycles = 0;

    @Column(name = "failed_cycles")
    @Builder.Default
    private Integer failedCycles = 0;

    @Column(name = "gateway_subscription_id", length = 100)
    private String gatewaySubscriptionId;

    @Column(name = "gateway_customer_profile_id", length = 100)
    private String gatewayCustomerProfileId;

    @Column(name = "gateway_payment_profile_id", length = 100)
    private String gatewayPaymentProfileId;

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

    @Column(name = "idempotency_key", length = 100, unique = true)
    private String idempotencyKey;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;
}

