package application;

import databasePart1.DatabaseHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class AdminHomePage {
    private DatabaseHelper dbHelper = new DatabaseHelper();
    private ListView<String> userListView;
    private ComboBox<String> roleComboBox;
    private Label inviteCodeLabel;

    private User currentAdmin; // Store the logged-in admin

    public void show(Stage primaryStage, DatabaseHelper databaseHelper, User user) {
        this.currentAdmin = user; // Save the logged-in admin
        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-padding: 20;");

        Label adminLabel = new Label("Hello, Admin!");
        adminLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        userListView = new ListView<>();
        loadUserList();

        Button deleteButton = new Button("Delete Selected User");
        deleteButton.setOnAction(event -> {
            String selectedUser = userListView.getSelectionModel().getSelectedItem();
            if (selectedUser != null) {
                String username = selectedUser.split(" - ")[0];
                showConfirmationDialog(username);
            } else {
                showAlert("Error", "Please select a user to delete.");
            }
        });

        // Role management components
        roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll("admin", "student", "instructor", "reviewer", "staff");
        Button addRoleButton = new Button("Add Role");
        Button removeRoleButton = new Button("Remove Role");

        addRoleButton.setOnAction(event -> addRoleToUser());
        removeRoleButton.setOnAction(event -> removeRoleFromUser());

        HBox roleManagementBox = new HBox(10, roleComboBox, addRoleButton, removeRoleButton);
        roleManagementBox.setAlignment(Pos.CENTER);

        // One-time password components
        TextField oneTimePasswordField = new TextField();
        oneTimePasswordField.setPromptText("Enter One-Time Password");
        oneTimePasswordField.setMaxWidth(250);

        Button setOneTimePasswordButton = new Button("Set One-Time Password");
        setOneTimePasswordButton.setOnAction(event -> {
            String selectedUser = userListView.getSelectionModel().getSelectedItem();
            if (selectedUser != null) {
                String username = selectedUser.split(" - ")[0];
                String oneTimePassword = oneTimePasswordField.getText();
                if (!oneTimePassword.isEmpty()) {
                    try {
                        dbHelper.setOneTimePassword(username, oneTimePassword);
                        showAlert("Success", "One-time password set for user: " + username);
                    } catch (SQLException e) {
                        showAlert("Error", "Failed to set one-time password.");
                    }
                } else {
                    showAlert("Error", "Please enter a one-time password.");
                }
            } else {
                showAlert("Error", "Please select a user to set a one-time password.");
            }
        });

        // Navigation to Invitation Page Only
        Button inviteBox = new Button("Invitation Code");
        inviteBox.setOnAction(e -> new InvitationPage().show(databaseHelper, primaryStage, user));

        // Logout Button
        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(e -> new UserLoginPage(databaseHelper).show(primaryStage));

        layout.getChildren().addAll(adminLabel, userListView, deleteButton, roleManagementBox, oneTimePasswordField, setOneTimePasswordButton, inviteBox, logoutButton);
        Scene adminScene = new Scene(layout, 800, 500);

        primaryStage.setScene(adminScene);
        primaryStage.setTitle("Admin Page");
    }

    private void addRoleToUser() {
        String selectedUser = userListView.getSelectionModel().getSelectedItem();
        String selectedRole = roleComboBox.getValue();
        if (selectedUser != null && selectedRole != null) {
            String username = selectedUser.split(" - ")[0];
            try {
                dbHelper.addRoleToUser(username, selectedRole);
                showAlert("Success", "Role added successfully.");
                loadUserList();
            } catch (SQLException e) {
                showAlert("Error", "Failed to add role.");
            }
        } else {
            showAlert("Error", "Please select a user and a role.");
        }
    }

    private void removeRoleFromUser() {
        String selectedUser = userListView.getSelectionModel().getSelectedItem();
        String selectedRole = roleComboBox.getValue();
        if (selectedUser != null && selectedRole != null) {
            String username = selectedUser.split(" - ")[0];
            try {
                dbHelper.removeRoleFromUser(username, selectedRole, currentAdmin.getUserName());
                showAlert("Success", "Role removed successfully.");
                loadUserList();
            } catch (SQLException e) {
                showAlert("Error", "Failed to remove role.");
            }
        } else {
            showAlert("Error", "Please select a user and a role.");
        }
    }

    private void deleteUser(String username) {
        try {
            dbHelper.deleteUser(username);
            showAlert("Success", "User deleted successfully.");
            loadUserList();
        } catch (SQLException e) {
            showAlert("Error", "Failed to delete user.");
        }
    }
}
