package org.ticketbooking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/util")
public class Util {
    private static final Logger logger = LoggerFactory.getLogger(Util.class);

    @GetMapping("/info")
    public ResponseEntity<String> info() {
        logger.info("Info endpoint hit");
        System.out.println("INOR ENDPOINT accessed");
        return ResponseEntity.ok("UP");
    }
}
