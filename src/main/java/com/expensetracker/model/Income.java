package com.expensetracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents an income transaction.
 * Extends Transaction abstract class.
 * Demonstrates inheritance and polymorphism.
 */
public class Income extends Transaction {
    
    /**
     * Constructor for creating a new income.
     */
    public Income(BigDecimal amount, String description, LocalDate date, int categoryId) {
        super(amount, description, date, categoryId);
    }
    
    /**
     * Constructor for loading existing income from database.
     */
    public Income(int id, BigDecimal amount, String description, LocalDate date, 
                 int categoryId, String categoryName) {
        super(id, amount, description, date, categoryId, categoryName);
    }
    
    @Override
    public String getType() {
        return "Income";
    }
    
    @Override
    public BigDecimal getSignedAmount() {
        // Income is positive for calculations
        return amount;
    }
}

