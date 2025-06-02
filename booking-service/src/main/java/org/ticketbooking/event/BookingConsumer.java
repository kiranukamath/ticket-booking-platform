package org.ticketbooking.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.ticketbooking.common.exception.CommonException;
import org.ticketbooking.common.model.BookingMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BookingConsumer {

    private final BookingServiceConsumer bookingService;

    public BookingConsumer(BookingServiceConsumer bookingService){
        this.bookingService = bookingService;
    }

    @KafkaListener(topics = "ticket.booked", groupId = "booking-group1", concurrency = "1")
    public void consumeBooking(String message) {
        BookingMessage bookingMessage = null;
        try {
            bookingMessage = new ObjectMapper().readValue(message, BookingMessage.class);
        } catch (JsonProcessingException ex) {
            // throw new CommonException("Booking Request not parsable");
            log.error("ERROR" + message, ex);
        }

        try {
            bookingService.bookTicket(bookingMessage);
            // Redirect to mock payment service
            bookingService.redirectToPayment(bookingMessage);
        } catch (CommonException e) {
            log.error("ERROR", e);
            bookingService.cancelTemporaryBooking(bookingMessage);
        }
    }

    @KafkaListener(topics = "ticket.cancelled", groupId = "booking-group1", concurrency = "1")
    public void consumeCancellation(String message) {
        try {
            bookingService.cancelTicket(message);
        } catch (CommonException e) {
            log.error("ERROR", e);
        }
    }
    
}