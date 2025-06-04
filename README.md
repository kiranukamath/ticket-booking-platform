# Ticket Booking System - Backend Documentation

## Overview

This document outlines the design and architecture of the Ticket Booking System, built using Java 21 with Spring Boot. The system focuses on handling high concurrency, ensuring data consistency, and implementing features like authentication and authorization.

The system also includes key features like JWT-based authentication, event-driven architecture, and transactional management.
- **Auth Service**: Handles user authentication and JWT token management.
- **Event Service**: Manages event and seat metadata.
- **Booking Service**: Handles booking, cancellation, and booking queries. Same service contains code for both pushing events to kafka, and consuming events from kafka. Can be deployed separately to scale.
- **Kafka**: Event bus for asynchronous, decoupled communication.
- **Redis**: Fast cache and atomic queue/lock manager.
- **PostgreSQL**: Source of truth for all persistent data.


## Key Features Implemented

### 1. Authentication with JWT
JWT (JSON Web Token) is used for user authentication and authorization.
Spring Security is integrated to handle the security aspect of the application.
The JWT token is created after user login and includes claims like the user details, roles, and expiration time.
JWT secret key is stored securely in application.yml to sign and validate tokens.
JWTFilter is used to intercept requests and validate tokens.
### 2. Ticket Booking Flow
The core functionality of the system involves booking tickets for events.
The system handles concurrency using Redis atomic keys and reliability with optimistic locking.
Atomicity of the booking process is ensured, meaning either all operations within the booking process succeed, or none of them do.
### 3. Event Handling and Ticket Availability
Event Service is responsible for managing events, including their details and seats.
Each event has a version field used for optimistic locking, ensuring that the state of the event does not get changed concurrently by multiple updates.
### 4. Concurrency and Locking Mechanisms
Have Event driven flow where the Validation and accepting booking is done based on Redis atomic key and actual booking is done asynchronously by service using kafka consumers.
### 5. Transactional Management
Spring Transaction Management is used to handle the entire ticket booking process in a single transaction, ensuring atomicity.
JpaTransactionManager manages the commits and rollbacks to maintain consistency during the booking process.
### 6, For Real time updates:
We can use websocket to establish realtime message updates. Enable real-time updates of seat availability to all clients viewing an event page.
Redis Pub/Sub to broadcast seat status updates across distributed services.
Spring WebSockets to push updates from server to frontend clients.


## Design Overview

### Architecture
The system is built with modularized code. Can be converted to microservice and ran separatly. The current modules include:
1. Auth-Service: Handles user authentication, JWT token generation, and validation.
2. Event-Service: Manages event details, including ticket availability, pricing, and scheduling.
3. Booking-Service: Manages ticket bookings, using concurrency mechanisms and transactional management to ensure atomicity. Can be deployed as seperate pods for publisher and consumer.
4. Payment-Service: Future scope - integrate with Razorpay
5. Notification-Service: Future scope - Sends email notifications to users upon successful booking.

The architecture is flexible and allows for easy extension of features, especially in terms of integrating a more distributed setup in the future.

### Database Schema
The system uses a PostgreSQL database, with the following key entities:
1. User: Represents a registered user in the system.
Fields: id, username, email, password, roles.
2. Event: Represents an event available for booking.
Fields: id, name, description, price, total_seats, start_time, end_time, location, version (for optimistic locking),booking_open.
4. Ticket: Represents a booking for a specific event.
Fields: booking_ref(id), user_id, event_id, booking_time, status, price.
5. Seats
Fields: id, booking_ref,seat_id, status,event_id

### Redis Key Structure
1. event:{event_id}  → This ensures fast read operations and validation for events.
2. event:{eventId}:seat:{seatId}  -> This will help in lock for seats which will help FIFS Booking.

### Flow of a Ticket Booking
1. A user logs in and receives a JWT token.
2. The user sends a booking request for an event. 
3. Based on if seats is available from Redis, booking request is validated and pushed to kafka.
4. The Booking Service fetches the event details and attempts to book the requested seats of tickets.
5. One more check to see if the event has seats available, the booking is processed, and the ticket is reserved.
6. If there’s a concurrency conflict (e.g., another user booked the same seat), the system retries the operation using the exponential backoff strategy.
7. Once the booking is successfully processed, the Payment Service simulates payment processing.
8. An email notification is sent to the user to confirm the booking. (Current implementation is logging)

## Setup and Installation

### Prerequisites
1. JDK 21 or higher
2. PostgreSQL database
3. Gradle for dependency management
4. Spring Boot 3.x
5. Redis
6. Kafka

### Steps to Run the Application
Clone the repository:
```
git clone https://github.com/yourusername/ticket-booking-system.git
cd ticket-booking-system
```
Set up the PostgreSQL database and configure the application.yml file with the correct database credentials.
Build the project:
Using grade:
```
gradle clean build
```
The application will start on port 8007 by default.

## Conclusion

This system is designed to efficiently handle high-concurrency ticket bookings, ensuring data consistency and correctness.