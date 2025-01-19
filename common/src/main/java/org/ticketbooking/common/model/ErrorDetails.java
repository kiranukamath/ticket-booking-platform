package org.ticketbooking.common.model;


import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class ErrorDetails {
    private String code;
    private String message;

    public ErrorDetails(String message, String code){
        this.code = code;
        this.message = message;
    }
}
