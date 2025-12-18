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

    // User-scoped INSERT - data is strictly scoped per user to prevent leakage
    private static final String INSERT_SQL = "INSERT INTO transactions (user_id, amount, description, date, category_id, type) VALUES (?, ?, ?, ?, ?, ?)";

    // User-scoped UPDATE - ensures user can only update their own transactions
    private static final String UPDATE_SQL = "UPDATE transactions SET amount = ?, description = ?, date = ?, category_id = ? WHERE id = ? AND user_id = ?";

    // User-scoped DELETE - ensures user can only delete their own transactions
    private static final String DELETE_SQL = "DELETE FROM transactions WHERE id = ? AND user_id = ?";

    private static final String FIND_BY_ID_SQL = "SELECT t.id, t.user_id, t.amount, t.description, t.date, t.category_id, t.type, c.name as category_name "
            +
            "FROM transactions t LEFT JOIN categories c ON t.category_id = c.id WHERE t.id = ?";

    // User-scoped FIND ALL - each user sees only their own transactions
    // User-scoped queries - all queries filter by user_id for data isolation
    private static final String FIND_ALL_BY_USER_SQL = 
            "SELECT t.id, t.user_id, t.amount, t.description, t.date, t.category_id, t.type, c.name as category_name " +
            "FROM transactions t LEFT JOIN categories c ON t.category_id = c.id WHERE t.user_id = ? ORDER BY t.date DESC";

    private static final String FIND_BY_DATE_RANGE_AND_USER_SQL = 
            "SELECT t.id, t.user_id, t.amount, t.description, t.date, t.category_id, t.type, c.name as category_name " +
            "FROM transactions t LEFT JOIN categories c ON t.category_id = c.id " +
            "WHERE t.user_id = ? AND t.date BETWEEN ? AND ? ORDER BY t.date DESC";

    private static final String FIND_BY_CATEGORY_AND_USER_SQL = 
            "SELECT t.id, t.user_id, t.amount, t.description, t.date, t.category_id, t.type, c.name as category_name " +
            "FROM transactions t LEFT JOIN categories c ON t.category_id = c.id " +
            "WHERE t.user_id = ? AND t.category_id = ? ORDER BY t.date DESC";

    private static final String FIND_BY_DATE_RANGE_AND_CATEGORY_AND_USER_SQL = 
            "SELECT t.id, t.user_id, t.amount, t.description, t.date, t.category_id, t.type, c.name as category_name " +
            "FROM transactions t LEFT JOIN categories c ON t.category_id = c.id " +
            "WHERE t.user_id = ? AND t.date BETWEEN ? AND ? AND t.category_id = ? ORDER BY t.date DESC";

    @Override
    public int insert(Transaction transaction, int userId) throws DatabaseException {
        // Data is strictly scoped per user to prevent leakage
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, userId);
            stmt.setBigDecimal(2, transaction.getAmount());
            stmt.setString(3, transaction.getDescription());
            stmt.setDate(4, Date.valueOf(transaction.getDate()));
            stmt.setInt(5, transaction.getCategoryId());
            stmt.setString(6, transaction.getType());

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
    public int insert(Transaction transaction) throws DatabaseException {
        // Uses userId from transaction object
        if (transaction.getUserId() <= 0) {
            throw new DatabaseException("Transaction must have a valid userId for data isolation");
        }
        return insert(transaction, transaction.getUserId());
    }

    @Override
    public void update(Transaction transaction) throws DatabaseException {
        // Legacy method - requires transaction to have userId set
        updateByUser(transaction, transaction.getUserId());
    }

    @Override
    public void updateByUser(Transaction transaction, int userId) throws DatabaseException {
        // Data is strictly scoped per user to prevent leakage
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {

            stmt.setBigDecimal(1, transaction.getAmount());
            stmt.setString(2, transaction.getDescription());
            stmt.setDate(3, Date.valueOf(transaction.getDate()));
            stmt.setInt(4, transaction.getCategoryId());
            stmt.setInt(5, transaction.getId());
            stmt.setInt(6, userId); // Ensures user can only update their own transactions

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DatabaseException("Transaction not found for update or access denied");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error updating transaction", e);
        }
    }

    @Override
    public void delete(int id) throws DatabaseException {
        // Legacy method - cannot enforce user ownership, throws exception
        throw new DatabaseException("delete(int id) is deprecated. Use deleteByUser(int id, int userId) instead.");
    }

    @Override
    public void deleteByUser(int id, int userId) throws DatabaseException {
        // Data is strictly scoped per user to prevent leakage
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {

            stmt.setInt(1, id);
            stmt.setInt(2, userId); // Ensures user can only delete their own transactions
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DatabaseException("Transaction not found for deletion or access denied");
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
    public List<Transaction> findAllByUser(int userId) throws DatabaseException {
        // Data is strictly scoped per user to prevent leakage
        List<Transaction> transactions = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(FIND_ALL_BY_USER_SQL)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding transactions for user", e);
        }
        return transactions;
    }

    @Override
    public List<Transaction> findAll() throws DatabaseException {
        // Enforce strict data isolation - disable global access
        throw new DatabaseException("Global data access is not allowed. Use findAllByUser(userId).");
    }

    @Override
    public List<Transaction> findByDateRangeAndUser(int userId, LocalDate startDate, LocalDate endDate)
            throws DatabaseException {
        // Data is strictly scoped per user to prevent leakage
        List<Transaction> transactions = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(FIND_BY_DATE_RANGE_AND_USER_SQL)) {

            stmt.setInt(1, userId);
            stmt.setDate(2, Date.valueOf(startDate));
            stmt.setDate(3, Date.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding transactions by date range for user", e);
        }
        return transactions;
    }

    @Override
    public List<Transaction> findByDateRange(LocalDate startDate, LocalDate endDate) throws DatabaseException {
        // Enforce strict data isolation - disable global access
        throw new DatabaseException("Global data access is not allowed. Use findByDateRangeAndUser(userId, ...).");
    }

    @Override
    public List<Transaction> findByCategoryAndUser(int userId, int categoryId) throws DatabaseException {
        // Data is strictly scoped per user to prevent leakage
        List<Transaction> transactions = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(FIND_BY_CATEGORY_AND_USER_SQL)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, categoryId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding transactions by category for user", e);
        }
        return transactions;
    }

    @Override
    public List<Transaction> findByCategory(int categoryId) throws DatabaseException {
        // Enforce strict data isolation - disable global access
        throw new DatabaseException("Global data access is not allowed. Use findByCategoryAndUser(userId, ...).");
    }

    @Override
    public List<Transaction> findByDateRangeAndCategoryAndUser(int userId, LocalDate startDate, LocalDate endDate,
            int categoryId)
            throws DatabaseException {
        // Data is strictly scoped per user to prevent leakage
        List<Transaction> transactions = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(FIND_BY_DATE_RANGE_AND_CATEGORY_AND_USER_SQL)) {

            stmt.setInt(1, userId);
            stmt.setDate(2, Date.valueOf(startDate));
            stmt.setDate(3, Date.valueOf(endDate));
            stmt.setInt(4, categoryId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapResultSetToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding transactions by date range and category for user", e);
        }
        return transactions;
    }

    @Override
    public List<Transaction> findByDateRangeAndCategory(LocalDate startDate, LocalDate endDate, int categoryId)
            throws DatabaseException {
        // Enforce strict data isolation - disable global access
        throw new DatabaseException(
                "Global data access is not allowed. Use findByDateRangeAndCategoryAndUser(userId, ...).");
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
                    if (transaction.getUserId() <= 0) {
                        throw new DatabaseException("Transaction in batch must have a valid userId for data isolation");
                    }
                    // Fix: Set user_id as first parameter (index 1)
                    stmt.setInt(1, transaction.getUserId());
                    stmt.setBigDecimal(2, transaction.getAmount());
                    stmt.setString(3, transaction.getDescription());
                    stmt.setDate(4, Date.valueOf(transaction.getDate()));
                    stmt.setInt(5, transaction.getCategoryId());
                    stmt.setString(6, transaction.getType());
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
     * Includes user_id for data ownership tracking.
     */
    private Transaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int userId = rs.getInt("user_id");
        java.math.BigDecimal amount = rs.getBigDecimal("amount");
        String description = rs.getString("description");
        LocalDate date = rs.getDate("date").toLocalDate();
        int categoryId = rs.getInt("category_id");
        String type = rs.getString("type");
        String categoryName = rs.getString("category_name");

        // Data is strictly scoped per user to prevent leakage
        if ("Expense".equals(type) || "EXPENSE".equals(type)) {
            return new Expense(id, userId, amount, description, date, categoryId, categoryName);
        } else {
            return new Income(id, userId, amount, description, date, categoryId, categoryName);
        }
    }
}
