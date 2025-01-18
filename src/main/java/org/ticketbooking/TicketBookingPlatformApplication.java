package org.ticketbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.ticketbooking")
public class TicketBookingPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicketBookingPlatformApplication.class, args);
    }
}