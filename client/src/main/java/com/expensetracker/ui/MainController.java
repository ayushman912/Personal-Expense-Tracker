package com.expensetracker.ui;

import com.expensetracker.client.service.ExpenseService;
import com.expensetracker.dao.CategoryDAO;
import com.expensetracker.dao.impl.CategoryDAOImpl;
import com.expensetracker.model.Category;
import com.expensetracker.model.Transaction;
import com.expensetracker.model.Expense;
import com.expensetracker.model.Income;
import com.expensetracker.service.*;
import com.expensetracker.client.service.ConnectivityManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Main controller for the expense tracker application.
 * Updated to use ExpenseService for client-server communication.
 */
public class MainController {
    
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    // Services
    private ExpenseService expenseService = new ExpenseService();
    private CategoryDAO categoryDAO = new CategoryDAOImpl(); // Keep local for now or move to service
    private ReportGenerator reportGenerator = new ReportGenerator();
    private CSVExporter csvExporter = new CSVExporter();

    // Multithreading
    private ExecutorService executorService = Executors.newFixedThreadPool(4);
    private ReadWriteLock dataLock = new ReentrantReadWriteLock();

    // In-memory cache
    private List<Transaction> transactionCache = new ArrayList<>();

    // UI Components
    @FXML
    private TableView<Transaction> transactionTable;
    @FXML
    private TableColumn<Transaction, String> typeColumn;
    @FXML
    private TableColumn<Transaction, BigDecimal> amountColumn;
    @FXML
    private TableColumn<Transaction, String> descriptionColumn;
    @FXML
    private TableColumn<Transaction, LocalDate> dateColumn;
    @FXML
    private TableColumn<Transaction, String> categoryColumn;

    @FXML
    private ComboBox<String> typeComboBox;
    @FXML
    private TextField amountField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private DatePicker datePicker;
    @FXML
    private ComboBox<Category> categoryComboBox;

    @FXML
    private ComboBox<Category> filterCategoryComboBox;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;

    @FXML
    private Label totalIncomeLabel;
    @FXML
    private Label totalExpenseLabel;
    @FXML
    private Label netBalanceLabel;

    @FXML
    private PieChart categoryPieChart;
    @FXML
    private LineChart<String, Number> monthlyLineChart;

    @FXML
    private ProgressBar importProgressBar;
    @FXML
    private Label importStatusLabel;

    @FXML
    private Label statusLabel;

    private ObservableList<Transaction> transactionList = FXCollections.observableArrayList();
    private ObservableList<Category> categoryList = FXCollections.observableArrayList();

    private void updateConnectionStatus(ConnectivityManager.ConnectionState state) {
        boolean isOnline = (state == ConnectivityManager.ConnectionState.ONLINE);
        Platform.runLater(() -> {
            statusLabel.setText(isOnline ? "Online" : "Offline");
            statusLabel.setStyle(isOnline
                    ? "-fx-text-fill: white; -fx-background-color: #27ae60; -fx-background-radius: 15; -fx-padding: 5 15;"
                    : "-fx-text-fill: white; -fx-background-color: #e74c3c; -fx-background-radius: 15; -fx-padding: 5 15;");
        });
    }

    /**
     * Sets the current logged-in user and initializes user-specific data.
     * Called by LoginController after successful authentication.
     */
    public void setCurrentUser(com.expensetracker.model.User user) {
        expenseService.setCurrentUser(user);
        logger.info("User logged in: {} (ID: {})", user.getUsername(), user.getId());
        // Now load transactions after user is set
        loadTransactions();
    }

    @FXML
    private void initialize() {
        setupTableColumns();
        loadCategories();
        // Don't load transactions here - wait for setCurrentUser to be called
        setupCharts();

        // Setup connectivity listener
        expenseService.getConnectivityManager().addListener(this::updateConnectionStatus);

        // Setup sync feedback
        expenseService.getSyncManager().setOnSyncComplete(() -> {
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.INFORMATION, "Sync Complete",
                        "Offline data has been synchronized with the server.");
                loadTransactions(); // Refresh data
            });
        });

        typeComboBox.getItems().addAll("Expense", "Income");
        typeComboBox.setValue("Expense");

        datePicker.setValue(LocalDate.now());

        // Login should be handled by LoginController
    }

    private void setupTableColumns() {
        typeColumn.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getType()));
        amountColumn.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getAmount()));
        descriptionColumn.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDescription()));
        dateColumn.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getDate()));
        categoryColumn.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCategoryName()));

        transactionTable.setItems(transactionList);
    }

    @FXML
    private void handleAddTransaction() {
        String type = typeComboBox.getValue();
        String amountText = amountField.getText() == null ? "" : amountField.getText().trim();
        String description = descriptionField.getText() == null ? "" : descriptionField.getText().trim();
        LocalDate date = datePicker.getValue();
        Category category = categoryComboBox.getValue();

        if (amountText.isEmpty() || description.isEmpty() || date == null || category == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please fill all fields");
            return;
        }
        try {
            BigDecimal amount = new BigDecimal(amountText);
            Transaction transaction;

            if ("Expense".equals(type)) {
                transaction = new Expense(amount, description, date, category.getId());
            } else {
                transaction = new Income(amount, description, date, category.getId());
            }
            transaction.setCategoryName(category.getName());

            expenseService.addTransaction(transaction)
                    .thenAccept(addedTx -> {
                        dataLock.writeLock().lock();
                        try {
                            transactionCache.add(addedTx);
                            Platform.runLater(() -> {
                                transactionList.add(addedTx);
                                updateSummary();
                                updateCharts();
                                clearForm();
                            });
                        } finally {
                            dataLock.writeLock().unlock();
                        }
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Error",
                                "Failed to add transaction: " + e.getMessage()));
                        return null;
                    });

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Invalid amount format");
        }
    }

    @FXML
    private void handleDeleteTransaction() {
        Transaction selected = transactionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please select a transaction to delete");
            return;
        }

        expenseService.deleteTransaction(selected.getId())
                .thenRun(() -> {
                    dataLock.writeLock().lock();
                    try {
                        transactionCache.remove(selected);
                        Platform.runLater(() -> {
                            transactionList.remove(selected);
                            updateSummary();
                            updateCharts();
                        });
                    } finally {
                        dataLock.writeLock().unlock();
                    }
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete transaction"));
                    return null;
                });
    }

    @FXML
    private void handleFilter() {
        // Filter logic can remain local on cached data or call server with query params
        // For simplicity, filtering local cache
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        Category category = filterCategoryComboBox.getValue();

        List<Transaction> filtered = new ArrayList<>();
        dataLock.readLock().lock();
        try {
            for (Transaction t : transactionCache) {
                boolean matchDate = true;
                if (startDate != null && endDate != null) {
                    matchDate = !t.getDate().isBefore(startDate) && !t.getDate().isAfter(endDate);
                }
                boolean matchCategory = true;
                if (category != null) {
                    matchCategory = t.getCategoryId() == category.getId();
                }

                if (matchDate && matchCategory) {
                    filtered.add(t);
                }
            }
        } finally {
            dataLock.readLock().unlock();
        }

        transactionList.clear();
        transactionList.addAll(filtered);
        updateSummary();
        updateCharts();
    }

    @FXML
    private void handleClearFilter() {
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        filterCategoryComboBox.setValue(null);
        transactionList.clear();
        transactionList.addAll(transactionCache);
        updateSummary();
        updateCharts();
    }

    @FXML
    private void handleExportCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Transactions");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(getStage());

        if (file != null) {
            Task<Void> exportTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    dataLock.readLock().lock();
                    try {
                        csvExporter.exportToCSV(new ArrayList<>(transactionCache), file.getAbsolutePath());
                    } finally {
                        dataLock.readLock().unlock();
                    }
                    return null;
                }
            };

            exportTask.setOnSucceeded(e -> {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Transactions exported successfully");
            });

            exportTask.setOnFailed(e -> {
                showAlert(Alert.AlertType.ERROR, "Error", "Export failed: " + exportTask.getException().getMessage());
            });

            executorService.submit(exportTask);
        }
    }

    @FXML
    private void handleImportCSV() {
        // Keep existing import logic, but maybe route through service to sync?
        // For now, keeping as is (local import), but ideally should batch insert via
        // service
        showAlert(Alert.AlertType.INFORMATION, "Info", "Import feature temporarily disabled in client-server mode");
    }

    @FXML
    private void handleDatabaseBackup() {
        // Trigger server backup
        expenseService.getRemoteRepository().triggerBackup()
                .thenAccept(success -> Platform.runLater(() -> {
                    if (success) {
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Server backup triggered successfully");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Error", "Server backup failed");
                    }
                }));
    }

    @FXML
    private void handleAddCategory() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Category");
        dialog.setHeaderText("Enter category name");
        dialog.setContentText("Category:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            Task<Integer> addTask = new Task<Integer>() {
                @Override
                protected Integer call() throws Exception {
                    Category category = new Category(name, "EXPENSE", "");
                    return categoryDAO.insert(category);
                }
            };

            addTask.setOnSucceeded(e -> {
                loadCategories();
            });

            executorService.submit(addTask);
        });
    }

    @SuppressWarnings("deprecation") // Intentional: Client uses findAll() for system categories; user-scoping handled server-side
    private void loadCategories() {
        Task<List<Category>> loadTask = new Task<List<Category>>() {
            @Override
            protected List<Category> call() throws Exception {
                return categoryDAO.findAll();
            }
        };

        loadTask.setOnSucceeded(e -> {
            categoryList.clear();
            categoryList.addAll(loadTask.getValue());
            categoryComboBox.setItems(categoryList);
            filterCategoryComboBox.setItems(categoryList);
        });

        executorService.submit(loadTask);
    }

    private void loadTransactions() {
        expenseService.getAllTransactions()
                .thenAccept(transactions -> {
                    dataLock.writeLock().lock();
                    try {
                        transactionCache.clear();
                        transactionCache.addAll(transactions);
                        Platform.runLater(() -> {
                            transactionList.clear();
                            transactionList.addAll(transactions);
                            updateSummary();
                            updateCharts();
                        });
                    } finally {
                        dataLock.writeLock().unlock();
                    }
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Error",
                            "Failed to load transactions: " + e.getMessage()));
                    return null;
                });
    }

    private void updateSummary() {
        dataLock.readLock().lock();
        try {
            BigDecimal income = reportGenerator.calculateTotalIncome(new ArrayList<>(transactionCache));
            BigDecimal expenses = reportGenerator.calculateTotalExpenses(new ArrayList<>(transactionCache));
            BigDecimal balance = income.subtract(expenses);

            totalIncomeLabel.setText("Total Income: $" + income);
            totalExpenseLabel.setText("Total Expenses: $" + expenses);
            netBalanceLabel.setText("Net Balance: $" + balance);
        } finally {
            dataLock.readLock().unlock();
        }
    }

    private void setupCharts() {
        updateCharts();
    }

    private void updateCharts() {
        dataLock.readLock().lock();
        try {
            List<Transaction> expenses = new ArrayList<>();
            for (Transaction t : transactionCache) {
                if (t instanceof Expense) {
                    expenses.add(t);
                }
            }

            Map<String, BigDecimal> categoryBreakdown = reportGenerator.generateCategoryBreakdown(expenses);

            categoryPieChart.getData().clear();
            for (Map.Entry<String, BigDecimal> entry : categoryBreakdown.entrySet()) {
                PieChart.Data data = new PieChart.Data(entry.getKey(), entry.getValue().doubleValue());
                categoryPieChart.getData().add(data);
            }

            // Monthly line chart
            SortedMap<java.time.YearMonth, ReportGenerator.MonthlySummary> monthly = reportGenerator
                    .generateMonthlySummary(new ArrayList<>(transactionCache));

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Monthly Balance");
            for (Map.Entry<java.time.YearMonth, ReportGenerator.MonthlySummary> entry : monthly.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey().toString(),
                        entry.getValue().getBalance().doubleValue()));
            }

            monthlyLineChart.getData().clear();
            monthlyLineChart.getData().add(series);
        } finally {
            dataLock.readLock().unlock();
        }
    }

    private void clearForm() {
        amountField.clear();
        descriptionField.clear();
        datePicker.setValue(LocalDate.now());
        categoryComboBox.setValue(null);
    }

    private Stage getStage() {
        return (Stage) transactionTable.getScene().getWindow();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleLogout() {
        try {
            getStage().close();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Scene scene = new Scene(loader.load(), 400, 300);
            Stage stage = new Stage();
            stage.setTitle("Personal Expense Tracker");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
        } catch (IOException e) {
            logger.error("Failed to logout", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to logout: " + e.getMessage());
        }
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
