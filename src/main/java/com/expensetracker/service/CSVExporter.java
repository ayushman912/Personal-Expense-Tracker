package com.expensetracker.service;

import com.expensetracker.model.Transaction;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Service class for exporting transactions to CSV format.
 */
public class CSVExporter {
    
    private static final String CSV_HEADER = "ID,Type,Amount,Description,Date,Category";
    
    /**
     * Export transactions to a CSV file.
     * @param transactions List of transactions to export
     * @param filePath Path to the output CSV file
     * @throws IOException if file writing fails
     */
    public void exportToCSV(List<Transaction> transactions, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append(CSV_HEADER).append("\n");
            
            for (Transaction transaction : transactions) {
                writer.append(String.valueOf(transaction.getId())).append(",");
                writer.append(transaction.getType()).append(",");
                writer.append(transaction.getAmount().toString()).append(",");
                writer.append(escapeCSV(transaction.getDescription())).append(",");
                writer.append(transaction.getDate().toString()).append(",");
                writer.append(escapeCSV(transaction.getCategoryName())).append("\n");
            }
        }
    }
    
    /**
     * Escape CSV special characters in a string.
     * @param value The string to escape
     * @return Escaped string
     */
    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

