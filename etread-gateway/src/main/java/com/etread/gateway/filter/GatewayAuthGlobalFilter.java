package com.etread.gateway.filter;

import com.etread.gateway.auth.TokenAuthService;
import com.etread.gateway.auth.TokenUserContext;
import com.etread.gateway.web.GatewayErrorCode;
import com.etread.gateway.web.GatewayResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class GatewayAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String HEADER_TOKEN = "token";
    private static final String HEADER_TRACE_ID = "X-Trace-Id";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_ACCOUNT = "X-Account";
    private static final String HEADER_NICKNAME = "X-Nickname";
    private static final String HEADER_AVATAR = "X-Avatar";
    private static final String AUTHORIZATION_PREFIX = "Bearer ";

    private final TokenAuthService tokenAuthService;
    private final ObjectMapper objectMapper;

    public GatewayAuthGlobalFilter(TokenAuthService tokenAuthService, ObjectMapper objectMapper) {
        this.tokenAuthService = tokenAuthService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (!requiresAuth(route)) {
            return chain.filter(exchange);
        }

        String token = extractToken(exchange.getRequest().getHeaders());
        if (token == null || token.isBlank()) {
            return writeUnauthorized(exchange, "未检测到登录凭证");
        }

        return tokenAuthService.loadUserByToken(token)
                .flatMap(userContext -> chain.filter(mutateExchange(exchange, token, userContext)))
                .switchIfEmpty(writeUnauthorized(exchange, "登录已过期，请重新登录"));
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean requiresAuth(Route route) {
        if (route == null) {
            return false;
        }
        Map<String, Object> metadata = route.getMetadata();
        Object authRequired = metadata.get("authRequired");
        if (authRequired instanceof Boolean boolValue) {
            return boolValue;
        }
        if (authRequired instanceof String stringValue) {
            return Boolean.parseBoolean(stringValue);
        }
        return true;
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

    private ServerWebExchange mutateExchange(ServerWebExchange exchange, String token, TokenUserContext userContext) {
        ServerHttpRequest mutatedRequest = exchange.getRequest()
                .mutate()
                .header(HEADER_TOKEN, token)
                .header(HEADER_USER_ID, String.valueOf(userContext.getUserId()))
                .headers(headers -> {
                    setHeaderIfPresent(headers, HEADER_ACCOUNT, userContext.getAccount());
                    setHeaderIfPresent(headers, HEADER_NICKNAME, userContext.getNickname());
                    setHeaderIfPresent(headers, HEADER_AVATAR, userContext.getAvatar());
                })
                .build();
        return exchange.mutate().request(mutatedRequest).build();
    }

    private void setHeaderIfPresent(HttpHeaders headers, String headerName, String value) {
        if (value != null && !value.isBlank()) {
            headers.set(headerName, value);
        }
    }

    private Mono<Void> writeUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set(HEADER_TRACE_ID, exchange.getRequest().getId());

        GatewayResult<Object> result = GatewayResult.error(
                GatewayErrorCode.UNAUTHORIZED.getCode(),
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
            String fallback = "{\"code\":401,\"msg\":\"登录已过期，请重新登录\",\"data\":null}";
            return fallback.getBytes(StandardCharsets.UTF_8);
        }
    }
}
