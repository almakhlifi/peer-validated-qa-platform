package application;

import databasePart1.DatabaseHelper;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import java.sql.SQLException;

public class InvitationPage {
    private DatabaseHelper dbHelper;

    public void show(DatabaseHelper databaseHelper, Stage primaryStage) {
        this.dbHelper = databaseHelper;

        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-padding: 20;");

        Label titleLabel = new Label("Enter Invitation Code");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        TextField emailField = new TextField();
        emailField.setPromptText("Enter your email");
        emailField.setMaxWidth(300);

        TextField codeField = new TextField();
        codeField.setPromptText("Enter invitation code");
        codeField.setMaxWidth(300);

        Button acceptButton = new Button("Accept Invitation");
        acceptButton.setOnAction(event -> {
            String email = emailField.getText();
            String code = codeField.getText();

            if (email.isEmpty() || code.isEmpty()) {
                showAlert("Error", "Please fill in all fields.");
                return;
            }

            try {
                if (dbHelper.validateInvitation(email, code)) {
                    dbHelper.addUserFromInvitation(email, code);
                    dbHelper.deleteUsedInvitation(email, code);
                    showAlert("Success", "You have been added to the system!");
                } else {
                    showAlert("Error", "Invalid or expired invitation.");
                }
            } catch (SQLException e) {
                showAlert("Database Error", "Failed to accept invitation.");
            }
        });

        Button backButton = new Button("Back");
        backButton.setOnAction(event -> new UserLoginPage(databaseHelper).show(primaryStage));

        layout.getChildren().addAll(titleLabel, emailField, codeField, acceptButton, backButton);
        Scene scene = new Scene(layout, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Accept Invitation");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
