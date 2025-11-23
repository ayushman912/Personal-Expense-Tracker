package com.expensetracker.util;

import com.expensetracker.exceptions.DatabaseException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Manages database connections using HikariCP connection pooling.
 * Implements singleton pattern for centralized connection management.
 * Uses try-with-resources for automatic resource management.
 */
public class DatabaseManager {
    private static DatabaseManager instance;
    private HikariDataSource dataSource;
    
    // Database configuration
    // Switched to H2 for portability
    // Added AUTO_SERVER=TRUE to prevent file lock issues
    private static final String DB_URL = "jdbc:h2:./expense_tracker_db;DB_CLOSE_DELAY=-1;MODE=MySQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE;AUTO_SERVER=TRUE";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";
    private static final int MAX_POOL_SIZE = 10;
    
    /**
     * Private constructor for singleton pattern.
     */
    private DatabaseManager() {
        initializeDataSource();
        initializeDatabase();
    }
    
    /**
     * Get singleton instance of DatabaseManager.
     * @return DatabaseManager instance
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }
    
    /**
     * Initialize HikariCP connection pool.
     */
    private void initializeDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DB_URL);
        config.setUsername(DB_USER);
        config.setPassword(DB_PASSWORD);
        config.setMaximumPoolSize(MAX_POOL_SIZE);
        // H2 driver
        config.setDriverClassName("org.h2.Driver");
        
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);
        
        dataSource = new HikariDataSource(config);
    }

    private void initializeDatabase() {
        try (Connection conn = getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            
            // Create Users table
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "username VARCHAR(50) UNIQUE NOT NULL, " +
                "password VARCHAR(255) NOT NULL, " +
                "email VARCHAR(100), " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // Create Categories table
            stmt.execute("CREATE TABLE IF NOT EXISTS categories (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(100) NOT NULL, " +
                "type VARCHAR(20) NOT NULL, " +
                "description TEXT, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE KEY unique_category_name (name, type))");

            // Create Transactions table
            stmt.execute("CREATE TABLE IF NOT EXISTS transactions (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "amount DECIMAL(10, 2) NOT NULL, " +
                "description VARCHAR(255) NOT NULL, " +
                "date DATE NOT NULL, " +
                "category_id INT NOT NULL, " +
                "type VARCHAR(20) NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (category_id) REFERENCES categories(id))");
                
            // Insert default admin user if not exists
            stmt.execute("MERGE INTO users (username, password, email) KEY(username) " +
                         "VALUES ('admin', 'admin123', 'admin@example.com')");
                         
            // Insert default categories
            // Using MERGE to ensure new categories are added even if DB exists
            stmt.execute("MERGE INTO categories (name, type, description) KEY(name, type) VALUES " +
                "('Salary', 'INCOME', 'Monthly salary'), " +
                "('Freelance', 'INCOME', 'Freelance work'), " +
                "('Food', 'EXPENSE', 'Groceries and dining'), " +
                "('Transport', 'EXPENSE', 'Public transport and fuel'), " +
                "('Utilities', 'EXPENSE', 'Electricity, water, internet'), " +
                "('Family', 'EXPENSE', 'Family related expenses'), " +
                "('Health', 'EXPENSE', 'Healthcare and medical'), " +
                "('Education', 'EXPENSE', 'School and learning'), " +
                "('Entertainment', 'EXPENSE', 'Movies, games, and fun'), " +
                "('Shopping', 'EXPENSE', 'Clothing and electronics'), " +
                "('Other', 'EXPENSE', 'Miscellaneous expenses')");
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
    
    /**
     * Get a connection from the pool.
     * @return Connection object
     * @throws DatabaseException if connection fails
     */
    public Connection getConnection() throws DatabaseException {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to get database connection", e);
        }
    }
    
    /**
     * Close the data source and release all connections.
     * Should be called on application shutdown.
     */
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
    
    /**
     * Test database connection.
     * @return true if connection is successful
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            return false;
        }
    }
}

