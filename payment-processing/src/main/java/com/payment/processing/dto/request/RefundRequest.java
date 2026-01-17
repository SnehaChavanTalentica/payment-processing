package com.payment.processing.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRequest {

    @NotBlank(message = "Transaction ID is required")
    private String transactionId;

    private BigDecimal amount;

    private String reason;

    @Builder.Default
    private boolean fullRefund = false;
}

