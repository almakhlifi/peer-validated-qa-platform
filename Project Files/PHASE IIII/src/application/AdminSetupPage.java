package application;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;

import databasePart1.*; 
import java.util.List;

/**
 * Handles the initial setup process for creating the first administrator account.
 * This page is typically shown only when no admin user exists in the system.
 */
public class AdminSetupPage {

    private final DatabaseHelper databaseHelper;

    /**
     * Constructs the AdminSetupPage.
     *
     * @param databaseHelper The database helper instance for database operations.
     */
    public AdminSetupPage(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    /**
     * Displays the administrator setup window.
     * Allows the first user to create an admin account by providing a username and password.
     *
     * @param primaryStage The primary stage for this application.
     */
    public void show(Stage primaryStage) {
    	// Instruction message
        Label instructionLabel = new Label("Please create your administrator account to access the system.");
        instructionLabel.setStyle(
            "-fx-font-size: 16px;" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 0 0 10 0;" +
            "-fx-text-alignment: center;"
        );
    	
    	// Input fields for userName and password
        TextField userNameField = new TextField();
        userNameField.setPromptText("Enter Admin userName");
        userNameField.setMaxWidth(250);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter Password");
        passwordField.setMaxWidth(250);

        Button setupButton = new Button("Setup");
        setupButton.setStyle(
                "-fx-font-size: 14px;" +
                "-fx-text-fill: white;" +
                "-fx-background-color: #3c3f41;" +
                "-fx-border-color: #5a5a5a;" +
                "-fx-border-radius: 5px;" +
                "-fx-background-radius: 5px;"
        );
        
        // Add error label for errors to appear
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");

        setupButton.setOnAction(a -> {
            // Retrieve user input
            String userName = userNameField.getText();
            String password = passwordField.getText();

            // Create an error builder
            StringBuilder errors = new StringBuilder();

            // Validate UserName using UserNameRecognizer
            String userNameValidationResult = UserNameRecognizer.checkForValidUserName(userName);
            if (!userNameValidationResult.isEmpty()) {
                errors.append(userNameValidationResult).append("\n");
            }

            // Validate password using PasswordEvaluator
            String passwordValidationResult = PasswordEvaluator.evaluatePassword(password);
            if (!passwordValidationResult.isEmpty()) {
                errors.append(passwordValidationResult).append("\n");
            }

            try {
                if (errors.length() > 0) {
                    // Display all errors
                    errorLabel.setText(errors.toString().trim());
                } else {
                    // Create a new User object with admin role and register in the database
                    User user = new User(userName, password, List.of("admin"));
                    databaseHelper.register(user);
                    System.out.println("Administrator setup completed.");

                    // Navigate to the Login Page instead of logging in directly
                    new UserLoginPage(databaseHelper).show(primaryStage);
                }
            } catch (SQLException e) {
                // Display database error to the user
                errorLabel.setText("Database error: Could not create admin user.");
                System.err.println("Database error: " + e.getMessage());
                e.printStackTrace();
            }
        });

        VBox layout = new VBox(12, instructionLabel, userNameField, passwordField, setupButton, errorLabel);
        layout.setStyle(
                "-fx-background-color: #1e1e1e;" +
                "-fx-alignment: center;" +
                "-fx-padding: 30;"
        );

        primaryStage.setScene(new Scene(layout, 800, 400));
        primaryStage.setTitle("Administrator Setup");
        primaryStage.show();
    }
}
