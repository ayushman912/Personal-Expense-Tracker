package com.expensetracker.dao.impl;

import com.expensetracker.dao.UserDAO;
import com.expensetracker.model.User;
import com.expensetracker.exceptions.DatabaseException;
import com.expensetracker.util.DatabaseManager;
import java.sql.*;

/**
 * Implementation of UserDAO interface.
 * Handles user authentication and registration.
 */
public class UserDAOImpl implements UserDAO {
    
    private static final String INSERT_SQL = 
        "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
    
    private static final String FIND_BY_USERNAME_SQL = 
        "SELECT id, username, password, email FROM users WHERE username = ?";
    
    @Override
    public int insert(User user) throws DatabaseException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getEmail());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DatabaseException("Failed to insert user");
            }
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new DatabaseException("Failed to get generated ID");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error inserting user", e);
        }
    }
    
    @Override
    public User findByUsername(String username) throws DatabaseException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_BY_USERNAME_SQL)) {
            
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("email")
                    );
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding user by username", e);
        }
    }
    
    @Override
    public User authenticate(String username, String password) throws DatabaseException {
        User user = findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }
}

