package com.expensetracker.service;

import com.expensetracker.model.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for generating financial reports.
 * Demonstrates polymorphism by accepting List<Transaction> and processing
 * both Expense and Income objects through the Transaction interface.
 */
public class ReportGenerator {
    
    /**
     * Calculate total income from a list of transactions.
     * Uses polymorphism to filter and process Income objects.
     * @param transactions List of transactions
     * @return Total income amount
     */
    public BigDecimal calculateTotalIncome(List<Transaction> transactions) {
        return transactions.stream()
            .filter(t -> t instanceof com.expensetracker.model.Income)
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calculate total expenses from a list of transactions.
     * Uses polymorphism to filter and process Expense objects.
     * @param transactions List of transactions
     * @return Total expense amount
     */
    public BigDecimal calculateTotalExpenses(List<Transaction> transactions) {
        return transactions.stream()
            .filter(t -> t instanceof com.expensetracker.model.Expense)
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calculate net balance (income - expenses).
     * @param transactions List of transactions
     * @return Net balance
     */
    public BigDecimal calculateNetBalance(List<Transaction> transactions) {
        return transactions.stream()
            .map(Transaction::getSignedAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Generate monthly summary grouped by month.
     * Returns a SortedMap for chronological ordering.
     * @param transactions List of transactions
     * @return SortedMap with YearMonth as key and summary data as value
     */
    public SortedMap<YearMonth, MonthlySummary> generateMonthlySummary(List<Transaction> transactions) {
        SortedMap<YearMonth, MonthlySummary> summary = new TreeMap<>();
        
        Map<YearMonth, List<Transaction>> byMonth = transactions.stream()
            .collect(Collectors.groupingBy(t -> YearMonth.from(t.getDate())));
        
        for (Map.Entry<YearMonth, List<Transaction>> entry : byMonth.entrySet()) {
            YearMonth month = entry.getKey();
            List<Transaction> monthTransactions = entry.getValue();
            
            BigDecimal income = calculateTotalIncome(monthTransactions);
            BigDecimal expenses = calculateTotalExpenses(monthTransactions);
            BigDecimal balance = income.subtract(expenses);
            
            summary.put(month, new MonthlySummary(month, income, expenses, balance, monthTransactions.size()));
        }
        
        return summary;
    }
    
    /**
     * Generate category breakdown for expenses.
     * Returns a Map with category names and their totals.
     * @param transactions List of transactions (should be expenses)
     * @return Map of category name to total amount
     */
    public Map<String, BigDecimal> generateCategoryBreakdown(List<Transaction> transactions) {
        return transactions.stream()
            .filter(t -> t instanceof com.expensetracker.model.Expense)
            .collect(Collectors.groupingBy(
                Transaction::getCategoryName,
                Collectors.reducing(
                    BigDecimal.ZERO,
                    Transaction::getAmount,
                    BigDecimal::add
                )
            ));
    }
    
    /**
     * Generate daily totals for a date range.
     * Useful for line charts.
     * @param transactions List of transactions
     * @param startDate Start date
     * @param endDate End date
     * @return SortedMap with date as key and daily total as value
     */
    public SortedMap<LocalDate, BigDecimal> generateDailyTotals(
            List<Transaction> transactions, LocalDate startDate, LocalDate endDate) {
        SortedMap<LocalDate, BigDecimal> dailyTotals = new TreeMap<>();
        
        // Initialize all dates in range with zero
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            dailyTotals.put(current, BigDecimal.ZERO);
            current = current.plusDays(1);
        }
        
        // Add transaction amounts
        transactions.stream()
            .filter(t -> !t.getDate().isBefore(startDate) && !t.getDate().isAfter(endDate))
            .forEach(t -> {
                LocalDate date = t.getDate();
                BigDecimal currentTotal = dailyTotals.getOrDefault(date, BigDecimal.ZERO);
                dailyTotals.put(date, currentTotal.add(t.getSignedAmount()));
            });
        
        return dailyTotals;
    }
    
    /**
     * Inner class to hold monthly summary data.
     */
    public static class MonthlySummary {
        private final YearMonth month;
        private final BigDecimal income;
        private final BigDecimal expenses;
        private final BigDecimal balance;
        private final int transactionCount;
        
        public MonthlySummary(YearMonth month, BigDecimal income, BigDecimal expenses, 
                            BigDecimal balance, int transactionCount) {
            this.month = month;
            this.income = income;
            this.expenses = expenses;
            this.balance = balance;
            this.transactionCount = transactionCount;
        }
        
        public YearMonth getMonth() { return month; }
        public BigDecimal getIncome() { return income; }
        public BigDecimal getExpenses() { return expenses; }
        public BigDecimal getBalance() { return balance; }
        public int getTransactionCount() { return transactionCount; }
    }
}

