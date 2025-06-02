package org.ticketbooking.event;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ticketbooking.common.cache.RedisCacheManager;
import org.ticketbooking.common.exception.CommonException;
import org.ticketbooking.common.model.BookingMessage;
import org.ticketbooking.common.model.BookingResponse;
import org.ticketbooking.common.model.Event;
import org.ticketbooking.common.model.EventHelper;
import org.ticketbooking.common.model.Seat;
import org.ticketbooking.common.model.Ticket;
import org.ticketbooking.common.model.User;
import org.ticketbooking.common.repository.SeatRepository;
import org.ticketbooking.common.repository.TicketRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BookingServiceConsumer {

    @Autowired
    EventHelper eventHelper;
    @Autowired
    TicketRepository ticketRepository;
    @Autowired
    RedisCacheManager cache;
    @Autowired
    SeatRepository seatRepository;

    public BookingResponse bookTicket(BookingMessage bookingMessage) throws CommonException {
        Ticket ticket = createTicket(bookingMessage.getEventId(), bookingMessage.getUser(), bookingMessage.getBookingRef(),
                bookingMessage.getPrice());
        String bookingRef = ticket.getBookingRef();
        Long eventId = ticket.getEventId();

        eventHelper.isSeatsAvailable(bookingMessage, bookingRef, eventId);

        List<Seat> seats = seatRepository.findByEventIdAndSeatIdIn(
                bookingMessage.getEventId(),
                bookingMessage.getSeatIds());

        for(Seat seat : seats) {
            if(seat.getStatus().equals("BOOKED")) {
                throw new CommonException("Seat " + seat.getSeatId() + " is not available for booking.");
            }
        }

        ticket.setStatus("PENDING");
        ticketRepository.save(ticket);
        return new BookingResponse(ticket.getUser().getId(), eventId, "PENDING", ticket.getPrice(),
                bookingMessage.getSeatIds(), bookingRef);
    }

    @Transactional
    public void cancelTicket(String ticketId) throws CommonException {
        // Fetch the ticket by its ID
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new CommonException("Ticket not found"));

        // Check the status of the ticket
        if (!ticket.getStatus().equals("PENDING") && !ticket.getStatus().equals("CONFIRMED")) {
            throw new CommonException("Ticket cannot be cancelled");
        }

        Long eventId = ticket.getEventId();
        String bookingRef = ticket.getBookingRef();

        List<Seat> bookedSeats = seatRepository.findByBookingRef(bookingRef);
        if (bookedSeats.isEmpty()) {
            throw new CommonException("No seats found for this ticket");
        } 

        for (Seat seat : bookedSeats) {
            seat.setStatus("AVAILABLE");
            seat.setBookingRef(null);
        }
        seatRepository.saveAll(bookedSeats);

        Set<String> seatIds = bookedSeats.stream()
                        .map(Seat::getSeatId)
                        .collect(Collectors.toSet());

        cache.unlockSeats(eventId, seatIds , bookingRef);
        // Update ticket status to "CANCELLED"
        ticket.setStatus("CANCELLED");
        ticketRepository.save(ticket);
    }

    public void redirectToPayment(BookingMessage request) throws CommonException {
        // Redirect user to payment
        PaymentService paymentService = new PaymentService();

        boolean paymentSuccess = paymentService.processPayment(request.getBookingRef(),
                request.getPrice());

        handlePayment(request, paymentSuccess);
    }

    public void handlePayment(BookingMessage message, boolean paymentSuccess) throws CommonException {
        if (paymentSuccess) {
            confirmBooking(message);
        } else {
            cancelTemporaryBooking(message);
        }
    }

    public void confirmBooking(BookingMessage bookingMessage) throws CommonException {
        // Update ticket status to CONFIRMED
        Ticket ticket = ticketRepository.findByBookingRef(bookingMessage.getBookingRef())
                .orElseThrow(() -> new CommonException("Booking not found"));

        eventHelper.isSeatsAvailable(bookingMessage, bookingMessage.getBookingRef(), ticket.getEventId());

        List<Seat> seats = seatRepository.findByEventIdAndSeatIdIn(
                bookingMessage.getEventId(),
                bookingMessage.getSeatIds());

        for (Seat seat : seats) {
            seat.setStatus("BOOKED");
            seat.setBookingRef(bookingMessage.getBookingRef());
        }

        Event event = eventHelper.getEventFromRedis(bookingMessage.getEventId());
        long ttl = eventHelper.calculateTTLForEvent(event.getEndTime()) + 3600;
        cache.reserveSeats(ticket.getEventId(), bookingMessage.getSeatIds(),bookingMessage.getBookingRef(),Duration.ofSeconds(ttl));

        seatRepository.saveAll(seats);
        ticket.setStatus("CONFIRMED");
        ticketRepository.save(ticket);
        notifyUser(ticket.getUser().getId(), "Your booking " + bookingMessage.getBookingRef() + " is confirmed.");
    }

    public void cancelTemporaryBooking(BookingMessage message) {
        Optional<Ticket> ticketOp = ticketRepository.findByBookingRef(message.getBookingRef());
        if (ticketOp.isPresent()) {
            Ticket ticket = ticketOp.get();

            Long eventId = ticket.getEventId();

            Set<String> seatIds = message.getSeatIds();

            cache.unlockSeats(eventId, seatIds, message.getBookingRef());

            ticket.setStatus("CANCELLED");
            ticketRepository.save(ticket);

            notifyUser(ticket.getUser().getId(), "Your payment failed, booking cancelled.");
        } else {
            log.error("Booking not found in DB");
        }
    }

    private Ticket createTicket(Long eventId, User user, String bookingRef,
            BigDecimal totalPrice) {
        Ticket ticket = new Ticket();
        ticket.setUser(user);
        ticket.setEventId(eventId);
        ticket.setPrice(totalPrice);
        ticket.setBookingRef(bookingRef);
        ticket.setStatus("PENDING"); // CONFIRMED
        return ticket;
    }

    public void notifyUser(Long userId, String message) {
        // Implement notification logic (e.g., email, SMS)
        log.info("Notifying user " + userId + ": " + message);
    }
}
