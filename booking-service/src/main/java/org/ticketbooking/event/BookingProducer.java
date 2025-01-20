package org.ticketbooking.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.ticketbooking.common.model.BookingRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class BookingProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public BookingProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendBookingRequest(String topic, BookingRequest request) {
        try {
            kafkaTemplate.send(topic, new ObjectMapper().writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to send booking request", e);
        }
    }
}
