package org.ticketbooking.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.ticketbooking.common.cache.RedisCacheManager;
import org.ticketbooking.common.exception.CommonException;
import org.ticketbooking.common.model.AvailabilityService;
import org.ticketbooking.common.model.BookingRequest;
import org.ticketbooking.common.model.BookingResponse;
import org.ticketbooking.common.model.CustomUserDetails;
import org.ticketbooking.common.model.Event;
import org.ticketbooking.common.model.Ticket;
import org.ticketbooking.common.model.User;
import org.ticketbooking.common.repository.EventRepository;
import org.ticketbooking.common.repository.TicketRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
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
    AvailabilityService availabilityService;

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

        String availablityEventKey = "event:" + request.getEventId() + ":availability";
        String blockedEventKey = "event:" + request.getEventId() + ":blocked";
        int requestedTickets = request.getQuantity();

        Long blockedTickets = availabilityService.blockTemporaryTickets(request.getEventId(), availablityEventKey,
                blockedEventKey, requestedTickets);

        if (blockedTickets == -1) {
            throw new CommonException("Not enough seats available");
        }

        // Send booking request to Kafka
        request.setBookingRef(bookingRef);
        request.setUser(user);

        bookingProducer.sendBookingRequest("booking-requests", request);

        return new BookingResponse(null, null, request.getEventId(), "PENDING",
                null, request.getQuantity(), bookingRef);
    }

    public BookingResponse bookTicket(BookingRequest request) throws CommonException {
        Long eventId = request.getEventId();
        String availablityEventKey = "event:" + eventId + ":availability";
        String blockedEventKey = "event:" + request.getEventId() + ":blocked";

        Event event = availabilityService.getEventFromRedis(eventId);
        if (!validateEvent(event)) {
            throw new CommonException("Booking is not allowed for this event yet or it has already started");
        }

        boolean bookingStatus = availabilityService.finalizeBooking(eventId, availablityEventKey,blockedEventKey,
                request.getQuantity());
        if (!bookingStatus) {
            throw new CommonException("Failed to finalize booking. Seats might have expired or been reclaimed.");
        }

        BigDecimal totalPrice = new BigDecimal(event.getPrice()).multiply(new BigDecimal(request.getQuantity()));

        Ticket ticket = createTicket(request.getEventId(), request.getQuantity(), request.getUser(),
                request.getBookingRef(), totalPrice);
        availabilityService.pushTicketToDatabase(ticket);

        availabilityService.syncAvailabilityToDatabase(eventId);
        return new BookingResponse(null, request.getUser().getId(), eventId, "CONFIRMED", null, request.getQuantity(),
                request.getBookingRef());
    }

    public void redirectToPayment(BookingRequest request) throws CommonException {
        // Redirect user to payment
        PaymentService paymentService = new PaymentService();
        // BigDecimal amountToPay = redisTemplate.opsForValue().get("event:" +
        // request.getEventId() + ":price")
        // .multiply(new BigDecimal(request.getQuantity())); // TODO::

        boolean paymentSuccess = paymentService.processPayment(request.getBookingRef(),
                new BigDecimal(request.getQuantity()));// TODO::change to amount

        handlePayment(request.getBookingRef(), paymentSuccess);
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

        String eventKey = "event:" + ticket.getEventId() + ":availability";
        cache.increment(eventKey, ticket.getQuantity());

        ticket.setStatus("CANCELLED");
        ticketRepository.save(ticket);

        notifyUser(ticket.getUser().getId(), "Your payment failed, booking cancelled.");
    }

    private Ticket createTicket(Long eventId, Integer ticketQuantity, User user, String bookingRef,
            BigDecimal totalPrice) throws CommonException {
        Ticket ticket = new Ticket();
        ticket.setUser(user);
        ticket.setEventId(eventId);
        ticket.setPrice(totalPrice);
        ticket.setQuantity(ticketQuantity);
        ticket.setBookingRef(bookingRef);
        ticket.setStatus("PENDING"); // CONFIRMED
        return ticket;
    }

    private boolean validateEvent(Event event) throws CommonException {
        // Check if booking is allowed for this event
        if (event.isBookingOpen() || LocalDateTime.now().isBefore(event.getStartTime())) {
            return true;
        }

        log.error("Booking is not allowed for this event yet or it has already started");
        return false;
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

        Event event = availabilityService.getEventFromRedis(ticket.getEventId());// eventRepository.findById(ticket.getEventId()).get();
        String eventKey = "event:" + event.getId() + ":availability";

        // Increment available seats in Redis
        cache.increment(eventKey, ticket.getQuantity());

        // Update ticket status to "CANCELLED"
        ticket.setStatus("CANCELLED");
        ticketRepository.save(ticket);

        availabilityService.syncAvailabilityToDatabase(ticket.getEventId());
    }

    public List<BookingResponse> getUserBookings() throws CommonException {
        Long userId = getAuthenticatedUser().getId();
        List<Ticket> tickets = ticketRepository.findByUserId(userId);
        return tickets.stream()
                .map(ticket -> new BookingResponse(ticket.getId(), ticket.getUser().getId(), ticket.getEventId(),
                        ticket.getStatus(), ticket.getPrice(), ticket.getQuantity(), ticket.getBookingRef()))
                .collect(Collectors.toList());
    }

    public void notifyUser(Long userId, String message) {
        // Implement notification logic (e.g., email, SMS)
        log.info("Notifying user " + userId + ": " + message);
    }

    public void handleOverbooking(BookingRequest request) {
        // Logic to reject or manage overbooked requests
        notifyUser(request.getUser().getId(), "Sorry, the event is fully booked.");
    }

}
