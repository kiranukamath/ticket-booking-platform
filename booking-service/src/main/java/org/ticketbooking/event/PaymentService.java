package org.ticketbooking.event;

import java.math.BigDecimal;
import java.util.Random;

import org.springframework.stereotype.Service;

@Service
public class PaymentService {
    Random random = new Random();

    public boolean processPayment(String bookingRef, BigDecimal amount) {
        // Mock payment logic, simulate success or failure
        return random.nextDouble() < 0.8;
    }
}
