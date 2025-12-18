package com.expensetracker.server.servlet;

import com.expensetracker.dao.TransactionDAO;
import com.expensetracker.dao.impl.TransactionDAOImpl;
import com.expensetracker.model.Transaction;
import com.expensetracker.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TransactionServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(TransactionServlet.class);
    private final TransactionDAO transactionDAO = new TransactionDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // Data is strictly scoped per user to prevent leakage
            Integer userId = (Integer) req.getAttribute("userId");
            if (userId == null || userId <= 0) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write(JsonUtil.toJson(Map.of("error", "User not authenticated")));
                return;
            }
            
            String startDateStr = req.getParameter("startDate");
            String endDateStr = req.getParameter("endDate");
            String categoryIdStr = req.getParameter("categoryId");

            List<Transaction> transactions;

            // All queries are scoped by userId to ensure data isolation
            if (startDateStr != null && endDateStr != null && categoryIdStr != null) {
                transactions = transactionDAO.findByDateRangeAndCategoryAndUser(
                        userId,
                        LocalDate.parse(startDateStr),
                        LocalDate.parse(endDateStr),
                        Integer.parseInt(categoryIdStr));
            } else if (startDateStr != null && endDateStr != null) {
                transactions = transactionDAO.findByDateRangeAndUser(
                        userId,
                        LocalDate.parse(startDateStr),
                        LocalDate.parse(endDateStr));
            } else if (categoryIdStr != null) {
                transactions = transactionDAO.findByCategoryAndUser(userId, Integer.parseInt(categoryIdStr));
            } else {
                transactions = transactionDAO.findAllByUser(userId);
            }

            resp.setContentType("application/json");
            resp.getWriter().write(JsonUtil.toJson(transactions));
        } catch (Exception e) {
            logger.error("Failed to retrieve transactions", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(Map.of("error", e.getMessage())));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // Data is strictly scoped per user to prevent leakage
            Integer userId = (Integer) req.getAttribute("userId");
            if (userId == null || userId <= 0) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write(JsonUtil.toJson(Map.of("error", "User not authenticated")));
                return;
            }
            
            String body = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            Transaction transaction = JsonUtil.fromJson(body, Transaction.class);

            // Basic validation
            if (transaction.getAmount() == null || transaction.getDescription() == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(Map.of("error", "Invalid transaction data")));
                return;
            }

            // Associate transaction with the authenticated user
            transaction.setUserId(userId);
            int id = transactionDAO.insert(transaction, userId);
            transaction.setId(id); // Set ID for response

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.setContentType("application/json");
            resp.getWriter().write(JsonUtil.toJson(transaction));
        } catch (Exception e) {
            logger.error("Failed to create transaction", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(Map.of("error", e.getMessage())));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // Data is strictly scoped per user to prevent leakage
            Integer userId = (Integer) req.getAttribute("userId");
            if (userId == null || userId <= 0) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write(JsonUtil.toJson(Map.of("error", "User not authenticated")));
                return;
            }
            
            String body = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            Transaction transaction = JsonUtil.fromJson(body, Transaction.class);

            if (transaction.getId() == 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(Map.of("error", "Transaction ID required for update")));
                return;
            }

            // Ensure transaction belongs to authenticated user
            transaction.setUserId(userId);
            transactionDAO.updateByUser(transaction, userId);

            resp.setContentType("application/json");
            resp.getWriter().write(JsonUtil.toJson(transaction));
        } catch (Exception e) {
            logger.error("Failed to update transaction", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(Map.of("error", e.getMessage())));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // Data is strictly scoped per user to prevent leakage
            Integer userId = (Integer) req.getAttribute("userId");
            if (userId == null || userId <= 0) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write(JsonUtil.toJson(Map.of("error", "User not authenticated")));
                return;
            }
            
            String idStr = req.getParameter("id");
            if (idStr == null) {
                // Try to get from path info if mapped as /api/transactions/*
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
            // Only delete if transaction belongs to authenticated user
            transactionDAO.deleteByUser(id, userId);

            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } catch (Exception e) {
            logger.error("Failed to delete transaction", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(Map.of("error", e.getMessage())));
        }
    }
}
