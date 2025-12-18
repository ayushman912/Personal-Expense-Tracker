package com.expensetracker.client;

import com.expensetracker.client.api.RemoteRepository;
import com.expensetracker.model.Transaction;
import com.expensetracker.model.Expense;
import com.expensetracker.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class ClientIntegrationTest {

    private static Server server;
    private static final int PORT = 8082;
    private static final String BASE_URL = "http://localhost:" + PORT + "/api";
    private static RemoteRepository repository;
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeAll
    static void startServer() throws Exception {
        server = new Server(PORT);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Mock Auth Servlet
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                String body = req.getReader().lines().collect(Collectors.joining());
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, String> credentials = mapper.readValue(body, Map.class);
                    if ("test".equals(credentials.get("username")) && "password".equals(credentials.get("password"))) {
                        resp.setStatus(200);
                        User user = new User(1, "test", "password", "test@example.com");
                        String responseJson = mapper.writeValueAsString(Map.of("token", "mock-token", "user", user));
                        resp.getWriter().write(responseJson);
                    } else {
                        resp.setStatus(401);
                    }
                } catch (Exception e) {
                    resp.setStatus(500);
                }
            }
        }), "/api/auth/login");

        // Mock Transactions Servlet
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                Transaction t1 = new Expense(new BigDecimal("100.00"), "Test 1", LocalDate.now(), 1);
                t1.setId(1);
                Transaction t2 = new Expense(new BigDecimal("200.00"), "Test 2", LocalDate.now(), 2);
                t2.setId(2);
                resp.getWriter().write(mapper.writeValueAsString(List.of(t1, t2)));
            }

            @Override
            protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                String body = req.getReader().lines().collect(Collectors.joining());
                Transaction t = mapper.readValue(body, Transaction.class);
                t.setId(123); // Simulate DB ID generation
                resp.setStatus(201);
                resp.getWriter().write(mapper.writeValueAsString(t));
            }

            @Override
            protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                String body = req.getReader().lines().collect(Collectors.joining());
                Transaction t = mapper.readValue(body, Transaction.class);
                resp.setStatus(200);
                resp.getWriter().write(mapper.writeValueAsString(t));
            }

            @Override
            protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.setStatus(204);
            }
        }), "/api/transactions/*");

        server.start();
        repository = new RemoteRepository(BASE_URL);
    }

    @AfterAll
    static void stopServer() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void testLogin() throws Exception {
        User user = repository.login("test", "password").get();
        assertNotNull(user);
        assertEquals("test", user.getUsername());
    }

    @Test
    void testGetTransactions() throws Exception {
        List<Transaction> transactions = repository.getTransactions().get();
        assertEquals(2, transactions.size());
        assertEquals("Test 1", transactions.get(0).getDescription());
    }

    @Test
    void testAddTransaction() throws Exception {
        Transaction t = new Expense(new BigDecimal("50.00"), "New Transaction", LocalDate.now(), 1);
        Transaction created = repository.addTransaction(t).get();
        assertEquals(123, created.getId());
        assertEquals("New Transaction", created.getDescription());
    }

    @Test
    void testUpdateTransaction() throws Exception {
        Transaction t = new Expense(new BigDecimal("75.00"), "Updated Transaction", LocalDate.now(), 1);
        t.setId(123);
        Transaction updated = repository.updateTransaction(t).get();
        assertEquals("Updated Transaction", updated.getDescription());
        assertEquals(new BigDecimal("75.00"), updated.getAmount());
    }

    @Test
    void testDeleteTransaction() throws Exception {
        assertDoesNotThrow(() -> repository.deleteTransaction(123).get());
    }
}
