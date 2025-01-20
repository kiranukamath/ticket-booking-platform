package org.ticketbooking.event;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    public boolean processPayment(String bookingRef, BigDecimal amount) {
        // Mock payment logic, simulate success or failure
        return true; // Assuming success for simplicity
    }
}
