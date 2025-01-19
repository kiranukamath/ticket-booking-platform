package org.ticketbooking.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.ticketbooking.common.model.Event;

public interface EventRepository extends JpaRepository<Event, Long> {
    
}