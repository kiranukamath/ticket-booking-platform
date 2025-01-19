package org.ticketbooking.common.model;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class CommonExceptionHandler {
    
    @ExceptionHandler(CommonException.class)
    public ResponseEntity<ErrorDetails> handleCommonException(WebRequest req, CommonException ex){
        log.error("Error", ex.getMessage());
        String msg = ex.getMessage();
        String code = ex.getCode();

        return new ResponseEntity<>(new ErrorDetails(msg,code), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
