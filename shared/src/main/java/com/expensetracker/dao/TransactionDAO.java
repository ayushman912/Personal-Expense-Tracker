package com.expensetracker.dao;

import com.expensetracker.model.Transaction;
import com.expensetracker.exceptions.DatabaseException;
import java.time.LocalDate;
import java.util.List;

/**
 * DAO interface for Transaction operations.
 * Demonstrates interface-based design and separation of concerns.
 * All operations are scoped by userId to ensure data isolation between users.
 */
public interface TransactionDAO {
    
    /**
     * Insert a new transaction into the database for a specific user.
     * @param transaction The transaction to insert
     * @param userId The ID of the user who owns this transaction
     * @return The generated ID of the inserted transaction
     * @throws DatabaseException if database operation fails
     */
    int insert(Transaction transaction, int userId) throws DatabaseException;
    
    /**
     * Insert a new transaction into the database (legacy - uses transaction.userId).
     * @param transaction The transaction to insert (must have userId set)
     * @return The generated ID of the inserted transaction
     * @throws DatabaseException if database operation fails
     */
    int insert(Transaction transaction) throws DatabaseException;
    
    /**
     * Update an existing transaction.
     * @param transaction The transaction to update
     * @throws DatabaseException if database operation fails
     */
    void update(Transaction transaction) throws DatabaseException;
    
    /**
     * Update an existing transaction, ensuring it belongs to the specified user.
     * @param transaction The transaction to update
     * @param userId The ID of the user who owns this transaction
     * @throws DatabaseException if database operation fails
     */
    void updateByUser(Transaction transaction, int userId) throws DatabaseException;
    
    /**
     * Delete a transaction by ID.
     * @param id The ID of the transaction to delete
     * @throws DatabaseException if database operation fails
     */
    void delete(int id) throws DatabaseException;
    
    /**
     * Delete a transaction by ID, ensuring it belongs to the specified user.
     * @param id The ID of the transaction to delete
     * @param userId The ID of the user who owns this transaction
     * @throws DatabaseException if database operation fails
     */
    void deleteByUser(int id, int userId) throws DatabaseException;
    
    /**
     * Find a transaction by ID.
     * @param id The ID of the transaction
     * @return The transaction, or null if not found
     * @throws DatabaseException if database operation fails
     */
    Transaction findById(int id) throws DatabaseException;
    
    /**
     * Get all transactions for a specific user.
     * Data is strictly scoped per user to prevent leakage.
     * @param userId The ID of the user
     * @return List of all transactions belonging to the user
     * @throws DatabaseException if database operation fails
     */
    List<Transaction> findAllByUser(int userId) throws DatabaseException;
    
    /**
     * Get all transactions (legacy method - should use findAllByUser for isolation).
     * @return List of all transactions
     * @throws DatabaseException if database operation fails
     */
    List<Transaction> findAll() throws DatabaseException;
    
    /**
     * Get transactions filtered by date range for a specific user.
     * @param userId The ID of the user
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of transactions in the date range belonging to the user
     * @throws DatabaseException if database operation fails
     */
    List<Transaction> findByDateRangeAndUser(int userId, LocalDate startDate, LocalDate endDate) throws DatabaseException;
    
    /**
     * Get transactions filtered by date range (legacy - no user filtering).
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of transactions in the date range
     * @throws DatabaseException if database operation fails
     */
    List<Transaction> findByDateRange(LocalDate startDate, LocalDate endDate) throws DatabaseException;
    
    /**
     * Get transactions filtered by category for a specific user.
     * @param userId The ID of the user
     * @param categoryId The category ID
     * @return List of transactions in the category belonging to the user
     * @throws DatabaseException if database operation fails
     */
    List<Transaction> findByCategoryAndUser(int userId, int categoryId) throws DatabaseException;
    
    /**
     * Get transactions filtered by category (legacy - no user filtering).
     * @param categoryId The category ID
     * @return List of transactions in the category
     * @throws DatabaseException if database operation fails
     */
    List<Transaction> findByCategory(int categoryId) throws DatabaseException;
    
    /**
     * Get transactions filtered by date range and category for a specific user.
     * @param userId The ID of the user
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param categoryId The category ID
     * @return List of matching transactions belonging to the user
     * @throws DatabaseException if database operation fails
     */
    List<Transaction> findByDateRangeAndCategoryAndUser(int userId, LocalDate startDate, LocalDate endDate, int categoryId) throws DatabaseException;
    
    /**
     * Get transactions filtered by date range and category (legacy - no user filtering).
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param categoryId The category ID
     * @return List of matching transactions
     * @throws DatabaseException if database operation fails
     */
    List<Transaction> findByDateRangeAndCategory(LocalDate startDate, LocalDate endDate, int categoryId) throws DatabaseException;
    
    /**
     * Batch insert transactions for efficient bulk operations.
     * @param transactions List of transactions to insert
     * @throws DatabaseException if database operation fails
     */
    void batchInsert(List<Transaction> transactions) throws DatabaseException;
}

