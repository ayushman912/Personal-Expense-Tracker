package com.expensetracker.exceptions;

/**
 * Custom exception for validation errors.
 * Thrown when user input or data doesn't meet business rules.
 */
public class ValidationException extends Exception {
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

