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
        
        // Create a list of checkboxes for role selection
        CheckBox studentCheckBox = new CheckBox("Student");
        CheckBox instructorCheckBox = new CheckBox("Instructor");
        CheckBox staffCheckBox = new CheckBox("Staff");
        CheckBox reviewerCheckBox = new CheckBox("Reviewer");

        VBox roleSelectionBox = new VBox(5, studentCheckBox, instructorCheckBox, staffCheckBox, reviewerCheckBox);
        roleSelectionBox.setStyle("-fx-padding: 10;");
        
        // Label to display error messages for invalid input or registration issues
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        

        Button setupButton = new Button("Setup");
        
        setupButton.setOnAction(a -> {
        	// Retrieve user input
            String userName = userNameField.getText();
            String password = passwordField.getText();
            String code = inviteCodeField.getText();
            Boolean UsedString = false;
            
            // Collect selected roles (Multiple Role Support)
            List<String> selectedRoles = new ArrayList<>();
            if (studentCheckBox.isSelected()) selectedRoles.add("student");
            if (instructorCheckBox.isSelected()) selectedRoles.add("instructor");
            if (staffCheckBox.isSelected()) selectedRoles.add("staff");
            if (reviewerCheckBox.isSelected()) selectedRoles.add("reviewer");
            
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
        		
        		// Ensure at least one role is selected
                if (selectedRoles.isEmpty()) {
                    errors.append("Please select at least one role.\n");
                    UsedString = true;
                }
            			
            	// Implement PasswordEvaluator to validate the password
                String passwordValidationResult = PasswordEvaluator.evaluatePassword(password);
                
                // Check for an empty password
                if (!passwordValidationResult.isEmpty()) {
                	errors.append(passwordValidationResult).append("\n");
                	UsedString = true;
                }
                
             // Validate the invitation code, used string checks if everything else is valid
                if(!UsedString) {
                	if(!databaseHelper.validateInvitationCode(code)) {
                		errors.append("Please enter a valid invitation code.\n");
        		
                	}
                }
                            
                // Check for errors
                if (errors.length() > 0) {
                	// Display all errors
                	errorLabel.setText(errors.toString().trim());
                } else {
                	// If no errors, proceed to register user
                	User user = new User(userName, password, selectedRoles);
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
        backButton.setOnAction(e -> new SetupLoginSelectionPage(databaseHelper).show(primaryStage));

        VBox layout = new VBox(10);
        layout.setStyle("-fx-padding: 20; -fx-alignment: center;");
        layout.getChildren().addAll(
                userNameField, passwordField, inviteCodeField, 
                new Label("Select Your Roles:"), roleSelectionBox,
                setupButton, backButton, errorLabel
        );

        primaryStage.setScene(new Scene(layout, 800, 400));
        primaryStage.setTitle("Account Setup");
        primaryStage.show();
    }
}
