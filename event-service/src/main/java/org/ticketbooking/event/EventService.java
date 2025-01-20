package org.ticketbooking.event;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.ticketbooking.common.exception.CommonException;
import org.ticketbooking.common.model.Event;
import org.ticketbooking.common.repository.EventRepository;

@Service
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public Event createEvent(Event event) {
        Event savedEvent = eventRepository.save(event);

        // Set initial available seats in Redis
        String eventKey = "event:" + savedEvent.getId() + ":availability";
        redisTemplate.opsForValue().set(eventKey, String.valueOf(savedEvent.getAvailableSeats()));

        return savedEvent;
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public Optional<Event> getEventById(Long id) {
        return eventRepository.findById(id);
    }

    public Event updateEvent(Long id, Event updatedEvent) throws CommonException {
        return eventRepository.findById(id).map(event -> {
            if(updatedEvent.getName() != null){
                event.setName(updatedEvent.getName());
            }
            if(updatedEvent.getDescription() != null){
                event.setDescription(updatedEvent.getDescription());
            }
            if(updatedEvent.getLocation() != null){
                event.setLocation(updatedEvent.getLocation());
            }

            event.setStartTime(updatedEvent.getStartTime());
            event.setEndTime(updatedEvent.getEndTime());
            event.setCapacity(updatedEvent.getCapacity());
            event.setAvailableSeats(updatedEvent.getAvailableSeats());
            event.setPrice(updatedEvent.getPrice());
            Event savedEvent = eventRepository.save(event);

            // Update available seats in Redis
            String eventKey = "event:" + savedEvent.getId() + ":availability";
            redisTemplate.opsForValue().set(eventKey, String.valueOf(savedEvent.getAvailableSeats()));

            return savedEvent;
        }).orElseThrow(() -> new CommonException("Event not found",HttpStatus.NOT_FOUND));
    }

    public Event makeBookingOpen(Long id) throws CommonException {
        return eventRepository.findById(id).map(event -> {
            event.setBookingOpen(true);
            return eventRepository.save(event);
        }).orElseThrow(() -> new CommonException("Event not found",HttpStatus.NOT_FOUND));
    }

    public void deleteEvent(Long id) {
        eventRepository.deleteById(id);
    }
    
}
