package org.ticketbooking.common.model;

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

    public CommonException(String details, String code){
        super(details);
        this.code = code;
    }
}
