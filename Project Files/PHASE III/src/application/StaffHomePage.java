/**
 * StaffHomePage
 * 
 * Class is responsible for rendering the home page for staff members in the application.
 */
package application;

import databasePart1.DatabaseHelper;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class StaffHomePage {

    /**
     * Displays the staff home page with options to navigate back to the login page.
     *
     * @param primaryStage The primary stage for the staff home page.
     * @param databaseHelper Database helper for managing data operations.
     * @param user The user object representing the currently logged-in staff member.
     */
    public void show(Stage primaryStage, DatabaseHelper databaseHelper, User user) {
        VBox layout = new VBox(10);
        layout.setStyle("-fx-alignment: center; -fx-padding: 20; -fx-background-color: #f4f4f4;");

        // Title label
        Label titleLabel = new Label("Staff Home");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Back button to return to WelcomeLoginPage
        Button backButton = new Button("Back");
        backButton.setOnAction(e -> new WelcomeLoginPage(databaseHelper).show(primaryStage, user));

        layout.getChildren().addAll(titleLabel, backButton);
        primaryStage.setScene(new Scene(layout, 800, 400));
        primaryStage.setTitle("Staff Home");
    }
}