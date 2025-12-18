package com.expensetracker.server.servlet;

import com.expensetracker.dao.CategoryDAO;
import com.expensetracker.dao.impl.CategoryDAOImpl;
import com.expensetracker.model.Category;
import com.expensetracker.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servlet for category CRUD operations.
 * 
 * Categories are user-scoped to ensure data isolation between users.
 * Each user sees their own categories plus system default categories.
 */
public class CategoryServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(CategoryServlet.class);
    private final CategoryDAO categoryDAO = new CategoryDAOImpl();

    @Override
    @SuppressWarnings("deprecation") // Intentional: findAll() used as fallback for unauthenticated requests to return system categories
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // User-scoped category retrieval for data isolation
            Integer userId = (Integer) req.getAttribute("userId");
            List<Category> categories;
            
            if (userId != null && userId > 0) {
                // Return user's categories plus system categories
                categories = categoryDAO.findAllByUser(userId);
            } else {
                // Fallback to system categories only (backward compatibility)
                categories = categoryDAO.findAll();
            }
            
            resp.setContentType("application/json");
            resp.getWriter().write(JsonUtil.toJson(categories));
        } catch (Exception e) {
            logger.error("Failed to retrieve categories", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(Map.of("error", e.getMessage())));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // User-scoped category creation for data isolation
            Integer userId = (Integer) req.getAttribute("userId");
            
            String body = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            Category category = JsonUtil.fromJson(body, Category.class);

            if (category.getName() == null || category.getType() == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(Map.of("error", "Name and Type required")));
                return;
            }

            // Associate category with authenticated user for data isolation
            if (userId != null && userId > 0) {
                category.setUserId(userId);
            }

            int id = categoryDAO.insert(category);
            category.setId(id);

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.setContentType("application/json");
            resp.getWriter().write(JsonUtil.toJson(category));
        } catch (Exception e) {
            logger.error("Failed to create category", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(Map.of("error", e.getMessage())));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // User-scoped category update for data isolation
            Integer userId = (Integer) req.getAttribute("userId");
            
            String body = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            Category category = JsonUtil.fromJson(body, Category.class);

            if (category.getId() == 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(Map.of("error", "ID required for update")));
                return;
            }

            // Set userId for ownership verification during update
            if (userId != null && userId > 0) {
                category.setUserId(userId);
            }

            categoryDAO.update(category);

            resp.setContentType("application/json");
            resp.getWriter().write(JsonUtil.toJson(category));
        } catch (Exception e) {
            logger.error("Failed to update category", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(Map.of("error", e.getMessage())));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // User-scoped category deletion for data isolation
            Integer userId = (Integer) req.getAttribute("userId");
            
            String idStr = req.getParameter("id");
            if (idStr == null) {
                String pathInfo = req.getPathInfo();
                if (pathInfo != null && pathInfo.length() > 1) {
                    idStr = pathInfo.substring(1);
                }
            }

            if (idStr == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(Map.of("error", "ID required")));
                return;
            }

            int id = Integer.parseInt(idStr);
            
            // Use user-scoped deletion if authenticated
            if (userId != null && userId > 0) {
                categoryDAO.deleteByUser(id, userId);
            } else {
                categoryDAO.delete(id);
            }

            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } catch (Exception e) {
            logger.error("Failed to delete category", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(Map.of("error", e.getMessage())));
        }
    }
}
