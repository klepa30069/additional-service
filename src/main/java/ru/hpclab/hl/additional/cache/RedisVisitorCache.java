package ru.hpclab.hl.additional.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.hpclab.hl.additional.model.Visitor;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RedisVisitorCache {
    private static final Logger logger = LoggerFactory.getLogger(RedisVisitorCache.class);
    private final RedisTemplate<String, Visitor> redisTemplate;
    private static final String CACHE_PREFIX = "visitor:";
    private static final long TTL = 3600;

    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);

    public RedisVisitorCache(
            @Qualifier("visitorRedisTemplate") RedisTemplate<String, Visitor> redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    public Visitor get(UUID visitorId) {
        try {
            String key = CACHE_PREFIX + visitorId;
            Visitor visitor = redisTemplate.opsForValue().get(key);

            if (visitor != null) {
                hits.incrementAndGet();
                logger.debug("Cache HIT for visitor ID: {}", visitorId);
            } else {
                misses.incrementAndGet();
                logger.debug("Cache MISS for visitor ID: {}", visitorId);
            }

            return visitor;
        } catch (Exception e) {
            misses.incrementAndGet();
            logger.error("Error getting visitor {} from cache", visitorId, e);
            return null;
        }
    }

    public void put(UUID visitorId, Visitor visitor) {
        try {
            String key = CACHE_PREFIX + visitorId;
            redisTemplate.opsForValue().set(key, visitor, TTL, TimeUnit.SECONDS);
            logger.debug("Cached visitor ID: {}", visitorId);
        } catch (Exception e) {
            logger.error("Error caching visitor {}", visitorId, e);
        }
    }

    public void clear() {
        try {
            logger.info("Clearing Redis visitor cache");
            redisTemplate.execute((RedisCallback<Boolean>) connection -> {
                connection.keyCommands().del(CACHE_PREFIX.getBytes());
                return true;
            });
        } catch (Exception e) {
            logger.error("Error clearing cache", e);
        }
    }

    @Scheduled(fixedRateString = "${cache.stats.print.interval:60000}")
    public void printStats() {
        try {
            Set<byte[]> keys = redisTemplate.execute((RedisCallback<Set<byte[]>>) connection ->
                connection.keyCommands().keys(CACHE_PREFIX.getBytes())
            );
            long size = keys != null ? keys.size() : 0;

            long total = hits.get() + misses.get();
            double hitRate = total > 0 ? (hits.get() * 100.0 / total) : 0;

            logger.info("Visitor Cache Stats:");
            logger.info("Redis keys count: {}", size);
            logger.info("Misses: {}", misses.get());
        } catch (Exception e) {
            logger.error("Error printing cache stats", e);
        }
    }

    public long size() {
        try {
            Set<byte[]> keys = redisTemplate.execute((RedisCallback<Set<byte[]>>) connection ->
                connection.keyCommands().keys(CACHE_PREFIX.getBytes())
            );
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            logger.error("Error getting cache size", e);
            return -1;
        }
    }
}

