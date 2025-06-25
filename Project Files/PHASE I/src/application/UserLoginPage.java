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
        backButton.setOnAction(e -> new SetupLoginSelectionPage(databaseHelper).show(primaryStage));

        VBox layout = new VBox(10);
        layout.setStyle("-fx-padding: 20; -fx-alignment: center;");
        layout.getChildren().addAll(userNameField, passwordField, oneTimePasswordCheckBox, loginButton, backButton, errorLabel);

        primaryStage.setScene(new Scene(layout, 800, 400));
        primaryStage.setTitle("User Login");
        primaryStage.show();
    }
}
