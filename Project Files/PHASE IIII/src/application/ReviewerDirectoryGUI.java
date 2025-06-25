package application;

import databasePart1.DatabaseHelper;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.Map;

import java.sql.SQLException;
import java.util.List;


/**
 * A GUI window that displays a list of all reviewers for the student to browse.
 * Students can view reviewer history or add reviewers to their trusted list.
 */
public class ReviewerDirectoryGUI {

    private final String currentUser;
    private final DatabaseHelper databaseHelper;
    
    /**
     * Constructs the ReviewerDirectoryGUI with the current user and database helper.
     *
     * @param currentUser     the username of the currently logged-in student
     * @param databaseHelper  the shared DatabaseHelper instance for accessing reviewer data
     */
    public ReviewerDirectoryGUI(String currentUser, DatabaseHelper databaseHelper) {
        this.currentUser = currentUser;
        this.databaseHelper = databaseHelper;
    }
    
    /**
     * Displays the reviewer directory window where the student can browse all reviewers,
     * view their review history, or add them to their trusted reviewer list.
     */
    public void show() {
        Stage stage = new Stage();
        stage.setTitle("All Reviewers");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        layout.setStyle("-fx-background-color: #1e1e1e;");
        
        // title label
        Label titleLabel = new Label("Browse All Reviewers:");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        layout.getChildren().add(titleLabel);

        List<String> reviewers = databaseHelper.getUsersByRole("reviewer");

        for (String reviewer : reviewers) {
            Label nameLabel = new Label(reviewer);
            nameLabel.setStyle("-fx-text-fill: white;");
            
            // view history button
            Button viewBtn = new Button("View History");
            viewBtn.setOnAction(e -> ReviewerDashboardGUI.show(reviewer, databaseHelper, true));
            viewBtn.setStyle("-fx-background-color: #6466f1; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
            
            // add to trusted button
            Button addBtn = new Button("Add to Trusted");
            addBtn.setOnAction(e -> {
                try {
                	Map<String, Integer> trusted = databaseHelper.getTrustedReviewersWithWeights(currentUser);
                	if (trusted.containsKey(reviewer)) {
                	    showAlert("Trusted Reviewer", reviewer + " is already in your trusted list.");
                	} else {
                	    databaseHelper.addOrUpdateTrustedReviewer(currentUser, reviewer, 1);
                	    showAlert("Trusted Reviewer", reviewer + " added to your trusted list.");
                	}
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showAlert("Error", "Failed to update trusted list.");
                }
            });
            addBtn.setStyle("-fx-background-color: gold; -fx-text-fill: black; -fx-background-radius: 8; -fx-cursor: hand;");

            HBox reviewerBox = new HBox(10, nameLabel, viewBtn, addBtn);
            reviewerBox.setStyle("-fx-background-color: #2e2e2e; -fx-padding: 8; -fx-border-color: gray; -fx-border-radius: 10;");
            layout.getChildren().add(reviewerBox);
        }
        
        // close pop up button
        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> stage.close());
        layout.getChildren().add(closeBtn);

        Scene scene = new Scene(layout, 450, 500);
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.show();
    }
    
    /**
     * Displays a pop-up alert with a title and message.
     *
     * @param title  the title of the alert window
     * @param msg    the message to display inside the alert
     */
    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}