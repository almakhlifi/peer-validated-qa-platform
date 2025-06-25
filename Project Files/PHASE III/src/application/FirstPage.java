package application;

import databasePart1.*; 
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Represents the initial screen shown when the application detects no existing admin user.
 * It prompts the first user to proceed to the administrator account setup.
 */
public class FirstPage {

    /**
     * Helper object for interacting with the application database.
     */
    private final DatabaseHelper databaseHelper;

    /**
     * Constructs the FirstPage.
     *
     * @param databaseHelper The database helper instance for database operations.
     */
    public FirstPage(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    /**
     * Sets up and displays the initial welcome page in the provided primary stage.
     * This page includes a message and a button to navigate to the admin setup screen.
     *
     * @param primaryStage The primary stage where the scene will be displayed.
     */
    public void show(Stage primaryStage) {
        VBox layout = new VBox(15); // Increased spacing for better visual separation

        // Label to display the welcome message for the first user
        layout.setStyle("-fx-alignment: center; -fx-padding: 20;");
        Label userLabel = new Label("Hello! You appear to be the first user.\nPlease select 'Continue' to set up administrator access.");
        userLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-alignment: center;"); // Centered text

        Button continueButton = new Button("Continue");
        continueButton.setStyle("-fx-font-size: 14px;"); // Slightly larger button text

        // Action for the continue button: navigate to the Admin Setup Page
        continueButton.setOnAction(a -> {
            new AdminSetupPage(databaseHelper).show(primaryStage);
        });

        layout.getChildren().addAll(userLabel, continueButton);
        Scene firstPageScene = new Scene(layout, 800, 400);

        // Set the scene to the primary stage
        primaryStage.setScene(firstPageScene);
        primaryStage.setTitle("Welcome - Initial Setup"); 
        primaryStage.show();
    }
}