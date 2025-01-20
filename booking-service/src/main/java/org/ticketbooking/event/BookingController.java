package org.ticketbooking.event;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ticketbooking.common.exception.CommonException;
import org.ticketbooking.common.model.BookingRequest;
import org.ticketbooking.common.model.BookingResponse;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @PostMapping
    public ResponseEntity<?> bookTicket(@RequestBody BookingRequest bookingRequest) throws CommonException {
        BookingResponse bookingResponse = bookingService.initiateBooking(bookingRequest);
        return ResponseEntity.ok(bookingResponse);
    }

    @DeleteMapping("cancel/{ticketId}")
    public ResponseEntity<?> cancelTicket(@PathVariable Long ticketId) throws CommonException {
        bookingService.cancelTicket(ticketId);
        return ResponseEntity.ok("Ticket cancelled successfully");
    }

    @GetMapping
    public ResponseEntity<List<BookingResponse>> getBookings() throws CommonException {
        List<BookingResponse> bookings = bookingService.getUserBookings();
        return ResponseEntity.ok(bookings);
    }
}
