package com.quradar.ingestion;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Fast-path duplicate filter scoped by (device, event) to mirror the DB unique
 * constraint. Redis is a cache here, NOT the source of truth: a hit
 * short-circuits with 409, a miss (or Redis outage) falls through to the
 * constraint, which makes the final call. Keys are written only after the
 * surrounding transaction commits, so a rolled-back attempt never poisons the
 * cache. Fail-open: Redis errors degrade to DB-only mode.
 */
@Component
public class IdempotencyCache {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyCache.class);

    private final StringRedisTemplate redis;
    private final long ttlHours;

    public IdempotencyCache(StringRedisTemplate redis,
                            @Value("${quradar.idempotency-ttl-hours:24}") long ttlHours) {
        this.redis = redis;
        this.ttlHours = ttlHours;
    }

    public boolean seen(String eventId, String deviceCode) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(key(eventId, deviceCode)));
        } catch (Exception ex) {
            log.warn("Redis unavailable, falling back to DB idempotency check", ex);
            return false;
        }
    }

    public void mark(String eventId, String deviceCode) {
        try {
            redis.opsForValue().setIfAbsent(key(eventId, deviceCode), "1",
                    Duration.ofHours(ttlHours));
        } catch (Exception ex) {
            log.warn("Redis unavailable, skipping idempotency cache write", ex);
        }
    }

    private static String key(String eventId, String deviceCode) {
        return "idem:event:" + (deviceCode != null ? deviceCode : "-") + ":" + eventId;
    }
}
