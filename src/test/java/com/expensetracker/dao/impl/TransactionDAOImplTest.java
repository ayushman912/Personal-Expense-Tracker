package com.expensetracker.dao.impl;

import com.expensetracker.dao.TransactionDAO;
import com.expensetracker.exceptions.DatabaseException;
import com.expensetracker.model.Expense;
import com.expensetracker.model.Income;
import com.expensetracker.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TransactionDAOImpl.
 * Note: These tests require a running MySQL database.
 * Disable if database is not available.
 */
@Disabled("Requires database connection")
class TransactionDAOImplTest {
    
    private TransactionDAO transactionDAO;
    
    @BeforeEach
    void setUp() {
        transactionDAO = new TransactionDAOImpl();
    }
    
    @Test
    void testInsertExpense() throws DatabaseException {
        Expense expense = new Expense(
            new BigDecimal("50.00"),
            "Test expense",
            LocalDate.now(),
            1
        );
        
        int id = transactionDAO.insert(expense);
        assertTrue(id > 0);
        
        Transaction found = transactionDAO.findById(id);
        assertNotNull(found);
        assertTrue(found instanceof Expense);
        assertEquals("Test expense", found.getDescription());
    }
    
    @Test
    void testInsertIncome() throws DatabaseException {
        Income income = new Income(
            new BigDecimal("1000.00"),
            "Test income",
            LocalDate.now(),
            6
        );
        
        int id = transactionDAO.insert(income);
        assertTrue(id > 0);
        
        Transaction found = transactionDAO.findById(id);
        assertNotNull(found);
        assertTrue(found instanceof Income);
    }
    
    @Test
    void testUpdate() throws DatabaseException {
        Expense expense = new Expense(
            new BigDecimal("50.00"),
            "Original description",
            LocalDate.now(),
            1
        );
        
        int id = transactionDAO.insert(expense);
        expense.setId(id);
        expense.setDescription("Updated description");
        
        transactionDAO.update(expense);
        
        Transaction updated = transactionDAO.findById(id);
        assertEquals("Updated description", updated.getDescription());
    }
    
    @Test
    void testDelete() throws DatabaseException {
        Expense expense = new Expense(
            new BigDecimal("50.00"),
            "To be deleted",
            LocalDate.now(),
            1
        );
        
        int id = transactionDAO.insert(expense);
        transactionDAO.delete(id);
        
        Transaction deleted = transactionDAO.findById(id);
        assertNull(deleted);
    }
    
    @Test
    void testFindByDateRange() throws DatabaseException {
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();
        
        List<Transaction> transactions = transactionDAO.findByDateRange(start, end);
        assertNotNull(transactions);
    }
    
    @Test
    void testBatchInsert() throws DatabaseException {
        List<Transaction> transactions = List.of(
            new Expense(new BigDecimal("10.00"), "Batch 1", LocalDate.now(), 1),
            new Expense(new BigDecimal("20.00"), "Batch 2", LocalDate.now(), 1),
            new Income(new BigDecimal("30.00"), "Batch 3", LocalDate.now(), 6)
        );
        
        transactionDAO.batchInsert(transactions);
        
        List<Transaction> all = transactionDAO.findAll();
        assertTrue(all.size() >= transactions.size());
    }
}

