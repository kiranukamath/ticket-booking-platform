package org.ticketbooking.event;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.ticketbooking.common.cache.RedisCacheManager;
import org.ticketbooking.common.exception.CommonException;
import org.ticketbooking.common.helper.EventHelper;
import org.ticketbooking.common.model.BookingMessage;
import org.ticketbooking.common.model.BookingRequest;
import org.ticketbooking.common.model.BookingResponse;
import org.ticketbooking.common.model.CustomUserDetails;
import org.ticketbooking.common.model.Event;
import org.ticketbooking.common.model.Seat;
import org.ticketbooking.common.model.Ticket;
import org.ticketbooking.common.model.User;
import org.ticketbooking.common.repository.SeatRepository;
import org.ticketbooking.common.repository.TicketRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BookingServiceProducer {

    @Autowired
    private EventHelper eventHelper;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private BookingProducer bookingProducer;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    RedisCacheManager cache;

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

        Set<String> seatIds = request.getSeatIds(); // Must be non-null

        if (seatIds == null || seatIds.isEmpty()) {
            throw new CommonException("No seat IDs provided for booking");
        }

        Event event = eventHelper.getEventFromRedis(request.getEventId());
        if (!validateEvent(event)) {
            throw new CommonException("Booking is not allowed for this event yet or it has already started");
        }

        boolean locked = cache.lockSeats(request.getEventId(), request.getSeatIds(),bookingRef,Duration.ofMinutes(2));
        if (!locked){
            throw new CommonException("Seat Not Available for booking");
        }

        BigDecimal totalPrice = new BigDecimal(event.getPrice()).multiply(new BigDecimal(request.getSeatIds().size()));

        BookingMessage message = createBookingMessage(request.getEventId(), request.getSeatIds(), user, bookingRef, totalPrice);

        // Emit Kafka event
        bookingProducer.publishTicketBooked(message);

        return new BookingResponse(user.getId(), request.getEventId(), "PENDING",
                null, request.getSeatIds(), bookingRef);
    }

    private BookingMessage createBookingMessage(Long eventId, Set<String> seatIds, User user, String bookingRef,
            BigDecimal totalPrice) {
        BookingMessage message = new BookingMessage();
        message.setUser(user);
        message.setEventId(eventId);
        message.setSeatIds(seatIds);
        message.setPrice(totalPrice);
        message.setBookingRef(bookingRef);
        message.setStatus("PENDING"); // CONFIRMED
        return message;
    }

    private boolean validateEvent(Event event) throws CommonException {
        // Check if booking is allowed for this event
        if (event.isBookingOpen() && LocalDateTime.now().isBefore(event.getStartTime())) {
            return true;
        }

        log.error("Booking is not allowed for this event yet or it has already started");
        return false;
    }

    public void cancelTicket(String ticketId) throws CommonException {
        bookingProducer.publishTicketCancelled(ticketId);
    }

    public List<BookingResponse> getUserBookings() throws CommonException {
        Long userId = getAuthenticatedUser().getId();
        List<Ticket> tickets = ticketRepository.findByUserId(userId);

        return tickets.stream()
                .map(ticket -> {
                    List<Seat> seats = seatRepository.findByBookingRef(ticket.getBookingRef());
                    Set<String> seatIds = seats.stream()
                            .map(Seat::getSeatId)
                            .collect(Collectors.toSet());

                    return new BookingResponse(
                            ticket.getUser().getId(),
                            ticket.getEventId(),
                            ticket.getStatus(),
                            ticket.getPrice(),
                            seatIds,
                            ticket.getBookingRef());
                })
                .collect(Collectors.toList());
    }


}
