package com.etread.exception;

import com.etread.Result; // 假设你有 Result 类
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j // 自动注入 log 对象
@RestControllerAdvice // 这是一个增强版的 Controller，专门处理异常
public class GlobalExceptionHandler {

    /**
     * 捕获所有 RuntimeException
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<String> handleRuntimeException(RuntimeException e) {
        // 1. 记录错误日志 (存到文件里)
        log.error("系统出错了: ", e);

        // 2. 返回漂亮的 JSON 给前端
        return Result.error("服务器错误: " + e.getMessage());
    }

    /**
     * 捕获所有 Exception (兜底)
     */
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("未知错误: ", e);
        return Result.error( "系统繁忙，请稍后再试 (正在抢修中...)");
    }
}