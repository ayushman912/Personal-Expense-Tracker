package com.expensetracker.service;

import com.expensetracker.model.Transaction;
import com.expensetracker.model.Expense;
import com.expensetracker.model.Income;
import com.expensetracker.exceptions.ValidationException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class for importing transactions from CSV format.
 */
public class CSVImporter {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    
    /**
     * Import transactions from a CSV file.
     * @param filePath Path to the CSV file
     * @return List of parsed transactions
     * @throws IOException if file reading fails
     * @throws ValidationException if CSV format is invalid
     */
    public List<Transaction> importFromCSV(String filePath) throws IOException, ValidationException {
        List<Transaction> transactions = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine(); // Skip header
            
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                try {
                    Transaction transaction = parseCSVLine(line);
                    transactions.add(transaction);
                } catch (Exception e) {
                    throw new ValidationException(
                        String.format("Error parsing line %d: %s", lineNumber, e.getMessage()), e);
                }
            }
        }
        
        return transactions;
    }
    
    /**
     * Parse a single CSV line into a Transaction object.
     * @param line CSV line
     * @return Transaction object
     * @throws ValidationException if parsing fails
     */
    private Transaction parseCSVLine(String line) throws ValidationException {
        String[] parts = parseCSVFields(line);
        
        if (parts.length < 6) {
            throw new ValidationException("Invalid CSV format: expected 6 fields");
        }
        
        try {
            String type = parts[1].trim();
            BigDecimal amount = new BigDecimal(parts[2].trim());
            String description = parts[3].trim();
            LocalDate date = LocalDate.parse(parts[4].trim(), DATE_FORMATTER);
            // parts[5] contains the category name; mapping to an ID not yet implemented
            int categoryId = 0;
            
            if ("Expense".equalsIgnoreCase(type)) {
                return new Expense(amount, description, date, categoryId);
            } else if ("Income".equalsIgnoreCase(type)) {
                return new Income(amount, description, date, categoryId);
            } else {
                throw new ValidationException("Invalid transaction type: " + type);
            }
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid amount format: " + parts[2]);
        } catch (Exception e) {
            throw new ValidationException("Error parsing transaction: " + e.getMessage());
        }
    }
    
    /**
     * Parse CSV fields, handling quoted values.
     * @param line CSV line
     * @return Array of field values
     */
    private String[] parseCSVFields(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    currentField.append('"');
                    i++; // Skip next quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }
        fields.add(currentField.toString());
        
        return fields.toArray(new String[0]);
    }
}

