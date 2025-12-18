package com.expensetracker.client.service;

import com.expensetracker.client.api.RemoteRepository;
import javafx.application.Platform;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Manages network connectivity state.
 * Periodically checks if the server is reachable and notifies listeners of
 * state changes.
 */
public class ConnectivityManager {

    public enum ConnectionState {
        ONLINE,
        OFFLINE
    }

    private final RemoteRepository remoteRepository;
    private final ScheduledExecutorService scheduler;
    private final List<Consumer<ConnectionState>> listeners = new ArrayList<>();

    private volatile ConnectionState currentState = ConnectionState.OFFLINE; // Default to offline until verified

    public ConnectivityManager(RemoteRepository remoteRepository) {
        this.remoteRepository = remoteRepository;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Connectivity-Checker");
            t.setDaemon(true);
            return t;
        });

        startMonitoring();
    }

    private void startMonitoring() {
        // Check immediately
        checkConnectivity();

        // Then every 5 seconds
        scheduler.scheduleAtFixedRate(this::checkConnectivity, 5, 5, TimeUnit.SECONDS);
    }

    private void checkConnectivity() {
        remoteRepository.isReachable()
                .thenAccept(isReachable -> {
                    ConnectionState newState = isReachable ? ConnectionState.ONLINE : ConnectionState.OFFLINE;
                    if (newState != currentState) {
                        currentState = newState;
                        notifyListeners(newState);
                    }
                });
    }

    private void notifyListeners(ConnectionState state) {
        // Notify on JavaFX thread if needed, or let listeners handle threading.
        // Usually UI listeners need FX thread. Let's run on FX thread to be safe for
        // UI.
        // But logic listeners might not need it.
        // For simplicity in this app, we'll dispatch to FX thread if Platform is
        // initialized.
        // If not (unit tests), run directly.
        try {
            Platform.runLater(() -> listeners.forEach(listener -> listener.accept(state)));
        } catch (IllegalStateException e) {
            // Toolkit not initialized, run directly
            listeners.forEach(listener -> listener.accept(state));
        }
    }

    public void addListener(Consumer<ConnectionState> listener) {
        listeners.add(listener);
        // Notify current state immediately
        listener.accept(currentState);
    }

    public ConnectionState getCurrentState() {
        return currentState;
    }

    public boolean isOnline() {
        return currentState == ConnectionState.ONLINE;
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
