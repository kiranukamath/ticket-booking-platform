package org.ticketbooking.common.cache;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
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

    public void saveHashWithTTL(String eventKey, Map<String, String> eventMap, long ttl, TimeUnit seconds) {
        redisTemplate.opsForHash().putAll(eventKey, eventMap);
        // Set TTL for the event key
        redisTemplate.expire(eventKey, Duration.ofSeconds(ttl));
    }

    public Map<Object, Object> getHash(String eventKey) {
        return redisTemplate.opsForHash().entries(eventKey);
    }

    // Get value by key
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public boolean delete(String key) {
        return redisTemplate.delete(key);
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
    @SuppressWarnings("null")
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

    public String getSeatKey(Long eventId, String seatId) {
        return "event:" + eventId + ":seat:" + seatId;
    }

    public boolean reserveSeats(Long eventId, Set<String> seatIds, String bookingRef, Duration ttl) {
        for (String seatId : seatIds) {
            String key = getSeatKey(eventId, seatId);
            
            redisTemplate.opsForValue().set(key, bookingRef, ttl);
            
        }
        return true;
    }

    public boolean lockSeats(Long eventId, Set<String> seatIds, String bookingRef, Duration ttl) {
        for (String seatId : seatIds) {
            String key = getSeatKey(eventId, seatId);
            Boolean success = redisTemplate.opsForValue().setIfAbsent(key, bookingRef, ttl);
            if (Boolean.FALSE.equals(success)) {
                unlockSeats(eventId, seatIds, bookingRef); // rollback
                return false;
            }
        }
        return true;
    }

    public boolean isSeatsAvailable(Long eventId, Set<String> seatIds, String bookingRef) {
        for (String seatId : seatIds) {
            String key = getSeatKey(eventId, seatId);
            String value = redisTemplate.opsForValue().get(key);
            if (!bookingRef.equals(value)) {
                return false; // Someone else has the seat or it expired
            }
        }
        return true;
    }

    public void unlockSeats(Long eventId, Set<String> seatIds, String bookingRef) {
        for (String seatId : seatIds) {
            String key = getSeatKey(eventId, seatId);
            String value = redisTemplate.opsForValue().get(key);
            if (bookingRef.equals(value)) {
                redisTemplate.delete(key);
            }
        }
    }
}
