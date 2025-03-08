package org.ticketbooking.event;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.ticketbooking.common.exception.CommonException;
import org.ticketbooking.common.model.BookingRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BookingConsumer {

    private final ExecutorService executorService;
    private final Semaphore semaphore;
    private final BookingService bookingService;
    private static final int MAX_CONCURRENT_REQUESTS = 10;

    public BookingConsumer(ExecutorService kafkaExecutor, BookingService bookingService){
        this.executorService = kafkaExecutor;
        this.semaphore = new Semaphore(MAX_CONCURRENT_REQUESTS);
        this.bookingService = bookingService;
    }

    @KafkaListener(topics = "booking-requests", groupId = "booking-group", concurrency = "1")
    public void consumeBookingRequest(String message) {
        log.debug("Start consumeBookingRequest {}",message);
        try {
            semaphore.acquire();
            executorService.submit(() -> {
                try {
                    processMessage(message);
                } finally {
                    semaphore.release();
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.debug("End consumeBookingRequest");
    }

    private void processMessage(String message) {
        BookingRequest request = null;
        try {
            request = new ObjectMapper().readValue(message, BookingRequest.class);
        } catch (JsonProcessingException ex) {
            // throw new CommonException("Booking Request not parsable");
            log.error("ERROR" + message, ex);
        }

        if (request != null) {
            try {
                bookingService.bookTicket(request);
                // Redirect to mock payment service
                bookingService.redirectToPayment(request);
            } catch (CommonException e) {
                log.error("ERROR",e);
                bookingService.handleOverbooking(request);
            }
        }
    }
}