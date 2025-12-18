package com.expensetracker.model;

/**
 * Represents a transaction category.
 * Used for both expenses and income categorization.
 * 
 * Categories are user-scoped to prevent data leakage between users.
 * Each user has their own independent set of categories.
 */
public class Category {
    private int id;
    private int userId; // User ownership for data isolation - prevents cross-user category leakage
    private String name;
    private String type; // "EXPENSE" or "INCOME"
    private String description;
    
    public Category(int id, String name, String type, String description) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.description = description;
    }

    public Category(int id, int userId, String name, String type, String description) {
        this.id = id;
        this.userId = userId;
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

    // User ownership for data isolation - each category belongs to exactly one user
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    @Override
    public String toString() {
        return name;
    }
}

