package org.ticketbooking.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.ticketbooking.common.cache.RedisCacheManager;
import org.ticketbooking.common.exception.CommonException;
import org.ticketbooking.common.helper.EventHelper;
import org.ticketbooking.common.model.BookingMessage;
import org.ticketbooking.common.model.BookingRequest;
import org.ticketbooking.common.model.BookingResponse;
import org.ticketbooking.common.model.CustomUserDetails;
import org.ticketbooking.common.model.Event;
import org.ticketbooking.common.model.User;
import org.ticketbooking.common.repository.SeatRepository;
import org.ticketbooking.common.repository.TicketRepository;

@ExtendWith(MockitoExtension.class)
public class BookingServiceProducerTest {

    @InjectMocks
    private BookingServiceProducer bookingServiceProducer;

    @Mock
    private EventHelper eventHelper;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private BookingProducer bookingProducer;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private RedisCacheManager cache;
    @Mock
    private Authentication authentication;

    private BookingRequest bookingRequest;
    private User user;
    private Event event;

    @BeforeEach
    void setUp() throws CommonException {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        event = new Event();
        event.setId(100L);
        event.setName("Test Event");
        event.setPrice(Double.valueOf(50.0));
        event.setBookingOpen(true);
        event.setStartTime(LocalDateTime.now().plusHours(1));

        bookingRequest = new BookingRequest();
        bookingRequest.setEventId(100L);
        Set<String> seatIds = new HashSet<>();
        seatIds.add("A1");
        seatIds.add("A2");
        bookingRequest.setSeatIds(seatIds);

        // Mock authentication
        // Authentication authentication = mock(Authentication.class);
        // CustomUserDetails userDetails = new CustomUserDetails(user);
        // when(authentication.getPrincipal()).thenReturn(userDetails);
        // SecurityContextHolder.getContext().setAuthentication(authentication);
        // when(bookingServiceProducer.getAuthenticatedUser()).thenReturn(user);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
    }

    @Test
    void initiateBooking_success() throws CommonException {
        when(eventHelper.getEventFromRedis(bookingRequest.getEventId())).thenReturn(event);
        when(cache.lockSeats(anyLong(), anySet(), anyString(), any(Duration.class))).thenReturn(true);
                
        BookingResponse response = bookingServiceProducer.initiateBooking(bookingRequest);

        assertNotNull(response);
        assertEquals(user.getId(), response.getUserId());
        assertEquals(bookingRequest.getEventId(), response.getEventId());
        assertEquals("PENDING", response.getStatus());
        assertNotNull(response.getBookingRef());

        verify(bookingProducer, times(1)).publishTicketBooked(any(BookingMessage.class));
    }

    @Test
    void initiateBooking_noSeatIdsProvided() {
        bookingRequest.setSeatIds(null);

        CommonException exception = assertThrows(CommonException.class, () -> {
            bookingServiceProducer.initiateBooking(bookingRequest);
        });

        assertEquals("No seat IDs provided for booking", exception.getMessage());
    }

    @Test
    void initiateBooking_eventNotOpenForBooking() throws CommonException {
        Event closedEvent = new Event();
        closedEvent.setId(100L);
        closedEvent.setName("Closed Event");
        closedEvent.setPrice(Double.valueOf(50.0));
        closedEvent.setBookingOpen(false);
        closedEvent.setStartTime(LocalDateTime.now().plusHours(1));

        when(eventHelper.getEventFromRedis(bookingRequest.getEventId())).thenReturn(closedEvent);

        CommonException exception = assertThrows(CommonException.class, () -> {
            bookingServiceProducer.initiateBooking(bookingRequest);
        });

        assertEquals("Booking is not allowed for this event yet or it has already started", exception.getMessage());
    }

    @Test
    void initiateBooking_seatNotAvailable() throws CommonException {
        when(eventHelper.getEventFromRedis(bookingRequest.getEventId())).thenReturn(event);
        when(cache.lockSeats(anyLong(), anySet(), anyString(), any(Duration.class))).thenReturn(false);

        CommonException exception = assertThrows(CommonException.class, () -> {
            bookingServiceProducer.initiateBooking(bookingRequest);
        });

        assertEquals("Seat Not Available for booking", exception.getMessage());
    }
}