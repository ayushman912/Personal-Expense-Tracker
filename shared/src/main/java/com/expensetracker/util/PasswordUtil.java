package com.expensetracker.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility class for password hashing and verification using BCrypt.
 * BCrypt automatically handles salt generation and storage.
 */
public final class PasswordUtil {
    
    // BCrypt work factor (cost) - 12 is a good balance of security and performance
    private static final int BCRYPT_ROUNDS = 12;
    
    private PasswordUtil() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Hashes a plain-text password using BCrypt.
     * 
     * @param plainPassword the plain-text password to hash
     * @return the BCrypt hashed password
     * @throws IllegalArgumentException if password is null or empty
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
    }
    
    /**
     * Verifies a plain-text password against a BCrypt hash.
     * 
     * @param plainPassword the plain-text password to verify
     * @param hashedPassword the BCrypt hashed password to check against
     * @return true if the password matches, false otherwise
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // Invalid hash format - could be legacy plain-text password
            // Fall back to direct comparison for migration support
            return plainPassword.equals(hashedPassword);
        }
    }
}
