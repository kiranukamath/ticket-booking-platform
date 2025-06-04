package org.ticketbooking.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ticketbooking.common.cache.RedisCacheManager;
import org.ticketbooking.common.exception.CommonException;
import org.ticketbooking.common.helper.EventHelper;
import org.ticketbooking.common.model.BookingMessage;
import org.ticketbooking.common.model.BookingResponse;
import org.ticketbooking.common.model.Event;
import org.ticketbooking.common.model.Seat;
import org.ticketbooking.common.model.Ticket;
import org.ticketbooking.common.model.User;
import org.ticketbooking.common.repository.SeatRepository;
import org.ticketbooking.common.repository.TicketRepository;

@ExtendWith(MockitoExtension.class)
public class BookingServiceConsumerTest {

    @InjectMocks
    private BookingServiceConsumer bookingServiceConsumer;

    @Mock
    private EventHelper eventHelper;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private RedisCacheManager cache;

    @Mock
    private SeatRepository seatRepository;

    private BookingMessage bookingMessage;
    private Ticket ticket;
    private User user;
    private Event event;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        event = new Event();
        event.setId(100L);
        event.setName("Test Event");

        bookingMessage = new BookingMessage();
        bookingMessage.setEventId(100L);
        bookingMessage.setUser(user);
        bookingMessage.setBookingRef("booking123");
        bookingMessage.setPrice(BigDecimal.valueOf(50.0));
        bookingMessage.setSeatIds(Set.of("1", "2"));

        ticket = new Ticket();
        ticket.setBookingRef("booking123");
        ticket.setEventId(100L);
        ticket.setUser(user);
        ticket.setPrice(BigDecimal.valueOf(50.0));
    }

    @Test
    void bookTicket_success() throws CommonException {
        List<Seat> availableSeats = new ArrayList<>();
        for (String seatId : bookingMessage.getSeatIds()) {
            Seat seat = new Seat();
            seat.setEvent(event);
            seat.setSeatId(seatId);
            seat.setStatus("AVAILABLE");
            availableSeats.add(seat);
        }

        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(seatRepository.findByEventIdAndSeatIdIn(bookingMessage.getEventId(), bookingMessage.getSeatIds()))
                .thenReturn(availableSeats);

        BookingResponse response = bookingServiceConsumer.bookTicket(bookingMessage);

        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    void bookTicket_seatNotAvailable() {
        List<Seat> bookedSeats = new ArrayList<>();
        for (String seatId : bookingMessage.getSeatIds()) {
            Seat seat = new Seat();
            seat.setEvent(event);
            seat.setSeatId(seatId);
            seat.setStatus("BOOKED");
            bookedSeats.add(seat);
        }

        when(seatRepository.findByEventIdAndSeatIdIn(bookingMessage.getEventId(), bookingMessage.getSeatIds()))
                .thenReturn(bookedSeats);

        CommonException exception = assertThrows(CommonException.class, () -> {
            bookingServiceConsumer.bookTicket(bookingMessage);
        });

        assertEquals("Seats are not available for booking.", exception.getMessage());
    }

    @Test
    void cancelTicket_success() throws CommonException {
        Ticket ticket = new Ticket();
        ticket.setBookingRef("booking123");
        ticket.setEventId(100L);
        ticket.setUser(user);
        ticket.setStatus("PENDING");

        List<Seat> bookedSeats = new ArrayList<>();
        for (String seatId : bookingMessage.getSeatIds()) {
            Seat seat = new Seat();
            seat.setEvent(event);
            seat.setSeatId(seatId);
            seat.setBookingRef("booking123");
            seat.setStatus("BOOKED");
            bookedSeats.add(seat);
        }

        when(ticketRepository.findById("booking123")).thenReturn(Optional.of(ticket));
        when(seatRepository.findByBookingRef("booking123")).thenReturn(bookedSeats);

        bookingServiceConsumer.cancelTicket("booking123");

        verify(seatRepository, times(1)).saveAll(anyList());
        verify(ticketRepository, times(1)).save(any(Ticket.class));
        verify(cache, times(1)).unlockSeats(eq(100L), anySet(), eq("booking123"));
        assertEquals("CANCELLED", ticket.getStatus());
        for (Seat seat : bookedSeats) {
            assertEquals("AVAILABLE", seat.getStatus());
            assertNull(seat.getBookingRef());
        }
    }

    @Test
    void cancelTicket_ticketNotFound() {
        when(ticketRepository.findById("booking123")).thenReturn(Optional.empty());

        CommonException exception = assertThrows(CommonException.class, () -> {
            bookingServiceConsumer.cancelTicket("booking123");
        });

        assertEquals("Ticket not found", exception.getMessage());
    }

    @Test
    void cancelTicket_ticketCannotBeCancelled() {
        Ticket ticket = new Ticket();
        ticket.setBookingRef("booking123");
        ticket.setEventId(100L);
        ticket.setUser(user);
        ticket.setStatus("CANCELLED");

        when(ticketRepository.findById("booking123")).thenReturn(Optional.of(ticket));

        CommonException exception = assertThrows(CommonException.class, () -> {
            bookingServiceConsumer.cancelTicket("booking123");
        });

        assertEquals("Ticket cannot be cancelled", exception.getMessage());
    }

    @Test
    void confirmBooking_success() throws CommonException {
        List<Seat> availableSeats = new ArrayList<>();
        for (String seatId : bookingMessage.getSeatIds()) {
            Seat seat = new Seat();
            seat.setEvent(event);
            seat.setSeatId(seatId);
            seat.setStatus("AVAILABLE");
            availableSeats.add(seat);
        }

        when(ticketRepository.findByBookingRef(bookingMessage.getBookingRef())).thenReturn(Optional.of(ticket));
        when(seatRepository.findByEventIdAndSeatIdIn(bookingMessage.getEventId(), bookingMessage.getSeatIds()))
                .thenReturn(availableSeats);
        when(eventHelper.getEventFromRedis(bookingMessage.getEventId())).thenReturn(event);
        when(eventHelper.calculateTTLForEvent(any())).thenReturn(3600L);

        bookingServiceConsumer.confirmBooking(bookingMessage);

        verify(seatRepository, times(1)).saveAll(anyList());
        verify(ticketRepository, times(1)).save(any(Ticket.class));
        verify(cache, times(1)).reserveSeats(eq(100L), anySet(), eq("booking123"), any(Duration.class));
        assertEquals("CONFIRMED", ticket.getStatus());
        for (Seat seat : availableSeats) {
            assertEquals("BOOKED", seat.getStatus());
            assertEquals("booking123", seat.getBookingRef());
        }
    }

    @Test
    void confirmBooking_bookingNotFound() {
        when(ticketRepository.findByBookingRef(bookingMessage.getBookingRef())).thenReturn(Optional.empty());

        CommonException exception = assertThrows(CommonException.class, () -> {
            bookingServiceConsumer.confirmBooking(bookingMessage);
        });

        assertEquals("Booking not found", exception.getMessage());
    }

    @Test
    void cancelTemporaryBooking_success() {
        when(ticketRepository.findByBookingRef(bookingMessage.getBookingRef())).thenReturn(Optional.of(ticket));

        bookingServiceConsumer.cancelTemporaryBooking(bookingMessage);

        verify(cache, times(1)).unlockSeats(eq(100L), anySet(), eq("booking123"));
        verify(ticketRepository, times(1)).save(any(Ticket.class));
        assertEquals("CANCELLED", ticket.getStatus());
    }

    @Test
    void cancelTemporaryBooking_bookingNotFound() {
        when(ticketRepository.findByBookingRef(bookingMessage.getBookingRef())).thenReturn(Optional.empty());

        bookingServiceConsumer.cancelTemporaryBooking(bookingMessage);

        verify(cache, never()).unlockSeats(anyLong(), anySet(), anyString());
        verify(ticketRepository, never()).save(any(Ticket.class));
    }
}