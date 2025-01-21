package org.ticketbooking.common.cache;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisCacheManager {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // Set key with TTL
    public void setWithTTL(String key, String value, long ttl, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, ttl, timeUnit);
    }

    // Get value by key
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // Increment value by key
    public void increment(String key, long value) {
        redisTemplate.opsForValue().increment(key, value);
    }

    // Decrement value by key
    public void decrement(String key, long value) {
        redisTemplate.opsForValue().decrement(key, value);
    }

    // Distributed Lock
    public boolean tryLock(String lockKey, String lockValue, long ttl, TimeUnit timeUnit) {
        return redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, ttl, timeUnit);
    }

    // Release Lock
    public void releaseLock(String lockKey, String lockValue) {
        String currentValue = redisTemplate.opsForValue().get(lockKey);
        if (lockValue.equals(currentValue)) {
            redisTemplate.delete(lockKey);
        }
    }
}
