package com.etread.gateway.web;

public enum GatewayErrorCode {
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "请求的资源不存在"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    SERVICE_UNAVAILABLE(503, "服务暂时不可用"),
    GATEWAY_TIMEOUT(504, "网关请求超时"),
    INTERNAL_ERROR(500, "网关内部异常");

    private final int code;
    private final String defaultMessage;

    GatewayErrorCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
