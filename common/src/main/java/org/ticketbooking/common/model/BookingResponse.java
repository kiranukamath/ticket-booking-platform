package org.ticketbooking.common.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BookingResponse {
    private Long userId;
    private Long eventId;
    private String status;
    private BigDecimal price;
    private int quantity;
    private String bookingRef;
}