package org.ticketbooking.common.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.ticketbooking.common.cache.RedisCacheManager;
import org.ticketbooking.common.exception.CommonException;
import org.ticketbooking.common.repository.EventRepository;
import org.ticketbooking.common.repository.TicketRepository;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AvailabilityService {

    @Autowired
    RedisCacheManager cache;

    @Autowired
    EventRepository eventRepository;
    @Autowired
    TicketRepository ticketRepository;

    public long calculateTTLForEvent(LocalDateTime localDateTime) {
        // Calculate TTL based on event's start time
        long secondsToEventStart = Duration.between(LocalDateTime.now(), localDateTime).toSeconds();
        return Math.max(secondsToEventStart, 3600);
    }

    public void handleMissingRedisKeys(Long eventId, String availablityEventKey) throws CommonException {
        // Fallback to database and set Redis key
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CommonException("Event not found"));
        long ttl = calculateTTLForEvent(event.getEndTime());
        cache.setWithTTL(availablityEventKey, String.valueOf(event.getAvailableSeats()), ttl, TimeUnit.SECONDS);
    }

    @Retryable(value = { CommonException.class }, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2))
    public Long blockTemporaryTickets(Long eventId, String availablityEventKey, String blockedEventKey,
            Integer quantity) throws CommonException {
        Long blockedTickets = cache.temporaryReserveTicket(availablityEventKey, blockedEventKey,
                quantity);

        if (blockedTickets == null) {
            throw new CommonException("Redis Lua execution failed");
        } else if (blockedTickets == -2) {
            handleMissingRedisKeys(eventId, availablityEventKey);
            throw new CommonException("Event availability data is missing in Redis, retrying...");
        }

        return blockedTickets;
    }

    @Retryable(value = { CommonException.class }, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2))
    public boolean finalizeBooking(Long eventId, String availablityEventKey, String blockedEventKey, Integer quantity) throws CommonException {
        Long confirmBooking = cache.finalizeBooking(availablityEventKey, blockedEventKey, quantity);

        if (confirmBooking == null) {
            throw new CommonException("Redis Lua execution failed");
        } else if (confirmBooking == -2) {
            handleMissingRedisKeys(eventId, availablityEventKey);
            throw new CommonException("Event availability data is missing in Redis, retrying...");
        }

        return confirmBooking == 1L;
    }

    @Recover
    public Long recoverFromRetries(CommonException e) {
        log.error("Failed to execute Lua script after retries: {}", e.getMessage());
        return null; // Optionally return null or handle failure gracefully
    }

    public void syncAvailabilityToDatabase(Long eventId) {
        String availablityEventKey = "event:" + eventId + ":availability";
        String currentAvailability = cache.get(availablityEventKey);
        // Persist availability to database (asynchronously)
        CompletableFuture.runAsync(() -> {
            Event event = eventRepository.findById(eventId).get();
            event.setAvailableSeats(Integer.parseInt(currentAvailability));
            eventRepository.save(event);
        });
    }

    public void pushTicketToDatabase(Ticket ticket) {
        ticketRepository.save(ticket);
    }

    public boolean saveEventToCache(Event event) {
        String eventKey = "event:" + event.getId();

        Map<String, String> eventMap = new HashMap<>();
        eventMap.put("price", String.valueOf(event.getPrice()));
        eventMap.put("bookingOpen", String.valueOf(event.isBookingOpen()));
        eventMap.put("startTime", event.getStartTime().toString());
        eventMap.put("endTime", event.getEndTime().toString());

        long ttl = calculateTTLForEvent(event.getEndTime()) + 3600;
        cache.saveHashWithTTL(eventKey, eventMap, ttl, TimeUnit.SECONDS);
        return true;
    }

    public Event getEventFromRedis(Long eventId) throws CommonException {
        String eventKey = "event:" + eventId;

        Map<Object, Object> eventMap = cache.getHash(eventKey);
        if (eventMap == null || eventMap.isEmpty()) {
            log.error("Event not found in cache");
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new CommonException("Event not found"));
            saveEventToCache(event);
            return event;
        }

        Event event = new Event();
        event.setId(eventId);
        event.setPrice(Double.parseDouble(eventMap.get("price").toString()));
        event.setStartTime(LocalDateTime.parse((String) eventMap.get("startTime")));
        event.setEndTime(LocalDateTime.parse((String) eventMap.get("endTime")));
        event.setBookingOpen(Boolean.valueOf(eventMap.get("bookingOpen").toString()));

        return event;
    }

}
