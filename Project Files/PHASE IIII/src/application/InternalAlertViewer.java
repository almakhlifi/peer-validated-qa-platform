package application;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import databasePart1.DatabaseHelper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * InternalAlertViewer
 *
 * Responsible for displaying system-generated internal alerts and feedback messages
 * to users with staff or instructor roles. Allows access to the related flagged content
 * through contextual buttons.
 */
public class InternalAlertViewer {
	
	/**
	 * Opens a window that displays internal alerts sent to staff or instructors.
	 *
	 * @param db The DatabaseHelper instance used to retrieve alerts.
	 * @param parentStage The parent stage to return to when this window closes.
	 * @param currentUser The user currently logged in.
	 */
	public static void show(DatabaseHelper db, Stage parentStage, User currentUser) {
        Stage stage = new Stage();
        stage.setTitle("Internal Staff Alerts");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setOnCloseRequest(e -> parentStage.show());

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #1e1e1e;");

        try {
        	List<Message> alerts = db.getInternalStaffAlerts(currentUser.getUserName());

            if (alerts.isEmpty()) {
                Label noAlerts = new Label("No internal staff alerts yet.");
                noAlerts.setStyle("-fx-text-fill: #aaa; -fx-font-style: italic;");
                root.getChildren().add(noAlerts);
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

                for (Message msg : alerts) {
                	
                	VBox msgBox = new VBox(5);
                    msgBox.setStyle("-fx-background-color: #333333; -fx-padding: 10; -fx-background-radius: 6;");

                    Label sender = new Label("From: " + msg.getSender() + " | " + msg.getTimestamp().toLocalDateTime().format(formatter));
                    sender.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                    
                    Label content = new Label(msg.getContent());
                    content.setWrapText(true);
                    content.setStyle("-fx-text-fill: white;");
                    
                    // view flag dashboard
                    Button viewBtn = new Button("View Related Flag");
                    viewBtn.setStyle("-fx-background-color: #5555aa; -fx-text-fill: white;");
                    viewBtn.setOnAction(e -> {
                        stage.hide();
                        FlaggedContentViewer.show(db, stage, currentUser);
                    });

                    msgBox.getChildren().addAll(sender, content, viewBtn);
                    root.getChildren().add(msgBox);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            Label error = new Label("Error loading alerts.");
            error.setStyle("-fx-text-fill: red;");
            root.getChildren().add(error);
        }

        Button backBtn = new Button("Back");
        backBtn.setStyle("-fx-background-color: #444; -fx-text-fill: white;");
        backBtn.setOnAction(e -> {
            parentStage.show();
            stage.close();
        });

        root.getChildren().add(backBtn);

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #1e1e1e; -fx-background-color: #1e1e1e;");

        Scene scene = new Scene(scrollPane, 650, 500);
        stage.setScene(scene);
        stage.show();
    }
}