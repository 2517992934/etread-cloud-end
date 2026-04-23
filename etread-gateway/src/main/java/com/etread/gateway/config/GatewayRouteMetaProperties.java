package com.etread.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "etread.gateway.route-meta")
public class GatewayRouteMetaProperties {

    private Map<String, Meta> routes = new HashMap<>();

    @Data
    public static class Meta {
        private Boolean authRequired = true;
        private String rateLimitRule;
        private String rewritePath;
        private String timeoutGroup;
    }
}
