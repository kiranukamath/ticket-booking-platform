package org.ticketbooking.event;

import java.util.concurrent.Semaphore;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.ticketbooking.common.model.BookingRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BookingConsumer {

    private final ThreadPoolTaskExecutor executorService;
    private final Semaphore semaphore;
    private BookingService bookingService;

    public BookingConsumer(ThreadPoolTaskExecutor kafkaExecutor, BookingService bookingService){
        this.executorService = kafkaExecutor;
        this.semaphore = new Semaphore(executorService.getMaxPoolSize());
        this.bookingService = bookingService;
    }

    @KafkaListener(topics = "booking-requests", groupId = "booking-group", concurrency = "1")
    public void consumeBookingRequest(String message) {
        try {
            semaphore.acquire();
            executorService.submit(() -> {
                processMessage(message);
                semaphore.release();
            });
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
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
            } catch (Exception e) {
                log.error("ERROR",e);
                bookingService.handleOverbooking(request);
            }
        }
    }
}