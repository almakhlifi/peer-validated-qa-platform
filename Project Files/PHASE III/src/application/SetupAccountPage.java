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
 * SetupAccountPage class handles the account setup process for new users.
 * Users provide their userName, password, and a valid invitation code to register.
 */
public class SetupAccountPage {
	
    private final DatabaseHelper databaseHelper;
    // DatabaseHelper to handle database operations.
    public SetupAccountPage(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    /**
     * Displays the Setup Account page in the provided stage.
     * @param primaryStage The primary stage where the scene will be displayed.
     */
    public void show(Stage primaryStage) {
    	
    	// Page Title
        Label titleLabel = new Label("Create Your Account");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 10;");
        
        Label instructionLabel = new Label("Fill in the details below to set up your account:");
        instructionLabel.setStyle("-fx-font-size: 14px; -fx-padding: 5;");
    	
    	// Input fields for userName, password, and invitation code
        TextField userNameField = new TextField();
        userNameField.setPromptText("Enter userName");
        userNameField.setMaxWidth(250);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter Password");
        passwordField.setMaxWidth(250);
        
        TextField inviteCodeField = new TextField();
        inviteCodeField.setPromptText("Enter InvitationCode");
        inviteCodeField.setMaxWidth(250);
        
        // Label to display error messages for invalid input or registration issues
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");

        // Setup button
        Button setupButton = new Button("Create Account");
        setupButton.setStyle("-fx-font-size: 14px; -fx-padding: 10 20;");
        
        setupButton.setOnAction(a -> {
        	// Retrieve user input
            String userName = userNameField.getText();
            String password = passwordField.getText();
            String code = inviteCodeField.getText();
            Boolean UsedString = false;
            
            // In order to display multiple errors, build a string to append error messages to for display
            StringBuilder errors = new StringBuilder();
            
            // Validate username using UserNameRecognizer
            String userNameValidationResult = UserNameRecognizer.checkForValidUserName(userName);
            if (!userNameValidationResult.isEmpty()) {
                errors.append(userNameValidationResult).append("\n");
                UsedString = true;
            }
            
            try {
            	// Error message if user already exists
            	if(databaseHelper.doesUserExist(userName)) {
            		errors.append("This username is taken! Please use another to set up an account.\n");
            		UsedString = true;
            	}
            		
        		// Validate the invitation code
        		if(!databaseHelper.validateInvitationCode(code)) {
        			errors.append("Please enter a valid invitation code.\n");
        			UsedString = true;
        		}
        		
        		// Retrieve the role(s) assigned to this invitation
                String assignedRoles = databaseHelper.getInvitationRoles(code);
                if (assignedRoles == null || assignedRoles.isEmpty()) {
                    errors.append("Invalid invitation code or no role assigned.\n");
                    UsedString = true;
                }
            			
            	// Implement PasswordEvaluator to validate the password
                String passwordValidationResult = PasswordEvaluator.evaluatePassword(password);
                if (!passwordValidationResult.isEmpty()) {
                	errors.append(passwordValidationResult).append("\n");
                	UsedString = true;
                }
                            
                // Check for errors
                if (errors.length() > 0) {
                	// Display all errors
                	errorLabel.setText(errors.toString().trim());
                } else {
                	// Convert role string to a list and register user
                    List<String> assignedRolesList = List.of(assignedRoles.split(","));
                	// If no errors, proceed to register user
                	User user = new User(userName, password, assignedRolesList);
                    databaseHelper.register(user);
                    
                    // Navigate to Welcome Login Page
                    new WelcomeLoginPage(databaseHelper).show(primaryStage, user);
                } 
                
            }   catch (SQLException e) {
                    System.err.println("Database error: " + e.getMessage());
                    e.printStackTrace();
                }
           });
        
        // Back Button - Returns to SetupLoginSelectionPage
        Button backButton = new Button("Back");
        backButton.setStyle("-fx-font-size: 12px; -fx-padding: 8 15;");
        backButton.setOnAction(e -> new SetupLoginSelectionPage(databaseHelper).show(primaryStage));

        // Layout
        VBox layout = new VBox(12);
        layout.setStyle("-fx-padding: 20; -fx-alignment: center;");
        layout.getChildren().addAll(
                titleLabel, instructionLabel, userNameField, passwordField, inviteCodeField,
                setupButton, backButton, errorLabel
        );

        primaryStage.setScene(new Scene(layout, 800, 400));
        primaryStage.setTitle("Account Setup");
        primaryStage.show();
    }
}
