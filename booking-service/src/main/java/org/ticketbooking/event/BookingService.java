package org.ticketbooking.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.ticketbooking.common.cache.RedisCacheManager;
import org.ticketbooking.common.exception.CommonException;
import org.ticketbooking.common.model.BookingRequest;
import org.ticketbooking.common.model.BookingResponse;
import org.ticketbooking.common.model.CustomUserDetails;
import org.ticketbooking.common.model.Event;
import org.ticketbooking.common.model.Ticket;
import org.ticketbooking.common.model.User;
import org.ticketbooking.common.model.Utils;
import org.ticketbooking.common.repository.EventRepository;
import org.ticketbooking.common.repository.TicketRepository;

import jakarta.transaction.Transactional;

@Service
public class BookingService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private BookingProducer bookingProducer;

    @Autowired
    RedisCacheManager cache;

    @Autowired
    Utils utils;

    public User getAuthenticatedUser() throws CommonException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) authentication.getPrincipal()).getUser();
        }
        throw new CommonException("User not authenticated");
    }

    public BookingResponse initiateBooking(BookingRequest request) throws CommonException {
        String bookingRef = UUID.randomUUID().toString();
        User user = getAuthenticatedUser();

        String eventKey = "event:" + request.getEventId() + ":availability";
        int availableSeats = utils.getAvailableSeats(request.getEventId());

        if (availableSeats < request.getQuantity()) {
            throw new CommonException("Not enough seats available");
        }

        // Temporarily reserve tickets in Redis
        cache.decrement(eventKey, request.getQuantity());

        // Send booking request to Kafka
        request.setBookingRef(bookingRef);
        request.setUser(user);

        bookingProducer.sendBookingRequest("booking-requests", request);

        return new BookingResponse(null, null, request.getEventId(), "PENDING",
                null, request.getQuantity(), bookingRef);
    }

    @Transactional
    @Retryable(
        value = { OptimisticLockingFailureException.class }, // Specify the exception to retry on
        maxAttempts = 3, // Max number of retries
        backoff = @Backoff(delay = 500, multiplier = 2) // Exponential backoff: delay starts at 1s, multiplies by 2 each retry
    )
    public BookingResponse bookTicket(BookingRequest request) throws CommonException {

        Ticket savedTicket = null;
        // int maxRetries = 3;
        // int retryCount = 0;
        // while (retryCount < maxRetries) {
        //     try {
                Event event = validateEvent(request.getEventId(), request.getQuantity());

                Ticket ticket = createTicket(event, request.getQuantity(), request.getUser(), request.getBookingRef());

                // Save the booking details
                eventRepository.save(event);
                savedTicket = ticketRepository.save(ticket);

                return new BookingResponse(savedTicket.getId(), savedTicket.getUser().getId(), savedTicket.getId(),
                        savedTicket.getStatus(), savedTicket.getPrice(), savedTicket.getQuantity(),
                        savedTicket.getBookingRef());
            // } catch (OptimisticLockingFailureException e) {
            //     retryCount++;
            //     if (retryCount == maxRetries) {
            //         throw new CommonException("Failed to book ticket after multiple retries", HttpStatus.CONFLICT);
            //     }
            // }
        // }
        // throw new CommonException("Failed to book ticket", HttpStatus.CONFLICT);
    }

    public void redirectToPayment(BookingRequest request) throws CommonException {
        // Redirect user to payment
        PaymentService paymentService = new PaymentService();
        boolean paymentSuccess = paymentService.processPayment(request.getBookingRef(), new BigDecimal(request.getQuantity()));// TODO::change to amount

        handlePayment(request.getBookingRef(),paymentSuccess);
    }

    public void handlePayment(String bookingRef, boolean paymentSuccess) throws CommonException {
        if (paymentSuccess) {
            confirmBooking(bookingRef);
        } else {
            cancelTemporaryBooking(bookingRef);
        }
    }

    public void confirmBooking(String bookingRef) throws CommonException {
        // Update ticket status to CONFIRMED
        Ticket ticket = ticketRepository.findByBookingRef(bookingRef)
                .orElseThrow(() -> new CommonException("Booking not found"));
                
        ticket.setStatus("CONFIRMED");
        ticketRepository.save(ticket);
        notifyUser(ticket.getUser().getId(), "Your booking is confirmed.");
    }

    public void cancelTemporaryBooking(String bookingRef) throws CommonException {
        Ticket ticket = ticketRepository.findByBookingRef(bookingRef)
                .orElseThrow(() -> new CommonException("Booking not found"));
        
        String eventKey = "event:" + ticket.getEvent().getId() + ":availability";
        cache.increment(eventKey, ticket.getQuantity());

        ticket.setStatus("CANCELLED");
        ticketRepository.save(ticket);

        notifyUser(ticket.getUser().getId(), "Your payment failed, booking cancelled.");
    }

    private Ticket createTicket(Event event,Integer ticketQuantity, User user,String bookingRef) throws CommonException {
        Ticket ticket = new Ticket();
        ticket.setUser(user);
        ticket.setEvent(event);
        ticket.setPrice(new BigDecimal(event.getPrice()).multiply(new BigDecimal(ticketQuantity)));
        ticket.setQuantity(ticketQuantity);
        ticket.setBookingRef(bookingRef);
        ticket.setStatus("PENDING"); //CONFIRMED
        return ticket;
    }

    private Event validateEvent(Long eventId,Integer ticketQuantity) throws CommonException {
        // Validate if event exists
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CommonException("Event not found"));

        // Check if booking is allowed for this event
        if (!event.isBookingOpen() || event.getStartTime().isBefore(LocalDateTime.now())) {
            throw new CommonException("Booking is not allowed for this event yet or it has already started");
        }

        // Check for seat availability
        if (event.getAvailableSeats() < ticketQuantity) {
            throw new CommonException("Not enough seats available");
        }

        // Decrease available seats
        event.setAvailableSeats(event.getAvailableSeats() - ticketQuantity);
        return event;
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

        Event event = ticket.getEvent();
        String eventKey = "event:" + event.getId() + ":availability";

        // Increment available seats in Redis
        cache.increment(eventKey, ticket.getQuantity());

        // Increase the available seats in the event
        event.setAvailableSeats(event.getAvailableSeats() + ticket.getQuantity()); // Assuming 1 ticket cancellation per request
        eventRepository.save(event);
    }

    public List<BookingResponse> getUserBookings() throws CommonException {
        Long userId = getAuthenticatedUser().getId();
        List<Ticket> tickets = ticketRepository.findByUserId(userId);
        return tickets.stream()
                .map(ticket -> new BookingResponse(ticket.getId(), ticket.getUser().getId(), ticket.getEvent().getId(),
                        ticket.getStatus(), ticket.getPrice(), ticket.getQuantity(),ticket.getBookingRef()))
                .collect(Collectors.toList());
    }

    public void notifyUser(Long userId, String message) {
        // Implement notification logic (e.g., email, SMS)
        System.out.println("Notifying user " + userId + ": " + message);
    }

    public void handleOverbooking(BookingRequest request) {
        // Logic to reject or manage overbooked requests
        // TODO:: notifyUser(request.getUser().getId(), "Sorry, the event is fully booked.");
    }

}
