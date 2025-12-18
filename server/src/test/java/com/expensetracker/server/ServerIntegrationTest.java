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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServerIntegrationTest {

        private static Server server;
        private static final int PORT = 8081;
        private static final String BASE_URL = "http://localhost:" + PORT;
        private static HttpClient client;

        @BeforeAll
        static void startServer() throws Exception {
                // Use in-memory DB for testing
                DatabaseManager
                                .setDbUrl("jdbc:h2:mem:integrationdb;DB_CLOSE_DELAY=-1;MODE=MySQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE");

                server = ServerApp.startServer(PORT);
                client = HttpClient.newHttpClient();
        }

        @AfterAll
        static void stopServer() throws Exception {
                if (server != null) {
                        server.stop();
                }
        }

        @Test
        void testHealthCheck() throws Exception {
                HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(BASE_URL + "/health"))
                                .GET()
                                .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                assertEquals(200, response.statusCode());
                assertEquals("Server is running", response.body());
        }

        @Test
        void testLogin() throws Exception {
                // Assuming admin user is created by default in DatabaseManager init
                String json = JsonUtil.toJson(Map.of("username", "admin", "password", "admin123"));

                HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(BASE_URL + "/api/auth/login"))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(json))
                                .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                assertEquals(200, response.statusCode());
                assertTrue(response.body().contains("token"));
        }

        private String loginAndGetToken() throws Exception {
                String json = JsonUtil.toJson(Map.of("username", "admin", "password", "admin123"));
                HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(BASE_URL + "/api/auth/login"))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(json))
                                .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                assertEquals(200, response.statusCode());
                Map<?, ?> map = JsonUtil.fromJson(response.body(), Map.class);
                return (String) map.get("token");
        }

        @Test
        void testTransactionLifecycle() throws Exception {
                String token = loginAndGetToken();

                // 1. Create Transaction
                String createJson = "{\"amount\": 100.50, \"description\": \"Test Transaction\", \"date\": \"2023-10-27\", \"categoryId\": 1, \"type\": \"EXPENSE\"}";

                HttpRequest createRequest = HttpRequest.newBuilder()
                                .uri(URI.create(BASE_URL + "/api/transactions"))
                                .header("Content-Type", "application/json")
                                .header("Authorization", "Bearer " + token)
                                .POST(HttpRequest.BodyPublishers.ofString(createJson))
                                .build();

                HttpResponse<String> createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString());
                assertEquals(201, createResponse.statusCode());
                assertTrue(createResponse.body().contains("\"id\":"));

                // Extract ID from response
                Map<?, ?> createdTx = JsonUtil.fromJson(createResponse.body(), Map.class);
                int id = ((Number) createdTx.get("id")).intValue();

                // 2. Get All Transactions
                HttpRequest listRequest = HttpRequest.newBuilder()
                                .uri(URI.create(BASE_URL + "/api/transactions"))
                                .header("Authorization", "Bearer " + token)
                                .GET()
                                .build();

                HttpResponse<String> listResponse = client.send(listRequest, HttpResponse.BodyHandlers.ofString());
                assertEquals(200, listResponse.statusCode());
                assertTrue(listResponse.body().contains("Test Transaction"));

                // 3. Update Transaction
                String updateJson = "{\"id\": " + id
                                + ", \"amount\": 150.00, \"description\": \"Updated Transaction\", \"date\": \"2023-10-28\", \"categoryId\": 1, \"type\": \"EXPENSE\"}";
                HttpRequest updateRequest = HttpRequest.newBuilder()
                                .uri(URI.create(BASE_URL + "/api/transactions"))
                                .header("Content-Type", "application/json")
                                .header("Authorization", "Bearer " + token)
                                .PUT(HttpRequest.BodyPublishers.ofString(updateJson))
                                .build();

                HttpResponse<String> updateResponse = client.send(updateRequest, HttpResponse.BodyHandlers.ofString());
                assertEquals(200, updateResponse.statusCode());
                assertTrue(updateResponse.body().contains("Updated Transaction"));
                assertTrue(updateResponse.body().contains("150.0"));

                // 4. Delete Transaction
                HttpRequest deleteRequest = HttpRequest.newBuilder()
                                .uri(URI.create(BASE_URL + "/api/transactions/" + id))
                                .header("Authorization", "Bearer " + token)
                                .DELETE()
                                .build();

                HttpResponse<String> deleteResponse = client.send(deleteRequest, HttpResponse.BodyHandlers.ofString());
                assertEquals(204, deleteResponse.statusCode());

                // 5. Verify Deletion
                HttpRequest verifyRequest = HttpRequest.newBuilder()
                                .uri(URI.create(BASE_URL + "/api/transactions"))
                                .header("Authorization", "Bearer " + token)
                                .GET()
                                .build();

                HttpResponse<String> verifyResponse = client.send(verifyRequest, HttpResponse.BodyHandlers.ofString());
                assertEquals(200, verifyResponse.statusCode());
        }
}
