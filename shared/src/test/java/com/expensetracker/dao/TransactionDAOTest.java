package com.expensetracker.dao;

import com.expensetracker.dao.impl.TransactionDAOImpl;
import com.expensetracker.dao.impl.CategoryDAOImpl;
import com.expensetracker.model.Category;
import com.expensetracker.model.Transaction;
import com.expensetracker.model.Expense;
import com.expensetracker.util.DatabaseManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionDAOTest {

    private TransactionDAO transactionDAO;
    private CategoryDAO categoryDAO;

    @BeforeAll
    static void setupDB() {
        // Use in-memory DB for testing
        DatabaseManager.setDbUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE");
    }

    @BeforeEach
    void setUp() throws Exception {
        // Reset singleton to force re-initialization with new URL if needed,
        // but since we set URL in BeforeAll and getInstance checks null, we might need
        // to reset instance via reflection or just close it.
        // Actually DatabaseManager doesn't have a reset method.
        // For simplicity, we rely on the fact that this test runs in isolation or we
        // add a reset method.
        // Let's assume we can just get instance.

        // Re-initialize tables
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute("DROP ALL OBJECTS");
        }
        
        DatabaseManager.getInstance().initializeDatabase();
        
        transactionDAO = new TransactionDAOImpl();
        categoryDAO = new CategoryDAOImpl();
    }

    @Test
    void testInsertAndFind() throws Exception {
        // Setup category with user_id (admin user has id=1 from initializeDatabase)
        Category cat = new Category("TestCat", "EXPENSE", "Desc");
        cat.setUserId(1); // Categories are user-scoped for data isolation
        int catId = categoryDAO.insert(cat);

        // Transaction requires userId for data isolation - use admin user (id=1)
        Transaction t = new Expense(new BigDecimal("100.00"), "Test Transaction", LocalDate.now(), catId);
        t.setUserId(1); // User ownership required for insert
        int id = transactionDAO.insert(t);

        Transaction found = transactionDAO.findById(id);
        assertNotNull(found);
        assertEquals(new BigDecimal("100.00"), found.getAmount());
        assertEquals("Test Transaction", found.getDescription());
    }
}
