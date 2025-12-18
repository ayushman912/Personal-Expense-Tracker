package com.expensetracker.server.filter;

import com.expensetracker.server.util.TokenStore;
import com.expensetracker.util.JsonUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

@WebFilter("/api/*")
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // Allow CORS preflight if needed, or other public paths
        // Assuming /api/auth/* is public, but let's check path
        String path = req.getRequestURI().substring(req.getContextPath().length());

        // Exclude login/auth endpoints
        if (path.startsWith("/api/auth/")) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = req.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (TokenStore.isValid(token)) {
                // Set user info as request attributes for data isolation
                // Each user's data is strictly scoped to prevent leakage
                String username = TokenStore.getUser(token);
                int userId = TokenStore.getUserId(token);
                req.setAttribute("username", username);
                req.setAttribute("userId", userId);
                chain.doFilter(request, response);
                return;
            }
        }

        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setContentType("application/json");
        resp.getWriter().write(JsonUtil.toJson(Map.of("error", "Unauthorized access")));
    }

    @Override
    public void destroy() {
    }
}
