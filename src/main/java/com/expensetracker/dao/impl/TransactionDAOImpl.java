package com.expensetracker.dao.impl;

import com.expensetracker.dao.TransactionDAO;
import com.expensetracker.model.Transaction;
import com.expensetracker.model.Expense;
import com.expensetracker.model.Income;
import com.expensetracker.exceptions.DatabaseException;
import com.expensetracker.util.DatabaseManager;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of TransactionDAO interface.
 * Uses prepared statements to prevent SQL injection.
 * Implements batch operations and transaction management.
 */
public class TransactionDAOImpl implements TransactionDAO {
    
    private static final String INSERT_SQL = 
        "INSERT INTO transactions (amount, description, date, category_id, type) VALUES (?, ?, ?, ?, ?)";
    
    private static final String UPDATE_SQL = 
        "UPDATE transactions SET amount = ?, description = ?, date = ?, category_id = ? WHERE id = ?";
    
    private static final String DELETE_SQL = "DELETE FROM transactions WHERE id = ?";
    
    private static final String FIND_BY_ID_SQL = 
        "SELECT t.id, t.amount, t.description, t.date, t.category_id, t.type, c.name as category_name " +
        "FROM transactions t LEFT JOIN categories c ON t.category_id = c.id WHERE t.id = ?";
    
    private static final String FIND_ALL_SQL = 
        "SELECT t.id, t.amount, t.description, t.date, t.category_id, t.type, c.name as category_name " +
        "FROM transactions t LEFT JOIN categories c ON t.category_id = c.id ORDER BY t.date DESC";
    
    private static final String FIND_BY_DATE_RANGE_SQL = 
        "SELECT t.id, t.amount, t.description, t.date, t.category_id, t.type, c.name as category_name " +
        "FROM transactions t LEFT JOIN categories c ON t.category_id = c.id " +
        "WHERE t.date BETWEEN ? AND ? ORDER BY t.date DESC";
    
    private static final String FIND_BY_CATEGORY_SQL = 
        "SELECT t.id, t.amount, t.description, t.date, t.category_id, t.type, c.name as category_name " +
        "FROM transactions t LEFT JOIN categories c ON t.category_id = c.id " +
        "WHERE t.category_id = ? ORDER BY t.date DESC";
    
    private static final String FIND_BY_DATE_RANGE_AND_CATEGORY_SQL = 
        "SELECT t.id, t.amount, t.description, t.date, t.category_id, t.type, c.name as category_name " +
        "FROM transactions t LEFT JOIN categories c ON t.category_id = c.id " +
        "WHERE t.date BETWEEN ? AND ? AND t.category_id = ? ORDER BY t.date DESC";
    
    @Override
    public int insert(Transaction transaction) throws DatabaseException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setBigDecimal(1, transaction.getAmount());
            stmt.setString(2, transaction.getDescription());
            stmt.setDate(3, Date.valueOf(transaction.getDate()));
            stmt.setInt(4, transaction.getCategoryId());
            stmt.setString(5, transaction.getType());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DatabaseException("Failed to insert transaction");
            }
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new DatabaseException("Failed to get generated ID");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error inserting transaction", e);
        }
    }
    
    @Override
    public void update(Transaction transaction) throws DatabaseException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {
            
            stmt.setBigDecimal(1, transaction.getAmount());
            stmt.setString(2, transaction.getDescription());
            stmt.setDate(3, Date.valueOf(transaction.getDate()));
            stmt.setInt(4, transaction.getCategoryId());
            stmt.setInt(5, transaction.getId());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DatabaseException("Transaction not found for update");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error updating transaction", e);
        }
    }
    
    @Override
    public void delete(int id) throws DatabaseException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {
            
            stmt.setInt(1, id);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DatabaseException("Transaction not found for deletion");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting transaction", e);
        }
    }
    
    @Override
    public Transaction findById(int id) throws DatabaseException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_BY_ID_SQL)) {
            
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToTransaction(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding transaction by ID", e);
        }
    }
    
    @Override
    public List<Transaction> findAll() throws DatabaseException {
        List<Transaction> transactions = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_ALL_SQL);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                transactions.add(mapResultSetToTransaction(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding all transactions", e);
        }
        return transactions;
    }
    
    @Override
    public List<Transaction> findByDateRange(LocalDate startDate, LocalDate endDate) throws DatabaseException {
        List<Transaction> transactions = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_BY_DATE_RANGE_SQL)) {
            
            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding transactions by date range", e);
        }
        return transactions;
    }
    
    @Override
    public List<Transaction> findByCategory(int categoryId) throws DatabaseException {
        List<Transaction> transactions = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_BY_CATEGORY_SQL)) {
            
            stmt.setInt(1, categoryId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding transactions by category", e);
        }
        return transactions;
    }
    
    @Override
    public List<Transaction> findByDateRangeAndCategory(LocalDate startDate, LocalDate endDate, int categoryId) 
            throws DatabaseException {
        List<Transaction> transactions = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_BY_DATE_RANGE_AND_CATEGORY_SQL)) {
            
            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));
            stmt.setInt(3, categoryId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding transactions by date range and category", e);
        }
        return transactions;
    }
    
    @Override
    public void batchInsert(List<Transaction> transactions) throws DatabaseException {
        if (transactions == null || transactions.isEmpty()) {
            return;
        }
        
        Connection conn = null;
        try {
            conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(false); // Start transaction
            
            try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL)) {
                for (Transaction transaction : transactions) {
                    stmt.setBigDecimal(1, transaction.getAmount());
                    stmt.setString(2, transaction.getDescription());
                    stmt.setDate(3, Date.valueOf(transaction.getDate()));
                    stmt.setInt(4, transaction.getCategoryId());
                    stmt.setString(5, transaction.getType());
                    stmt.addBatch();
                }
                
                stmt.executeBatch();
                conn.commit(); // Commit transaction
            } catch (SQLException e) {
                if (conn != null) {
                    conn.rollback(); // Rollback on error
                }
                throw new DatabaseException("Error in batch insert", e);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error setting up batch insert", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    // Log error
                }
            }
        }
    }
    
    /**
     * Maps a ResultSet row to a Transaction object.
     * Uses polymorphism to create Expense or Income based on type.
     */
    private Transaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        java.math.BigDecimal amount = rs.getBigDecimal("amount");
        String description = rs.getString("description");
        LocalDate date = rs.getDate("date").toLocalDate();
        int categoryId = rs.getInt("category_id");
        String type = rs.getString("type");
        String categoryName = rs.getString("category_name");
        
        if ("Expense".equals(type)) {
            return new Expense(id, amount, description, date, categoryId, categoryName);
        } else {
            return new Income(id, amount, description, date, categoryId, categoryName);
        }
    }
}

