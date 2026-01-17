package com.payment.processing.security;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
@Getter
@Setter
public class JwtProperties {
    private String secretKey = "default";
    private Long expiration = 86400000L;
    private String issuer = "payment-processing-system";
}

