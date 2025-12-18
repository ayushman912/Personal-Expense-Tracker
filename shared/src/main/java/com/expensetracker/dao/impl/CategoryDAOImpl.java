package com.expensetracker.dao.impl;

import com.expensetracker.dao.CategoryDAO;
import com.expensetracker.model.Category;
import com.expensetracker.exceptions.DatabaseException;
import com.expensetracker.util.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of CategoryDAO interface.
 * Uses prepared statements for all database operations.
 * 
 * Categories are user-scoped to ensure data isolation between users.
 * Why user-scoped: Without user_id, all users share the same categories,
 * causing privacy leaks (User B sees User A's categories) and conflicts
 * (UNIQUE constraint on name+type blocks other users from creating same category).
 */
public class CategoryDAOImpl implements CategoryDAO {
    
    // User-scoped INSERT - categories belong to specific users for data isolation
    private static final String INSERT_SQL = 
        "INSERT INTO categories (user_id, name, type, description) VALUES (?, ?, ?, ?)";
    
    // User-scoped UPDATE - only owner can update their categories
    private static final String UPDATE_SQL = 
        "UPDATE categories SET name = ?, type = ?, description = ? WHERE id = ? AND user_id = ?";
    
    private static final String DELETE_SQL = "DELETE FROM categories WHERE id = ?";

    // User-scoped DELETE - ensures user can only delete their own categories
    private static final String DELETE_BY_USER_SQL = "DELETE FROM categories WHERE id = ? AND user_id = ?";
    
    private static final String FIND_BY_ID_SQL = 
        "SELECT id, user_id, name, type, description FROM categories WHERE id = ?";
    
    // Legacy global query - kept for backward compatibility with system categories
    private static final String FIND_ALL_SQL = 
        "SELECT id, user_id, name, type, description FROM categories WHERE user_id IS NULL OR user_id = 0 ORDER BY CASE WHEN name = 'Other' THEN 1 ELSE 0 END, name";

    // User-scoped FIND ALL - returns only categories belonging to user OR system categories
    private static final String FIND_ALL_BY_USER_SQL = 
        "SELECT id, user_id, name, type, description FROM categories WHERE user_id = ? OR user_id IS NULL OR user_id = 0 ORDER BY CASE WHEN name = 'Other' THEN 1 ELSE 0 END, name";
    
    // Legacy global type query
    private static final String FIND_BY_TYPE_SQL = 
        "SELECT id, user_id, name, type, description FROM categories WHERE type = ? AND (user_id IS NULL OR user_id = 0) ORDER BY CASE WHEN name = 'Other' THEN 1 ELSE 0 END, name";

    // User-scoped type query
    private static final String FIND_BY_TYPE_AND_USER_SQL = 
        "SELECT id, user_id, name, type, description FROM categories WHERE type = ? AND (user_id = ? OR user_id IS NULL OR user_id = 0) ORDER BY CASE WHEN name = 'Other' THEN 1 ELSE 0 END, name";
    
    // Legacy global name query
    private static final String FIND_BY_NAME_SQL = 
        "SELECT id, user_id, name, type, description FROM categories WHERE name = ? AND (user_id IS NULL OR user_id = 0)";

    // User-scoped name query
    private static final String FIND_BY_NAME_AND_USER_SQL = 
        "SELECT id, user_id, name, type, description FROM categories WHERE name = ? AND (user_id = ? OR user_id IS NULL OR user_id = 0)";
    
    @Override
    public int insert(Category category) throws DatabaseException {
        // Categories must have userId for data isolation
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            
            // user_id is required for user-scoped categories; 0 or NULL for system categories
            if (category.getUserId() > 0) {
                stmt.setInt(1, category.getUserId());
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            stmt.setString(2, category.getName());
            stmt.setString(3, category.getType());
            stmt.setString(4, category.getDescription());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DatabaseException("Failed to insert category");
            }
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new DatabaseException("Failed to get generated ID");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error inserting category", e);
        }
    }
    
    @Override
    public void update(Category category) throws DatabaseException {
        // User-scoped update - only owner can update their categories
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {
            
            stmt.setString(1, category.getName());
            stmt.setString(2, category.getType());
            stmt.setString(3, category.getDescription());
            stmt.setInt(4, category.getId());
            // Enforce user ownership
            if (category.getUserId() > 0) {
                stmt.setInt(5, category.getUserId());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DatabaseException("Category not found for update or access denied");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error updating category", e);
        }
    }
    
    @Override
    public void delete(int id) throws DatabaseException {
        // Legacy method - no user ownership check
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {
            
            stmt.setInt(1, id);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DatabaseException("Category not found for deletion");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting category", e);
        }
    }

    @Override
    public void deleteByUser(int id, int userId) throws DatabaseException {
        // User-scoped deletion - ensures user can only delete their own categories
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_BY_USER_SQL)) {
            
            stmt.setInt(1, id);
            stmt.setInt(2, userId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DatabaseException("Category not found for deletion or access denied");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting category", e);
        }
    }
    
    @Override
    public Category findById(int id) throws DatabaseException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_BY_ID_SQL)) {
            
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCategory(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding category by ID", e);
        }
    }
    
    @Override
    @Deprecated
    public List<Category> findAll() throws DatabaseException {
        // Legacy method - returns only system categories (user_id IS NULL or 0)
        List<Category> categories = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_ALL_SQL);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                categories.add(mapResultSetToCategory(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding all categories", e);
        }
        return categories;
    }

    @Override
    public List<Category> findAllByUser(int userId) throws DatabaseException {
        // User-scoped query - returns user's categories plus system categories
        List<Category> categories = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_ALL_BY_USER_SQL)) {
            
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    categories.add(mapResultSetToCategory(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding categories by user", e);
        }
        return categories;
    }
    
    @Override
    @Deprecated
    public List<Category> findByType(String type) throws DatabaseException {
        // Legacy method - returns only system categories of the specified type
        List<Category> categories = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_BY_TYPE_SQL)) {
            
            stmt.setString(1, type);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    categories.add(mapResultSetToCategory(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding categories by type", e);
        }
        return categories;
    }

    @Override
    public List<Category> findByTypeAndUser(String type, int userId) throws DatabaseException {
        // User-scoped query - returns user's categories plus system categories of specified type
        List<Category> categories = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_BY_TYPE_AND_USER_SQL)) {
            
            stmt.setString(1, type);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    categories.add(mapResultSetToCategory(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding categories by type and user", e);
        }
        return categories;
    }
    
    @Override
    @Deprecated
    public Category findByName(String name) throws DatabaseException {
        // Legacy method - returns only system category with the specified name
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_BY_NAME_SQL)) {
            
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCategory(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding category by name", e);
        }
    }

    @Override
    public Category findByNameAndUser(String name, int userId) throws DatabaseException {
        // User-scoped query - returns user's or system category with the specified name
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_BY_NAME_AND_USER_SQL)) {
            
            stmt.setString(1, name);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCategory(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding category by name and user", e);
        }
    }
    
    private Category mapResultSetToCategory(ResultSet rs) throws SQLException {
        return new Category(
            rs.getInt("id"),
            rs.getInt("user_id"),
            rs.getString("name"),
            rs.getString("type"),
            rs.getString("description")
        );
    }
}

