/**
 * StudentHomePage
 * 
 * Class is responsible for rendering the home page for students in the application.
 */
package application;

import databasePart1.DatabaseHelper;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class StudentHomePage {

    /**
     * Stores the user information for navigation purposes.
     */
    private User user; // Store user info for navigation

    /**
     * Displays the student home page with options to access the question system, inbox, and log out.
     *
     * @param primaryStage The primary stage for the student home page.
     * @param databaseHelper Database helper for managing data operations.
     * @param user The user object representing the currently logged-in student.
     */
    public void show(Stage primaryStage, DatabaseHelper databaseHelper, User user) {
        this.user = user; // Store the correct user object

        VBox layout = new VBox(15);
        layout.setStyle("-fx-alignment: center; -fx-padding: 30; -fx-background-color: #1e1e1e;");

        // Title label
        Label titleLabel = new Label("Student Home");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Question System Button
        Button questionSystemButton = new Button("Question System");
        questionSystemButton.setStyle("-fx-font-size: 14px; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 8px 12px;");
        questionSystemButton.setOnAction(e -> {
            primaryStage.close(); // Close Student Home Page

            // Open the Question System
            QuestionSystemGUI gui = new QuestionSystemGUI(user.getUserName(), "student", databaseHelper, primaryStage);
            Stage stage = new Stage();
            gui.start(stage);
        });

        // Inbox Button to access private messages
        Button inboxButton = new Button("Inbox");
        inboxButton.setStyle("-fx-font-size: 14px; -fx-background-color: #6c63ff; -fx-text-fill: white; -fx-padding: 8px 12px;");
        inboxButton.setOnAction(e -> {
            primaryStage.hide(); // Hide this window temporarily
            InboxGUI.show(user.getUserName(), databaseHelper, primaryStage);
        });

        // Back Button to return to WelcomeLoginPage
        Button backButton = new Button("Back");
        backButton.setStyle("-fx-font-size: 14px; -fx-background-color: #2196F3; -fx-text-fill: white; -fx-padding: 8px 12px;");
        backButton.setOnAction(e -> new WelcomeLoginPage(databaseHelper).show(primaryStage, user));

        layout.getChildren().addAll(titleLabel, questionSystemButton, inboxButton, backButton);
        primaryStage.setScene(new Scene(layout, 800, 400));
        primaryStage.setTitle("Student Home");
    }
}