package application;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.SQLException;
import databasePart1.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The UserLoginPage class provides a login interface for users to access their accounts.
 * It validates the user's credentials and navigates to the appropriate page upon successful login.
 */
public class UserLoginPage {
    
    private final DatabaseHelper databaseHelper;

    public UserLoginPage(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    public void show(Stage primaryStage) {
    	
    	// Title Label
        Label titleLabel = new Label("Welcome to User Login");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Instruction Label
        Label instructionLabel = new Label("Enter your credentials to log in:");
        instructionLabel.setStyle("-fx-font-size: 14px;");
    	
        // Input field for the user's userName, password
        TextField userNameField = new TextField();
        userNameField.setPromptText("Enter userName");
        userNameField.setMaxWidth(250);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter Password");
        passwordField.setMaxWidth(250);
        
        CheckBox oneTimePasswordCheckBox = new CheckBox("Use One-Time Password");

        // Label to display error messages
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");

        Button loginButton = new Button("Login");
        loginButton.setStyle("-fx-background-color: #0073e6; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 5 15;");
        
        loginButton.setOnAction(a -> {
            String userName = userNameField.getText();
            String password = passwordField.getText();
            boolean useOneTimePassword = oneTimePasswordCheckBox.isSelected();
            
            try {
                if (databaseHelper.isConnectionClosed()) {
                    databaseHelper.connectToDatabase();
                }

                User user = new User(userName, password, new ArrayList<>());
                WelcomeLoginPage welcomeLoginPage = new WelcomeLoginPage(databaseHelper);
                
                List<String> roles = databaseHelper.getUserRoles(userName);
                
                if (roles != null) {
                    user.setRoles(roles);
                    if (useOneTimePassword) {
                        if (databaseHelper.validateOneTimePassword(userName, password)) {
                            databaseHelper.clearOneTimePassword(userName);
                            new SetNewPasswordPage(databaseHelper, userName).show(primaryStage);
                        } else {
                            errorLabel.setText("Invalid one-time password.");
                        }
                    } else {
                        if (databaseHelper.login(user)) {
                            welcomeLoginPage.show(primaryStage, user);
                        } else {
                            errorLabel.setText("Error logging in");
                        }
                    }
                } else {
                    errorLabel.setText("User account doesn't exist");
                }
            } catch (SQLException e) {
                System.err.println("Database error: " + e.getMessage());
                e.printStackTrace();
            } 
        });
        
        // Back Button - Returns to SetupLoginSelectionPage
        Button backButton = new Button("Back");
        backButton.setStyle("-fx-background-color: #c7c7c7; -fx-font-size: 14px; -fx-padding: 5 15;");
        backButton.setOnAction(e -> new SetupLoginSelectionPage(databaseHelper).show(primaryStage));

        VBox layout = new VBox(10);
        layout.setStyle("-fx-padding: 30; -fx-alignment: center;");
        layout.getChildren().addAll(titleLabel, instructionLabel, userNameField, passwordField, oneTimePasswordCheckBox, loginButton, backButton, errorLabel);

        primaryStage.setScene(new Scene(layout, 800, 400));
        primaryStage.setTitle("User Login");
        primaryStage.show();
    }
}
