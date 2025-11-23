package com.expensetracker.dao;

import com.expensetracker.model.Category;
import com.expensetracker.exceptions.DatabaseException;
import java.util.List;

/**
 * DAO interface for Category operations.
 */
public interface CategoryDAO {
    
    int insert(Category category) throws DatabaseException;
    
    void update(Category category) throws DatabaseException;
    
    void delete(int id) throws DatabaseException;
    
    Category findById(int id) throws DatabaseException;
    
    List<Category> findAll() throws DatabaseException;
    
    List<Category> findByType(String type) throws DatabaseException;
    
    Category findByName(String name) throws DatabaseException;
}

