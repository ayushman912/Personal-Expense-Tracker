package com.expensetracker.model;

/**
 * Represents a transaction category.
 * Used for both expenses and income categorization.
 */
public class Category {
    private int id;
    private String name;
    private String type; // "EXPENSE" or "INCOME"
    private String description;
    
    public Category(int id, String name, String type, String description) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.description = description;
    }
    
    public Category(String name, String type, String description) {
        this.name = name;
        this.type = type;
        this.description = description;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return name;
    }
}

