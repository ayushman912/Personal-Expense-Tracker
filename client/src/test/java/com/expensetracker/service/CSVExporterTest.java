package com.expensetracker.service;

import com.expensetracker.model.Expense;
import com.expensetracker.model.Income;
import com.expensetracker.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CSVExporter service.
 */
class CSVExporterTest {
    
    private CSVExporter csvExporter;
    private List<Transaction> transactions;
    
    @BeforeEach
    void setUp() {
        csvExporter = new CSVExporter();
        transactions = new ArrayList<>();
        
        transactions.add(new Expense(new BigDecimal("50.00"), "Test expense", LocalDate.now(), 1));
        transactions.add(new Income(new BigDecimal("100.00"), "Test income", LocalDate.now(), 6));
    }
    
    @Test
    void testExportToCSV(@TempDir Path tempDir) throws IOException {
        File csvFile = tempDir.resolve("test_export.csv").toFile();
        
        csvExporter.exportToCSV(transactions, csvFile.getAbsolutePath());
        
        assertTrue(csvFile.exists());
        assertTrue(csvFile.length() > 0);
        
        List<String> lines = Files.readAllLines(csvFile.toPath());
        assertTrue(lines.size() >= 2); // Header + at least one data row
        assertTrue(lines.get(0).contains("ID,Type,Amount"));
    }
}

