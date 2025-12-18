package com.expensetracker.util;

import com.expensetracker.exceptions.DatabaseException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Manages database connections using HikariCP connection pooling.
 * Implements singleton pattern for centralized connection management.
 * Uses try-with-resources for automatic resource management.
 */
public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static DatabaseManager instance;
    private HikariDataSource dataSource;

    // Database configuration
    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;
    private static final int MAX_POOL_SIZE = 10;

    public static void setDbUrl(String url) {
        dbUrl = url;
    }

    /**
     * Private constructor for singleton pattern.
     */
    private DatabaseManager() {
        loadConfig();
        initializeDataSource();
        initializeDatabase();
    }

    private void loadConfig() {
        try (java.io.InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            java.util.Properties prop = new java.util.Properties();
            if (input == null) {
                // Fallback to defaults if config not found (or throw exception)
                System.out.println("Sorry, unable to find config.properties, using defaults");
                dbUrl = "jdbc:h2:./expense_tracker_db;DB_CLOSE_DELAY=-1;MODE=MySQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE;AUTO_SERVER=TRUE";
                dbUser = "sa";
                dbPassword = "";
                return;
            }
            prop.load(input);
            dbUrl = prop.getProperty("db.url");
            dbUser = prop.getProperty("db.user");
            dbPassword = prop.getProperty("db.password");
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Error loading configuration", ex);
        }
    }

    /**
     * Get singleton instance of DatabaseManager.
     * 
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
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(MAX_POOL_SIZE);
        
        // Dynamic driver selection based on JDBC URL to support both MySQL and H2
        // This ensures the correct driver is used based on configuration, not hardcoded
        String driverClassName = inferDriverFromUrl(dbUrl);
        config.setDriverClassName(driverClassName);
        logger.info("Using database driver: {} for URL: {}", driverClassName, dbUrl);

        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);

        dataSource = new HikariDataSource(config);
    }

    public void initializeDatabase() {
        try (Connection conn = getConnection();
                java.sql.Statement stmt = conn.createStatement()) {

            // Create Users table
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(50) UNIQUE NOT NULL, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "email VARCHAR(100), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // Create Categories table with user_id for data isolation
            // Why user_id: Categories must be user-scoped to prevent:
            // 1. Privacy leaks (User B seeing User A's categories)
            // 2. UNIQUE constraint conflicts (two users can't have same category name)
            // user_id NULL or 0 = system/default categories visible to all users
            stmt.execute("CREATE TABLE IF NOT EXISTS categories (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id INT, " +  // User ownership for data isolation; NULL for system categories
                    "name VARCHAR(100) NOT NULL, " +
                    "type VARCHAR(20) NOT NULL, " +
                    "description TEXT, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (user_id) REFERENCES users(id), " +
                    "CONSTRAINT unique_category_per_user UNIQUE (user_id, name, type))");

            // Schema migration: Add user_id column to existing categories table if missing
            // This handles upgrades from older schema without user_id column
            migrateCategoriesTableIfNeeded(conn);

            // Create Transactions table with user_id for data isolation
            // Note: New users start with zero financial data by design.
            // No default/demo transactions are auto-inserted - users must add their own.
            // Each transaction is strictly scoped to a user to prevent data leakage.
            stmt.execute("CREATE TABLE IF NOT EXISTS transactions (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id INT NOT NULL, " +  // User ownership for data isolation
                    "amount DECIMAL(10, 2) NOT NULL, " +
                    "description VARCHAR(255) NOT NULL, " +
                    "date DATE NOT NULL, " +
                    "category_id INT NOT NULL, " +
                    "type VARCHAR(20) NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (user_id) REFERENCES users(id), " +
                    "FOREIGN KEY (category_id) REFERENCES categories(id))");

            // Insert default admin user if not exists (with hashed password)
            String hashedAdminPassword = PasswordUtil.hashPassword("admin123");
            stmt.execute("MERGE INTO users (username, password, email) KEY(username) " +
                    "VALUES ('admin', '" + hashedAdminPassword + "', 'admin@example.com')");
            logger.info("Default admin user initialized with hashed password");

            // Insert default/system categories (user_id = NULL for shared categories)
            // Using MERGE to ensure new categories are added even if DB exists
            stmt.execute("MERGE INTO categories (user_id, name, type, description) KEY(user_id, name, type) VALUES " +
                    "(NULL, 'Salary', 'INCOME', 'Monthly salary'), " +
                    "(NULL, 'Freelance', 'INCOME', 'Freelance work'), " +
                    "(NULL, 'Food', 'EXPENSE', 'Groceries and dining'), " +
                    "(NULL, 'Transport', 'EXPENSE', 'Public transport and fuel'), " +
                    "(NULL, 'Utilities', 'EXPENSE', 'Electricity, water, internet'), " +
                    "(NULL, 'Family', 'EXPENSE', 'Family related expenses'), " +
                    "(NULL, 'Health', 'EXPENSE', 'Healthcare and medical'), " +
                    "(NULL, 'Education', 'EXPENSE', 'School and learning'), " +
                    "(NULL, 'Entertainment', 'EXPENSE', 'Movies, games, and fun'), " +
                    "(NULL, 'Shopping', 'EXPENSE', 'Clothing and electronics'), " +
                    "(NULL, 'Other', 'EXPENSE', 'Miscellaneous expenses')");

        } catch (Exception e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    /**
     * Migrates the categories table to include user_id column if it doesn't exist.
     * This handles schema upgrades from older versions without user-scoped categories.
     */
    private void migrateCategoriesTableIfNeeded(Connection conn) {
        try {
            // Check if user_id column exists in categories table
            boolean hasUserIdColumn = false;
            try (java.sql.ResultSet rs = conn.getMetaData().getColumns(null, null, "CATEGORIES", "USER_ID")) {
                hasUserIdColumn = rs.next();
            }
            
            if (!hasUserIdColumn) {
                logger.info("Migrating categories table: adding user_id column for data isolation");
                try (java.sql.Statement stmt = conn.createStatement()) {
                    // Add user_id column
                    stmt.execute("ALTER TABLE categories ADD COLUMN user_id INT");
                    // Add foreign key constraint
                    stmt.execute("ALTER TABLE categories ADD CONSTRAINT fk_category_user FOREIGN KEY (user_id) REFERENCES users(id)");
                    // Drop old unique constraint and add new one including user_id
                    try {
                        stmt.execute("ALTER TABLE categories DROP CONSTRAINT unique_category_name");
                    } catch (SQLException e) {
                        // Constraint might not exist or have different name, ignore
                        logger.debug("Could not drop old constraint: {}", e.getMessage());
                    }
                    stmt.execute("ALTER TABLE categories ADD CONSTRAINT unique_category_per_user UNIQUE (user_id, name, type)");
                    logger.info("Categories table migration completed successfully");
                }
            }
        } catch (SQLException e) {
            logger.warn("Categories migration check failed, assuming new schema: {}", e.getMessage());
        }
    }

    /**
     * Get a connection from the pool.
     * 
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
     * 
     * @return true if connection is successful
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Infers the appropriate JDBC driver class name from the database URL.
     * Supports MySQL and H2 databases as per project requirements.
     * 
     * Why dynamic: Hardcoding "org.h2.Driver" breaks MySQL support promised in README.
     * The driver MUST match the configured JDBC URL to ensure correct database connectivity.
     * 
     * @param url the JDBC URL
     * @return the driver class name
     */
    private static String inferDriverFromUrl(String url) {
        if (url == null) {
            throw new IllegalArgumentException("Database URL cannot be null");
        }
        if (url.startsWith("jdbc:mysql:")) {
            return "com.mysql.cj.jdbc.Driver";
        } else if (url.startsWith("jdbc:h2:")) {
            return "org.h2.Driver";
        } else if (url.startsWith("jdbc:mariadb:")) {
            return "org.mariadb.jdbc.Driver";
        } else if (url.startsWith("jdbc:postgresql:")) {
            return "org.postgresql.Driver";
        } else {
            // Default to H2 for backward compatibility with tests
            logger.warn("Unknown JDBC URL scheme: {}. Defaulting to H2 driver.", url);
            return "org.h2.Driver";
        }
    }
}
