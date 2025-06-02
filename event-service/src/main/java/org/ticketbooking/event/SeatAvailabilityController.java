package org.ticketbooking.event;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ticketbooking.common.model.Seat;
import org.ticketbooking.common.repository.SeatRepository;

@RestController
@RequestMapping("/api/seats")
public class SeatAvailabilityController {
    @Autowired
    private SeatRepository seatRepository;

    @GetMapping("/{eventId}")
    public ResponseEntity<List<Seat>> getSeatAvailability(@PathVariable Long eventId) {
        List<Seat> seats = seatRepository.findByEventId(eventId);
        return ResponseEntity.ok(seats);
    }
}