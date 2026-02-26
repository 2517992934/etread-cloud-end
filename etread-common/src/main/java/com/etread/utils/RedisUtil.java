package com.etread.utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
@Component
public class RedisUtil {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    public void set(String key, String value,long timeout, TimeUnit timeUnit) {
        stringRedisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }
    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     *检查key是否存在
     */
    public Boolean hasKey(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    /**
     * 续期
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return stringRedisTemplate.expire(key, timeout, unit);
    }

}
