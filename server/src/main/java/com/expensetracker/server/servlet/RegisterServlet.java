package com.expensetracker.server.servlet;

import com.expensetracker.dao.UserDAO;
import com.expensetracker.dao.impl.UserDAOImpl;
import com.expensetracker.exceptions.DatabaseException;
import com.expensetracker.model.User;
import com.expensetracker.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Servlet handling user registration at POST /api/auth/register.
 * 
 * Security considerations:
 * - Passwords are hashed using BCrypt (via PasswordUtil in UserDAOImpl)
 * - Input validation prevents injection and malformed data
 * - Username/email uniqueness enforced at DB level (UNIQUE constraint)
 * - No sensitive data (password) returned in responses
 * - Prepared statements used in DAO layer to prevent SQL injection
 * 
 * Request format:
 * POST /api/auth/register
 * Content-Type: application/json
 * Body: { "username": "...", "email": "...", "password": "..." }
 * 
 * Response codes:
 * - 201 Created: User successfully registered
 * - 400 Bad Request: Validation failed (invalid input format)
 * - 409 Conflict: Username or email already exists
 * - 500 Internal Server Error: Unexpected server error
 */
public class RegisterServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(RegisterServlet.class);
    private final UserDAO userDAO;

    // Validation patterns matching UserDAOImpl for consistency
    // Username: 3-30 chars, letters/numbers/underscore only
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,30}$");
    // Email: basic RFC-like pattern (simplified for practicality)
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    // Password: minimum 6 characters
    private static final int MIN_PASSWORD_LENGTH = 6;

    public RegisterServlet() {
        this.userDAO = new UserDAOImpl();
    }

    // Constructor for dependency injection (useful for testing)
    public RegisterServlet(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Handles POST request for user registration.
     * 
     * Workflow:
     * 1. Parse JSON body for username, email, password
     * 2. Validate all inputs (format, length)
     * 3. Check if username already exists (uniqueness)
     * 4. Create user (password hashed in DAO layer)
     * 5. Return success response with userId
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        
        try {
            // Step 1: Parse JSON request body
            String body = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            Map<?, ?> requestData = JsonUtil.fromJson(body, Map.class);

            String username = (String) requestData.get("username");
            String email = (String) requestData.get("email");
            String password = (String) requestData.get("password");

            // Step 2: Validate inputs - return 400 for validation errors
            String validationError = validateInputs(username, email, password);
            if (validationError != null) {
                logger.warn("Registration validation failed: {}", validationError);
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(JsonUtil.toJson(Map.of("error", validationError)));
                return;
            }

            // Step 3: Check if username already exists - return 409 for duplicates
            // Note: DB also has UNIQUE constraint as fallback, but we check explicitly
            // for a cleaner error message and to avoid DB exception handling
            User existingUser = userDAO.findByUsername(username.trim());
            if (existingUser != null) {
                logger.info("Registration rejected: username '{}' already exists", username);
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                resp.getWriter().write(JsonUtil.toJson(Map.of("error", "Username already exists")));
                return;
            }

            // Step 4: Create new user
            // Password will be hashed by UserDAOImpl.insert() using PasswordUtil.hashPassword()
            User newUser = new User();
            newUser.setUsername(username.trim());
            newUser.setEmail(email != null ? email.trim() : null);
            newUser.setPassword(password); // Raw password - DAO will hash it

            int userId = userDAO.insert(newUser);

            // Step 5: Return success response (201 Created)
            // Note: We do NOT auto-login or return token here.
            // This follows the project pattern where login is a separate action.
            // Client should call /api/auth/login after successful registration.
            logger.info("User '{}' registered successfully with ID: {}", username, userId);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write(JsonUtil.toJson(Map.of(
                    "message", "User created successfully",
                    "userId", userId
            )));

        } catch (DatabaseException e) {
            // Handle DB constraint violations (e.g., duplicate username if race condition)
            logger.error("Database error during registration", e);
            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("UNIQUE") || errorMsg.contains("Duplicate"))) {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                resp.getWriter().write(JsonUtil.toJson(Map.of("error", "Username or email already exists")));
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write(JsonUtil.toJson(Map.of("error", "Registration failed: " + errorMsg)));
            }
        } catch (Exception e) {
            // Catch-all for unexpected errors
            logger.error("Unexpected error during registration", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(JsonUtil.toJson(Map.of("error", "Internal server error")));
        }
    }

    /**
     * Validates registration inputs.
     * 
     * @param username the username to validate
     * @param email the email to validate (optional but validated if present)
     * @param password the password to validate
     * @return error message if validation fails, null if all inputs are valid
     */
    private String validateInputs(String username, String email, String password) {
        // Username validation
        if (username == null || username.trim().isEmpty()) {
            return "Username is required";
        }
        if (!USERNAME_PATTERN.matcher(username.trim()).matches()) {
            return "Username must be 3-30 characters, containing only letters, numbers, and underscores";
        }

        // Email validation (optional, but validated if provided)
        if (email != null && !email.trim().isEmpty()) {
            if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
                return "Invalid email format";
            }
        }

        // Password validation
        if (password == null || password.isEmpty()) {
            return "Password is required";
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters";
        }

        return null; // All validations passed
    }
}
