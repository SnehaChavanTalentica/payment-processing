package com.payment.processing.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayResponse {
    private boolean success;
    private String transactionId;
    private String subscriptionId;
    private String authCode;
    private String avsResult;
    private String cvvResult;
    private String responseCode;
    private String responseMessage;
    private String errorCode;
    private String errorMessage;
    private String customerProfileId;
    private String paymentProfileId;

    public static GatewayResponse success(String transactionId, String authCode) {
        return GatewayResponse.builder()
                .success(true)
                .transactionId(transactionId)
                .authCode(authCode)
                .build();
    }

    public static GatewayResponse failure(String errorCode, String errorMessage) {
        return GatewayResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
}

