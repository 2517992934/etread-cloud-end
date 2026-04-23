package com.etread.gateway.ratelimit;

import com.etread.gateway.config.GatewayRateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SlidingWindowRateLimitService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SlidingWindowRateLimitService.class);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RedisScript<Long> slidingWindowRedisScript;

    public SlidingWindowRateLimitService(ReactiveStringRedisTemplate redisTemplate,
                                         RedisScript<Long> slidingWindowRedisScript) {
        this.redisTemplate = redisTemplate;
        this.slidingWindowRedisScript = slidingWindowRedisScript;
    }

    public Mono<RateLimitDecision> check(String ruleName, String businessKey, GatewayRateLimitProperties.Rule rule) {
        long now = System.currentTimeMillis();
        long windowMillis = Math.max(1, safeInt(rule.getWindowSeconds())) * 1000L;
        int maxRequests = Math.max(1, safeInt(rule.getMaxRequests()));
        int lockSeconds = Math.max(0, safeInt(rule.getLockSeconds()));
        String mode = normalizeMode(rule.getMode());

        String rateKey = "gateway:ratelimit:window:" + ruleName + ":" + businessKey;
        String lockKey = "gateway:ratelimit:lock:" + ruleName + ":" + businessKey;
        String member = now + "-" + ThreadLocalRandom.current().nextInt(1_000_000);

        return redisTemplate.execute(
                        slidingWindowRedisScript,
                        List.of(rateKey, lockKey),
                        List.of(
                                String.valueOf(now),
                                String.valueOf(windowMillis),
                                String.valueOf(maxRequests),
                                String.valueOf(lockSeconds),
                                mode,
                                member
                        )
                )
                .next()
                .map(result -> mapDecision(result, mode))
                .switchIfEmpty(Mono.just(RateLimitDecision.allow()))
                .onErrorResume(ex -> {
                    LOGGER.error("Rate limit script execution failed, ruleName={}, key={}", ruleName, businessKey, ex);
                    return Mono.just(RateLimitDecision.allow());
                });
    }

    private RateLimitDecision mapDecision(Long result, String mode) {
        if (result == null || result == 1L) {
            return RateLimitDecision.allow();
        }
        if (result == 2L) {
            return RateLimitDecision.locked("操作过于频繁，已临时锁定，请稍后再试");
        }
        if ("lockable".equals(mode)) {
            return RateLimitDecision.reject("请求过于频繁，已触发保护，请稍后再试");
        }
        return RateLimitDecision.reject("请求过于频繁，请稍后再试");
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String normalizeMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return "rejectOnly";
        }
        return mode.trim();
    }
}
