package com.etread.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "etread.gateway.jwt")
public class GatewayJwtProperties {

    private String secret;
    private String headerName = "Authorization";
    private String tokenPrefix = "Bearer ";
    private Long expireSeconds = 86400L;
}
