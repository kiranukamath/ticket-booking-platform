package org.ticketbooking.common.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "ticket")
@Data
public class Ticket {

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Long eventId;

    private LocalDateTime bookingTime = LocalDateTime.now();

    private String status = "PENDING"; // PENDING, CONFIRMED, CANCELLED

    private BigDecimal price;

    @Column(nullable = false)
    private int quantity;

    @Id
    private String bookingRef;
}
