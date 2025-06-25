package application;

import databasePart1.DatabaseHelper;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.SQLException;
import java.time.LocalDate;
import javafx.scene.layout.HBox;


public class AdminInvitationPage {
    private DatabaseHelper dbHelper;
    private Stage stage;
    private User adminUser;

    public AdminInvitationPage(DatabaseHelper dbHelper, Stage stage, User adminUser) {
        this.dbHelper = dbHelper;
        this.stage = stage;
        this.adminUser = adminUser;
    }

    public void show() {
        VBox layout = new VBox(15);
        layout.setAlignment(javafx.geometry.Pos.CENTER);
        layout.setStyle("-fx-padding: 20;");

        Label titleLabel = new Label("Generate Invitation Code");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Role Selection
        ComboBox<String> roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll("admin", "student", "instructor", "reviewer", "staff");
        roleComboBox.setPromptText("Select Role");

        // Expiration Date Picker
        DatePicker expirationDatePicker = new DatePicker();
        expirationDatePicker.setPromptText("Select Expiration Date");

        // Time Selection (12-hour format)
        ComboBox<String> hourComboBox = new ComboBox<>();
        for (int i = 1; i <= 12; i++) {
            hourComboBox.getItems().add(String.format("%02d", i)); // 01 - 12
        }
        hourComboBox.setPromptText("Hour");

        ComboBox<String> minuteComboBox = new ComboBox<>();
        for (int i = 0; i < 60; i += 5) { // 5-minute intervals
            minuteComboBox.getItems().add(String.format("%02d", i)); // 00, 05, 10, ..., 55
        }
        minuteComboBox.setPromptText("Minute");

        ComboBox<String> amPmComboBox = new ComboBox<>();
        amPmComboBox.getItems().addAll("AM", "PM");
        amPmComboBox.setPromptText("AM/PM");

        HBox timeSelectionBox = new HBox(10, hourComboBox, minuteComboBox, amPmComboBox);
        timeSelectionBox.setAlignment(javafx.geometry.Pos.CENTER);

        // Generate Invitation Button
        Button generateInvitationButton = new Button("Generate Code");
        generateInvitationButton.setOnAction(event -> {
            String selectedRole = roleComboBox.getValue();
            LocalDate expirationDate = expirationDatePicker.getValue();
            String selectedHour = hourComboBox.getValue();
            String selectedMinute = minuteComboBox.getValue();
            String amPm = amPmComboBox.getValue();

            if (selectedRole == null) {
                showAlert("Error", "Please select a role.");
                return;
            }
            if (expirationDate == null || expirationDate.isBefore(LocalDate.now())) {
                showAlert("Error", "Please select a valid expiration date in the future.");
                return;
            }
            if (selectedHour == null || selectedMinute == null || amPm == null) {
                showAlert("Error", "Please select a valid time.");
                return;
            }

            // Convert 12-hour format to 24-hour format
            int hour = Integer.parseInt(selectedHour);
            if (amPm.equals("PM") && hour != 12) {
                hour += 12; // Convert PM times (except 12 PM)
            } else if (amPm.equals("AM") && hour == 12) {
                hour = 0; // Convert 12 AM to 00
            }
            String formattedTime = String.format("%02d:%s", hour, selectedMinute);

            String inviteCode = dbHelper.generateInvitationCode();

            try {
                dbHelper.storeInvitation(inviteCode, selectedRole, expirationDate, formattedTime);
                showAlert("Success", "Generated Code: " + inviteCode + "\nExpires on: " + expirationDate + " " + formattedTime);
            } catch (SQLException e) {
                showAlert("Error", "Failed to generate invitation.");
                e.printStackTrace();
            }
        });

        // Back Button
        Button backButton = new Button("Back");
        backButton.setOnAction(e -> new AdminHomePage().show(stage, dbHelper, adminUser));

        layout.getChildren().addAll(titleLabel, roleComboBox, expirationDatePicker, timeSelectionBox, generateInvitationButton, backButton);
        Scene scene = new Scene(layout, 400, 350);

        stage.setScene(scene);
        stage.setTitle("Admin - Invitation");
        stage.show();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
