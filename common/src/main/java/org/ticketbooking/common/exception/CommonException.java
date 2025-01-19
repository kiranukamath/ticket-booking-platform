package org.ticketbooking.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommonException extends Exception{
    private String code;
    private String details;

    public CommonException(){
        super();
    }

    public CommonException(String details){
        super(details);
    }

    public CommonException(String details, String code){
        super(details);
        this.code = code;
    }

    public CommonException(String details, HttpStatus status) {
        super(details);
        this.code = String.valueOf(status);
    }
}
