package com.etread.component;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.etread.dto.UserDTO;
import com.etread.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
/*
*token chedck
 */
@Component
public class BookUserResolver {

    private static final String TOKEN_PREFIX = "login:token:";

    @Autowired
    private RedisUtil redisUtil;

    public Long requireUserId(String token) {
        UserDTO user = requireUser(token);
        if (user.getUser_id() == null) {
            throw new IllegalArgumentException("token 信息缺少 user_id");
        }
        return user.getUser_id();
    }

    public String requireNickname(String token) {
        UserDTO user = requireUser(token);
        String nickname = user.getNickname();
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("token 信息缺少 nickname");
        }
        return nickname.trim();
    }

    public UserDTO requireUser(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token 不能为空");
        }
        String userJson = redisUtil.get(TOKEN_PREFIX + token);
        if (userJson == null || userJson.isBlank()) {
            throw new IllegalArgumentException("token 无效或已过期");
        }
        UserDTO user = JSON.parseObject(userJson, UserDTO.class);
        if (user == null) {
            throw new IllegalArgumentException("token 用户信息解析失败");
        }
        return user;
    }
}
