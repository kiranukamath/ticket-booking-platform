# Ticket Booking System - Backend Documentation

## Overview

This document outlines the design and architecture of the Ticket Booking System, built using Java 21 with Spring Boot. The system focuses on handling high concurrency, ensuring data consistency, and implementing features like authentication and authorization. 
It is designed to handle complex ticket booking rules, such as prioritizing users based on arrival, ticket quantity, and managing concurrency in the booking process.

The system also includes key features like JWT-based authentication, event-driven architecture, and transactional management. In this phase, we have covered critical areas such as concurrency management, optimistic locking, transaction management, and retry mechanisms.

## Key Features Implemented

### 1. Authentication with JWT
JWT (JSON Web Token) is used for user authentication and authorization.
Spring Security is integrated to handle the security aspect of the application.
The JWT token is created after user login and includes claims like the user details, roles, and expiration time.
JWT secret key is stored securely in application.yml to sign and validate tokens.
JWTFilter is used to intercept requests and validate tokens.
### 2. Ticket Booking Flow
The core functionality of the system involves booking tickets for events.
The system handles concurrency using Redis atomic keys and reliability with optimistic locking. The version field is used to check for updates to the ticket count and prevent conflicts.
Atomicity of the booking process is ensured, meaning either all operations within the booking process succeed, or none of them do.
### 3. Event Handling and Ticket Availability
Event Service is responsible for managing events, including their details and available seats.
Each event has a version field used for optimistic locking, ensuring that the state of the event does not get changed concurrently by multiple transactions.
The Ticket Service uses this version field to lock the event record, ensuring that the ticket booking process is transactional and consistent.
### 4. Concurrency and Locking Mechanisms
Concurrency Handling is managed and also handled for scenarios where tickets are booked at the first minute. Have Event driven flow where the Validation and accepting booking is done based on Redis atomic key and actual booking is done asynchronously by service using kafka consumers.
Retry Mechanism: An exponential backoff strategy was implemented using Spring's @Retryable to handle retries for optimistic locking failures (like OptimisticLockingFailureException). The backoff delay starts at 1 second and doubles after each failure, ensuring that the database doesn't get overwhelmed with retries.
### 5. Transactional Management
Spring Transaction Management is used to handle the entire ticket booking process in a single transaction, ensuring atomicity.
JpaTransactionManager manages the commits and rollbacks to maintain consistency during the booking process.

## Design Overview

### Architecture
The system is built with a monolithic architecture but code is modularized. Can be converted to microservice and ran separatly. The current modules include:
1. Auth-Service: Handles user authentication, JWT token generation, and validation.
2. Event-Service: Manages event details, including ticket availability, pricing, and scheduling.
3. Booking-Service: Manages ticket bookings, using concurrency mechanisms and transactional management to ensure atomicity.
4. Payment-Service: Future scope - integrate with Razorpay
5. Notification-Service: Future scope - Sends email notifications to users upon successful booking.

The architecture is flexible and allows for easy extension of features, especially in terms of integrating a more distributed setup in the future.

### Database Schema
The system uses a PostgreSQL database, with the following key entities:
1. User: Represents a registered user in the system.
Fields: id, username, email, password, roles.
2. Event: Represents an event available for booking.
Fields: id, name, description, price, available_seats, capacity, start_time, end_time, location, version (for optimistic locking).
3. Ticket: Represents a booking for a specific event.
Fields: id, user_id, event_id, quantity, booking_ref, booking_time, status, price.

### Flow of a Ticket Booking
1. A user logs in and receives a JWT token.
2. The user sends a booking request for an event. 
3. Based on available seats from Redis booking request is validated and pushed to kafka.
4. The Booking Service fetches the event details and attempts to book the requested quantity of tickets.
5. One more check to see if the event has enough available seats, the booking is processed, and the ticket is reserved.
6. If there’s a concurrency conflict (e.g., another user booked the same ticket), the system retries the operation using the exponential backoff strategy.
7. Once the booking is successfully processed, the Payment Service simulates payment processing.
8. An email notification is sent to the user to confirm the booking.

### Future Enhancements

1. Refresh Token Implementation
Implementing a refresh token mechanism to allow users to stay logged in without needing to re-enter their credentials. The refresh token will be issued alongside the access token and can be used to obtain a new access token when the current one expires.
This feature will require the creation of a refresh token endpoint, validation of refresh tokens, and proper handling of refresh token expiration.
2. An email notification is sent to the user to confirm the booking.

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

This system is designed to efficiently handle high-concurrency ticket bookings, ensuring data consistency and correctness. The optimistic locking mechanism, along with retry strategies, ensures that bookings are processed correctly even during high traffic periods. As we continue to build and improve the system, features like refresh tokens and distributed architecture will further enhance scalability and user experience.

