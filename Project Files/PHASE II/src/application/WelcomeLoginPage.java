package application;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.application.Platform;
import databasePart1.*;
import java.sql.SQLException;
import java.util.List;

/**
 * The WelcomeLoginPage class displays a welcome screen for authenticated users.
 * It allows users to navigate to their respective pages based on their role or quit the application.
 */
public class WelcomeLoginPage {
	
	private final DatabaseHelper databaseHelper;

    public WelcomeLoginPage(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }
    public void show( Stage primaryStage, User user) {
    	
    	VBox layout = new VBox(15);
        layout.setStyle("-fx-alignment: center; -fx-padding: 30; -fx-background-color: white;");
	    
        Label welcomeLabel = new Label("Welcome, " + user.getUserName() + "!");
        welcomeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        Label roleSelectionLabel = new Label("Select your role to continue:");
        roleSelectionLabel.setStyle("-fx-font-size: 14px;");
	    
	    // Retrieve all roles assigned to this user
        List<String> userRoles;
        try {
            userRoles = databaseHelper.getUserRoles(user.getUserName());
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }
        
        // If user is an admin, redirect them immediately
        if (userRoles.contains("admin")) {
            new AdminHomePage().show(primaryStage, databaseHelper, user);
            return;
        }
	   
        Button continueButton = new Button("Continue");
        continueButton.setStyle("-fx-font-size: 14px; -fx-padding: 8px 16px;");
	    
	    // If user has only one role, automatically redirect them
        if (userRoles.size() == 1) {
        	continueButton.setOnAction(a -> navigateToUserHome(primaryStage, userRoles.get(0), user));
        } else {
            // Otherwise, let them select a role
            ComboBox<String> roleSelection = new ComboBox<>();
            roleSelection.getItems().addAll(userRoles);
            roleSelection.setPromptText("Select Role");
            roleSelection.setMaxWidth(250);

            continueButton.setOnAction(a -> {
                String selectedRole = roleSelection.getValue();
                if (selectedRole == null) {
                    showAlert("Selection Required", "Please select a role before proceeding.");
                    return;
                }
                navigateToUserHome(primaryStage, selectedRole, user);
            });

            layout.getChildren().addAll(welcomeLabel, roleSelectionLabel, roleSelection);
        }
        
        // Logout Button
        Button logoutButton = new Button("Logout");
        logoutButton.setStyle("-fx-font-size: 14px; -fx-background-color: #d9534f; -fx-text-fill: white;");
        logoutButton.setOnAction(a -> {
            databaseHelper.closeConnection();
            new UserLoginPage(databaseHelper).show(primaryStage);
        });

	    layout.getChildren().addAll(continueButton, logoutButton);
	    Scene welcomeScene = new Scene(layout, 800, 400);

	    // Set the scene to primary stage
	    primaryStage.setScene(welcomeScene);
	    primaryStage.setTitle("Welcome Page");
    }
    
    // Redirects users to the correct home page based on their role
    private void navigateToUserHome(Stage primaryStage, String role, User user) {
        switch (role) {
            case "admin":
        	new AdminHomePage().show(primaryStage, databaseHelper, user);
        	break;
            case "student":
                new StudentHomePage().show(primaryStage, databaseHelper, user);
                break;
            case "instructor":
                new InstructorHomePage().show(primaryStage, databaseHelper, user);
                break;
            case "staff":
                new StaffHomePage().show(primaryStage, databaseHelper, user);
                break;
            case "reviewer":
                new ReviewerHomePage().show(primaryStage, databaseHelper, user);
                break;
            default:
                System.out.println("Invalid role selected.");
        }
    }
    
    // Display an alert message
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
