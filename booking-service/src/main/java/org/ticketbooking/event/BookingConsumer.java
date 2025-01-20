package org.ticketbooking.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.ticketbooking.common.model.BookingRequest;
import org.ticketbooking.common.model.BookingResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BookingConsumer {

    @Autowired
    private BookingService bookingService;

    @KafkaListener(topics = "booking-requests", groupId = "booking-group")
    public void consumeBookingRequest(String message) {
        BookingRequest request = null;
        try {
            request = new ObjectMapper().readValue(message, BookingRequest.class);
        } catch (JsonProcessingException ex) {
            // throw new CommonException("Booking Request not parsable");
            log.error("ERROR" + message, ex);
        }
        
        try {
            if(request != null){
                BookingResponse response = bookingService.bookTicket(request);
                // Redirect to mock payment service
                bookingService.redirectToPayment(request);
            }
        } catch (Exception e) {
            bookingService.handleOverbooking(request);
        }
    }
}