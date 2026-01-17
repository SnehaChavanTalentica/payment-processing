package com.payment.processing.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelRequest {

    @NotBlank(message = "Transaction ID is required")
    private String transactionId;

    private String reason;
}

