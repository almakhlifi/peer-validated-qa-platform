package application;

import application.WelcomeLoginPage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import databasePart1.*;
import java.util.List;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;

/**
 * Represents the home page for instructors.
 */
public class InstructorHomePage {

    /**
     * Displays the instructor home page.
     * @param primaryStage The primary stage for the application.
     * @param databaseHelper The database helper instance for database interactions.
     * @param user The current instructor user.
     */
    public void show(Stage primaryStage, DatabaseHelper databaseHelper, User user) {
        VBox layout = new VBox(10);
        layout.setStyle("-fx-alignment: center; -fx-padding: 20; -fx-background-color: #1e1e1e;");

        Label titleLabel = new Label("Instructor Home");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Pending requests
        List<String> pendingRequests;
        try {
            pendingRequests = databaseHelper.getPendingReviewerRequests();
            if (pendingRequests.isEmpty()) {
                Label noRequestsLabel = new Label("No pending reviewer requests.");
                noRequestsLabel.setStyle("-fx-text-fill: white;");
                layout.getChildren().add(noRequestsLabel);
            } else {
                Label requestsLabel = new Label("Pending Reviewer Requests:");
                requestsLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10px; -fx-text-fill: white;");

                for (String requester : pendingRequests) {
                    HBox requestBox = new HBox(10);
                    requestBox.setStyle("-fx-alignment: center; -fx-padding: 10;");
                    requestBox.setAlignment(javafx.geometry.Pos.CENTER);
                    Label nameLabel = new Label(requester);
                    nameLabel.setStyle("-fx-text-fill: white;");

                    // deny and approve buttons
                    Button approveBtn = new Button("Approve");
                    approveBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                    Button denyBtn = new Button("Deny");
                    denyBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");

                    approveBtn.setOnAction(ev -> {
                        try {
                            databaseHelper.approveReviewerRequest(requester);
                            showAlert("Success", requester + " has been granted the reviewer role.");
                            show(primaryStage, databaseHelper, user); // refresh
                        } catch (Exception ex) {
                            showAlert("Error", "Could not approve request.");
                            ex.printStackTrace();
                        }
                    });

                    denyBtn.setOnAction(ev -> {
                        try {
                            databaseHelper.denyReviewerRequest(requester);
                            showAlert("Request Denied", requester + "'s request was denied.");
                            show(primaryStage, databaseHelper, user); // refresh
                        } catch (Exception ex) {
                            showAlert("Error", "Could not deny request.");
                            ex.printStackTrace();
                        }
                    });

                    requestBox.getChildren().addAll(nameLabel, approveBtn, denyBtn);
                    layout.getChildren().add(requestBox);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Back button to return to WelcomeLoginPage
        Button backButton = new Button("Back");
        backButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        backButton.setOnAction(e -> new WelcomeLoginPage(databaseHelper).show(primaryStage, user));

        layout.getChildren().add(0, titleLabel); // title stays at the top
        layout.getChildren().add(backButton);    // back button at the bottom
        Scene scene = new Scene(layout, 800, 400);
        scene.getStylesheets().add(StudentHomePage.class.getResource("dark-theme.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle("Instructor Home");
    }

    /**
     * Displays a simple alert dialog with the given title and message.
     * @param title The title of the alert.
     * @param message The content message of the alert.
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}