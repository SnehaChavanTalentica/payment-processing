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
public class CaptureRequest {

    @NotBlank(message = "Transaction ID is required")
    private String transactionId;

    private BigDecimal amount;

    private String description;
}

