package org.ticketbooking.common.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.ticketbooking.common.cache.RedisCacheManager;
import org.ticketbooking.common.exception.CommonException;
import org.ticketbooking.common.repository.EventRepository;

@Component
public class Utils {

    @Autowired
    RedisCacheManager cache;

    @Autowired
    EventRepository eventRepository;

    public int getAvailableSeats(Long eventId) throws CommonException {
        String eventKey = "event:" + eventId + ":availability";
        String availableSeats = cache.get(eventKey);
    
        if (availableSeats == null) {
            // Fallback to database and set Redis key
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new CommonException("Event not found"));
            // Set TTL based on event start time (if not already set)
            long ttl = calculateTTLForEvent(event);
            cache.setWithTTL(eventKey, String.valueOf(event.getAvailableSeats()), ttl, TimeUnit.SECONDS);

            return event.getAvailableSeats();
        }
    
        return Integer.parseInt(availableSeats);
    }

    public long calculateTTLForEvent(Event event) {
        // Calculate TTL based on event's start time
        return Duration.between(LocalDateTime.now(), event.getStartTime()).toSeconds();
    }
    
}
