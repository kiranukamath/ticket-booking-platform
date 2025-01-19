package org.ticketbooking.event;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.ticketbooking.common.exception.CommonException;
import org.ticketbooking.common.model.Event;
import org.ticketbooking.common.repository.EventRepository;

@Service
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    public Event createEvent(Event event) {
        return eventRepository.save(event);
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
            return eventRepository.save(event);
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
