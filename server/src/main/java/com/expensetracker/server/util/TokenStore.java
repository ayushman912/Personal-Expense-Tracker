package com.expensetracker.server.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe token store with automatic expiration (TTL).
 * Tokens expire after 30 minutes of inactivity.
 */
public class TokenStore {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenStore.class);
    
    // Token TTL in milliseconds (30 minutes)
    private static final long TOKEN_TTL_MS = 30 * 60 * 1000;
    
    // Cleanup interval in minutes
    private static final long CLEANUP_INTERVAL_MINUTES = 5;
    
    /**
     * Internal class to store token data with timestamp and user info.
     * Stores both username and userId for proper data isolation.
     */
    private static class TokenEntry {
        final String username;
        final int userId;  // User ID for data isolation
        long lastAccessed;
        
        TokenEntry(String username, int userId) {
            this.username = username;
            this.userId = userId;
            this.lastAccessed = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - lastAccessed > TOKEN_TTL_MS;
        }
        
        void touch() {
            this.lastAccessed = System.currentTimeMillis();
        }
    }
    
    private static final Map<String, TokenEntry> activeTokens = new ConcurrentHashMap<>();
    
    // Background cleanup scheduler
    private static final ScheduledExecutorService cleanupScheduler;
    
    static {
        cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TokenStore-Cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupScheduler.scheduleAtFixedRate(
                TokenStore::cleanupExpiredTokens,
                CLEANUP_INTERVAL_MINUTES,
                CLEANUP_INTERVAL_MINUTES,
                TimeUnit.MINUTES
        );
        logger.info("TokenStore initialized with {}ms TTL", TOKEN_TTL_MS);
    }

    public static void addToken(String token, String username, int userId) {
        activeTokens.put(token, new TokenEntry(username, userId));
        logger.debug("Token added for user: {} (id: {})", username, userId);
    }
    
    // Legacy method for backward compatibility
    public static void addToken(String token, String username) {
        addToken(token, username, 0);
    }

    public static boolean isValid(String token) {
        TokenEntry entry = activeTokens.get(token);
        if (entry == null) {
            return false;
        }
        if (entry.isExpired()) {
            activeTokens.remove(token);
            logger.debug("Token expired and removed");
            return false;
        }
        // Refresh token on successful validation (sliding expiration)
        entry.touch();
        return true;
    }

    public static String getUser(String token) {
        TokenEntry entry = activeTokens.get(token);
        if (entry == null || entry.isExpired()) {
            return null;
        }
        entry.touch();
        return entry.username;
    }
    
    /**
     * Get the user ID associated with a token.
     * Returns -1 if token is invalid or expired.
     */
    public static int getUserId(String token) {
        TokenEntry entry = activeTokens.get(token);
        if (entry == null || entry.isExpired()) {
            return -1;
        }
        entry.touch();
        return entry.userId;
    }

    public static void removeToken(String token) {
        TokenEntry removed = activeTokens.remove(token);
        if (removed != null) {
            logger.debug("Token removed for user: {}", removed.username);
        }
    }
    
    /**
     * Removes all expired tokens from the store.
     */
    private static void cleanupExpiredTokens() {
        int removedCount = 0;
        for (Map.Entry<String, TokenEntry> entry : activeTokens.entrySet()) {
            if (entry.getValue().isExpired()) {
                activeTokens.remove(entry.getKey());
                removedCount++;
            }
        }
        if (removedCount > 0) {
            logger.info("Cleaned up {} expired tokens", removedCount);
        }
    }
    
    /**
     * Shuts down the cleanup scheduler. Call this on application shutdown.
     */
    public static void shutdown() {
        cleanupScheduler.shutdown();
        try {
            if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("TokenStore shutdown complete");
    }
}
