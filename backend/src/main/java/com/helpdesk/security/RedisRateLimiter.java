package com.helpdesk.security;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisRateLimiter {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisRateLimiter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean allow(
            String key,
            int limit,
            Duration window
    ) {
        Long count = redisTemplate.opsForValue().increment(key);

        if (count == null) {
            return false;
        }

        if (count == 1) {
            redisTemplate.expire(key, window);
        }

        return count <= limit;
    }

    public long retryAfterSeconds(String key) {
        Long ttl = redisTemplate.getExpire(
                key,
                java.util.concurrent.TimeUnit.SECONDS
        );

        if (ttl == null || ttl < 0) {
            return 1;
        }

        return Math.max(1, ttl);
    }
}
