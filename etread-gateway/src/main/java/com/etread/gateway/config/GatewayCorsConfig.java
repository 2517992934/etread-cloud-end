package com.etread.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class GatewayCorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter(GatewayCorsProperties corsProperties) {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = corsProperties.getAllowedOrigins();
        if (origins != null) {
            for (String origin : origins) {
                if (origin != null && !origin.isBlank()) {
                    config.addAllowedOriginPattern(origin.trim());
                }
            }
        }

        List<String> methods = corsProperties.getAllowedMethods();
        if (methods != null) {
            for (String method : methods) {
                if (method != null && !method.isBlank()) {
                    config.addAllowedMethod(method.trim());
                }
            }
        }

        List<String> headers = corsProperties.getAllowedHeaders();
        if (headers != null) {
            for (String header : headers) {
                if (header != null && !header.isBlank()) {
                    config.addAllowedHeader(header.trim());
                }
            }
        }

        config.setAllowCredentials(Boolean.TRUE.equals(corsProperties.getAllowCredentials()));
        config.setMaxAge(corsProperties.getMaxAge());
        config.addExposedHeader("X-Trace-Id");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
