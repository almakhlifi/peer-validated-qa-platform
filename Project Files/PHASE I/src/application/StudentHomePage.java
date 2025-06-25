package application;

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
		
        VBox layout = new VBox(10);
        layout.setStyle("-fx-alignment: center; -fx-padding: 20;");
        
        Label titleLabel = new Label("Student Home");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
     // Fixed Back Button - Now returns the actual user object
        Button backButton = new Button("Back");
        backButton.setOnAction(e -> new WelcomeLoginPage(databaseHelper).show(primaryStage, user));

        layout.getChildren().addAll(titleLabel, backButton);
        primaryStage.setScene(new Scene(layout, 800, 400));
        primaryStage.setTitle("Student Home");
    }
}