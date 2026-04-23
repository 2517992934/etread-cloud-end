package com.etread.gateway.auth;

import com.etread.constant.AuthConstant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TokenAuthService {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public TokenAuthService(ReactiveStringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Mono<TokenUserContext> loadUserByToken(String token) {
        if (token == null || token.isBlank()) {
            return Mono.empty();
        }

        String redisKey = AuthConstant.LOGIN_TOKEN_PREFIX + token;
        return redisTemplate.opsForValue()
                .get(redisKey)
                .flatMap(this::parseUserContext)
                .onErrorResume(ex -> Mono.empty());
    }

    private Mono<TokenUserContext> parseUserContext(String userJson) {
        try {
            JsonNode jsonNode = objectMapper.readTree(userJson);
            JsonNode userIdNode = jsonNode.get("user_id");
            if (userIdNode == null || userIdNode.isNull()) {
                return Mono.empty();
            }

            TokenUserContext context = new TokenUserContext();
            context.setUserId(userIdNode.asLong());
            context.setAccount(readText(jsonNode, "account"));
            context.setNickname(readText(jsonNode, "nickname"));
            context.setAvatar(readText(jsonNode, "avatar"));
            return Mono.just(context);
        } catch (Exception ex) {
            return Mono.empty();
        }
    }

    private String readText(JsonNode jsonNode, String fieldName) {
        JsonNode field = jsonNode.get(fieldName);
        return field == null || field.isNull() ? null : field.asText();
    }
}
