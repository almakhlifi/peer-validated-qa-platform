
/**
 * ReviewerHomePage
 * 
 * Class is responsible for rendering the home page for reviewers in the application.
 */
package application;

import databasePart1.DatabaseHelper;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.util.Optional;

public class ReviewerHomePage {

    /**
     * Displays the reviewer home page with options to access the question system, reviews dashboard,
     * and log out.
     *
     * @param primaryStage The primary stage for the reviewer home page.
     * @param databaseHelper Database helper for managing data operations.
     * @param user The user object representing the currently logged-in reviewer.
     */
    public void show(Stage primaryStage, DatabaseHelper databaseHelper, User user) {
        VBox layout = new VBox(15);
        layout.setStyle("-fx-alignment: center; -fx-padding: 30; -fx-background-color: #1e1e1e;");

        Label titleLabel = new Label("Reviewer Home");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Question System Button
        Button questionSystemButton = new Button("Question System");
        questionSystemButton.setStyle("-fx-font-size: 14px; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 8px 12px;");
        questionSystemButton.setOnAction(e -> {
            primaryStage.close();
            QuestionSystemGUI gui = new QuestionSystemGUI(user.getUserName(), "reviewer", databaseHelper, primaryStage);
            Stage stage = new Stage();
            gui.start(stage);
        });

        // Dashboard Button
        Button viewDashboardButton = new Button("Reviews Dashboard");
        viewDashboardButton.setStyle("-fx-font-size: 14px; -fx-background-color: orange; -fx-text-fill: white; -fx-padding: 8px 12px;");
        viewDashboardButton.setOnAction(e -> {
            primaryStage.hide();
            Stage dashboardStage = new Stage();
            QuestionSystem qsManager = new QuestionSystem();
            qsManager.databaseHelper = databaseHelper;
            new ReviewerDashboardGUI(user.getUserName(), databaseHelper, qsManager, primaryStage).start(dashboardStage);
        });

        // Back Button
        Button backButton = new Button("Back");
        backButton.setStyle("-fx-font-size: 14px; -fx-background-color: #2196F3; -fx-text-fill: white; -fx-padding: 8px 12px;");
        backButton.setOnAction(e -> new WelcomeLoginPage(databaseHelper).show(primaryStage, user));

        layout.getChildren().addAll(titleLabel, questionSystemButton, viewDashboardButton, backButton);
        Scene scene = new Scene(layout, 800, 400);
        scene.getStylesheets().add(StudentHomePage.class.getResource("dark-theme.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle("Reviewer Home");
    }
}