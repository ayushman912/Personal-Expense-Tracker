package com.expensetracker.client.api;

import com.expensetracker.model.Transaction;
import com.expensetracker.model.Category;
import com.expensetracker.model.User;
import com.expensetracker.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RemoteRepository {

    private final String baseUrl;
    private final HttpClient client;
    private String authToken;

    public RemoteRepository() {
        this("http://localhost:8080/api");
    }

    public RemoteRepository(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void setAuthToken(String token) {
        this.authToken = token;
    }

    private HttpRequest.Builder requestBuilder(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(this.baseUrl + path));
        if (authToken != null) {
            builder.header("Authorization", "Bearer " + authToken);
        }
        return builder;
    }

    public CompletableFuture<User> login(String username, String password) {
        String json = JsonUtil.toJson(Map.of("username", username, "password", password));
        HttpRequest request = requestBuilder("/auth/login")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = JsonUtil.fromJson(response.body(), Map.class);
                        this.authToken = (String) map.get("token");
                        return JsonUtil.getMapper().convertValue(map.get("user"), User.class);
                    } else {
                        throw new RuntimeException("Login failed: " + response.statusCode());
                    }
                });
    }

    /**
     * Registers a new user with the server.
     * 
     * @param username the desired username (3-30 chars, alphanumeric + underscore)
     * @param email the user's email address (optional but validated if provided)
     * @param password the password (min 6 characters)
     * @return CompletableFuture with registration result:
     *         - Completes with userId on success (HTTP 201)
     *         - Throws RuntimeException with "exists" message on duplicate (HTTP 409)
     *         - Throws RuntimeException with validation message on bad input (HTTP 400)
     *         - Throws RuntimeException on other errors
     * 
     * Note: Registration does not auto-login. Call login() separately after registration.
     * Security: Password is not logged or stored locally - sent directly to server.
     */
    public CompletableFuture<Integer> register(String username, String email, String password) {
        // Build request body - email may be null/empty
        Map<String, String> requestBody = new java.util.HashMap<>();
        requestBody.put("username", username);
        requestBody.put("password", password);
        if (email != null && !email.isEmpty()) {
            requestBody.put("email", email);
        }
        
        String json = JsonUtil.toJson(requestBody);
        HttpRequest request = requestBuilder("/auth/register")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> responseMap = JsonUtil.fromJson(response.body(), Map.class);
                    
                    if (response.statusCode() == 201) {
                        // Success - return the new user ID
                        Number userId = (Number) responseMap.get("userId");
                        return userId != null ? userId.intValue() : 0;
                    } else if (response.statusCode() == 409) {
                        // Conflict - username or email already exists
                        String error = (String) responseMap.get("error");
                        throw new RuntimeException("Registration failed: " + (error != null ? error : "User already exists"));
                    } else if (response.statusCode() == 400) {
                        // Bad request - validation error
                        String error = (String) responseMap.get("error");
                        throw new RuntimeException("Validation error: " + (error != null ? error : "Invalid input"));
                    } else {
                        // Other error
                        String error = (String) responseMap.get("error");
                        throw new RuntimeException("Registration failed: " + (error != null ? error : "Unknown error"));
                    }
                });
    }

    public CompletableFuture<List<Transaction>> getTransactions() {
        HttpRequest request = requestBuilder("/transactions")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return JsonUtil.getMapper().convertValue(
                                JsonUtil.fromJson(response.body(), List.class),
                                new TypeReference<List<Transaction>>() {
                                });
                    } else {
                        throw new RuntimeException("Failed to fetch transactions");
                    }
                });
    }

    public CompletableFuture<Transaction> addTransaction(Transaction transaction) {
        String json = JsonUtil.toJson(transaction);
        HttpRequest request = requestBuilder("/transactions")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 201) {
                        return JsonUtil.fromJson(response.body(), Transaction.class);
                    } else {
                        throw new RuntimeException("Failed to add transaction");
                    }
                });
    }

    public CompletableFuture<Transaction> updateTransaction(Transaction transaction) {
        String json = JsonUtil.toJson(transaction);
        HttpRequest request = requestBuilder("/transactions")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return JsonUtil.fromJson(response.body(), Transaction.class);
                    } else {
                        throw new RuntimeException("Failed to update transaction");
                    }
                });
    }

    public CompletableFuture<Void> deleteTransaction(int id) {
        HttpRequest request = requestBuilder("/transactions/" + id)
                .DELETE()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 204) {
                        return null;
                    } else {
                        throw new RuntimeException("Failed to delete transaction");
                    }
                });
    }

    public CompletableFuture<List<Category>> getCategories() {
        HttpRequest request = requestBuilder("/categories")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return JsonUtil.getMapper().convertValue(
                                JsonUtil.fromJson(response.body(), List.class),
                                new TypeReference<List<Category>>() {
                                });
                    } else {
                        throw new RuntimeException("Failed to fetch categories");
                    }
                });
    }

    public CompletableFuture<Boolean> triggerBackup() {
        HttpRequest request = requestBuilder("/backup")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> response.statusCode() == 200);
    }

    public CompletableFuture<Boolean> isReachable() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.baseUrl + "/categories")) // Simple GET
                .timeout(Duration.ofSeconds(2)) // Short timeout
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> response.statusCode() == 200)
                .exceptionally(e -> false);
    }
}
