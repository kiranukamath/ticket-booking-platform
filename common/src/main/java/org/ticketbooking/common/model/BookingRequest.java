package org.ticketbooking.common.model;

import java.util.Set;

import lombok.Data;

@Data
public class BookingRequest {
    private Long eventId;
    private Set<String> seatIds;
}