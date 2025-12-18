package com.expensetracker.server;

import com.expensetracker.server.servlet.AuthServlet;
import com.expensetracker.server.servlet.RegisterServlet;
import com.expensetracker.server.servlet.TransactionServlet;
import com.expensetracker.server.servlet.CategoryServlet;
import com.expensetracker.server.servlet.BackupServlet;
import com.expensetracker.util.DatabaseManager;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * Main class for the Expense Tracker Server.
 * Starts an embedded Jetty server and configures servlets.
 */
public class ServerApp {

    public static void main(String[] args) throws Exception {
        Server server = startServer(8080);
        server.join();
    }

    public static Server startServer(int port) throws Exception {
        // Initialize Database
        System.out.println("Initializing Database...");
        DatabaseManager.getInstance(); // This triggers table creation

        // Create Jetty Server
        Server server = new Server(port);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Register Servlets
        context.addServlet(new ServletHolder(new AuthServlet()), "/api/auth/login");
        context.addServlet(new ServletHolder(new RegisterServlet()), "/api/auth/register");
        context.addServlet(new ServletHolder(new TransactionServlet()), "/api/transactions/*");
        context.addServlet(new ServletHolder(new CategoryServlet()), "/api/categories/*");
        context.addServlet(new ServletHolder(new BackupServlet()), "/api/backup");

        // Register Auth Filter
        context.addFilter(new org.eclipse.jetty.servlet.FilterHolder(new com.expensetracker.server.filter.AuthFilter()),
                "/api/*", java.util.EnumSet.of(jakarta.servlet.DispatcherType.REQUEST));

        // Add a simple health check
        context.addServlet(new ServletHolder(new jakarta.servlet.http.HttpServlet() {
            @Override
            protected void doGet(jakarta.servlet.http.HttpServletRequest req,
                    jakarta.servlet.http.HttpServletResponse resp)
                    throws java.io.IOException {
                resp.setStatus(200);
                resp.getWriter().write("Server is running");
            }
        }), "/health");

        System.out.println("Starting server on port " + port + "...");
        server.start();
        return server;
    }
}
