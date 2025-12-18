package com.expensetracker.service;

import com.expensetracker.model.Expense;
import com.expensetracker.model.Income;
import com.expensetracker.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReportGenerator service.
 * Tests polymorphism with List<Transaction> containing both Expense and Income.
 */
class ReportGeneratorTest {
    
    private ReportGenerator reportGenerator;
    private List<Transaction> transactions;
    
    @BeforeEach
    void setUp() {
        reportGenerator = new ReportGenerator();
        transactions = new ArrayList<>();
        
        // Add sample expenses
        transactions.add(new Expense(new BigDecimal("50.00"), "Groceries", LocalDate.now(), 1));
        transactions.add(new Expense(new BigDecimal("25.00"), "Gas", LocalDate.now(), 2));
        transactions.add(new Expense(new BigDecimal("100.00"), "Shopping", LocalDate.now(), 3));
        
        // Add sample income
        transactions.add(new Income(new BigDecimal("1000.00"), "Salary", LocalDate.now(), 6));
        transactions.add(new Income(new BigDecimal("200.00"), "Freelance", LocalDate.now(), 7));
    }
    
    @Test
    void testCalculateTotalIncome() {
        BigDecimal totalIncome = reportGenerator.calculateTotalIncome(transactions);
        assertEquals(new BigDecimal("1200.00"), totalIncome);
    }
    
    @Test
    void testCalculateTotalExpenses() {
        BigDecimal totalExpenses = reportGenerator.calculateTotalExpenses(transactions);
        assertEquals(new BigDecimal("175.00"), totalExpenses);
    }
    
    @Test
    void testCalculateNetBalance() {
        BigDecimal balance = reportGenerator.calculateNetBalance(transactions);
        // Income (1200) - Expenses (175) = 1025
        assertEquals(new BigDecimal("1025.00"), balance);
    }
    
    @Test
    void testGenerateMonthlySummary() {
        SortedMap<java.time.YearMonth, ReportGenerator.MonthlySummary> summary = 
            reportGenerator.generateMonthlySummary(transactions);
        
        assertNotNull(summary);
        assertFalse(summary.isEmpty());
        
        ReportGenerator.MonthlySummary monthSummary = summary.values().iterator().next();
        assertNotNull(monthSummary);
        assertTrue(monthSummary.getIncome().compareTo(BigDecimal.ZERO) > 0);
    }
    
    @Test
    void testGenerateCategoryBreakdown() {
        List<Transaction> expenses = new ArrayList<>();
        Expense e1 = new Expense(new BigDecimal("50.00"), "Food", LocalDate.now(), 1);
        e1.setCategoryName("Food");
        expenses.add(e1);
        
        Expense e2 = new Expense(new BigDecimal("30.00"), "Food", LocalDate.now(), 1);
        e2.setCategoryName("Food");
        expenses.add(e2);
        
        Expense e3 = new Expense(new BigDecimal("20.00"), "Transport", LocalDate.now(), 2);
        e3.setCategoryName("Transport");
        expenses.add(e3);
        
        Map<String, BigDecimal> breakdown = reportGenerator.generateCategoryBreakdown(expenses);
        
        assertNotNull(breakdown);
        assertTrue(breakdown.containsKey("Food") || breakdown.size() > 0);
    }
    
    @Test
    void testGenerateDailyTotals() {
        LocalDate start = LocalDate.now().minusDays(2);
        LocalDate end = LocalDate.now();
        
        SortedMap<LocalDate, BigDecimal> dailyTotals = 
            reportGenerator.generateDailyTotals(transactions, start, end);
        
        assertNotNull(dailyTotals);
        assertFalse(dailyTotals.isEmpty());
    }
}

