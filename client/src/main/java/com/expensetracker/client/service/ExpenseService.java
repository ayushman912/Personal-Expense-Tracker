package com.expensetracker.client.service;

import com.expensetracker.client.api.RemoteRepository;
import com.expensetracker.client.model.SyncOperation;
import com.expensetracker.dao.TransactionDAO;
import com.expensetracker.dao.impl.TransactionDAOImpl;
import com.expensetracker.model.Transaction;
import com.expensetracker.model.User;
import com.expensetracker.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExpenseService {
    private static final Logger logger = LoggerFactory.getLogger(ExpenseService.class);
    private final TransactionDAO localDAO;
    private final RemoteRepository remoteRepository;
    private final SyncManager syncManager;
    private final ExecutorService executorService;
    private final ConnectivityManager connectivityManager;
    private User currentUser;  // Current logged-in user for data isolation

    public ExpenseService() {
        this.localDAO = new TransactionDAOImpl();
        this.remoteRepository = new RemoteRepository();
        this.connectivityManager = new ConnectivityManager(remoteRepository);
        this.syncManager = new SyncManager(remoteRepository);
        this.executorService = Executors.newCachedThreadPool();

        // Listen for connectivity changes
        this.connectivityManager.addListener(state -> {
            boolean online = (state == ConnectivityManager.ConnectionState.ONLINE);
            logger.info("Connectivity changed: {}", state);
            if (online) {
                syncManager.sync();
            }
        });
    }

    /**
     * Sets the current logged-in user for data isolation in offline mode.
     * Must be called after successful authentication.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        logger.info("Current user set to: {} (ID: {})", user.getUsername(), user.getId());
    }

    /**
     * Gets the current logged-in user.
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Gets the current user's ID, or 0 if not logged in.
     */
    public int getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : 0;
    }

    public ConnectivityManager getConnectivityManager() {
        return connectivityManager;
    }

    private boolean isOnline() {
        return connectivityManager.isOnline();
    }

    public CompletableFuture<List<Transaction>> getAllTransactions() {
        if (isOnline()) {
            return remoteRepository.getTransactions()
                    .exceptionally(e -> {
                        logger.warn("Remote fetch failed, falling back to local: {}", e.getMessage());
                        // ConnectivityManager should handle state change via scheduled checks, but we
                        // can hint it or just rely on fallback
                        // setOnline(false); // No longer manually setting flag
                        return getLocalTransactions();
                    });
        } else {
            return CompletableFuture.supplyAsync(this::getLocalTransactions, executorService);
        }
    }

    /**
     * Gets transactions from local database for the current user.
     * Returns empty list if no user is logged in.
     */
    private List<Transaction> getLocalTransactions() {
        int userId = getCurrentUserId();
        if (userId <= 0) {
            logger.warn("No user logged in, cannot fetch local transactions");
            return Collections.emptyList();
        }
        try {
            return localDAO.findAllByUser(userId);
        } catch (Exception e) {
            logger.error("Failed to fetch local transactions for user {}", userId, e);
            throw new RuntimeException(e);
        }
    }

    public CompletableFuture<Transaction> addTransaction(Transaction transaction) {
        if (isOnline()) {
            return remoteRepository.addTransaction(transaction)
                    .thenApply(remoteTx -> {
                        // Also save to local DB for cache/fallback
                        try {
                            localDAO.insert(remoteTx);
                        } catch (Exception e) {
                            logger.error("Failed to cache transaction locally", e);
                        }
                        return remoteTx;
                    })
                    .exceptionally(e -> {
                        logger.warn("Remote add failed, queuing local: {}", e.getMessage());
                        // setOnline(false);
                        return addLocalAndQueue(transaction);
                    });
        } else {
            return CompletableFuture.supplyAsync(() -> addLocalAndQueue(transaction), executorService);
        }
    }

    private Transaction addLocalAndQueue(Transaction transaction) {
        try {
            // Set user ID for data isolation
            int userId = getCurrentUserId();
            if (userId <= 0) {
                throw new RuntimeException("No user logged in, cannot add transaction");
            }
            transaction.setUserId(userId);
            
            int id = localDAO.insert(transaction, userId);
            transaction.setId(id);

            SyncOperation op = new SyncOperation("INSERT", "TRANSACTION", JsonUtil.toJson(transaction));
            syncManager.queueOperation(op);

            return transaction;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public CompletableFuture<Void> deleteTransaction(int id) {
        if (isOnline()) {
            return remoteRepository.deleteTransaction(id)
                    .thenRun(() -> {
                        try {
                            localDAO.delete(id);
                        } catch (Exception e) {
                            logger.error("Failed to delete transaction from local cache", e);
                        }
                    })
                    .exceptionally(e -> {
                        logger.warn("Remote delete failed, queuing local: {}", e.getMessage());
                        // setOnline(false);
                        deleteLocalAndQueue(id);
                        return null;
                    });
        } else {
            return CompletableFuture.runAsync(() -> deleteLocalAndQueue(id), executorService);
        }
    }

    private void deleteLocalAndQueue(int id) {
        try {
            // Set user ID for data isolation
            int userId = getCurrentUserId();
            if (userId <= 0) {
                throw new RuntimeException("No user logged in, cannot delete transaction");
            }
            
            // We need the transaction object to queue a delete if we want to be consistent,
            // but for delete we mainly need ID. However, SyncOperation stores payload.
            // For simplicity, we'll create a dummy transaction with just ID.
            // Since Transaction is abstract, we use Expense as a placeholder.
            Transaction t = new com.expensetracker.model.Expense();
            t.setId(id);
            t.setUserId(userId);

            localDAO.deleteByUser(id, userId);

            SyncOperation op = new SyncOperation("DELETE", "TRANSACTION", JsonUtil.toJson(t));
            syncManager.queueOperation(op);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public SyncManager getSyncManager() {
        return syncManager;
    }

    public RemoteRepository getRemoteRepository() {
        return remoteRepository;
    }
}
