package application;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import databasePart1.*;
import javafx.application.Platform;
import javafx.geometry.Pos;

/**
 * The SetupLoginSelectionPage class allows users to choose between setting up a new account
 * or logging into an existing account. It provides two buttons for navigation to the respective pages.
 */
public class SetupLoginSelectionPage {
	
    private final DatabaseHelper databaseHelper;

    public SetupLoginSelectionPage(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    public void show(Stage primaryStage) {
    	
    	// Welcome Message at the Top**
        Label welcomeLabel = new Label("Welcome to the User System");
        welcomeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        Label instructionLabel = new Label("Please select an option to continue:");
        instructionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #34495e;");
        
        // Create Buttons
        Button setupButton = new Button("Create an Account");
        Button loginButton = new Button("Login to Existing Account");
        Button quitButton = new Button("Exit");
        
        // Button Styling
        setupButton.setStyle("-fx-font-size: 14px; -fx-padding: 8px 20px;");
        loginButton.setStyle("-fx-font-size: 14px; -fx-padding: 8px 20px;");
        quitButton.setStyle("-fx-font-size: 14px; -fx-padding: 8px 20px; -fx-background-color: #d9534f; -fx-text-fill: white;");
        
        // Quit button now exits the application
        quitButton.setOnAction(a -> Platform.exit());
        
        setupButton.setOnAction(a -> {
            new SetupAccountPage(databaseHelper).show(primaryStage);
        });
        loginButton.setOnAction(a -> {
        	new UserLoginPage(databaseHelper).show(primaryStage);
        });

        // Layout Styling
        VBox layout = new VBox(15); // spacing for better visual clarity
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-padding: 40px;");
        
        layout.getChildren().addAll(welcomeLabel, instructionLabel, setupButton, loginButton, quitButton);

        primaryStage.setScene(new Scene(layout, 800, 400));
        primaryStage.setTitle("Account Setup");
        primaryStage.show();
    }
}
