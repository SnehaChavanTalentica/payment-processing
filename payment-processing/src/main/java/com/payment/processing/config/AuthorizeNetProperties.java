package com.payment.processing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "authorize-net")
@Data
public class AuthorizeNetProperties {
    private String apiLoginId;
    private String transactionKey;
    private String signatureKey;
    private boolean sandbox = true;
}

