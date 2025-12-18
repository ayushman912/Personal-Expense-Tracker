package com.expensetracker.server;

import com.expensetracker.server.util.TokenStore;
import com.expensetracker.util.DatabaseManager;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SecurityIntegrationTest {

    private static Server server;
    private static HttpClient client;
    private static final int PORT = 8089;
    private static final String BASE_URL = "http://localhost:" + PORT + "/api";

    @BeforeAll
    static void setUp() throws Exception {
        // Setup in-memory DB for test
        DatabaseManager.setDbUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL");

        server = ServerApp.startServer(PORT);
        client = HttpClient.newHttpClient();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void testUnauthorizedAccess() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/transactions"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, response.statusCode(), "Should return 401 for missing token");
    }

    @Test
    void testAuthorizedAccess() throws Exception {
        // Manually create a token
        String token = UUID.randomUUID().toString();
        TokenStore.addToken(token, "testuser");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/categories"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), "Should return 200 for valid token");
    }
}
