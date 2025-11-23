package com.expensetracker.exceptions;

/**
 * Custom exception for database-related errors.
 * Used to wrap SQLExceptions and provide meaningful error messages.
 */
public class DatabaseException extends Exception {
    
    public DatabaseException(String message) {
        super(message);
    }
    
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}

