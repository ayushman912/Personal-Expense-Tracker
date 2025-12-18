package com.expensetracker.util;

import com.expensetracker.exceptions.DatabaseException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Utility class for creating database backups (SQL dumps).
 * Provides functionality to export database schema and data.
 */
public class DatabaseBackup {
    
    /**
     * Create a SQL dump of the database.
     * @param filePath Path to save the SQL dump file
     * @throws DatabaseException if backup fails
     */
    public void createBackup(String filePath) throws DatabaseException {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             FileWriter writer = new FileWriter(filePath)) {
            
            // Write header
            writer.write("-- Personal Expense Tracker Database Backup\n");
            writer.write("-- Generated: " + java.time.LocalDateTime.now() + "\n\n");
            writer.write("USE expense_tracker;\n\n");
            
            // Backup users table
            backupTable(conn, writer, "users");
            
            // Backup categories table
            backupTable(conn, writer, "categories");
            
            // Backup transactions table
            backupTable(conn, writer, "transactions");
            
        } catch (IOException | java.sql.SQLException e) {
            throw new DatabaseException("Failed to create database backup", e);
        }
    }
    
    /**
     * Backup a specific table to SQL INSERT statements.
     */
    private void backupTable(Connection conn, FileWriter writer, String tableName) 
            throws java.sql.SQLException, IOException {
        
        writer.write("-- Backup of " + tableName + " table\n");
        writer.write("DELETE FROM " + tableName + ";\n\n");
        
        String query = "SELECT * FROM " + tableName;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            java.sql.ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                writer.write("INSERT INTO " + tableName + " (");
                
                // Write column names
                for (int i = 1; i <= columnCount; i++) {
                    if (i > 1) writer.write(", ");
                    writer.write(metaData.getColumnName(i));
                }
                
                writer.write(") VALUES (");
                
                // Write values
                for (int i = 1; i <= columnCount; i++) {
                    if (i > 1) writer.write(", ");
                    Object value = rs.getObject(i);
                    if (value == null) {
                        writer.write("NULL");
                    } else if (value instanceof String) {
                        writer.write("'" + escapeSQL((String) value) + "'");
                    } else if (value instanceof java.sql.Date || value instanceof java.sql.Timestamp) {
                        writer.write("'" + value.toString() + "'");
                    } else {
                        writer.write(value.toString());
                    }
                }
                
                writer.write(");\n");
            }
        }
        
        writer.write("\n");
    }
    
    /**
     * Escape SQL special characters in strings.
     */
    private String escapeSQL(String value) {
        return value.replace("'", "''").replace("\\", "\\\\");
    }
}

