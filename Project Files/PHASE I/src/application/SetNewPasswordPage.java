package application;

import databasePart1.DatabaseHelper;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.SQLException;

public class SetNewPasswordPage {
    private DatabaseHelper databaseHelper;
    private String userName;

    public SetNewPasswordPage(DatabaseHelper databaseHelper, String userName) {
        this.databaseHelper = databaseHelper;
        this.userName = userName;
    }

    public void show(Stage primaryStage) {
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Enter New Password");

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm New Password");

        Label errorLabel = new Label();
        Button setPasswordButton = new Button("Set Password");

        setPasswordButton.setOnAction(a -> {
            String newPassword = newPasswordField.getText();
            String confirmPassword = confirmPasswordField.getText();

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                errorLabel.setText("Fill in all fields.");
            } else if (!newPassword.equals(confirmPassword)) {
                errorLabel.setText("Passwords do not match.");
            } else {
                try {
                    databaseHelper.updatePassword(userName, newPassword);
                    new UserLoginPage(databaseHelper).show(primaryStage);
                } catch (SQLException e) {
                    errorLabel.setText("Failed to update password.");
                }
            }
        });

        VBox layout = new VBox(10, newPasswordField, confirmPasswordField, setPasswordButton, errorLabel);
        primaryStage.setScene(new Scene(layout, 800, 400));
        primaryStage.setTitle("Set New Password");
    }
}
