package com.expensetracker.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Paginator generic utility class.
 * Tests pagination functionality with different data types.
 */
class PaginatorTest {
    
    private List<String> testData;
    
    @BeforeEach
    void setUp() {
        testData = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            testData.add("Item " + i);
        }
    }
    
    @Test
    void testGetCurrentPage() {
        Paginator<String> paginator = new Paginator<>(testData, 10);
        List<String> page = paginator.getCurrentPage();
        
        assertEquals(10, page.size());
        assertEquals("Item 1", page.get(0));
        assertEquals("Item 10", page.get(9));
    }
    
    @Test
    void testNextPage() {
        Paginator<String> paginator = new Paginator<>(testData, 10);
        
        assertTrue(paginator.hasNextPage());
        assertTrue(paginator.nextPage());
        assertEquals(1, paginator.getCurrentPageNumber());
        
        List<String> page = paginator.getCurrentPage();
        assertEquals("Item 11", page.get(0));
    }
    
    @Test
    void testPreviousPage() {
        Paginator<String> paginator = new Paginator<>(testData, 10);
        paginator.nextPage();
        
        assertTrue(paginator.hasPreviousPage());
        assertTrue(paginator.previousPage());
        assertEquals(0, paginator.getCurrentPageNumber());
    }
    
    @Test
    void testGoToPage() {
        Paginator<String> paginator = new Paginator<>(testData, 10);
        
        assertTrue(paginator.goToPage(2));
        assertEquals(2, paginator.getCurrentPageNumber());
        
        assertFalse(paginator.goToPage(10)); // Out of range
    }
    
    @Test
    void testTotalPages() {
        Paginator<String> paginator = new Paginator<>(testData, 10);
        assertEquals(3, paginator.getTotalPages());
        
        Paginator<String> paginator2 = new Paginator<>(testData, 5);
        assertEquals(5, paginator2.getTotalPages());
    }
    
    @Test
    void testEmptyList() {
        List<String> empty = new ArrayList<>();
        Paginator<String> paginator = new Paginator<>(empty, 10);
        
        assertTrue(paginator.getCurrentPage().isEmpty());
        assertEquals(1, paginator.getTotalPages());
    }
    
    @Test
    void testReset() {
        Paginator<String> paginator = new Paginator<>(testData, 10);
        paginator.nextPage();
        paginator.nextPage();
        
        paginator.reset();
        assertEquals(0, paginator.getCurrentPageNumber());
    }
}

