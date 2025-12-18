package com.expensetracker.server.servlet;

import com.expensetracker.dao.UserDAO;
import com.expensetracker.dao.impl.UserDAOImpl;
import com.expensetracker.model.User;
import com.expensetracker.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class AuthServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(AuthServlet.class);
    private final UserDAO userDAO = new UserDAOImpl();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String body = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            Map<?, ?> credentials = JsonUtil.fromJson(body, Map.class);

            String username = (String) credentials.get("username");
            String password = (String) credentials.get("password");

            if (username == null || password == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(Map.of("error", "Username and password required")));
                return;
            }

            User user = userDAO.authenticate(username, password);
            if (user != null) {
                String token = UUID.randomUUID().toString();
                // Store token with user ID for data isolation
                // Each user's data is strictly scoped to prevent leakage
                com.expensetracker.server.util.TokenStore.addToken(token, user.getUsername(), user.getId());

                Map<String, Object> response = new HashMap<>();
                response.put("token", token);
                response.put("user", user);
                response.put("userId", user.getId());  // Include userId for client-side reference

                resp.setContentType("application/json");
                resp.getWriter().write(JsonUtil.toJson(response));
                logger.info("User {} (id: {}) authenticated successfully", user.getUsername(), user.getId());
            } else {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write(JsonUtil.toJson(Map.of("error", "Invalid credentials")));
            }
        } catch (Exception e) {
            logger.error("Authentication error", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(Map.of("error", e.getMessage())));
        }
    }
}
