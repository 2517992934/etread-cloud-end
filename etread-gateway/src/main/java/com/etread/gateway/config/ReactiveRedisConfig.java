package com.etread.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
public class ReactiveRedisConfig {

    @Bean
    public RedisScript<Long> slidingWindowRedisScript() {
        String script = """
                local rateKey = KEYS[1]
                local lockKey = KEYS[2]
                local now = tonumber(ARGV[1])
                local windowMillis = tonumber(ARGV[2])
                local maxRequests = tonumber(ARGV[3])
                local lockSeconds = tonumber(ARGV[4])
                local mode = ARGV[5]
                local member = ARGV[6]
                local ttlSeconds = math.max(math.ceil(windowMillis / 1000), 1) + 1

                if mode == 'lockable' then
                    local locked = redis.call('GET', lockKey)
                    if locked then
                        return 2
                    end
                end

                local minScore = now - windowMillis
                redis.call('ZREMRANGEBYSCORE', rateKey, 0, minScore)
                local current = redis.call('ZCARD', rateKey)

                if current >= maxRequests then
                    if mode == 'lockable' and lockSeconds > 0 then
                        redis.call('SET', lockKey, '1', 'EX', lockSeconds)
                    end
                    return 0
                end

                redis.call('ZADD', rateKey, now, member)
                redis.call('EXPIRE', rateKey, ttlSeconds)
                return 1
                """;
        return RedisScript.of(script, Long.class);
    }
}
