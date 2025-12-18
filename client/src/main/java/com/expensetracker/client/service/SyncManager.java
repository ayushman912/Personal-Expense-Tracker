package com.expensetracker.client.service;

import com.expensetracker.client.api.RemoteRepository;
import com.expensetracker.client.model.SyncOperation;
import com.expensetracker.model.Transaction;
import com.expensetracker.util.DatabaseManager;
import com.expensetracker.util.JsonUtil;
import com.expensetracker.exceptions.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SyncManager {
    private static final Logger logger = LoggerFactory.getLogger(SyncManager.class);
    private final RemoteRepository remoteRepository;
    private Runnable onSyncComplete;

    public void setOnSyncComplete(Runnable onSyncComplete) {
        this.onSyncComplete = onSyncComplete;
    }

    public SyncManager(RemoteRepository remoteRepository) {
        this.remoteRepository = remoteRepository;
        initializeSyncTable();
    }

    private void initializeSyncTable() {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS sync_queue (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "operation_type VARCHAR(20) NOT NULL, " +
                    "entity_type VARCHAR(20) NOT NULL, " +
                    "payload TEXT, " +
                    "timestamp BIGINT, " +
                    "retry_count INT DEFAULT 0)");
            
            // Add retry_count column if it doesn't exist (for existing tables)
            try {
                stmt.execute("ALTER TABLE sync_queue ADD COLUMN IF NOT EXISTS retry_count INT DEFAULT 0");
            } catch (SQLException ignored) {
                // Column might already exist or syntax not supported
            }
        } catch (SQLException | DatabaseException e) {
            logger.error("Failed to initialize sync table", e);
        }
    }

    public void queueOperation(SyncOperation operation) {
        String sql = "INSERT INTO sync_queue (operation_type, entity_type, payload, timestamp) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, operation.getOperationType());
            stmt.setString(2, operation.getEntityType());
            stmt.setString(3, operation.getPayload());
            stmt.setLong(4, operation.getTimestamp());
            stmt.executeUpdate();
        } catch (SQLException | DatabaseException e) {
            logger.error("Failed to queue sync operation", e);
        }
    }

    private static final int MAX_RETRY_COUNT = 3;

    public CompletableFuture<Void> sync() {
        return CompletableFuture.runAsync(() -> {
            List<SyncOperation> operations = getPendingOperations();
            for (SyncOperation op : operations) {
                try {
                    processOperation(op);
                    deleteOperation(op.getId());
                } catch (Exception e) {
                    logger.warn("Failed to sync operation {}: {}", op.getId(), e.getMessage());
                    incrementRetryCount(op.getId());
                    
                    // Check if we've exceeded max retries
                    if (op.getRetryCount() >= MAX_RETRY_COUNT - 1) {
                        logger.warn("Operation {} exceeded max retries, removing from queue", op.getId());
                        deleteOperation(op.getId());
                    }
                }
            }
            if (!operations.isEmpty() && onSyncComplete != null) {
                onSyncComplete.run();
            }
        });
    }

    private void incrementRetryCount(int id) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement("UPDATE sync_queue SET retry_count = retry_count + 1 WHERE id = ?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException | DatabaseException e) {
            logger.error("Failed to increment retry count", e);
        }
    }

    private List<SyncOperation> getPendingOperations() {
        List<SyncOperation> operations = new ArrayList<>();
        String sql = "SELECT * FROM sync_queue ORDER BY timestamp ASC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                SyncOperation op = new SyncOperation();
                op.setId(rs.getInt("id"));
                op.setOperationType(rs.getString("operation_type"));
                op.setEntityType(rs.getString("entity_type"));
                op.setPayload(rs.getString("payload"));
                op.setTimestamp(rs.getLong("timestamp"));
                try {
                    op.setRetryCount(rs.getInt("retry_count"));
                } catch (SQLException e) {
                    op.setRetryCount(0); // Default if column doesn't exist
                }
                operations.add(op);
            }
        } catch (SQLException | DatabaseException e) {
            logger.error("Failed to get pending operations", e);
        }
        return operations;
    }

    private void processOperation(SyncOperation op) throws Exception {
        if ("TRANSACTION".equals(op.getEntityType())) {
            Transaction transaction = JsonUtil.fromJson(op.getPayload(), Transaction.class);
            if ("INSERT".equals(op.getOperationType())) {
                remoteRepository.addTransaction(transaction).join();
            } else if ("UPDATE".equals(op.getOperationType())) {
                remoteRepository.updateTransaction(transaction).join();
            } else if ("DELETE".equals(op.getOperationType())) {
                remoteRepository.deleteTransaction(transaction.getId()).join();
            }
        } else if ("CATEGORY".equals(op.getEntityType())) {
            // Category category = JsonUtil.fromJson(op.getPayload(), Category.class);
            if ("INSERT".equals(op.getOperationType())) {
                // remoteRepository.addCategory(category).join();
            }
        }
    }

    private void deleteOperation(int id) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                PreparedStatement stmt = conn.prepareStatement("DELETE FROM sync_queue WHERE id = ?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException | DatabaseException e) {
            logger.error("Failed to delete operation", e);
        }
    }

    /**
     * Clears all pending sync operations from the queue.
     * Useful for cleaning up corrupted or stale data.
     */
    public void clearQueue() {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                Statement stmt = conn.createStatement()) {
            int deleted = stmt.executeUpdate("DELETE FROM sync_queue");
            logger.info("Cleared {} pending sync operations from queue", deleted);
        } catch (SQLException | DatabaseException e) {
            logger.error("Failed to clear sync queue", e);
        }
    }

    /**
     * Gets the count of pending operations in the queue.
     */
    public int getPendingCount() {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM sync_queue")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException | DatabaseException e) {
            logger.error("Failed to get pending count", e);
        }
        return 0;
    }
}
