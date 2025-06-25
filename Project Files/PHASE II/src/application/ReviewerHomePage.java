package application;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import databasePart1.*;
import java.util.List;

public class ReviewerHomePage {

    public void show(Stage primaryStage, DatabaseHelper databaseHelper, User user) {
        VBox layout = new VBox(10);
        layout.setStyle("-fx-alignment: center; -fx-padding: 20;");
        
        Label titleLabel = new Label("Reviewer Home");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Back button to return to WelcomeLoginPage
        Button backButton = new Button("Back");
        backButton.setOnAction(e -> new WelcomeLoginPage(databaseHelper).show(primaryStage, user));

        layout.getChildren().addAll(titleLabel, backButton);
        primaryStage.setScene(new Scene(layout, 800, 400));
        primaryStage.setTitle("Reviewer Home");
    }
}