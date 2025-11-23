package com.expensetracker.dao;

import com.expensetracker.model.User;
import com.expensetracker.exceptions.DatabaseException;

/**
 * DAO interface for User operations.
 */
public interface UserDAO {
    
    int insert(User user) throws DatabaseException;
    
    User findByUsername(String username) throws DatabaseException;
    
    User authenticate(String username, String password) throws DatabaseException;
}

