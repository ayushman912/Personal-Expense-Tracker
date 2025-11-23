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
 */
public class CategoryDAOImpl implements CategoryDAO {
    
    private static final String INSERT_SQL = 
        "INSERT INTO categories (name, type, description) VALUES (?, ?, ?)";
    
    private static final String UPDATE_SQL = 
        "UPDATE categories SET name = ?, type = ?, description = ? WHERE id = ?";
    
    private static final String DELETE_SQL = "DELETE FROM categories WHERE id = ?";
    
    private static final String FIND_BY_ID_SQL = 
        "SELECT id, name, type, description FROM categories WHERE id = ?";
    
    private static final String FIND_ALL_SQL = 
        "SELECT id, name, type, description FROM categories ORDER BY CASE WHEN name = 'Other' THEN 1 ELSE 0 END, name";
    
    private static final String FIND_BY_TYPE_SQL = 
        "SELECT id, name, type, description FROM categories WHERE type = ? ORDER BY CASE WHEN name = 'Other' THEN 1 ELSE 0 END, name";
    
    private static final String FIND_BY_NAME_SQL = 
        "SELECT id, name, type, description FROM categories WHERE name = ?";
    
    @Override
    public int insert(Category category) throws DatabaseException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, category.getName());
            stmt.setString(2, category.getType());
            stmt.setString(3, category.getDescription());
            
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
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {
            
            stmt.setString(1, category.getName());
            stmt.setString(2, category.getType());
            stmt.setString(3, category.getDescription());
            stmt.setInt(4, category.getId());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DatabaseException("Category not found for update");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error updating category", e);
        }
    }
    
    @Override
    public void delete(int id) throws DatabaseException {
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
    public List<Category> findAll() throws DatabaseException {
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
    public List<Category> findByType(String type) throws DatabaseException {
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
    public Category findByName(String name) throws DatabaseException {
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
    
    private Category mapResultSetToCategory(ResultSet rs) throws SQLException {
        return new Category(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("type"),
            rs.getString("description")
        );
    }
}

