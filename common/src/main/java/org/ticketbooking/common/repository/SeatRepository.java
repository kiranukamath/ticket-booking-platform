package org.ticketbooking.common.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.ticketbooking.common.model.Seat;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByEventIdAndSeatIdIn(Long eventId, Set<String> seatIds);
    List<Seat> findByBookingRef(String bookingRef);
    List<Seat> findByEventId(Long eventId);
}

