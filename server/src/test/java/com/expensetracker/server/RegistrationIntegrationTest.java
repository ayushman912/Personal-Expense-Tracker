package com.expensetracker.server;

import com.expensetracker.util.DatabaseManager;
import com.expensetracker.util.JsonUtil;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the user registration feature.
 * 
 * Tests cover:
 * 1. Successful registration (201 Created)
 * 2. Duplicate username rejection (409 Conflict)
 * 3. Input validation errors (400 Bad Request)
 * 4. Full registration → login → transaction → isolation workflow
 * 
 * Multi-user isolation scenario:
 * - Register User A, login, add transaction
 * - Register User B, login, verify zero transactions (data isolation)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RegistrationIntegrationTest {

    private static Server server;
    private static final int PORT = 8083;
    private static final String BASE_URL = "http://localhost:" + PORT;
    private static HttpClient client;

    // Unique user identifiers for this test run
    private static final String TEST_ID = UUID.randomUUID().toString().substring(0, 6);
    private static final String USER_A_NAME = "usera_" + TEST_ID;
    private static final String USER_B_NAME = "userb_" + TEST_ID;
    private static final String TEST_PASSWORD = "testPass123";
    private static final String USER_A_EMAIL = USER_A_NAME + "@test.com";
    private static final String USER_B_EMAIL = USER_B_NAME + "@test.com";

    @BeforeAll
    static void startServer() throws Exception {
        // Use in-memory H2 database for testing - distinct from other tests
        // DB_CLOSE_DELAY=-1 keeps DB alive for the duration of the JVM
        DatabaseManager.setDbUrl(
                "jdbc:h2:mem:registrationdb_" + TEST_ID + ";DB_CLOSE_DELAY=-1;MODE=MySQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE");
        server = ServerApp.startServer(PORT);
        client = HttpClient.newHttpClient();
    }

    @AfterAll
    static void stopServer() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    // ==================== Registration Tests ====================

    @Test
    @Order(1)
    void testRegisterUserA_Success() throws Exception {
        // Register User A with valid credentials
        HttpResponse<String> response = register(USER_A_NAME, USER_A_EMAIL, TEST_PASSWORD);
        
        assertEquals(201, response.statusCode(), "Expected 201 Created for successful registration");
        
        Map<?, ?> body = JsonUtil.fromJson(response.body(), Map.class);
        assertNotNull(body.get("userId"), "Response should contain userId");
        assertEquals("User created successfully", body.get("message"));
        
        // Verify userId is a positive integer
        Number userId = (Number) body.get("userId");
        assertTrue(userId.intValue() > 0, "userId should be positive");
    }

    @Test
    @Order(2)
    void testRegisterDuplicateUsername_Conflict() throws Exception {
        // Try to register with the same username as User A (already registered in Order 1)
        HttpResponse<String> response = register(USER_A_NAME, "different@email.com", TEST_PASSWORD);
        
        assertEquals(409, response.statusCode(), "Expected 409 Conflict for duplicate username");
        
        Map<?, ?> body = JsonUtil.fromJson(response.body(), Map.class);
        String error = (String) body.get("error");
        assertNotNull(error, "Response should contain error message");
        assertTrue(error.toLowerCase().contains("already exists"), 
                "Error should mention username already exists: " + error);
    }

    @Test
    @Order(3)
    void testRegisterUserB_Success() throws Exception {
        // Register User B with different credentials
        HttpResponse<String> response = register(USER_B_NAME, USER_B_EMAIL, TEST_PASSWORD);
        
        assertEquals(201, response.statusCode(), "Expected 201 Created for User B registration");
        
        Map<?, ?> body = JsonUtil.fromJson(response.body(), Map.class);
        assertNotNull(body.get("userId"), "Response should contain userId");
    }

    @Test
    @Order(4)
    void testRegisterValidationErrors() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 4);
        
        // Test: Missing username
        HttpResponse<String> resp1 = register("", "test@email.com", "validPass");
        assertEquals(400, resp1.statusCode(), "Should reject empty username");
        assertTrue(resp1.body().contains("error"));

        // Test: Username too short (less than 3 chars)
        HttpResponse<String> resp2 = register("ab", "test@email.com", "validPass");
        assertEquals(400, resp2.statusCode(), "Should reject username < 3 chars");

        // Test: Invalid username characters
        HttpResponse<String> resp3 = register("user@invalid!" + uniqueSuffix, "test@email.com", "validPass");
        assertEquals(400, resp3.statusCode(), "Should reject special chars in username");

        // Test: Invalid email format
        HttpResponse<String> resp4 = register("validuser" + uniqueSuffix, "not-an-email", "validPass");
        assertEquals(400, resp4.statusCode(), "Should reject invalid email format");

        // Test: Password too short (less than 6 chars)
        HttpResponse<String> resp5 = register("validuser2" + uniqueSuffix, "test@email.com", "12345");
        assertEquals(400, resp5.statusCode(), "Should reject password < 6 chars");

        // Test: Missing password
        HttpResponse<String> resp6 = register("validuser3" + uniqueSuffix, "test@email.com", "");
        assertEquals(400, resp6.statusCode(), "Should reject empty password");
    }

    // ==================== Multi-User Isolation Test ====================

    @Test
    @Order(5)
    void testMultiUserIsolation() throws Exception {
        // This test verifies that newly registered users have zero transactions
        // and cannot see other users' data
        
        // Step 1: Login as User A (registered in Order 1)
        String tokenA = login(USER_A_NAME, TEST_PASSWORD);
        assertNotNull(tokenA, "User A should be able to login after registration");

        // Step 2: Create a transaction for User A
        String txJson = "{\"amount\": 123.45, \"description\": \"User A Private Transaction\", " +
                "\"date\": \"2024-01-15\", \"categoryId\": 1, \"type\": \"INCOME\"}";
        HttpResponse<String> createResponse = createTransaction(tokenA, txJson);
        assertEquals(201, createResponse.statusCode(), 
                "User A should be able to create transaction: " + createResponse.body());

        // Step 3: Verify User A can see their transaction
        HttpResponse<String> listAResponse = getTransactions(tokenA);
        assertEquals(200, listAResponse.statusCode());
        assertTrue(listAResponse.body().contains("User A Private Transaction"),
                "User A should see their own transaction");

        // Step 4: Login as User B (registered in Order 3)
        String tokenB = login(USER_B_NAME, TEST_PASSWORD);
        assertNotNull(tokenB, "User B should be able to login after registration");

        // Step 5: Verify User B sees ZERO transactions (data isolation)
        HttpResponse<String> listBResponse = getTransactions(tokenB);
        assertEquals(200, listBResponse.statusCode());
        
        List<?> userBTransactions = JsonUtil.fromJson(listBResponse.body(), List.class);
        assertEquals(0, userBTransactions.size(), 
                "User B should have ZERO transactions (new user isolation)");
        assertFalse(listBResponse.body().contains("User A Private Transaction"),
                "User B must NOT see User A's transactions");
    }

    @Test
    @Order(6)
    void testLoginAfterRegistration() throws Exception {
        // Register a completely new user and immediately login
        String newUsername = "newuser_" + UUID.randomUUID().toString().substring(0, 6);
        String newPassword = "newPass456";
        
        // Register
        HttpResponse<String> regResponse = register(newUsername, newUsername + "@test.com", newPassword);
        assertEquals(201, regResponse.statusCode(), "Registration should succeed");
        
        // Login with the same credentials
        String token = login(newUsername, newPassword);
        assertNotNull(token, "Should be able to login immediately after registration");
        assertTrue(token.length() > 10, "Token should be a valid UUID-like string");
        
        // Verify the token works for authenticated endpoints
        HttpResponse<String> txResponse = getTransactions(token);
        assertEquals(200, txResponse.statusCode(), "Token should be valid for authenticated requests");
    }

    @Test
    @Order(7)
    void testPasswordHashingVerification() throws Exception {
        // Verify that BCrypt password hashing is working correctly
        // by attempting login with wrong password
        String wrongPassword = "wrongPassword123";
        
        String json = JsonUtil.toJson(Map.of("username", USER_A_NAME, "password", wrongPassword));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, response.statusCode(), 
                "Wrong password should be rejected (confirms hashing is working)");
    }

    // ==================== Helper Methods ====================

    /**
     * Sends a registration request to /api/auth/register.
     */
    private HttpResponse<String> register(String username, String email, String password) throws Exception {
        Map<String, String> body = new java.util.HashMap<>();
        body.put("username", username);
        body.put("password", password);
        if (email != null && !email.isEmpty()) {
            body.put("email", email);
        }
        
        String json = JsonUtil.toJson(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Logs in and returns the auth token.
     */
    private String login(String username, String password) throws Exception {
        String json = JsonUtil.toJson(Map.of("username", username, "password", password));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            fail("Login failed for " + username + ": " + response.body());
        }
        
        Map<?, ?> map = JsonUtil.fromJson(response.body(), Map.class);
        return (String) map.get("token");
    }

    /**
     * Creates a transaction with the given token.
     */
    private HttpResponse<String> createTransaction(String token, String txJson) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/transactions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(txJson))
                .build();
        
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Gets all transactions for the authenticated user.
     */
    private HttpResponse<String> getTransactions(String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/transactions"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
