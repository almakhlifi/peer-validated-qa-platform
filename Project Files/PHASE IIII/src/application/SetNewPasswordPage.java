/**
 * SetNewPasswordPage
 * 
 * Class is responsible for allowing users to set a new password through a dedicated page.
 */
package application;

import databasePart1.DatabaseHelper;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.SQLException;

public class SetNewPasswordPage {
    private DatabaseHelper databaseHelper;
    private String userName;

    /**
     * Constructs a SetNewPasswordPage instance with the specified database helper and username.
     *
     * @param databaseHelper The database helper for managing password updates.
     * @param userName       The username of the user setting the new password.
     */
    public SetNewPasswordPage(DatabaseHelper databaseHelper, String userName) {
        this.databaseHelper = databaseHelper;
        this.userName = userName;
    }

    /**
     * Displays the Set New Password page with input fields and button functionality.
     *
     * @param primaryStage The primary stage for the password setting page.
     */
    public void show(Stage primaryStage) {
        // Title label
        Label titleLabel = new Label("Set Your New Password");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // New password field
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Enter New Password");
        newPasswordField.setStyle("-fx-font-size: 14px;");

        // Confirm password field
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm New Password");
        confirmPasswordField.setStyle("-fx-font-size: 14px;");

        // Error label
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");

        // Set password button
        Button setPasswordButton = new Button("Set Password");
        setPasswordButton.setStyle("-fx-background-color: #5cb85c; -fx-text-fill: white; -fx-font-size: 14px;");

        // Button functionality
        setPasswordButton.setOnAction(a -> {
            String newPassword = newPasswordField.getText();
            String confirmPassword = confirmPasswordField.getText();

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                errorLabel.setText("Fill in all fields.");
            } else if (!newPassword.equals(confirmPassword)) {
                errorLabel.setText("Passwords do not match. Please try again.");
            } else {
                try {
                    databaseHelper.updatePassword(userName, newPassword);
                    showAlert("Success", "Password updated successfully. Please log in.");
                    new UserLoginPage(databaseHelper).show(primaryStage);
                } catch (SQLException e) {
                    errorLabel.setText("Failed to update password. Please try again later.");
                }
            }
        });

        // Layout configuration
        VBox layout = new VBox(15, titleLabel, newPasswordField, confirmPasswordField, setPasswordButton, errorLabel);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-padding: 30; -fx-background-color: #f4f4f4;");

        // Scene setup
        primaryStage.setScene(new Scene(layout, 400, 300));
        primaryStage.setTitle("Set New Password");
    }

    /**
     * Displays an alert message with the specified title and content.
     *
     * @param title   The title of the alert.
     * @param message The content of the alert.
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}