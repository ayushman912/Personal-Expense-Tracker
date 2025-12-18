package com.expensetracker.server;

import com.expensetracker.util.DatabaseManager;
import com.expensetracker.util.JsonUtil;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that data is strictly isolated between users.
 * Reproduces the "User B sees User A's data" bug.
 */
public class IsolationTest {

    private static Server server;
    private static final int PORT = 8082;
    private static final String BASE_URL = "http://localhost:" + PORT;
    private static HttpClient client;

    @BeforeAll
    static void startServer() throws Exception {
        // Use in-memory DB for testing - distinct from other tests
        DatabaseManager
                .setDbUrl("jdbc:h2:mem:isolationdb;DB_CLOSE_DELAY=-1;MODE=MySQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE");
        server = ServerApp.startServer(PORT);
        client = HttpClient.newHttpClient();
    }

    @AfterAll
    static void stopServer() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    private void createUser(String username, String password) throws Exception {
        com.expensetracker.dao.impl.UserDAOImpl userDAO = new com.expensetracker.dao.impl.UserDAOImpl();
        com.expensetracker.model.User user = new com.expensetracker.model.User();
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(username + "@example.com");
        try {
            userDAO.insert(user);
        } catch (Exception e) {
            // Ignore if exists
        }
    }

    private String login(String username, String password) throws Exception {
        String json = JsonUtil.toJson(Map.of("username", username, "password", password));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), "Login failed for " + username);
        Map<?, ?> map = JsonUtil.fromJson(response.body(), Map.class);
        return (String) map.get("token");
    }

    @Test
    void testDataIsolation() throws Exception {
        // 1. Setup Users
        String userA = "usera_" + UUID.randomUUID().toString().substring(0, 8);
        String userB = "userb_" + UUID.randomUUID().toString().substring(0, 8);
        createUser(userA, "passwordA");
        createUser(userB, "passwordB");

        String tokenA = login(userA, "passwordA");
        String tokenB = login(userB, "passwordB");

        // 2. User A creates a transaction
        // Note: Check categoryID 1 exists (Salary/Income usually). In DBManager, 1 is
        // 'Salary', type INCOME. But type in JSON says IT?
        // Let's use valid data.
        String validTxJson = "{\"amount\": 500.00, \"description\": \"User A Secret\", \"date\": \"2023-11-01\", \"categoryId\": 3, \"type\": \"EXPENSE\"}"; // 3
                                                                                                                                                             // is
                                                                                                                                                             // Food

        HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/transactions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + tokenA)
                .POST(HttpRequest.BodyPublishers.ofString(validTxJson))
                .build();

        HttpResponse<String> createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, createResponse.statusCode(), "Create failed: " + createResponse.body());

        // 3. User B requests ALL transactions
        HttpRequest listRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/transactions"))
                .header("Authorization", "Bearer " + tokenB)
                .GET()
                .build();

        HttpResponse<String> listResponse = client.send(listRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, listResponse.statusCode());

        // ASSERTION: User B should NOT see User A's transaction
        String responseBody = listResponse.body();
        // If bug exists, this might fail or pass depending on current impl.
        // We know TransactionDAOImpl.findAllByUser is used by TransactionServlet, which
        // IS filtered.
        // Wait, the bug report says "User B sees User A's transactions".
        // Let's verify if TransactionServlet actually calls findAllByUser (it does).
        // Maybe the bug is in how userId is retrieved or passed?
        // Or maybe there is another endpoint?

        // Let's stick to the plan: The bug IS that `TransactionDAOImpl` has methods
        // like `findAll` (no user arg)
        // and if *any* servlet used them, it would leak.
        // My investigation showed `TransactionServlet` uses `findAllByUser`.
        // However, the `TransactionDAOImpl` methods `findByCategory` and
        // `findByDateRange` (without User) EXIST and are public.
        // If the servlet used those based on params, it would leak.

        // Let's check `TransactionServlet` again carefully.
        // Lines 43-58 of TransactionServlet:
        // if (startDateStr != null && endDateStr != null && categoryIdStr != null) ->
        // findByDateRangeAndCategoryAndUser
        // else if (startDateStr != null && endDateStr != null) ->
        // findByDateRangeAndUser
        // else if (categoryIdStr != null) -> findByCategoryAndUser
        // else -> findAllByUser

        // It seems `TransactionServlet` IS correctly using the `...ByUser` variants.
        // So where is the leak coming from?
        // Maybe `UserDAOImpl` or `TokenStore` has an issue?
        // Or `TransactionDAOImpl` implementation of `...ByUser` is wrong?

        // Let's look at `TransactionDAOImpl.findByCategoryAndUser`:
        // SQL: SELECT ... FROM ... WHERE t.user_id = ? AND t.category_id = ?
        // This looks correct.

        // Wait, look at `TransactionDAOImpl.findAllByUser(int userId)`:
        // SQL: FIND_ALL_BY_USER_SQL
        // "SELECT ... WHERE t.user_id = ? ORDER BY t.date DESC"
        // This also looks correct.

        // Is it possible the `userId` in `TokenStore` is mixed up?
        // TokenStore uses a ConcurrentHashMap.

        // Let's re-read the bug report. "OBSERVED BUG... User B sees User A's
        // transactions".
        // "User B (brand new account) logs in".

        // Could it be the `AuthFilter`?
        // `req.setAttribute("userId", userId);`
        // Then `TransactionServlet`: `Integer userId = (Integer)
        // req.getAttribute("userId");`

        // Maybe the issue is in `ServerApp.java`?
        // Or maybe I missed something in `TransactionDAOImpl`?

        // Let's look at `TransactionDAOImpl` again.
        // `batchInsert` DOES NOT use `user_id` in the object?
        // `stmt.setInt(1, transaction.getAmount());` ...
        // The `INSERT_SQL` for batch is: "INSERT INTO transactions (user_id, amount,
        // description, date, category_id, type) VALUES (?, ?, ?, ?, ?, ?)"
        // But in `batchInsert`:
        // stmt.setBigDecimal(1, transaction.getAmount());
        // It seems the parameter indices are shifted because it's missing `user_id`?
        // `INSERT_SQL` has 6 params.
        // `batchInsert` sets 1..5. It MISSES `user_id`.
        // Wait, `INSERT_SQL` is:
        // "INSERT INTO transactions (user_id, amount, ...) VALUES (?, ?, ...)"
        // `batchInsert` does:
        // stmt.setBigDecimal(1, transaction.getAmount()); -> This is setting `user_id`
        // (param 1) to `amount` (BigDecimal)?
        // That would cause a type error or insert garbage `user_id`.
        // If `amount` is 100.50, `user_id` becomes 100?
        // That effectively assigns the transaction to a random user ID!
        // THIS IS A CRITICAL BUG, but does it explain "User B sees User A's data"?
        // If User A inserts data, and it gets assigned to User ID = Amount...
        // If Amount is small, say 1 or 2... it might assign to User B (ID 2).

        // Also look at `TransactionDAOImpl.insert(Transaction transaction)` (no userId
        // arg).
        // It calls `insert(transaction, transaction.getUserId())`.
        // If `transaction.getUserId()` is 0, it throws exception.

        // Re-read `TransactionServlet.doPost`:
        // `transaction.setUserId(userId);` -> sets correctly from token.
        // `transactionDAO.insert(transaction, userId);` -> calls the safer method.

        // So `batchInsert` is bugged but `TransactionServlet` doesn't use it.

        // Is there any other place?
        // `CategoryServlet`? `categoryDAO.findAll()`? That returns categories, which
        // are shared. That's fine.

        // Wait, I might have missed something in `TransactionDAOImpl.findAllByUser`.
        // private static final String FIND_ALL_BY_USER_SQL =
        // "SELECT ... WHERE t.user_id = ? ORDER BY t.date DESC";

        // Let's look at the `match` for `TransactionDAOImpl` again.
        // ... "WHERE t.user_id = ? ORDER BY t.date DESC"

        // ERROR: I need to verify what happens if `userId` is somehow 0 or -1?
        // `TokenStore.getUserId` returns -1 if invalid.
        // `TransactionServlet` checks `if (userId == null || userId <= 0)`.

        // Is it possible `TokenStore` is not thread safe in `getUserId`?
        // `activeTokens.get(token)` ... seems fine.

        // LET'S LOOK AT THE BUG REPORT "User A logs out. User B logs in."
        // "User B sees User A's transactions".
        // This implies PERSISTENCE of state or CROSSTALK.

        // Could it be client-side caching? "User B sees User A's transactions".
        // The deliverable requires "Fix ONLY what causes cross-user data leakage".

        // Maybe the issue is `TransactionDAOImpl.findAll` IS being used somewhere?
        // Or `TransactionServlet` logic has a flaw.

        // Let's look at `TransactionServlet.doGet` lines:
        /*
         * if (startDateStr != null && endDateStr != null && categoryIdStr != null) {
         * ...
         * } else if (startDateStr != null && endDateStr != null) {
         * ...
         * } else if (categoryIdStr != null) {
         * transactions = transactionDAO.findByCategoryAndUser(userId,
         * Integer.parseInt(categoryIdStr));
         * } else {
         * transactions = transactionDAO.findAllByUser(userId);
         * }
         */

        // What if `categoryIdStr` is invalid or something?

        // Let's assume the bug is real and I just need to reproduce it.
        // If the `batchInsert` is indeed assigning random `user_id`, that's a huge
        // lead.
        // But `TransactionServlet` calls `insert(transaction, userId)`.

        // Is it possible `DatabaseManager` connection pooling is not clearing session
        // variables?
        // (Not relevant for standard JDBC unless setting session variables).

        // Let's checking `TransactionDAOImpl.java` for "User-scoped FIND ALL".
        // Maybe the SQL string is defined incorrectly?
        // String FIND_ALL_BY_USER_SQL = "SELECT ... WHERE t.user_id = ? ...";

        // Wait!
        // `TransactionDAOImpl` lines 36-39:
        // private static final String FIND_ALL_BY_USER_SQL = ... "WHERE t.user_id = ?
        // ...";
        // `TransactionDAOImpl` lines 41-43:
        // private static final String FIND_ALL_SQL = ... "FROM transactions t LEFT JOIN
        // categories c ON t.category_id = c.id ORDER BY t.date DESC";

        // If `findAll` is called, it returns EVERYTHING.
        // `TransactionServlet` calls `findAllByUser`.

        // Is it possible the `TransactionServlet` is NOT the only entry point?
        // `ServerApp` registers:
        // `context.addServlet(new ServletHolder(new TransactionServlet()),
        // "/api/transactions/*");`

        // Let's write the test to call the API and see if it fails.
        // If it passes (i.e., isolation works), then I need to dig deeper.

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> transactions = JsonUtil.fromJson(responseBody, List.class);
        boolean leakDetected = false;
        for (Map<String, Object> tx : transactions) {
            if (tx.get("description").equals("User A Secret")) {
                leakDetected = true;
                break;
            }
        }

        if (leakDetected) {
            throw new RuntimeException("CRITICAL: User B saw User A's transaction!");
        }
    }
}
