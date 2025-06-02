package org.ticketbooking.common.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.ticketbooking.common.cache.RedisCacheManager;
import org.ticketbooking.common.exception.CommonException;
import org.ticketbooking.common.repository.EventRepository;
import org.ticketbooking.common.repository.TicketRepository;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EventHelper {

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

    public void isSeatsAvailable(BookingMessage ticket, String bookingRef, Long eventId) throws CommonException {
        boolean success = cache.isSeatsAvailable(eventId, ticket.getSeatIds(), bookingRef);
        if(success) {
            log.info("Seats are available for booking: " + bookingRef);
        } else {
            log.error("Seats are not available for booking: " + bookingRef);
            throw new CommonException("Seats are not available for booking");
        }
    }

}
