package com.expensetracker.service;

import com.expensetracker.exceptions.ValidationException;
import java.util.regex.Pattern;

/**
 * Abstract base class for service layer.
 * Provides common validation methods.
 * Demonstrates abstract class usage in OOP design.
 */
public abstract class AbstractService {
    
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    
    /**
     * Validate that a string is not null or empty.
     * @param value The string to validate
     * @param fieldName The name of the field for error message
     * @throws ValidationException if validation fails
     */
    protected void validateNotEmpty(String value, String fieldName) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " cannot be empty");
        }
    }
    
    /**
     * Validate that a number is positive.
     * @param value The number to validate
     * @param fieldName The name of the field for error message
     * @throws ValidationException if validation fails
     */
    protected void validatePositive(java.math.BigDecimal value, String fieldName) throws ValidationException {
        if (value == null || value.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new ValidationException(fieldName + " must be positive");
        }
    }
    
    /**
     * Validate email format.
     * @param email The email to validate
     * @throws ValidationException if validation fails
     */
    protected void validateEmail(String email) throws ValidationException {
        validateNotEmpty(email, "Email");
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("Invalid email format");
        }
    }
    
    /**
     * Validate date range.
     * @param startDate Start date
     * @param endDate End date
     * @throws ValidationException if validation fails
     */
    protected void validateDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) 
            throws ValidationException {
        if (startDate == null || endDate == null) {
            throw new ValidationException("Start date and end date are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new ValidationException("Start date cannot be after end date");
        }
    }
}

