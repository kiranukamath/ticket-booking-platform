package org.ticketbooking.common.cache;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

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

    public Map<Object,Object> getHash(String eventKey) {
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

    private final String LUA_SCRIPT_TEMPORARY_TICKET_RESERVE_v0 = """
            local available = tonumber(redis.call('GET',KEYS[1]))
            local initialcapacity = tonumber(redis.call('GET',KEYS[2]))
            local buffer_percent = tonumber(ARGV[2])
            local requested = tonumber(ARGV[1])

            if not available then
                return -2;
            end

            local buffer = 0;
            if initialcapacity then
                buffer = math.floor((buffer_percent/100)* tonumber(initialcapacity))
            end

            local extralimit = buffer

            if available + extralimit >= requested then
                redis.call('DECRBY', KEYS[1],requested)
                return available - requested;
            else
                return -1;
            end
            """;

    private final String LUA_SCRIPT_TEMPORARY_TICKET_RESERVE = """
            local availability = tonumber(redis.call('GET',KEYS[1]))
            local blocked = tonumber(redis.call('GET',KEYS[2])) or 0
            local requested = tonumber(ARGV[1])
            local blockLimit = math.floor(0.90 * blocked)

            if not availability then
                return -2;
            end

            if (blockLimit+requested) <= availability then
                if blocked == 0 then
                    redis.call('SET', KEYS[2], 0)
                end

                redis.call('INCRBY', KEYS[2],requested)

                redis.call('EXPIRE', KEYS[2], 60)

                return blocked + requested;
            else
                return -1;
            end
            """;
    private final String LUA_FINALIZE_TICKET = """
            local availability = tonumber(redis.call('GET',KEYS[1]))
            local requested = tonumber(ARGV[1])
            local blocked = tonumber(redis.call('GET',KEYS[2])) or 0

            if not availability then
                return -2;
            end

            if availability < requested then
                return -1;
            end

            redis.call('DECRBY', KEYS[1], requested)
            
            if blocked > 0 then
                redis.call('DECRBY', KEYS[2], requested)
            end

            return 1
            """;

    DefaultRedisScript<Long> temporaryReserveTicket = new DefaultRedisScript<>();
    DefaultRedisScript<Long> confirmTicket = new DefaultRedisScript<>();

    @PostConstruct
    public void luaScriptSetup() {
        temporaryReserveTicket.setScriptText(LUA_SCRIPT_TEMPORARY_TICKET_RESERVE);
        temporaryReserveTicket.setResultType(Long.class);

        confirmTicket.setScriptText(LUA_FINALIZE_TICKET);
        confirmTicket.setResultType(Long.class);
    }

    public Long temporaryReserveTicket(String availablityEventKey, String blockedEventKey, Integer quantity) {
        Long redisResult = redisTemplate.execute(temporaryReserveTicket, Arrays.asList(availablityEventKey, blockedEventKey),
                        String.valueOf(quantity));
        return redisResult;
    }

    public Long finalizeBooking(String availablityEventKey, String blockedEventKey, Integer quantity){
        Long redisResult = redisTemplate.execute(confirmTicket, Arrays.asList(availablityEventKey, blockedEventKey),
                        String.valueOf(quantity));
        return redisResult;
    }
}
