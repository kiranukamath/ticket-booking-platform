package org.ticketbooking.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ticketbooking.common.exception.CommonException;
import org.ticketbooking.common.model.BookingRequest;
import org.ticketbooking.common.model.BookingResponse;
import org.ticketbooking.common.model.Event;
import org.ticketbooking.common.model.Ticket;
import org.ticketbooking.common.repository.EventRepository;
import org.ticketbooking.common.repository.TicketRepository;
import org.ticketbooking.common.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class BookingService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public BookingResponse bookTicket(BookingRequest request) throws CommonException {
        // Validate if event exists
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new CommonException("Event not found"));

        // Check if booking is allowed for this event
        if (!event.isBookingOpen() || event.getStartTime().isBefore(LocalDateTime.now())) {
            throw new CommonException("Booking is not allowed for this event yet or it has already started");
        }

        // Check for seat availability
        if (event.getAvailableSeats() < request.getQuantity()) {
            throw new CommonException("Not enough seats available");
        }

        // Decrease available seats
        event.setAvailableSeats(event.getAvailableSeats() - request.getQuantity());
        eventRepository.save(event);

        // Save the booking details
        Ticket ticket = new Ticket();
        ticket.setUser(userRepository.findById(3L).orElseThrow(() -> new CommonException("User not found")));
        ticket.setEvent(event);
        ticket.setPrice(new BigDecimal(event.getPrice()).multiply(new BigDecimal(request.getQuantity())));
        ticket.setQuantity(request.getQuantity());
        ticket.setStatus("CONFIRMED");

        Ticket savedTicket = ticketRepository.save(ticket);

        // Prepare and return response
        return new BookingResponse(savedTicket.getId(), ticket.getUser().getId(), event.getId(), ticket.getStatus(),
                ticket.getPrice(),ticket.getQuantity());
    }

    @Transactional
    public void cancelTicket(Long ticketId) throws CommonException {
        // Fetch the ticket by its ID
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new CommonException("Ticket not found"));

        // Check the status of the ticket
        if (!ticket.getStatus().equals("PENDING") && !ticket.getStatus().equals("CONFIRMED")) {
            throw new CommonException("Ticket cannot be cancelled");
        }

        // Update ticket status to "CANCELLED"
        ticket.setStatus("CANCELLED");
        ticketRepository.save(ticket);

        // Increase the available seats in the event
        Event event = ticket.getEvent();
        event.setAvailableSeats(event.getAvailableSeats() + ticket.getQuantity()); // Assuming 1 ticket cancellation per request
        eventRepository.save(event);
    }

    public List<BookingResponse> getUserBookings(Long userId) {
        // Get user bookings
        List<Ticket> tickets = ticketRepository.findByUserId(userId);
        return tickets.stream()
                .map(ticket -> new BookingResponse(ticket.getId(), ticket.getUser().getId(), ticket.getEvent().getId(),
                        ticket.getStatus(), ticket.getPrice(), ticket.getQuantity()))
                .collect(Collectors.toList());
    }

}
