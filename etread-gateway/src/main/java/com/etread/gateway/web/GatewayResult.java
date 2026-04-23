package com.etread.gateway.web;

public class GatewayResult<T> {

    private Integer code;
    private String msg;
    private T data;
    private String traceId;
    private Long timestamp;

    public static <T> GatewayResult<T> success(String msg, T data, String traceId) {
        GatewayResult<T> result = new GatewayResult<>();
        result.setCode(200);
        result.setMsg(msg);
        result.setData(data);
        result.setTraceId(traceId);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }

    public static <T> GatewayResult<T> error(int code, String msg, T data, String traceId) {
        GatewayResult<T> result = new GatewayResult<>();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(data);
        result.setTraceId(traceId);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
