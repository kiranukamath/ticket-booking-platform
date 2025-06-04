package org.ticketbooking.common.model;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SeatUpdateMessage {
    private Long eventId;
    private Set<String> seatId;
    private String status;
}
