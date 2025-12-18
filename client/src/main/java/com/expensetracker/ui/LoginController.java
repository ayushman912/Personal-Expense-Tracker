package com.expensetracker.ui;

import com.expensetracker.dao.UserDAO;
import com.expensetracker.dao.impl.UserDAOImpl;
import com.expensetracker.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

/**
 * Controller for the login screen.
 * Handles user authentication.
 */
public class LoginController {
    
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
    
    @FXML
    private TextField usernameField;
    
    @FXML
    private PasswordField passwordField;
    
    private UserDAO userDAO = new UserDAOImpl();
    
    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please enter username and password");
            return;
        }
        
        try {
            User user = userDAO.authenticate(username, password);
            if (user != null) {
                openMainWindow(user);
            } else {
                showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid username or password");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Database connection failed: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please enter username and password");
            return;
        }
        
        try {
            User existingUser = userDAO.findByUsername(username);
            if (existingUser != null) {
                showAlert(Alert.AlertType.ERROR, "Registration Failed", "Username already exists");
                return;
            }
            
            User newUser = new User(username, password, username + "@example.com");
            userDAO.insert(newUser);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Registration successful! Please login.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Registration failed: " + e.getMessage());
        }
    }
    
    private void openMainWindow(User user) {
        try {
            Stage stage = (Stage) usernameField.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Main.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);
            
            // Pass the logged-in user to MainController
            MainController mainController = loader.getController();
            mainController.setCurrentUser(user);
            
            stage.setScene(scene);
            stage.setTitle("Personal Expense Tracker - Main");
            stage.setResizable(true);
        } catch (IOException e) {
            logger.error("Failed to open main window", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open main window: " + e.getMessage());
        }
    }
    
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

