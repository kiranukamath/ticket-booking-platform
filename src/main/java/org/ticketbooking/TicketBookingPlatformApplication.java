package org.ticketbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication(scanBasePackages = "org.ticketbooking")
@EnableRetry
public class TicketBookingPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicketBookingPlatformApplication.class, args);
    }
}