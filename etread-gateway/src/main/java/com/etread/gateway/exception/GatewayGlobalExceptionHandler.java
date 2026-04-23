package com.etread.gateway.exception;

import com.etread.gateway.web.GatewayErrorCode;
import com.etread.gateway.web.GatewayResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

@Component
@Order(-2)
public class GatewayGlobalExceptionHandler implements WebExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayGlobalExceptionHandler.class);

    private final ObjectMapper objectMapper;

    public GatewayGlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        GatewayErrorCode errorCode = resolveErrorCode(ex);
        String message = resolveMessage(ex, errorCode);
        String traceId = exchange.getRequest().getId();

        LOGGER.error("Gateway request failed, traceId={}, path={}, message={}",
                traceId,
                exchange.getRequest().getURI().getPath(),
                ex.getMessage(),
                ex);

        response.setStatusCode(resolveHttpStatus(errorCode));
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set("X-Trace-Id", traceId);

        GatewayResult<Object> result = GatewayResult.error(
                errorCode.getCode(),
                message,
                null,
                traceId
        );

        byte[] body = writeBody(result);
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }

    private GatewayErrorCode resolveErrorCode(Throwable ex) {
        if (ex instanceof ServerWebInputException) {
            return GatewayErrorCode.BAD_REQUEST;
        }
        if (ex instanceof ResponseStatusException responseStatusException) {
            return mapStatus(responseStatusException.getStatusCode());
        }
        if (ex instanceof ErrorResponseException errorResponseException) {
            return mapStatus(errorResponseException.getStatusCode());
        }
        if (ex instanceof NotFoundException) {
            return GatewayErrorCode.SERVICE_UNAVAILABLE;
        }
        if (ex instanceof TimeoutException) {
            return GatewayErrorCode.GATEWAY_TIMEOUT;
        }
        Throwable cause = ex.getCause();
        if (cause instanceof TimeoutException) {
            return GatewayErrorCode.GATEWAY_TIMEOUT;
        }
        return GatewayErrorCode.INTERNAL_ERROR;
    }

    private GatewayErrorCode mapStatus(HttpStatusCode statusCode) {
        int value = statusCode.value();
        if (value == 400) {
            return GatewayErrorCode.BAD_REQUEST;
        }
        if (value == 401) {
            return GatewayErrorCode.UNAUTHORIZED;
        }
        if (value == 403) {
            return GatewayErrorCode.FORBIDDEN;
        }
        if (value == 404) {
            return GatewayErrorCode.NOT_FOUND;
        }
        if (value == 429) {
            return GatewayErrorCode.TOO_MANY_REQUESTS;
        }
        if (value == 503) {
            return GatewayErrorCode.SERVICE_UNAVAILABLE;
        }
        if (value == 504) {
            return GatewayErrorCode.GATEWAY_TIMEOUT;
        }
        return GatewayErrorCode.INTERNAL_ERROR;
    }

    private HttpStatus resolveHttpStatus(GatewayErrorCode errorCode) {
        return HttpStatus.valueOf(errorCode.getCode());
    }

    private String resolveMessage(Throwable ex, GatewayErrorCode errorCode) {
        if (ex instanceof ResponseStatusException responseStatusException) {
            String reason = responseStatusException.getReason();
            if (reason != null && !reason.isBlank()) {
                return reason;
            }
        }
        if (ex instanceof ServerWebInputException) {
            return "请求参数格式错误";
        }
        if (ex instanceof NotFoundException) {
            return "目标服务暂时不可用";
        }
        if (ex instanceof TimeoutException || ex.getCause() instanceof TimeoutException) {
            return "下游服务响应超时";
        }
        return errorCode.getDefaultMessage();
    }

    private byte[] writeBody(GatewayResult<Object> result) {
        try {
            return objectMapper.writeValueAsBytes(result);
        } catch (JsonProcessingException e) {
            String fallback = "{\"code\":500,\"msg\":\"网关异常序列化失败\",\"data\":null}";
            return fallback.getBytes(StandardCharsets.UTF_8);
        }
    }
}
