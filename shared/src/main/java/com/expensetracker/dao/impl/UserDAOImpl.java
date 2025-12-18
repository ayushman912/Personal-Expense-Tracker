package com.expensetracker.dao.impl;

import com.expensetracker.dao.UserDAO;
import com.expensetracker.model.User;
import com.expensetracker.util.DatabaseManager;
import com.expensetracker.util.PasswordUtil;
import com.expensetracker.exceptions.DatabaseException;
import com.expensetracker.exceptions.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

public class UserDAOImpl implements UserDAO {

    private static final Logger logger = LoggerFactory.getLogger(UserDAOImpl.class);
    
    private static final String INSERT_SQL = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
    private static final String FIND_BY_USERNAME_SQL = "SELECT * FROM users WHERE username = ?";
    
    // Validation patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_]{3,50}$");

    /**
     * Validates user input before database operations.
     */
    private void validateUser(User user) throws ValidationException {
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new ValidationException("Username is required");
        }
        if (!USERNAME_PATTERN.matcher(user.getUsername()).matches()) {
            throw new ValidationException("Username must be 3-50 characters, alphanumeric and underscores only");
        }
        if (user.getEmail() != null && !user.getEmail().trim().isEmpty() 
                && !EMAIL_PATTERN.matcher(user.getEmail()).matches()) {
            throw new ValidationException("Invalid email format");
        }
        if (user.getPassword() == null || user.getPassword().length() < 6) {
            throw new ValidationException("Password must be at least 6 characters");
        }
    }

    @Override
    public int insert(User user) throws DatabaseException {
        try {
            validateUser(user);
        } catch (ValidationException e) {
            throw new DatabaseException("Validation failed: " + e.getMessage(), e);
        }
        
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getUsername());
            // Hash password before storing
            stmt.setString(2, PasswordUtil.hashPassword(user.getPassword()));
            stmt.setString(3, user.getEmail());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new DatabaseException("Failed to insert user");
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    logger.info("User created successfully: {}", user.getUsername());
                    return id;
                } else {
                    throw new DatabaseException("Failed to get generated ID");
                }
            }
        } catch (SQLException e) {
            logger.error("Error inserting user: {}", user.getUsername(), e);
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
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setPassword(rs.getString("password"));
                    user.setEmail(rs.getString("email"));
                    return user;
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding user by username: {}", username, e);
            throw new DatabaseException("Error finding user by username", e);
        }
        return null;
    }

    @Override
    public User authenticate(String username, String password) throws DatabaseException {
        User user = findByUsername(username);
        if (user != null && PasswordUtil.verifyPassword(password, user.getPassword())) {
            logger.info("User authenticated successfully: {}", username);
            return user;
        }
        logger.warn("Authentication failed for user: {}", username);
        return null;
    }
}
