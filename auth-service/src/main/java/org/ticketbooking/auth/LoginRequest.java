package org.ticketbooking.auth;

import lombok.Data;

@Data
public class LoginRequest {
    String username; 
    String password;
}
