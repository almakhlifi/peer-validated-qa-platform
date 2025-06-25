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

/**
 * Provides the admin user interface for managing users, roles, passwords, and invitations.
 */
public class AdminHomePage {
    private DatabaseHelper dbHelper = new DatabaseHelper();
    private ListView<String> userListView;
    private ComboBox<String> roleComboBox;
    private User currentAdmin; // Store the logged-in Admin

    /**
     * Displays the main admin dashboard window.
     *
     * @param primaryStage The primary stage for this application.
     * @param databaseHelper The helper object for database operations.
     * @param user The currently logged-in admin user.
     */
    public void show(Stage primaryStage, DatabaseHelper databaseHelper, User user) {
        this.currentAdmin = user; // Save the logged-in Admin

        // Layout setup
        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-padding: 25; -fx-alignment: center;");

        // Admin greeting label
        Label adminLabel = new Label("Admin Dashboard");
        adminLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 10;");

        // User List Box
        userListView = new ListView<>();
        userListView.setPrefSize(500, 200);
        loadUserList();

        // Delete User Button
        Button deleteButton = new Button("Delete Selected User");
        deleteButton.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white;");
        deleteButton.setOnAction(event -> {
            String selectedUser = userListView.getSelectionModel().getSelectedItem();
            if (selectedUser != null) {
                String username = selectedUser.split(" - ")[0];
                showConfirmationDialog(username);
            } else {
                showAlert("Error", "Please select a user to delete.");
            }
        });

        // Role Management Section
        Label roleLabel = new Label("Manage User Roles:");
        roleLabel.setStyle("-fx-font-weight: bold; -fx-padding: 5;");
        roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll("admin", "student", "instructor", "reviewer", "staff");

        // Add and remove role buttons
        Button addRoleButton = new Button("Add Role");
        Button removeRoleButton = new Button("Remove Role");

        // Button styling
        addRoleButton.setStyle("-fx-background-color: #d9d9d9; -fx-text-fill: black;");
        removeRoleButton.setStyle("-fx-background-color: #d9d9d9; -fx-text-fill: black;");

        // Button functionality
        addRoleButton.setOnAction(event -> addRoleToUser());
        removeRoleButton.setOnAction(event -> removeRoleFromUser());

        HBox roleManagementBox = new HBox(10, roleComboBox, addRoleButton, removeRoleButton);
        roleManagementBox.setAlignment(Pos.CENTER);

        // One-Time Password Section
        Label otpLabel = new Label("Set One-Time Password:");
        otpLabel.setStyle("-fx-font-weight: bold; -fx-padding: 5;");
        TextField oneTimePasswordField = new TextField();
        oneTimePasswordField.setPromptText("Enter One-Time Password");
        oneTimePasswordField.setMaxWidth(250);

        Button setOneTimePasswordButton = new Button("Set One-Time Password");
        setOneTimePasswordButton.setStyle("-fx-background-color: #d9d9d9; -fx-text-fill: black;");

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

        // Send Invitation
        Button inviteBox = new Button("Send Invitation");
        inviteBox.setStyle("-fx-background-color: #d9d9d9; -fx-text-fill: black;");
        inviteBox.setOnAction(e -> {
            AdminInvitationPage invitationPage = new AdminInvitationPage(databaseHelper, primaryStage, currentAdmin);
            invitationPage.show();
        });

        HBox inviteBoxContainer = new HBox(inviteBox);
        inviteBoxContainer.setAlignment(Pos.CENTER);

        // Logout Button
        Button logoutButton = new Button("Logout");
        logoutButton.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white;");
        logoutButton.setOnAction(e -> new UserLoginPage(databaseHelper).show(primaryStage));

        // Add components to layout
        layout.getChildren().addAll(
                adminLabel, userListView, deleteButton,
                roleLabel, roleManagementBox, inviteBoxContainer,
                otpLabel, oneTimePasswordField, setOneTimePasswordButton,
                logoutButton
        );

        // Create and set scene
        Scene adminScene = new Scene(layout, 800, 600);
        primaryStage.setScene(adminScene);
        primaryStage.setTitle("Admin Page");
    }


    /**
     * Fetches users and their roles from the database and populates the user list view.
     */
    private void loadUserList() {
        try {
            dbHelper.connectToDatabase();
            List<String> usersWithRoles = dbHelper.getUsersWithRolesIncludingNoRole();
            ObservableList<String> userObservableList = FXCollections.observableArrayList(usersWithRoles);
            userListView.setItems(userObservableList);
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load users.");
        }
    }


    /**
     * Shows a confirmation dialog before deleting a user.
     *
     * @param username The username of the user to be deleted.
     */
    private void showConfirmationDialog(String username) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete User");
        confirmation.setContentText("Are you sure you want to delete user: " + username + "?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteUser(username);
        }
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

    /**
     * Assigns the selected role to the selected user in the database.
     */
    private void addRoleToUser() {
        String selectedUser = userListView.getSelectionModel().getSelectedItem();
        String selectedRole = roleComboBox.getValue();

        if (selectedUser != null && selectedRole != null) {
            String username = selectedUser.split(" - ")[0];
            try {
                dbHelper.addRoleToUser(username, selectedRole);
                showAlert("Success", "Role added successfully.");
                loadUserList(); // Refresh user list after adding role
            } catch (SQLException e) {
                if (e.getMessage().contains("User already has the role")) {
                    showAlert("Error", "The user already has this role.");
                } else {
                    showAlert("Error", "Failed to add role.");
                }
            }
        } else {
            showAlert("Error", "Please select a user and a role.");
        }
    }

    /**
     * Removes the selected role from the selected user in the database.
     */
    private void removeRoleFromUser() {
        String selectedUser = userListView.getSelectionModel().getSelectedItem();
        String selectedRole = roleComboBox.getValue();

        if (selectedUser != null && selectedRole != null) {
            String username = selectedUser.split(" - ")[0];
            try {
                // Ensure currentAdmin is not null and has a username before proceeding
                if (currentAdmin == null || currentAdmin.getUserName() == null) {
                     showAlert("Error", "Admin information is missing. Cannot remove role.");
                     return;
                }
                dbHelper.removeRoleFromUser(username, selectedRole, currentAdmin.getUserName());
                showAlert("Success", "Role removed successfully.");
                loadUserList(); // Refresh user list after removing role
            } catch (SQLException e) {
                if (e.getMessage().contains("User does not have the role")) {
                    showAlert("Error", "The user does not have this role.");
                } else {
                    showAlert("Error", e.getMessage()); // Show specific SQL errors if needed
                }
            }
        } else {
            showAlert("Error", "Please select a user and a role.");
        }
    }

    /**
     * Deletes the specified user from the database.
     *
     * @param username The username of the user to delete.
     */
    private void deleteUser(String username) {
        try {
            dbHelper.deleteUser(username);
            showAlert("Success", "User deleted successfully.");
            loadUserList(); // Refresh user list after deletion
        } catch (SQLException e) {
            showAlert("Error", "Failed to delete user.");
        }
    }
}