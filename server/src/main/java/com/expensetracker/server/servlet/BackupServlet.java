package com.expensetracker.server.servlet;

import com.expensetracker.util.DatabaseManager;
import com.expensetracker.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Servlet for creating database backups.
 * Backups are stored in a fixed, secure directory to prevent path traversal attacks.
 * 
 * Database-aware backup: Supports both H2 (for testing) and MySQL (for production).
 * The backup mechanism is selected at runtime based on the configured database type.
 */
public class BackupServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(BackupServlet.class);
    
    // Fixed backup directory - prevents path traversal attacks
    private static final String BACKUP_DIR = "backups";

    @Override
    public void init() throws ServletException {
        super.init();
        // Ensure backup directory exists
        File backupDir = new File(BACKUP_DIR);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
            logger.info("Created backup directory: {}", backupDir.getAbsolutePath());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            
            // Determine file extension based on database type
            String dbType = detectDatabaseType();
            String fileExtension = "H2".equalsIgnoreCase(dbType) ? ".zip" : ".sql";
            
            // Sanitize filename - allow only alphanumeric, underscore, and hyphen
            String safeFilename = "backup_" + timestamp.replaceAll("[^a-zA-Z0-9_-]", "") + fileExtension;
            
            // Build safe absolute path within backup directory
            Path backupPath = Paths.get(BACKUP_DIR, safeFilename).toAbsolutePath().normalize();
            
            // Security check: ensure the resolved path is still within backup directory
            Path backupDirPath = Paths.get(BACKUP_DIR).toAbsolutePath().normalize();
            if (!backupPath.startsWith(backupDirPath)) {
                logger.warn("Path traversal attempt detected");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(Map.of("error", "Invalid backup path")));
                return;
            }
            
            String absolutePath = backupPath.toString().replace("\\", "/");

            // Backup implementation varies by database engine:
            // - H2: Uses native SCRIPT TO command (fast, compressed)
            // - MySQL/MariaDB: Uses JDBC-based SQL export (portable, no external tools required)
            if ("H2".equalsIgnoreCase(dbType)) {
                performH2Backup(absolutePath);
            } else {
                performJdbcBackup(absolutePath);
            }
            
            logger.info("Backup created successfully: {} (database: {})", safeFilename, dbType);

            resp.setContentType("application/json");
            resp.getWriter().write(JsonUtil.toJson(Map.of(
                    "status", "success",
                    "message", "Backup created successfully",
                    "filename", safeFilename)));
        } catch (Exception e) {
            logger.error("Backup failed", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(Map.of("error", e.getMessage())));
        }
    }

    /**
     * Detects the database type at runtime from the connection metadata.
     * @return Database product name (e.g., "H2", "MySQL", "MariaDB")
     */
    private String detectDatabaseType() {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String productName = metaData.getDatabaseProductName();
            logger.debug("Detected database type: {}", productName);
            return productName;
        } catch (Exception e) {
            logger.warn("Could not detect database type, defaulting to JDBC backup: {}", e.getMessage());
            return "Unknown";
        }
    }

    /**
     * Performs H2-specific backup using native SCRIPT TO command.
     * This is only used when H2 database is detected (typically for testing/development).
     */
    private void performH2Backup(String absolutePath) throws Exception {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute("SCRIPT TO '" + absolutePath + "' COMPRESSION ZIP");
        }
    }

    /**
     * Performs database-agnostic backup using JDBC metadata and SELECT statements.
     * Generates a standard SQL dump file compatible with MySQL and other databases.
     * This approach does not require external tools like mysqldump.
     */
    private void performJdbcBackup(String absolutePath) throws Exception {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PrintWriter writer = new PrintWriter(new FileWriter(absolutePath))) {
            
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = conn.getCatalog();
            
            writer.println("-- Database Backup");
            writer.println("-- Generated: " + LocalDateTime.now());
            writer.println("-- Database: " + metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion());
            writer.println();
            writer.println("SET FOREIGN_KEY_CHECKS=0;");
            writer.println();

            // Get all tables in the database
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = metaData.getTables(catalog, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    // Skip system tables
                    if (!tableName.startsWith("sys") && !tableName.startsWith("INFORMATION_SCHEMA")) {
                        tables.add(tableName);
                    }
                }
            }

            // Export each table's structure and data
            for (String tableName : tables) {
                exportTableStructure(conn, metaData, tableName, writer);
                exportTableData(conn, tableName, writer);
                writer.println();
            }

            writer.println("SET FOREIGN_KEY_CHECKS=1;");
            writer.println();
            writer.println("-- Backup completed");
        }
    }

    /**
     * Exports table structure (CREATE TABLE statement) for MySQL-compatible databases.
     */
    private void exportTableStructure(Connection conn, DatabaseMetaData metaData, 
                                       String tableName, PrintWriter writer) throws Exception {
        writer.println("-- Table structure for " + tableName);
        writer.println("DROP TABLE IF EXISTS `" + tableName + "`;");
        
        StringBuilder createSql = new StringBuilder("CREATE TABLE `" + tableName + "` (\n");
        List<String> columnDefs = new ArrayList<>();
        List<String> primaryKeys = new ArrayList<>();

        // Get columns
        try (ResultSet columns = metaData.getColumns(null, null, tableName, null)) {
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String typeName = columns.getString("TYPE_NAME");
                int columnSize = columns.getInt("COLUMN_SIZE");
                int decimalDigits = columns.getInt("DECIMAL_DIGITS");
                boolean nullable = columns.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                String defaultValue = columns.getString("COLUMN_DEF");
                String isAutoIncrement = columns.getString("IS_AUTOINCREMENT");

                StringBuilder colDef = new StringBuilder("  `" + columnName + "` ");
                colDef.append(mapColumnType(typeName, columnSize, decimalDigits));
                
                if (!nullable) {
                    colDef.append(" NOT NULL");
                }
                if ("YES".equalsIgnoreCase(isAutoIncrement)) {
                    colDef.append(" AUTO_INCREMENT");
                } else if (defaultValue != null && !defaultValue.isEmpty()) {
                    colDef.append(" DEFAULT ").append(formatDefaultValue(defaultValue, typeName));
                }
                
                columnDefs.add(colDef.toString());
            }
        }

        // Get primary keys
        try (ResultSet pks = metaData.getPrimaryKeys(null, null, tableName)) {
            while (pks.next()) {
                primaryKeys.add("`" + pks.getString("COLUMN_NAME") + "`");
            }
        }

        createSql.append(String.join(",\n", columnDefs));
        if (!primaryKeys.isEmpty()) {
            createSql.append(",\n  PRIMARY KEY (").append(String.join(", ", primaryKeys)).append(")");
        }
        createSql.append("\n);");
        
        writer.println(createSql);
        writer.println();
    }

    /**
     * Maps database-specific column types to MySQL-compatible types.
     */
    private String mapColumnType(String typeName, int size, int decimalDigits) {
        String upperType = typeName.toUpperCase();
        switch (upperType) {
            case "INT":
            case "INTEGER":
            case "INT4":
                return "INT";
            case "BIGINT":
            case "INT8":
                return "BIGINT";
            case "VARCHAR":
            case "VARCHAR2":
            case "CHARACTER VARYING":
                return "VARCHAR(" + size + ")";
            case "DECIMAL":
            case "NUMERIC":
                return "DECIMAL(" + size + "," + decimalDigits + ")";
            case "TIMESTAMP":
            case "DATETIME":
                return "TIMESTAMP";
            case "DATE":
                return "DATE";
            case "TEXT":
            case "CLOB":
                return "TEXT";
            case "BOOLEAN":
            case "BOOL":
                return "TINYINT(1)";
            default:
                return typeName + (size > 0 ? "(" + size + ")" : "");
        }
    }

    /**
     * Formats default values for SQL output.
     */
    private String formatDefaultValue(String defaultValue, String typeName) {
        if (defaultValue == null) {
            return "NULL";
        }
        String upperDefault = defaultValue.toUpperCase();
        if (upperDefault.equals("NULL") || upperDefault.contains("CURRENT_TIMESTAMP") 
            || upperDefault.startsWith("NEXT VALUE") || upperDefault.contains("()")) {
            return defaultValue;
        }
        String upperType = typeName.toUpperCase();
        if (upperType.contains("INT") || upperType.contains("DECIMAL") || upperType.contains("NUMERIC") 
            || upperType.contains("FLOAT") || upperType.contains("DOUBLE")) {
            return defaultValue;
        }
        return "'" + defaultValue.replace("'", "''") + "'";
    }

    /**
     * Exports table data as INSERT statements.
     */
    private void exportTableData(Connection conn, String tableName, PrintWriter writer) throws Exception {
        writer.println("-- Data for " + tableName);
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName)) {
            
            ResultSetMetaData rsMeta = rs.getMetaData();
            int columnCount = rsMeta.getColumnCount();
            
            while (rs.next()) {
                StringBuilder insertSql = new StringBuilder("INSERT INTO `" + tableName + "` VALUES (");
                List<String> values = new ArrayList<>();
                
                for (int i = 1; i <= columnCount; i++) {
                    Object value = rs.getObject(i);
                    if (value == null) {
                        values.add("NULL");
                    } else if (value instanceof Number) {
                        values.add(value.toString());
                    } else if (value instanceof Boolean) {
                        values.add((Boolean) value ? "1" : "0");
                    } else {
                        values.add("'" + value.toString().replace("'", "''") + "'");
                    }
                }
                
                insertSql.append(String.join(", ", values));
                insertSql.append(");");
                writer.println(insertSql);
            }
        }
    }
}
