package com.springrest.exception;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials() {
        return new ResponseEntity<>(
                new ErrorResponse("Invalid email or password", 401),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthException() {
        return new ResponseEntity<>(
                new ErrorResponse("Unauthorized access", 401),
                HttpStatus.UNAUTHORIZED
        );
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied() {
        return new ResponseEntity<>(
                new ErrorResponse("Access denied", 403),
                HttpStatus.FORBIDDEN
        );
    }
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustom(CustomException ex) {
        return new ResponseEntity<>(
                new ErrorResponse(ex.getMessage(), ex.getStatus()),
                HttpStatus.valueOf(ex.getStatus())
        );
    }
    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound() {
        return new ResponseEntity<>(
                new ErrorResponse("Resource not found", 404),
                HttpStatus.NOT_FOUND
        );
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex) {
        ex.printStackTrace();

        return new ResponseEntity<>(
                new ErrorResponse("Something went wrong", 500),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}