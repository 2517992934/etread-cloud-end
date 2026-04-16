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
        // 1. 获取 Token (优先尝试 "token" 头，兼容 UploadController 的逻辑)
        String token = request.getHeader("token");
        if (!StringUtils.hasText(token)) {
            // 如果 "token" 头没有，再尝试标准的 "Authorization" 头
            token = request.getHeader("Authorization");
        }

        // 2. 判空
        if (!StringUtils.hasText(token)) {
            writeUnauthorizedResponse(response,"未登录，请先登录哦");
            return false;
        }

        // 3. 查 Redis
        String key = "login:token:" + token; // 直接使用 AuthConstant.LOGIN_TOKEN_PREFIX 可能会有问题，这里为了稳妥直接拼接
        if (!redisUtil.hasKey(key)) {
            writeUnauthorizedResponse(response,"抱歉，登录过期了");
            return false;
        }

        // 4. 续期
        redisUtil.expire(key, 30L, TimeUnit.MINUTES);
        return true;
    }
    //编辑报错信息体
    private void writeUnauthorizedResponse(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(401);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"msg\":\"" + msg + "\",\"data\":null}");
        response.getWriter().flush();
    }


}