package com.college.scholarship;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiErrorHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,Object>> responseStatus(ResponseStatusException ex){return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message",ex.getReason()==null?"Request failed.":ex.getReason(),"status",ex.getStatusCode().value()));}
}
