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
 * The AdminHomePage class allows an admin to manage users, assign roles, 
 * delete users, set one-time passwords, and send invitations.
 */
public class AdminHomePage {
    private DatabaseHelper dbHelper = new DatabaseHelper();
    private ListView<String> userListView;
    private ComboBox<String> roleComboBox;
    private User currentAdmin; // Store the logged-in admin

    /**
     * Displays the admin home page.
     * 
     * @param primaryStage   The primary stage for the application.
     * @param databaseHelper The database helper instance.
     * @param user           The currently logged-in admin.
     */
    public void show(Stage primaryStage, DatabaseHelper databaseHelper, User user) {
        this.currentAdmin = user; // Save the logged-in admin

        // Layout setup
        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-padding: 20;");

        // Admin greeting label
        Label adminLabel = new Label("Hello, Admin!");
        adminLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // User list
        userListView = new ListView<>();
        loadUserList();

        // Button to delete a selected user
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

        // Invitation button
        Button inviteBox = new Button("Send Invitation");
        inviteBox.setOnAction(e -> {
            AdminInvitationPage invitationPage = new AdminInvitationPage(databaseHelper, primaryStage, currentAdmin);
            invitationPage.show(); // Properly call the show method
        });



        // Logout button (properly declared before use)
        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(e -> new UserLoginPage(databaseHelper).show(primaryStage));

        // Add components to layout
        layout.getChildren().addAll(adminLabel, userListView, deleteButton, roleManagementBox, 
            oneTimePasswordField, setOneTimePasswordButton, inviteBox, logoutButton);

        // Create and set scene
        Scene adminScene = new Scene(layout, 800, 500);
        primaryStage.setScene(adminScene);
        primaryStage.setTitle("Admin Page");
    }

    /**
     * Loads the list of users from the database.
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
     * @param username The username of the user to delete.
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
     * Displays an alert message.
     * 
     * @param title   The title of the alert.
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
     * Adds a role to the selected user.
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
                showAlert("Error", "Failed to add role.");
            }
        } else {
            showAlert("Error", "Please select a user and a role.");
        }
    }

    /**
     * Removes a role from the selected user.
     */
    private void removeRoleFromUser() {
        String selectedUser = userListView.getSelectionModel().getSelectedItem();
        String selectedRole = roleComboBox.getValue();
        if (selectedUser != null && selectedRole != null) {
            String username = selectedUser.split(" - ")[0];
            try {
                dbHelper.removeRoleFromUser(username, selectedRole, currentAdmin.getUserName());
                showAlert("Success", "Role removed successfully.");
                loadUserList(); // Refresh user list after removing role
            } catch (SQLException e) {
                showAlert("Error", "Failed to remove role.");
            }
        } else {
            showAlert("Error", "Please select a user and a role.");
        }
    }

    /**
     * Deletes the selected user from the database.
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
