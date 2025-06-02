package org.ticketbooking.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.ticketbooking.common.model.BookingMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Service
public class BookingProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public BookingProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper = mapper;
    }

    public void publishTicketBooked(BookingMessage ticket) {
        try {
            kafkaTemplate.send("ticket.booked", objectMapper.writeValueAsString(ticket));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to send booking request", e);
        }
    }

    public void publishTicketCancelled(String ticketId) {
        kafkaTemplate.send("ticket.cancelled", ticketId);
    }

}
