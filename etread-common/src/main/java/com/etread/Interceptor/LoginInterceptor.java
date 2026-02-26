package com.etread.Interceptor;

import com.etread.constant.AuthConstant;
import com.etread.utils.RedisUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.concurrent.TimeUnit;

// 这里不加 @Component，通过配置类统一管理
public class LoginInterceptor implements HandlerInterceptor {

    private RedisUtil redisUtil;

    public LoginInterceptor(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取 Token
        String token = request.getHeader("Authorization");

        // 2. 判空
        if (!StringUtils.hasText(token)) {
            response.setStatus(401);
            return false;
        }

        // 3. 查 Redis
        String key = AuthConstant.LOGIN_TOKEN_PREFIX + token;
        if (!redisUtil.hasKey(key)) {
            response.setStatus(401);
            return false;
        }

        // 4. 续期
        redisUtil.expire(key, AuthConstant.LOGIN_TOKEN_TTL, TimeUnit.MINUTES);
        return true;
    }
}