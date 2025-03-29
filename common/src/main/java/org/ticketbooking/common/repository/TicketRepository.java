package org.ticketbooking.common.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.ticketbooking.common.model.Ticket;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, String> {
    List<Ticket> findByUserId(Long userId);
    List<Ticket> findByEventId(Long eventId);
    
    Optional<Ticket> findByBookingRef(String bookingRef);
}