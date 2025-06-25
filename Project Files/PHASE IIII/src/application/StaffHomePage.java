package application;

import databasePart1.DatabaseHelper;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import application.QuestionSystemGUI;

/**
 * StaffHomePage
 *
 * Builds and renders the home interface for staff users, giving them access to key tools
 * such as the flagged content dashboard, internal alerts, and the question system.
 */
public class StaffHomePage {

	/**
	 * Displays the staff home page with role-specific controls and alerts.
	 *
	 * @param primaryStage The main application window stage.
	 * @param databaseHelper A reference to the shared database access object.
	 * @param user The currently logged-in staff user.
	 */
	public void show(Stage primaryStage, DatabaseHelper databaseHelper, User user) {
	    VBox layout = new VBox(15);
	    layout.setStyle("-fx-alignment: center; -fx-padding: 30; -fx-background-color: #1e1e1e;");

	    // Title label
	    Label titleLabel = new Label("Staff Home");
	    titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");
	    layout.getChildren().add(titleLabel);
	    
	    // unresolved flags notifications
	    int unresolvedFlags = databaseHelper.getUnresolvedFlagCountByUser(user.getUserName());
	    if (unresolvedFlags > 0) {
	        Label flagNotificationLabel = new Label("🔔 You have " + unresolvedFlags + " unresolved flags you submitted.");
	        flagNotificationLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold; -fx-padding: 10px;");
	        layout.getChildren().add(flagNotificationLabel);
	    }

	    // Flag Viewer Button
	    Button viewFlagsButton = new Button("View Flagged Content");
	    viewFlagsButton.setStyle("-fx-font-size: 14px; -fx-background-color: orange; -fx-text-fill: white; -fx-padding: 8px 12px;");
	    viewFlagsButton.setOnAction(e -> FlaggedContentViewer.show(databaseHelper, primaryStage, user));
	    
	    // Question System Button
	    Button questionSystemButton = new Button("Question System");
	    questionSystemButton.setStyle("-fx-font-size: 14px; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 8px 12px;");
	    questionSystemButton.setOnAction(e -> {
	        primaryStage.hide();
	        new QuestionSystemGUI(user.getUserName(), "staff", databaseHelper, primaryStage).start(new Stage());
	    });

	    // Internal Alerts Button
	    Button internalAlertsBtn = new Button("View Internal Alerts");
	    internalAlertsBtn.setStyle("-fx-font-size: 14px; -fx-background-color: #6c63ff; -fx-text-fill: white; -fx-padding: 8px 12px;");
	    internalAlertsBtn.setOnAction(e -> {
	        primaryStage.hide();
	        InboxGUI.showStaffAlerts(databaseHelper, primaryStage, user.getUserName());
	    });

	    // Back button
	    Button backButton = new Button("Back");
	    backButton.setStyle("-fx-font-size: 14px; -fx-background-color: #2196F3; -fx-text-fill: white; -fx-padding: 8px 12px;");
	    backButton.setOnAction(e -> new WelcomeLoginPage(databaseHelper).show(primaryStage, user));

	    layout.getChildren().addAll(questionSystemButton, viewFlagsButton, internalAlertsBtn, backButton);
	    Scene scene = new Scene(layout, 800, 400);

	    scene.getStylesheets().add(StudentHomePage.class.getResource("dark-theme.css").toExternalForm());
	    primaryStage.setScene(scene);
	    primaryStage.setTitle("Staff Home");
	}
}