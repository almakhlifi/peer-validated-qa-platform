package application;

import databasePart1.DatabaseHelper;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.SQLException;
import java.time.LocalDate;
import javafx.scene.layout.HBox;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides the user interface for administrators to generate invitation codes
 * for new users, assigning roles and setting an expiration date/time.
 */
public class AdminInvitationPage {
    private DatabaseHelper dbHelper;
    private Stage stage;
    private User adminUser;

    /**
     * Constructs the AdminInvitationPage.
     *
     * @param dbHelper   The database helper instance for database operations.
     * @param stage      The primary stage of the application.
     * @param adminUser  The currently logged-in administrator user.
     */
    public AdminInvitationPage(DatabaseHelper dbHelper, Stage stage, User adminUser) {
        this.dbHelper = dbHelper;
        this.stage = stage;
        this.adminUser = adminUser;
    }

    /**
     * Sets up and displays the invitation code generation window.
     */
    public void show() {
        VBox layout = new VBox(15);
        layout.setAlignment(javafx.geometry.Pos.CENTER);
        layout.setStyle("-fx-padding: 20; -fx-background-color: #1e1e1e;");

        Label titleLabel = new Label("Generate Invitation Code");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Role Selection - checkboxes
        CheckBox studentCheckBox = new CheckBox("Student");
        studentCheckBox.setStyle("-fx-text-fill: white;");
        
        CheckBox instructorCheckBox = new CheckBox("Instructor");
        instructorCheckBox.setStyle("-fx-text-fill: white;");
        
        CheckBox staffCheckBox = new CheckBox("Staff");
        staffCheckBox.setStyle("-fx-text-fill: white;");
        
        CheckBox reviewerCheckBox = new CheckBox("Reviewer");
        reviewerCheckBox.setStyle("-fx-text-fill: white;");

        VBox roleSelectionBox = new VBox(5, studentCheckBox, instructorCheckBox, staffCheckBox, reviewerCheckBox);
        roleSelectionBox.setStyle("-fx-padding: 10;");

        // Expiration Date Picker
        DatePicker expirationDatePicker = new DatePicker();
        expirationDatePicker.setStyle("-fx-background-color: #2e2e2e; -fx-text-fill: white;");
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
        generateInvitationButton.setStyle("-fx-background-color: #6466f1; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        generateInvitationButton.setOnAction(event -> {
            // Collect selected roles from checkboxes
            List<String> selectedRoles = new ArrayList<>();
            if (studentCheckBox.isSelected()) selectedRoles.add("student");
            if (instructorCheckBox.isSelected()) selectedRoles.add("instructor");
            if (staffCheckBox.isSelected()) selectedRoles.add("staff");
            if (reviewerCheckBox.isSelected()) selectedRoles.add("reviewer");

            // Checks for at least one role selected
            if (selectedRoles.isEmpty()) {
                showAlert("Error", "Please select at least one role.");
                return;
            }

            String assignedRoles = String.join(",", selectedRoles); // Store as a comma-separated string

            LocalDate expirationDate = expirationDatePicker.getValue();
            String selectedHour = hourComboBox.getValue();
            String selectedMinute = minuteComboBox.getValue();
            String amPm = amPmComboBox.getValue();

            // Checks expiration date
            // Ensure date is not null and not in the past (allow today)
            if (expirationDate == null || expirationDate.isBefore(LocalDate.now())) {
                 showAlert("Error", "Please select a valid expiration date (today or future).");
                 return;
             }


            // Checks time
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
            String formattedTime = String.format("%02d:%s:00", hour, selectedMinute); // Add seconds for SQL TIME format

            // Wrap invitation code generation in try-catch to handle SQLException
            String inviteCode;
            try {
                inviteCode = dbHelper.generateInvitationCode();
            } catch (SQLException e) {
                e.printStackTrace(); // Debugging: Print error in console
                showAlert("Database Error", "Failed to generate invitation code.");
                return; // Stop execution if code generation fails
            }

            // Store the invitation in the database
            try {
                dbHelper.storeInvitation(inviteCode, assignedRoles, expirationDate, formattedTime);
                showAlert("Success", "Generated Code: " + inviteCode + "\nExpires on: " + expirationDate + " " + selectedHour + ":" + selectedMinute + " " + amPm);
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Error", "Failed to store invitation.");
            }
        });

        // Back Button
        Button backButton = new Button("Back");
        backButton.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        backButton.setOnAction(e -> new AdminHomePage().show(stage, dbHelper, adminUser));

        layout.getChildren().addAll(titleLabel, roleSelectionBox, expirationDatePicker, timeSelectionBox, generateInvitationButton, backButton);
        Scene scene = new Scene(layout, 400, 350);

        stage.setScene(scene);
        stage.setTitle("Admin - Invitation");
        stage.show();
    }

    /**
     * Displays an informational alert dialog.
     *
     * @param title   The title of the alert window.
     * @param message The message content of the alert.
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
