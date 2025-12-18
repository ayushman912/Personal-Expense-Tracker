package com.expensetracker.util;

import java.util.List;

/**
 * Generic utility class for pagination.
 * Demonstrates generics and collections usage.
 * @param <T> The type of items to paginate
 */
public class Paginator<T> {
    private final List<T> items;
    private final int pageSize;
    private int currentPage;
    
    /**
     * Constructor for Paginator.
     * @param items The list of items to paginate
     * @param pageSize Number of items per page
     */
    public Paginator(List<T> items, int pageSize) {
        this.items = items;
        this.pageSize = pageSize > 0 ? pageSize : 10;
        this.currentPage = 0;
    }
    
    /**
     * Get the current page of items.
     * @return List of items for the current page
     */
    public List<T> getCurrentPage() {
        int start = currentPage * pageSize;
        int end = Math.min(start + pageSize, items.size());
        
        if (start >= items.size()) {
            return List.of();
        }
        
        return items.subList(start, end);
    }
    
    /**
     * Move to the next page.
     * @return true if moved to next page, false if already on last page
     */
    public boolean nextPage() {
        if (hasNextPage()) {
            currentPage++;
            return true;
        }
        return false;
    }
    
    /**
     * Move to the previous page.
     * @return true if moved to previous page, false if already on first page
     */
    public boolean previousPage() {
        if (hasPreviousPage()) {
            currentPage--;
            return true;
        }
        return false;
    }
    
    /**
     * Go to a specific page (0-indexed).
     * @param page The page number
     * @return true if page exists and was navigated to
     */
    public boolean goToPage(int page) {
        if (page >= 0 && page < getTotalPages()) {
            currentPage = page;
            return true;
        }
        return false;
    }
    
    /**
     * Check if there is a next page.
     * @return true if next page exists
     */
    public boolean hasNextPage() {
        return currentPage < getTotalPages() - 1;
    }
    
    /**
     * Check if there is a previous page.
     * @return true if previous page exists
     */
    public boolean hasPreviousPage() {
        return currentPage > 0;
    }
    
    /**
     * Get the current page number (0-indexed).
     * @return Current page number
     */
    public int getCurrentPageNumber() {
        return currentPage;
    }
    
    /**
     * Get the total number of pages.
     * @return Total pages
     */
    public int getTotalPages() {
        if (items.isEmpty()) {
            return 1;
        }
        return (int) Math.ceil((double) items.size() / pageSize);
    }
    
    /**
     * Get the total number of items.
     * @return Total items count
     */
    public int getTotalItems() {
        return items.size();
    }
    
    /**
     * Get the page size.
     * @return Items per page
     */
    public int getPageSize() {
        return pageSize;
    }
    
    /**
     * Reset to first page.
     */
    public void reset() {
        currentPage = 0;
    }
}

