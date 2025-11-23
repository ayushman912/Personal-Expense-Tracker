package com.expensetracker.dao;

import com.expensetracker.model.Transaction;
import com.expensetracker.exceptions.DatabaseException;
import java.time.LocalDate;
import java.util.List;

/**
 * DAO interface for Transaction operations.
 * Demonstrates interface-based design and separation of concerns.
 */
public interface TransactionDAO {
    
    /**
     * Insert a new transaction into the database.
     * @param transaction The transaction to insert
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
     * Delete a transaction by ID.
     * @param id The ID of the transaction to delete
     * @throws DatabaseException if database operation fails
     */
    void delete(int id) throws DatabaseException;
    
    /**
     * Find a transaction by ID.
     * @param id The ID of the transaction
     * @return The transaction, or null if not found
     * @throws DatabaseException if database operation fails
     */
    Transaction findById(int id) throws DatabaseException;
    
    /**
     * Get all transactions.
     * @return List of all transactions
     * @throws DatabaseException if database operation fails
     */
    List<Transaction> findAll() throws DatabaseException;
    
    /**
     * Get transactions filtered by date range.
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of transactions in the date range
     * @throws DatabaseException if database operation fails
     */
    List<Transaction> findByDateRange(LocalDate startDate, LocalDate endDate) throws DatabaseException;
    
    /**
     * Get transactions filtered by category.
     * @param categoryId The category ID
     * @return List of transactions in the category
     * @throws DatabaseException if database operation fails
     */
    List<Transaction> findByCategory(int categoryId) throws DatabaseException;
    
    /**
     * Get transactions filtered by date range and category.
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

