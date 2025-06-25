package application;

import databasePart1.DatabaseHelper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.*;

public class ReviewerScorecardGUI {
    private final DatabaseHelper db;

    public ReviewerScorecardGUI(DatabaseHelper db) {
        this.db = db;
    }

    public void show() {
        Stage stage = new Stage();
        stage.setTitle("Reviewer Scorecard");

        TableView<Map<String, Object>> table = new TableView<>();

        TableColumn<Map<String, Object>, String> nameCol = new TableColumn<>("Reviewer");
        nameCol.setCellValueFactory(data -> new ReadOnlyStringWrapper((String) data.getValue().get("name")));

        TableColumn<Map<String, Object>, String> avgRatingCol = new TableColumn<>("Avg. Rating");
        avgRatingCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.format("%.2f", data.getValue().get("avg"))));

        TableColumn<Map<String, Object>, String> countCol = new TableColumn<>("# Reviews");
        countCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().get("count"))));

        TableColumn<Map<String, Object>, String> feedbackCol = new TableColumn<>("Feedback Count");
        feedbackCol.setCellValueFactory(data -> new ReadOnlyStringWrapper(String.valueOf(data.getValue().get("feedback"))));

        table.getColumns().addAll(nameCol, avgRatingCol, countCol, feedbackCol);

        try {
            List<String> reviewers = db.getUsersByRole("reviewer");
            for (String r : reviewers) {
                List<Review> reviews = db.getAllReviewsByReviewer(r).stream()
                        .filter(Review::isLatest)
                        .toList();

                if (reviews.isEmpty()) continue;

                double avgRating = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
                int feedbackTotal = reviews.stream()
                        .filter(rev -> rev.getTargetType().equals("answer"))
                        .mapToInt(rev -> {
                            try {
                                return db.getFeedbackCountForAnswer(r, rev.getTargetId());
                            } catch (SQLException e) {
                                return 0;
                            }
                        }).sum();

                Map<String, Object> row = new HashMap<>();
                row.put("name", r);
                row.put("avg", avgRating);
                row.put("count", reviews.size());
                row.put("feedback", feedbackTotal);
                table.getItems().add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        VBox root = new VBox(10, new Label("Reviewer Scorecard:"), table);
        root.setStyle("-fx-padding: 15; -fx-background-color: #1e1e1e;");
        table.setStyle("-fx-background-color: #2e2e2e; -fx-text-fill: white;");
        Scene scene = new Scene(root, 600, 400);
        stage.setScene(scene);
        stage.show();
    }
}
