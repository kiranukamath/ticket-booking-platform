package org.ticketbooking.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.ticketbooking.common.cache.RedisCacheManager;
import org.ticketbooking.common.exception.CommonException;
import org.ticketbooking.common.model.AvailabilityService;
import org.ticketbooking.common.model.Event;
import org.ticketbooking.common.repository.EventRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    RedisCacheManager cache;

    @Autowired
    AvailabilityService availabilityService;

    public Event createEvent(Event event) {
        Event savedEvent = eventRepository.save(event);

        String eventKey = "event:" + savedEvent.getId() + ":availability";
        cache.setWithTTL(eventKey, String.valueOf(savedEvent.getAvailableSeats()), availabilityService.calculateTTLForEvent(savedEvent.getEndTime()), TimeUnit.SECONDS);
        availabilityService.saveEventToCache(savedEvent);
        return savedEvent;
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public Event getEventById(Long id) throws CommonException {
        return eventRepository.findById(id)
                    .orElseThrow(() -> new CommonException("Event not found"));
    }

    public Event updateEvent(Long id, Map<String,Object> requestBody) throws CommonException {
        Event event =  eventRepository.findById(id).orElseThrow(() -> new CommonException("Event not found",HttpStatus.NOT_FOUND));
        
        if(requestBody.containsKey("name")) {
            event.setName((String) requestBody.get("name"));
        }
        if(requestBody.containsKey("description")) {
            event.setDescription((String) requestBody.get("description"));
        }
        if(requestBody.containsKey("location")) {
            event.setLocation((String) requestBody.get("location"));
        }
        if(requestBody.containsKey("startTime")) {
            event.setStartTime((LocalDateTime) requestBody.get("startTime"));
        }
        if(requestBody.containsKey("endTime")) {
            event.setEndTime((LocalDateTime) requestBody.get("endTime"));
        }
        if(requestBody.containsKey("capacity")) {
            event.setCapacity((Integer) requestBody.get("capacity"));
        }
        
        if(requestBody.containsKey("price")) {
            event.setPrice((Double) requestBody.get("price"));
        }

        if(requestBody.containsKey("availableSeats")){
            event.setAvailableSeats((Integer) requestBody.get("availableSeats"));
        }

        Event savedEvent = eventRepository.save(event);
        availabilityService.saveEventToCache(savedEvent);
        
        if (requestBody.containsKey("availableSeats")) {
            // Update available seats in Redis - actual key
            String eventKey = "event:" + savedEvent.getId() + ":availability";
            cache.setWithTTL(eventKey, String.valueOf(savedEvent.getAvailableSeats()),
                    availabilityService.calculateTTLForEvent(savedEvent.getEndTime()), TimeUnit.SECONDS); // availability
        }
        return savedEvent;
    }

    public Event makeBookingOpen(Long id) throws CommonException {
        Event event = eventRepository.findById(id).orElseThrow(() -> new CommonException("Event not found",HttpStatus.NOT_FOUND));
        event.setBookingOpen(true);
        Event savedEvent = eventRepository.save(event);
        availabilityService.saveEventToCache(savedEvent);
        return savedEvent;
    }

    public void deleteEvent(Long id) {
        String eventKey = "event:" + id;
        eventRepository.deleteById(id);
        cache.delete(eventKey);
    }

    
    
}
