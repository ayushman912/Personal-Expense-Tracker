package com.expensetracker.dao;

import com.expensetracker.model.Category;
import com.expensetracker.exceptions.DatabaseException;
import java.util.List;

/**
 * DAO interface for Category operations.
 * 
 * Categories are user-scoped to ensure data isolation between users.
 * All operations that access category data should use user-scoped methods.
 */
public interface CategoryDAO {
    
    /**
     * Insert a category. The category must have userId set for data isolation.
     */
    int insert(Category category) throws DatabaseException;
    
    /**
     * Update a category. Only the owning user can update their categories.
     */
    void update(Category category) throws DatabaseException;
    
    /**
     * Delete a category by ID. Use deleteByUser for user-scoped deletion.
     */
    void delete(int id) throws DatabaseException;

    /**
     * Delete a category ensuring user ownership (data isolation).
     */
    void deleteByUser(int id, int userId) throws DatabaseException;
    
    Category findById(int id) throws DatabaseException;
    
    /**
     * Find all categories for a specific user (data isolation).
     * This replaces the global findAll() for user-facing operations.
     */
    List<Category> findAllByUser(int userId) throws DatabaseException;

    /**
     * @deprecated Use findAllByUser(int userId) for proper data isolation.
     * This method returns global/system categories only for backward compatibility.
     */
    @Deprecated
    List<Category> findAll() throws DatabaseException;
    
    /**
     * Find categories by type for a specific user (data isolation).
     */
    List<Category> findByTypeAndUser(String type, int userId) throws DatabaseException;

    /**
     * @deprecated Use findByTypeAndUser for proper data isolation.
     */
    @Deprecated
    List<Category> findByType(String type) throws DatabaseException;
    
    /**
     * Find category by name for a specific user (data isolation).
     */
    Category findByNameAndUser(String name, int userId) throws DatabaseException;

    /**
     * @deprecated Use findByNameAndUser for proper data isolation.
     */
    @Deprecated
    Category findByName(String name) throws DatabaseException;
}

