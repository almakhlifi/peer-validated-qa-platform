package application;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import databasePart1.*;
import java.util.List;

public class StudentHomePage {
	
	private User user; // Store user info for navigation

	public void show(Stage primaryStage, DatabaseHelper databaseHelper, User user) {
		this.user = user; // Store the correct user object
		
        VBox layout = new VBox(15);
        layout.setStyle("-fx-alignment: center; -fx-padding: 30;");
        
        Label titleLabel = new Label("Student Home");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #333;");
        
        // Button for Question System
        Button questionSystemButton = new Button("Question System");
        questionSystemButton.setStyle("-fx-font-size: 14px; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 8px 12px;");
        questionSystemButton.setOnAction(e -> {
            primaryStage.close(); // Close Student Home Page

            // Open the Question System
            QuestionSystemGUI gui = new QuestionSystemGUI(user.getUserName(), databaseHelper, primaryStage);
            Stage stage = new Stage();
            gui.start(stage);
        });
        
        // Returns the actual user object
        Button backButton = new Button("Back");
        backButton.setStyle("-fx-font-size: 14px; -fx-background-color: #2196F3; -fx-text-fill: white; -fx-padding: 8px 12px;");
        backButton.setOnAction(e -> new WelcomeLoginPage(databaseHelper).show(primaryStage, user));

        layout.getChildren().addAll(titleLabel, questionSystemButton, backButton);
        primaryStage.setScene(new Scene(layout, 800, 400));
        primaryStage.setTitle("Student Home");
    }
}