package com.expensetracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents an expense transaction.
 * Extends Transaction abstract class.
 * Demonstrates inheritance and polymorphism.
 */
public class Expense extends Transaction {
    
    /**
     * Constructor for creating a new expense.
     */
    public Expense(BigDecimal amount, String description, LocalDate date, int categoryId) {
        super(amount, description, date, categoryId);
    }
    
    /**
     * Constructor for loading existing expense from database.
     */
    public Expense(int id, BigDecimal amount, String description, LocalDate date, 
                  int categoryId, String categoryName) {
        super(id, amount, description, date, categoryId, categoryName);
    }
    
    @Override
    public String getType() {
        return "Expense";
    }
    
    @Override
    public BigDecimal getSignedAmount() {
        // Expenses are negative for calculations
        return amount.negate();
    }
}

