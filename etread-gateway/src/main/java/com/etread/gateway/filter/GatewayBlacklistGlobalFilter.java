package com.etread.gateway.filter;

import com.etread.gateway.config.GatewayBlacklistProperties;
import com.etread.gateway.web.GatewayErrorCode;
import com.etread.gateway.web.GatewayResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class GatewayBlacklistGlobalFilter implements GlobalFilter, Ordered {

    private static final String HEADER_TRACE_ID = "X-Trace-Id";
    private static final String HEADER_TOKEN = "token";
    private static final String HEADER_ACCOUNT = "X-Account";
    private static final String AUTHORIZATION_PREFIX = "Bearer ";

    private final GatewayBlacklistProperties blacklistProperties;
    private final ObjectMapper objectMapper;

    public GatewayBlacklistGlobalFilter(GatewayBlacklistProperties blacklistProperties, ObjectMapper objectMapper) {
        this.blacklistProperties = blacklistProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!blacklistProperties.isEnabled()) {
            return chain.filter(exchange);
        }

        String clientIp = resolveClientIp(exchange);
        if (contains(blacklistProperties.getIpList(), clientIp)) {
            return writeForbidden(exchange, "当前 IP 已被禁止访问");
        }

        String token = extractToken(exchange.getRequest().getHeaders());
        if (contains(blacklistProperties.getTokenList(), token)) {
            return writeForbidden(exchange, "当前登录凭证已被禁止访问");
        }

        String account = resolveAccount(exchange);
        if (contains(blacklistProperties.getAccountList(), account)) {
            return writeForbidden(exchange, "当前账号已被禁止访问");
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -110;
    }

    private boolean contains(List<String> values, String target) {
        if (target == null || target.isBlank() || values == null || values.isEmpty()) {
            return false;
        }
        String normalizedTarget = target.trim();
        for (String value : values) {
            if (value != null && normalizedTarget.equalsIgnoreCase(value.trim())) {
                return true;
            }
        }
        return false;
    }

    private String resolveAccount(ServerWebExchange exchange) {
        String account = exchange.getRequest().getHeaders().getFirst(HEADER_ACCOUNT);
        if (account != null && !account.isBlank()) {
            return account.trim();
        }
        account = exchange.getRequest().getQueryParams().getFirst("account");
        if (account != null && !account.isBlank()) {
            return account.trim();
        }
        return null;
    }

    private String extractToken(HttpHeaders headers) {
        String token = headers.getFirst(HEADER_TOKEN);
        if (token != null && !token.isBlank()) {
            return token.trim();
        }
        String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        String trimmed = authorization.trim();
        if (trimmed.regionMatches(true, 0, AUTHORIZATION_PREFIX, 0, AUTHORIZATION_PREFIX.length())) {
            return trimmed.substring(AUTHORIZATION_PREFIX.length()).trim();
        }
        return trimmed;
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String xForwardedFor = headers.getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = headers.getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            return "unknown";
        }
        return remoteAddress.getAddress().getHostAddress();
    }

    private Mono<Void> writeForbidden(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set(HEADER_TRACE_ID, exchange.getRequest().getId());

        GatewayResult<Object> result = GatewayResult.error(
                GatewayErrorCode.FORBIDDEN.getCode(),
                message,
                null,
                exchange.getRequest().getId()
        );

        byte[] body = writeBody(result);
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }

    private byte[] writeBody(GatewayResult<Object> result) {
        try {
            return objectMapper.writeValueAsBytes(result);
        } catch (JsonProcessingException e) {
            String fallback = "{\"code\":403,\"msg\":\"当前请求已被禁止访问\",\"data\":null}";
            return fallback.getBytes(StandardCharsets.UTF_8);
        }
    }
}
