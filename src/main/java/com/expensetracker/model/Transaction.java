package com.expensetracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Abstract base class for all financial transactions.
 * Demonstrates OOP principles: abstraction, inheritance, and polymorphism.
 * Subclasses: Expense and Income
 */
public abstract class Transaction {
    protected int id;
    protected BigDecimal amount;
    protected String description;
    protected LocalDate date;
    protected int categoryId;
    protected String categoryName;
    
    /**
     * Constructor for creating a new transaction (no ID yet).
     */
    public Transaction(BigDecimal amount, String description, LocalDate date, int categoryId) {
        this.amount = amount;
        this.description = description;
        this.date = date;
        this.categoryId = categoryId;
    }
    
    /**
     * Constructor for loading existing transaction from database.
     */
    public Transaction(int id, BigDecimal amount, String description, LocalDate date, 
                      int categoryId, String categoryName) {
        this.id = id;
        this.amount = amount;
        this.description = description;
        this.date = date;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }
    
    /**
     * Abstract method to get transaction type.
     * Must be implemented by subclasses.
     */
    public abstract String getType();
    
    /**
     * Abstract method to get the sign-adjusted amount.
     * Expenses return negative, Income returns positive.
     */
    public abstract BigDecimal getSignedAmount();
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDate getDate() {
        return date;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
    }
    
    public int getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }
    
    public String getCategoryName() {
        return categoryName;
    }
    
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
    
    @Override
    public String toString() {
        return String.format("%s [ID: %d, Amount: %s, Date: %s, Category: %s]", 
                           getType(), id, amount, date, categoryName);
    }
}

