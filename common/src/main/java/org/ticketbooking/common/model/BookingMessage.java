package org.ticketbooking.common.model;

import java.math.BigDecimal;
import java.util.Set;

import lombok.Data;

@Data
public class BookingMessage {
    private String bookingRef;
    private Long eventId;
    private Set<String> seatIds;
    private User user;
    private BigDecimal price;
    private String status;
}
