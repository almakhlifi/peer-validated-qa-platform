package application;

import databasePart1.DatabaseHelper;
import java.sql.SQLException;
import java.util.List;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import application.QuestionSystemGUI;

/**
 * InstructorHomePage
 *
 * <p>This class builds and renders the main interface for instructor users. It provides
 * at-a-glance metrics and quick access to content review functionalities. The page
 * is divided into sections for metrics, review actions, and navigation controls.</p>
 */
public class InstructorHomePage {

    /**
     * Constructs and displays the instructor home page.
     * 
     * @param primaryStage   the main application window stage
     * @param databaseHelper the shared database access helper
     * @param user           the currently logged-in instructor user
     */
    public void show(Stage primaryStage, DatabaseHelper databaseHelper, User user) {
        // Root layout container with vertical spacing and dark background
        VBox layout = new VBox(15);
        layout.setStyle(
            "-fx-alignment: center; -fx-padding: 10 30 10 30; -fx-background-color: #1e1e1e;"
        );

        /**
         * Title label displaying the page name.
         */
        Label titleLabel = new Label("Instructor Home");
        titleLabel.setStyle(
            "-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;"
        );
        layout.getChildren().add(titleLabel);

        /**
         * At-a-glance metrics section: total questions and answers count.
         */
        int totalQuestions = 0;
        int totalAnswers = 0;
        try {
            totalQuestions = databaseHelper.loadQuestions().size();
            totalAnswers = databaseHelper.getAllAnswers().size();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Label questionsCountLabel = new Label("Total questions: " + totalQuestions);
        questionsCountLabel.setStyle("-fx-text-fill: white;");
        Label answersCountLabel = new Label("Total answers:   " + totalAnswers);
        answersCountLabel.setStyle("-fx-text-fill: white;");
        layout.getChildren().addAll(questionsCountLabel, answersCountLabel);

        // Flexible spacer pushing the action buttons downward
        Region topSpacer = new Region();
        VBox.setVgrow(topSpacer, Priority.ALWAYS);
        layout.getChildren().add(topSpacer);

        /**
         * Review action row (Row 1):
         * - Question System button
         * - Review Content button
         */
        Button questionSystemButton = new Button("Question System");
        questionSystemButton.setStyle(
            "-fx-font-size:14px; -fx-background-color:#4CAF50; -fx-text-fill:white; -fx-padding:8px 12px;"
        );
        questionSystemButton.setOnAction(e -> {
            primaryStage.hide();
            new QuestionSystemGUI(
                user.getUserName(), "instructor", databaseHelper, primaryStage
            ).start(new Stage());
        });

            Button scorecardBtn = new Button("Reviewer Scorecard");
        scorecardBtn.setStyle("-fx-font-size:14px; -fx-background-color:#00BCD4; -fx-text-fill:white; -fx-padding:8px 12px;");
        scorecardBtn.setOnAction(e -> {
            new ReviewerScorecardGUI(databaseHelper).show();
        });
        layout.getChildren().add(scorecardBtn);

        Button reviewContentButton = new Button("Review Content");
        reviewContentButton.setStyle(
            "-fx-font-size:14px; -fx-background-color:purple; -fx-text-fill:white; -fx-padding:8px 12px;"
        );
        reviewContentButton.setOnAction(e -> {
            primaryStage.hide();
            new QuestionSystemGUI(
                user.getUserName(), "instructor", databaseHelper, primaryStage
            ).start(new Stage());
        });
        HBox topBox = new HBox(10, questionSystemButton, reviewContentButton);
        topBox.setAlignment(Pos.CENTER);

        /**
         * Moderation action row (Row 2):
         * - Flagged Items count button
         * - Pending Reviewer Requests button
         */
        Button flaggedCountBtn = new Button("Flagged Items");
        flaggedCountBtn.setStyle(
            "-fx-font-size:14px; -fx-background-color:#f44336; -fx-text-fill:white; -fx-padding:8px 12px;"
        );
        flaggedCountBtn.setOnAction(e -> {
            try {
                int count = databaseHelper.getAllFlags().size();
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Flagged Content");
                alert.setHeaderText(null);
                // Apply dark theme to alert
                alert.getDialogPane().getStylesheets().add(
                    StudentHomePage.class.getResource("dark-theme.css").toExternalForm()
                );
                alert.getDialogPane().setStyle("-fx-background-color:#1e1e1e; -fx-text-fill:white;");
                alert.setContentText(
                    "There are " + count + " flagged items in the system."
                );
                alert.showAndWait();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Button pendingReqBtn = new Button("Pending Reviews");
        pendingReqBtn.setStyle(
            "-fx-font-size:14px; -fx-background-color:#FF9800; -fx-text-fill:white; -fx-padding:8px 12px;"
        );
        pendingReqBtn.setOnAction(e -> {
            try {
                List<String> pending = databaseHelper.getPendingReviewerRequests();
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Pending Reviewer Requests");
                alert.setHeaderText(null);
                alert.getDialogPane().getStylesheets().add(
                    StudentHomePage.class.getResource("dark-theme.css").toExternalForm()
                );
                alert.getDialogPane().setStyle("-fx-background-color:#1e1e1e; -fx-text-fill:white;");
                String content =
                    pending.isEmpty() ? "No pending requests." : String.join("\n", pending);
                alert.setContentText(content);
                alert.showAndWait();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        HBox midBox = new HBox(10, flaggedCountBtn, pendingReqBtn);
        midBox.setAlignment(Pos.CENTER);

        // Flexible spacer pushing the bottom row downward
        Region bottomSpacer = new Region();
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);

        /**
         * Navigation row (Row 3):
         * - Refresh Counts button
         * - Back button
         */
        Button refreshBtn = new Button("Refresh Counts");
        refreshBtn.setStyle(
            "-fx-font-size:14px; -fx-background-color:#9E9E9E; -fx-text-fill:white; -fx-padding:8px 12px;"
        );
        refreshBtn.setOnAction(e -> {
            try {
                questionsCountLabel.setText(
                    "Total questions: " + databaseHelper.loadQuestions().size()
                );
                answersCountLabel.setText(
                    "Total answers:   " + databaseHelper.getAllAnswers().size()
                );
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        Button backButton = new Button("Back");
        backButton.setStyle(
            "-fx-font-size:14px; -fx-background-color:#2196F3; -fx-text-fill:white; -fx-padding:8px 12px;"
        );
        backButton.setOnAction(e -> 
            new WelcomeLoginPage(databaseHelper).show(primaryStage, user)
        );
        HBox bottomBox = new HBox(10, refreshBtn, backButton);
        bottomBox.setAlignment(Pos.CENTER);

        // Assemble layout with spacing
        layout.getChildren().addAll(topBox, midBox, bottomSpacer, bottomBox);

        // Finalize and show scene
        Scene scene = new Scene(layout, 800, 400);
        scene.getStylesheets().add(
            StudentHomePage.class.getResource("dark-theme.css").toExternalForm()
        );
        primaryStage.setScene(scene);
        primaryStage.setTitle("Instructor Home");
        primaryStage.show();
    }
}
