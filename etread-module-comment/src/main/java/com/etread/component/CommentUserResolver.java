package com.etread.component;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.etread.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CommentUserResolver {

    private static final String TOKEN_PREFIX = "login:token:";

    @Autowired
    private RedisUtil redisUtil;

    public Long requireUserId(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token 不能为空");
        }
        String userJson = redisUtil.get(TOKEN_PREFIX + token);
        if (userJson == null || userJson.isBlank()) {
            throw new IllegalArgumentException("token 无效或已过期");
        }
        JSONObject json = JSON.parseObject(userJson);
        Long userId = json.getLong("user_id");
        if (userId == null) {
            throw new IllegalArgumentException("token 信息缺少 user_id");
        }
        return userId;
    }
}
