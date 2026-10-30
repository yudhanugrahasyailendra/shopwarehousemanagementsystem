package com.example.inventory.exception;

/** Request is syntactically valid but breaks a business rule (reported as HTTP 400). */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
