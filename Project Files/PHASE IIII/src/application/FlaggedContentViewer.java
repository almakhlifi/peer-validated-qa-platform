package application;

import databasePart1.DatabaseHelper;
import application.Flag;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Modality;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextArea;
import java.sql.SQLException;

/**
 * FlaggedContentViewer
 *
 * Displays a dashboard for staff to review all flagged content in the system.
 * Allows staff to view flagged questions, answers, and messages, mark flags as resolved,
 * and send internal alerts to instructors or other staff.
 */
public class FlaggedContentViewer {
	

	/**
	 * Displays the flagged content dashboard for staff users.
	 *
	 * @param db The DatabaseHelper instance used for data operations.
	 * @param parentStage The stage to return to after this window closes.
	 * @param currentUser The user currently logged in.
	 */
	public static void show(DatabaseHelper db, Stage parentStage, User currentUser) {
	    Stage stage = new Stage();
	    stage.setTitle("Flagged Content Dashboard");

	    VBox scrollContent = new VBox(15);
	    scrollContent.setPadding(new Insets(15));
	    scrollContent.setStyle("-fx-background-color: #1e1e1e;");

	    Label title = new Label("Flagged Content");
	    title.setStyle("-fx-text-fill: white; -fx-font-size: 20px;");

	    VBox flagsContainer = new VBox(10);
	    refreshFlagList(flagsContainer, db, stage);

	    scrollContent.getChildren().addAll(title, flagsContainer);

	    ScrollPane scrollPane = new ScrollPane(scrollContent);
	    scrollPane.setFitToWidth(true);
	    scrollPane.setStyle("-fx-background: #1e1e1e;");

	    // back button (now in a fixed footer)
	    Button backBtn = new Button("Back");
	    backBtn.setStyle("-fx-background-color: #555555; -fx-text-fill: white;");
	    backBtn.setOnAction(e -> {
	        stage.close();
	        parentStage.show();
	    });

	    VBox root = new VBox(10, scrollPane, backBtn);
	    root.setStyle("-fx-background-color: #1e1e1e;");
	    root.setPadding(new Insets(10));

	    Scene scene = new Scene(root, 600, 500);
	    stage.setScene(scene);
	    stage.initOwner(parentStage);
	    parentStage.hide();
	    stage.setOnCloseRequest(e -> {
	        parentStage.show();
	    });
	    stage.show();
	}
    
	/**
	 * Refreshes the flag list display with current unresolved flags from the database.
	 *
	 * @param container The VBox container where flag elements are displayed.
	 * @param db The DatabaseHelper instance used for fetching flag data.
	 * @param parentStage The parent stage for contextual view redirection.
	 */
	private static void refreshFlagList(VBox container, DatabaseHelper db, Stage parentStage) {
	    container.getChildren().clear();
	    List<Flag> allFlags = db.getAllFlags();
	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	    boolean hasUnresolvedFlags = false;

	    for (Flag flag : allFlags) {
	        if (!db.isFlagResolved(flag.getItemId(), flag.getType())) {
	            hasUnresolvedFlags = true;
	            break;
	        }
	    }

	    if (!hasUnresolvedFlags) {
	        Label noFlags = new Label("No flagged content at the moment.");
	        noFlags.setStyle("-fx-text-fill: #aaa; -fx-font-style: italic;");
	        container.getChildren().add(noFlags);
	        return;
	    }

	    for (Flag flag : allFlags) {
	        
	    	if (db.isFlagResolved(flag.getItemId(), flag.getType())) {
	            continue;
	        }
	    	
	    	VBox flagBox = new VBox(5);
	        flagBox.setStyle("-fx-background-color: #333333; -fx-padding: 10; -fx-background-radius: 6;");

	        Label item = new Label(
	            "Type: " + flag.getType() +
	            " | ID: " + flag.getItemId() +
	            " | By: " + flag.getFlaggedBy() +
	            " | On: " + flag.getTimestamp().format(formatter) +
	            "\nReason: " + flag.getReason()
	        );
	        item.setStyle("-fx-text-fill: white;");
	        
	        // mark as resolved button
	        Button resolveBtn = new Button("Mark as Resolved");
	        resolveBtn.setStyle("-fx-background-color: #228B22; -fx-text-fill: white;");
	        resolveBtn.setOnAction(e -> {
	            // confirmation
	            Stage confirmStage = new Stage();
	            confirmStage.initModality(Modality.APPLICATION_MODAL);
	            confirmStage.setTitle("Confirm Resolution");

	            VBox msgLayout = new VBox(10);
	            msgLayout.setPadding(new Insets(15));
	            msgLayout.setStyle("-fx-background-color: #2a2a2a;");
	            
	            // confirm message
	            Label confirmLabel = new Label("Are you sure you want to mark this flag as resolved?");
	            confirmLabel.setStyle("-fx-text-fill: white;");
	            
	            // yes button
	            Button yesBtn = new Button("Yes");
	            yesBtn.setStyle("-fx-background-color: #228B22; -fx-text-fill: white;");
	            yesBtn.setOnAction(event -> {
	                db.markFlagAsResolved(flag.getItemId(), flag.getType());
	                confirmStage.close();
	                
	                // remove the flagBox from the container directly
	                container.getChildren().remove(flagBox);
	            });
	            
	            // cancel button
	            Button cancelBtn = new Button("Cancel");
	            cancelBtn.setStyle("-fx-background-color: #444; -fx-text-fill: white;");
	            cancelBtn.setOnAction(event -> confirmStage.close());
	            
	            HBox buttonBox = new HBox(10, yesBtn, cancelBtn);
	            buttonBox.setAlignment(Pos.CENTER_RIGHT);

				msgLayout.getChildren().addAll(confirmLabel, buttonBox);
				confirmStage.setScene(new Scene(msgLayout));
				confirmStage.showAndWait();
	        });

	        flagBox.getChildren().addAll(item, resolveBtn);
	        
	        // Contextual view buttons
	        if (flag.getType() == Flag.FlagType.QUESTION) {
	            // view question button
	        	Button viewBtn = new Button("View Question");
	            viewBtn.setStyle("-fx-background-color: #5555aa; -fx-text-fill: white;");
	            viewBtn.setOnAction(event -> {
	            	parentStage.hide();
	            	QuestionSystemGUI.viewQuestionDetails(Integer.parseInt(flag.getItemId()), parentStage);
	            });
	            flagBox.getChildren().add(viewBtn);

	        } else if (flag.getType() == Flag.FlagType.ANSWER) {
	            // view answer button
	        	Button viewBtn = new Button("View Answer Context");
	            viewBtn.setStyle("-fx-background-color: #5555aa; -fx-text-fill: white;");
	            viewBtn.setOnAction(event -> {
	            	parentStage.hide();
	            	QuestionSystemGUI.viewQuestionContainingAnswer(Integer.parseInt(flag.getItemId()), parentStage);
	            });
	            flagBox.getChildren().add(viewBtn);

	        } else if (flag.getType() == Flag.FlagType.MESSAGE) {
	            // view message thread button
	        	Button viewBtn = new Button("View Message Thread");
	            viewBtn.setStyle("-fx-background-color: #5555aa; -fx-text-fill: white;");
	            viewBtn.setOnAction(event -> {
	            	InboxGUI.showThreadForMessageFlag(Integer.parseInt(flag.getItemId()), parentStage);
	            });
	            flagBox.getChildren().add(viewBtn);
	        }
	        
	        // notify staff/instructor button for messaging
	        Button notifyBtn = new Button("Notify Staff/Instructor");
	        notifyBtn.setStyle("-fx-background-color: #0066cc; -fx-text-fill: white;");
	        notifyBtn.setOnAction(ev -> {
	            showNotifyPopup(flag, db, parentStage);
	        });
	        flagBox.getChildren().add(notifyBtn);
	        
	        container.getChildren().add(flagBox);
	    }
	}
	
	/**
	 * Displays a pop-up for composing and sending internal notifications to staff and instructors
	 * related to a flagged item.
	 *
	 * @param flag The flag being discussed.
	 * @param db The DatabaseHelper instance for sending messages.
	 * @param parentStage The parent stage to maintain window ownership.
	 */
	private static void showNotifyPopup(Flag flag, DatabaseHelper db, Stage parentStage) {
	    Stage popup = new Stage();
	    popup.initModality(Modality.APPLICATION_MODAL);
	    popup.setTitle("Notify Staff/Instructor");

	    VBox layout = new VBox(10);
	    layout.setPadding(new Insets(15));
	    layout.setStyle("-fx-background-color: #2a2a2a;");

	    Label prompt = new Label("Write your message about this flag:");
	    prompt.setStyle("-fx-text-fill: white;");

	    TextArea messageArea = new TextArea();
	    messageArea.setPromptText("Enter message...");
	    messageArea.setWrapText(true);
	    messageArea.setStyle("-fx-control-inner-background: #3a3a3a; -fx-text-fill: white;");

	    Button sendBtn = new Button("Send");
	    sendBtn.setStyle("-fx-background-color: #228B22; -fx-text-fill: white;");
	    sendBtn.setOnAction(e -> {
	        String message = messageArea.getText().trim();
	        if (!message.isEmpty()) {
	            try {
	                List<String> recipients = db.getUsersByRole("staff");
	                recipients.addAll(db.getUsersByRole("instructor"));

	                for (String recipient : recipients) {
	                    db.sendMessage(
	                        "staff",  // sender
	                        recipient,
	                        -1,       // no specific question
	                        -1,       // no specific answer
	                        "[Flagged Item " + flag.getType() + " #" + flag.getItemId() + "] " + message,
	                        "staff_alert"  // message type
	                    );
	                }

	                popup.close();
	                Alert alert = new Alert(AlertType.INFORMATION, "Notification sent to staff and instructors.");
	                alert.showAndWait();
	            } catch (SQLException ex) {
	                ex.printStackTrace();
	                Alert errorAlert = new Alert(AlertType.ERROR, "Failed to send message: " + ex.getMessage());
	                errorAlert.showAndWait();
	            }
	        } else {
	            Alert emptyAlert = new Alert(AlertType.WARNING, "Message cannot be empty.");
	            emptyAlert.showAndWait();
	        }
	    });
	    
	    // cancel button for notification pop up
	    Button cancelBtn = new Button("Cancel");
	    cancelBtn.setStyle("-fx-background-color: #555; -fx-text-fill: white;");
	    cancelBtn.setOnAction(e -> popup.close());

	    HBox buttonBox = new HBox(10, sendBtn, cancelBtn);
	    buttonBox.setAlignment(Pos.CENTER_RIGHT);

	    layout.getChildren().addAll(prompt, messageArea, buttonBox);

	    popup.setScene(new Scene(layout, 400, 250));
	    popup.initOwner(parentStage);
	    popup.showAndWait();
	}
}