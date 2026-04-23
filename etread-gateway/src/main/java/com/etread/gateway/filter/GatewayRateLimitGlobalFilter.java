package com.etread.gateway.filter;

import com.etread.gateway.config.GatewayRateLimitProperties;
import com.etread.gateway.ratelimit.RateLimitDecision;
import com.etread.gateway.ratelimit.SlidingWindowRateLimitService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class GatewayRateLimitGlobalFilter implements GlobalFilter, Ordered {

    private static final String HEADER_TRACE_ID = "X-Trace-Id";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String STRATEGY_ACCOUNT_IP = "account_ip";
    private static final String STRATEGY_USER_URI = "user_uri";
    private static final String STRATEGY_IP_URI = "ip_uri";

    private final GatewayRateLimitProperties rateLimitProperties;
    private final SlidingWindowRateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    public GatewayRateLimitGlobalFilter(GatewayRateLimitProperties rateLimitProperties,
                                        SlidingWindowRateLimitService rateLimitService,
                                        ObjectMapper objectMapper) {
        this.rateLimitProperties = rateLimitProperties;
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String ruleName = resolveRuleName(route);
        if (ruleName == null || ruleName.isBlank()) {
            return chain.filter(exchange);
        }

        GatewayRateLimitProperties.Rule rule = rateLimitProperties.getRules().get(ruleName);
        if (rule == null) {
            return chain.filter(exchange);
        }

        return buildBusinessKey(exchange, ruleName, rule)
                .flatMap(key -> rateLimitService.check(ruleName, key, rule))
                .flatMap(decision -> {
                    if (decision.isAllowed()) {
                        return chain.filter(exchange);
                    }
                    return writeLimitedResponse(exchange, decision);
                });
    }

    @Override
    public int getOrder() {
        return -90;
    }

    private String resolveRuleName(Route route) {
        if (route == null) {
            return null;
        }
        Map<String, Object> metadata = route.getMetadata();
        Object ruleName = metadata.get("rateLimitRule");
        return ruleName == null ? null : String.valueOf(ruleName);
    }

    private Mono<String> buildBusinessKey(ServerWebExchange exchange,
                                          String ruleName,
                                          GatewayRateLimitProperties.Rule rule) {
        String strategy = normalizeStrategy(rule.getKeyStrategy());
        String path = normalize(exchange.getRequest().getPath().value());
        String ip = normalize(resolveClientIp(exchange));

        if (STRATEGY_ACCOUNT_IP.equals(strategy)) {
            String account = exchange.getRequest().getQueryParams().getFirst("account");
            if (account != null && !account.isBlank()) {
                return Mono.just(ruleName + ":" + strategy + ":" + normalize(account) + ":" + ip);
            }
            return Mono.just(ruleName + ":ip_uri:" + ip + ":" + path);
        }

        if (STRATEGY_USER_URI.equals(strategy)) {
            String userId = exchange.getRequest().getHeaders().getFirst(HEADER_USER_ID);
            if (userId != null && !userId.isBlank()) {
                return Mono.just(ruleName + ":" + strategy + ":" + normalize(userId) + ":" + path);
            }
            return Mono.just(ruleName + ":ip_uri:" + ip + ":" + path);
        }

        return Mono.just(ruleName + ":" + STRATEGY_IP_URI + ":" + ip + ":" + path);
    }

    private String normalizeStrategy(String strategy) {
        if (strategy == null || strategy.isBlank()) {
            return STRATEGY_IP_URI;
        }
        return strategy.trim().toLowerCase();
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

    private String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "unknown";
        }
        return raw.trim()
                .replace(':', '_')
                .replace('/', '_')
                .replace(' ', '_');
    }

    private Mono<Void> writeLimitedResponse(ServerWebExchange exchange, RateLimitDecision decision) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set(HEADER_TRACE_ID, exchange.getRequest().getId());

        GatewayResult<Object> result = GatewayResult.error(
                GatewayErrorCode.TOO_MANY_REQUESTS.getCode(),
                decision.getMessage(),
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
            String fallback = "{\"code\":429,\"msg\":\"请求过于频繁，请稍后再试\",\"data\":null}";
            return fallback.getBytes(StandardCharsets.UTF_8);
        }
    }
}
