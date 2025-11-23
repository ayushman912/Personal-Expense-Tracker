package com.expensetracker;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.expensetracker.util.DatabaseManager;

/**
 * Main JavaFX application entry point.
 * Initializes the UI and manages application lifecycle.
 */
public class MainApp extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        Scene scene = new Scene(loader.load(), 400, 300);
        
        primaryStage.setTitle("Personal Expense Tracker");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }
    
    @Override
    public void stop() throws Exception {
        // Close database connections on application shutdown
        DatabaseManager.getInstance().close();
        super.stop();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}

