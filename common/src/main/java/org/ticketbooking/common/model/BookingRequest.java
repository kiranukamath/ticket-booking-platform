package org.ticketbooking.common.model;

import lombok.Data;

@Data
public class BookingRequest {
    private Long eventId;
    private Integer quantity;
    String bookingRef;
    User user;
}