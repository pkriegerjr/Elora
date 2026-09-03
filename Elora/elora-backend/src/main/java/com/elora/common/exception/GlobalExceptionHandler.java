package com.elora.common.exception;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handle(RuntimeException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of("erro", ex.getMessage()));
    }
    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<?> handleNotFound(jakarta.persistence.EntityNotFoundException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(java.util.Map.of("erro", ex.getMessage()));
    }
}
