package com.quradar.common;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * Fixed-window rate limiter on Redis (INCR + EXPIRE in one Lua script, so the
 * check-and-count is atomic). Fail-open: any Redis error allows the request and
 * logs, because throttling must never take down ingestion or login.
 */
@Component
public class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    private static final String LUA =
            "local current = redis.call('INCR', KEYS[1]) "
            + "if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end "
            + "if current > tonumber(ARGV[2]) then return 0 else return 1 end";

    private final StringRedisTemplate redis;

    public RateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean allow(String key, int limit, int windowSeconds) {
        try {
            Long allowed = redis.execute(new DefaultRedisScript<>(LUA, Long.class),
                    List.of(key), String.valueOf(windowSeconds), String.valueOf(limit));
            return allowed != null && allowed == 1L;
        } catch (Exception ex) {
            log.warn("Redis unavailable, bypassing rate limit", ex);
            return true;
        }
    }
}
