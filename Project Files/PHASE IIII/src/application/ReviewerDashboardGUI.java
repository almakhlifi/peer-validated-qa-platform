package application;

import databasePart1.DatabaseHelper;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The {@code ReviewerDashboardGUI} class represents the graphical user interface for a reviewer's dashboard.
 * It allows reviewers to view, manage, and update their reviews, as well as view feedback and profile information.
 */
public class ReviewerDashboardGUI extends Application {

    private String currentUser;
    private DatabaseHelper databaseHelper;
    private QuestionSystem questionSystemManager;
    private ObservableList<Review> myReviewsList = FXCollections.observableArrayList();
    private ListView<Review> reviewListView;
    private Stage previousStage;

    /**
     * Constructs a {@code ReviewerDashboardGUI} object with the specified parameters.
     *
     * @param currentUser The username of the current reviewer.
     * @param dbHelper    The {@code DatabaseHelper} object for database interactions.
     * @param qsManager   The {@code QuestionSystem} object for managing questions and answers.
     * @param previousStage The stage to return to when the dashboard is closed.
     * @throws IllegalArgumentException if any of the parameters are null.
     */
    public ReviewerDashboardGUI(String currentUser, DatabaseHelper dbHelper, QuestionSystem qsManager, Stage previousStage) {
        if (currentUser == null || dbHelper == null || qsManager == null) {
            throw new IllegalArgumentException("CurrentUser, DatabaseHelper, and QuestionSystem cannot be null.");
        }
        this.currentUser = currentUser;
        this.databaseHelper = dbHelper;
        this.questionSystemManager = qsManager;
        this.previousStage = previousStage;
        if (this.questionSystemManager.databaseHelper == null) {
            this.questionSystemManager.databaseHelper = this.databaseHelper;
            System.out.println("ReviewerDashboardGUI: Set DatabaseHelper in QuestionSystem.");
        }
    }

    /**
     * Shows the reviewer dashboard GUI.
     * @param stage The stage to display the GUI on.
     */
    public void show(Stage stage) {
        start(stage);
    }

    /**
     * Shows the reviewer dashboard GUI as read-only.
     * @param reviewer The username of the reviewer.
     * @param db The database helper.
     * @param readOnly If read only.
     */
    public static void show(String reviewer, DatabaseHelper db, boolean readOnly) {
	    showReadOnly(reviewer, "", db);
	}
    
    /**
     * Starts the JavaFX application and sets up the reviewer dashboard GUI.
     *
     * @param primaryStage The primary stage for the application.
     */
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("My Reviews Dashboard - " + currentUser);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #1e1e1e;");

        Label titleLabel = new Label("My Reviews");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white; -fx-padding: 0 0 10 0;");
        Label subtitleLabel = new Label("Manage and view all versions of your reviews");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #aaaaaa;");
        VBox headerBox = new VBox(5, titleLabel, subtitleLabel);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        root.setTop(headerBox);
        BorderPane.setMargin(headerBox, new Insets(0, 0, 15, 0));

        reviewListView = new ListView<>(myReviewsList);
        reviewListView.setStyle("-fx-control-inner-background: #2e2e2e; -fx-border-color: #444; -fx-border-width: 1;");
        reviewListView.setCellFactory(lv -> new ReviewListCell());
        VBox.setVgrow(reviewListView, Priority.ALWAYS);

        Label emptyListLabel = new Label("You haven't submitted any reviews yet");
        emptyListLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 14px; -fx-text-alignment: center;");
        emptyListLabel.setWrapText(true);
        reviewListView.setPlaceholder(emptyListLabel);

        VBox centerBox = new VBox(10, reviewListView);
        centerBox.setPadding(new Insets(5, 0, 5, 0));
        root.setCenter(centerBox);

        Button refreshButton = new Button("🔄 Refresh List");
        refreshButton.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white; -fx-font-size: 13px;");
        refreshButton.setOnAction(e -> loadMyReviews(false));

        Button closeButton = new Button("❌ Close Dashboard");
        closeButton.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-size: 13px;");
        closeButton.setOnAction(e -> {
            primaryStage.close();
            if (previousStage != null) {
                previousStage.show();  // bring back the previous window
            }
        });
        
        Button viewProfileBtn = new Button("👤 View Profile"); // Add an emoji/icon for clarity
        viewProfileBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        viewProfileBtn.setOnAction(e -> showProfile());

        HBox bottomBox = new HBox(15, refreshButton, closeButton, viewProfileBtn); // Add viewProfileBtn here
        bottomBox.setAlignment(Pos.CENTER_LEFT);
        bottomBox.setPadding(new Insets(15, 0, 0, 0));
        
        CheckBox toggleHistory = new CheckBox("Show all versions");
        toggleHistory.setStyle("-fx-text-fill: white;");

        toggleHistory.setOnAction(e -> loadMyReviews(toggleHistory.isSelected()));

        VBox bottomCombined = new VBox(10, toggleHistory, bottomBox);
        root.setBottom(bottomCombined);

        loadMyReviews(false); // Default: only latest

        Scene scene = new Scene(root, 850, 650);
        try {
            String cssPath = getClass().getResource("dark-theme.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
        } catch (Exception e) {
            System.err.println("ReviewerDashboardGUI Error loading CSS: " + e.getMessage() + ". Applying fallback styles.");
             root.setStyle("-fx-base: #1e1e1e; -fx-background: #1e1e1e; -fx-control-inner-background: #2e2e2e; -fx-text-fill: white;");
        }
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    /**
     * Shows feedback for each answer from a specific reviewer.
     *
     * @param answerId         The ID of the answer to retrieve feedback for.
     * @param reviewerUsername The username of the reviewer who provided the feedback.
     */
    private void showFeedbackForAnswer(int answerId, String reviewerUsername) {
        Stage feedbackStage = new Stage();
        feedbackStage.setTitle("Feedback for Answer ID " + answerId);

        ListView<Message> feedbackList = new ListView<>();
        feedbackList.setStyle("-fx-background-color: #2e2e2e; -fx-control-inner-background: #2e2e2e;");
        feedbackList.setPrefWidth(500);
        feedbackList.setCellFactory(lv -> new ListCell<>() {
        	@Override
        	protected void updateItem(Message msg, boolean empty) {
        	    super.updateItem(msg, empty);
        	    if (empty || msg == null) {
        	        setText(null);
        	        setStyle("-fx-background-color: #2e2e2e;");
        	    } else {
        	        String preview = String.format("🗨 From: %s | %s\n%s",
        	                msg.getSender(),
        	                msg.getTimestamp().toString().substring(0, 19),
        	                msg.getContent());
        	        setText(preview);
        	        setTextFill(javafx.scene.paint.Color.WHITE);
        	        setStyle("-fx-background-color: #2e2e2e; -fx-font-size: 12px;");
        	    }
        	}
        });

        try {
            List<Message> messages = databaseHelper.getFeedbackMessagesForAnswer(reviewerUsername, answerId);
            feedbackList.getItems().addAll(messages);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load feedback messages.");
        }

        VBox layout = new VBox(10, feedbackList);
        layout.setPadding(new Insets(15));
        layout.setStyle("-fx-background-color: #2e2e2e;");
        Scene scene = new Scene(layout);
        feedbackStage.setScene(scene);
        feedbackStage.show();
    }

    /**
     * Loads the reviews associated with the current user from the database.
     *
     * @param showAllVersions A boolean indicating whether to load all versions of reviews or only the latest versions.
     */
    private void loadMyReviews(boolean showAllVersions) {
    	try {
    	    List<Review> allReviews = databaseHelper.getAllReviewsByReviewer(currentUser);
    	    if (showAllVersions) {
    	        myReviewsList.setAll(allReviews);
    	    } else {
    	        List<Review> latestOnly = allReviews.stream()
    	            .filter(Review::isLatest)
    	            .toList();
    	        myReviewsList.setAll(latestOnly);
    	    }
    	} catch (SQLException e) {
    	    e.printStackTrace();
    	    showAlert("Database Error", "Failed to load your reviews: " + e.getMessage());
    	}
    }

    /**
     * Shows a dialog for adding or editing a review.
     *
     * @param targetType    The type of the target being reviewed (e.g., "question", "answer").
     * @param targetId      The ID of the target being reviewed.
     * @param existingReview The existing {@code Review} object if editing; otherwise, null for a new review.
     */
    public void showReviewDialog(String targetType, int targetId, Review existingReview) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle((existingReview == null ? "Add New" : "Edit") + " Review for " + targetType + " ID: " + targetId);

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #1e1e1e;");

        Label ratingLabel = new Label("Select Rating:");
        ratingLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        HBox ratingBox = new HBox(8);
        ratingBox.setAlignment(Pos.CENTER_LEFT);
        ToggleGroup ratingGroup = new ToggleGroup();
        RadioButton[] ratingButtons = new RadioButton[5];
        for (int i = 0; i < 5; i++) {
            ratingButtons[i] = new RadioButton((i + 1) + " ★");
            ratingButtons[i].setToggleGroup(ratingGroup);
            ratingButtons[i].setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-mark-color: gold;");
            ratingButtons[i].setUserData(i + 1);
            ratingBox.getChildren().add(ratingButtons[i]);
        }

        Label commentLabel = new Label("Comment (Optional):");
        commentLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        TextArea commentArea = new TextArea();
        commentArea.setPromptText("Enter your comments here...");
        commentArea.setWrapText(true);
        commentArea.setPrefRowCount(5);
        commentArea.setStyle("-fx-control-inner-background: #2e2e2e; -fx-text-fill: white; -fx-prompt-text-fill: gray; -fx-border-color: #555; -fx-font-size: 13px;");

        if (existingReview != null) {
            int existingRating = existingReview.getRating();
            if (existingRating >= 1 && existingRating <= 5) {
                for (RadioButton rb : ratingButtons) {
                    if (rb.getUserData().equals(existingRating)) {
                        rb.setSelected(true);
                        break;
                    }
                }
            }
            commentArea.setText(existingReview.getComment() != null ? existingReview.getComment() : "");
        }

        Button saveButton = new Button(existingReview == null ? "💾 Save Review" : "💾 Update Review");
        saveButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        Button cancelButton = new Button("❌ Cancel");
        cancelButton.setStyle("-fx-background-color: #555; -fx-text-fill: white; -fx-font-size: 13px;");

        saveButton.setOnAction(e -> {
            RadioButton selectedToggle = (RadioButton) ratingGroup.getSelectedToggle();
            if (selectedToggle == null) {
                showAlert("Missing Rating", "Please select a rating (1-5 stars).");
                return;
            }
            int rating = (int) selectedToggle.getUserData();
            String comment = commentArea.getText();

            Review reviewToSave;
            Timestamp now = Timestamp.from(Instant.now());

            try {
                if (existingReview != null) {
                    boolean ratingChanged = existingReview.getRating() != rating;
                    boolean commentChanged = !Objects.equals(existingReview.getComment(), comment == null || comment.trim().isEmpty() ? null : comment.trim());

                    if (!ratingChanged && !commentChanged) {
                        showAlert("No Changes", "The rating and comment are the same as the existing review.");
                        dialog.close();
                        return;
                    }
                    reviewToSave = new Review(currentUser, targetType, targetId, rating, comment, existingReview.getId());
                    System.out.println("Preparing to update review (creating new version linked to ID: " + existingReview.getId() + ")");
                } else {
                    reviewToSave = new Review(currentUser, targetType, targetId, rating, comment);
                    System.out.println("Preparing to save new review.");
                }
                reviewToSave.setTimestamp(now);

                databaseHelper.saveReview(reviewToSave);

                showAlert("Success", "Review saved successfully.");
                dialog.close();
                loadMyReviews(false); // Default: only latest

            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert("Database Error", "Failed to save review: " + ex.getMessage());
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Error", "An unexpected error occurred: " + ex.getMessage());
            }
        });

        cancelButton.setOnAction(e -> dialog.close());

        HBox buttonBox = new HBox(15, saveButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        layout.getChildren().addAll(ratingLabel, ratingBox, commentLabel, commentArea, buttonBox);

        Scene scene = new Scene(layout);
        setupDarkScene(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    
    /**
     * Shows the original review in a read-only dialog.
     *
     * @param reviewId The ID of the original review to display.
     */
    private void showOriginalReviewDialog(int reviewId) {
        try {
            Review original = databaseHelper.getReviewById(reviewId);
            if (original == null) {
                showAlert("Not Found", "Original review could not be found.");
                return;
            }

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Original Review (ID: " + reviewId + ")");

            Label rating = new Label("Rating: " + "★".repeat(original.getRating()) + "☆".repeat(5 - original.getRating()));
            rating.setStyle("-fx-text-fill: white;");

            Label timestamp = new Label("Reviewed on: " + original.getTimestamp().toString().substring(0, 19));
            timestamp.setStyle("-fx-text-fill: white;");

            TextArea comment = new TextArea(original.getComment());
            comment.setWrapText(true);
            comment.setEditable(false);
            comment.setStyle("-fx-control-inner-background: #2e2e2e; -fx-text-fill: white;");
            comment.setPrefHeight(150);

            VBox layout = new VBox(10, rating, timestamp, comment);
            layout.setPadding(new Insets(15));
            layout.setStyle("-fx-background-color: #1e1e1e;");
            Scene scene = new Scene(layout, 400, 250);
            setupDarkScene(scene);
            dialog.setScene(scene);
            dialog.showAndWait();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load the original review.");
        }
    }

    /**
     * Custom {@code ListCell} for displaying {@code Review} objects in the review list.
     */
    private class ReviewListCell extends ListCell<Review> {
        private VBox contentBox = new VBox(8);
        private Label targetLabel = new Label();
        private Label targetContentLabel = new Label();
        private Label ratingLabel = new Label();
        private Label commentLabel = new Label();
        private Label timestampLabel = new Label();
        private Label versionLabel = new Label();
        private HBox buttonBox = new HBox(10);
        private Button editButton = new Button("Update review");
        private Button deleteButton = new Button("Delete History");
        private Button viewTargetButton = new Button("View Answer");


        /**
         * Constructs a {@code ReviewListCell} object and initializes the UI elements.
         */
        public ReviewListCell() {
            super();
            targetLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: white;");
            targetContentLabel.setStyle("-fx-text-fill: lightblue; -fx-font-size: 12px; -fx-font-style: italic; -fx-wrap-text: true;");
            ratingLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: gold;");
            commentLabel.setStyle("-fx-text-fill: #dddddd; -fx-font-size: 13px; -fx-wrap-text: true;");
            timestampLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #aaaaaa;");
            versionLabel.setStyle("-fx-font-size: 11px; -fx-font-style: italic;");

            editButton.setStyle("-fx-font-size: 11px; -fx-background-color: #ffc107; -fx-text-fill: black;");
            deleteButton.setStyle("-fx-font-size: 11px; -fx-background-color: #dc3545; -fx-text-fill: white;");
            viewTargetButton.setStyle("-fx-font-size: 11px; -fx-background-color: #17a2b8; -fx-text-fill: white;");

            editButton.setOnAction(e -> {
                Review review = getItem();
                if (review != null && review.isLatest()) {
                     try {
                         Review latestVersion = databaseHelper.getLatestReviewByUserForTarget(
                                                    currentUser, review.getTargetType(), review.getTargetId());
                         if (latestVersion != null) {
                            showReviewDialog(latestVersion.getTargetType(), latestVersion.getTargetId(), latestVersion);
                         } else {
                             showAlert("Error", "Could not find the latest version of this review to edit.");
                             loadMyReviews(false); // Default: only latest
                         }
                     } catch (SQLException ex) {
                         showAlert("Database Error", "Failed to fetch latest review version: " + ex.getMessage());
                     }
                }
            });
            
            // delete button action
            deleteButton.setOnAction(e -> {
                Review review = getItem();
                if (review != null) {
                    confirmAndDeleteReviewHistory(review);
                }
            });
            
            // view answer details button action
            viewTargetButton.setOnAction(e -> {
                 Review review = getItem();
                 if(review != null) {
                     showTargetDetailsDialog(review.getTargetType(), review.getTargetId());
                 }
             });

            buttonBox.getChildren().addAll(editButton, deleteButton, viewTargetButton);
            buttonBox.setAlignment(Pos.CENTER_LEFT);

            contentBox.getChildren().addAll(targetLabel, targetContentLabel, ratingLabel, commentLabel, timestampLabel, versionLabel, buttonBox);
            contentBox.setPadding(new Insets(10));
            contentBox.setStyle("-fx-border-color: #444; -fx-border-width: 0 0 1 0;");
            setPadding(Insets.EMPTY);
             setStyle("-fx-background-color: #2e2e2e;");
        }

        /**
         * Updates the item displayed in the list cell.
         *
         * @param review The new item for the cell.
         * @param empty  Whether this cell represents data from an actual item. If true, there is no data.
         */
        @Override
        protected void updateItem(Review review, boolean empty) {
            super.updateItem(review, empty);
            
            if (empty || review == null) {
                setText(null);
                setGraphic(null);
                setStyle("-fx-background-color: #2e2e2e;");
            } else {
            	// target label
                targetLabel.setText(String.format("%s ID: %d",
                    Character.toUpperCase(review.getTargetType().charAt(0)) + review.getTargetType().substring(1),
                    review.getTargetId()));
                
                // Feedback count
                if ("answer".equalsIgnoreCase(review.getTargetType())) {
                    try {
                        int feedbackCount = databaseHelper.getFeedbackCountForAnswer(currentUser, review.getTargetId());
                        targetLabel.setText(targetLabel.getText() + " (💬 " + feedbackCount + " feedback)");
                    } catch (SQLException e) {
                        System.err.println("Error getting feedback count for answer " + review.getTargetId() + ": " + e.getMessage());
                    }
                }
                
                // star rating
                int ratingValue = review.getRating();
                ratingLabel.setText("Rating: " + "★".repeat(ratingValue) + "☆".repeat(5 - ratingValue));
                
                // comment
                String commentText = review.getComment();
                boolean hasComment = commentText != null && !commentText.trim().isEmpty();
                commentLabel.setText(hasComment ? commentText : "(No comment provided)");
                commentLabel.setManaged(hasComment);
                commentLabel.setVisible(hasComment);
                
                // time stamp
                timestampLabel.setText("Reviewed on: " + (review.getTimestamp() != null ? review.getTimestamp().toString().substring(0, 19) : "N/A"));
                
                // version label and edit state
                if (review.isLatest()) {
                    versionLabel.setText("(Latest Version)");
                    versionLabel.setStyle("-fx-font-size: 11px; -fx-font-style: italic; -fx-text-fill: #aaccaa;");
                    editButton.setDisable(false);
                    
                    // view original button for seeing the previous review before change
                    if (review.getPreviousReviewId() != null) {
                        Button viewOriginal = new Button("View Original");
                        viewOriginal.setStyle("-fx-font-size: 11px; -fx-background-color: #555; -fx-text-fill: white;");
                        viewOriginal.setOnAction(ev -> ReviewerDashboardGUI.this.showOriginalReviewDialog(review.getPreviousReviewId()));
                        buttonBox.getChildren().add(viewOriginal);
                    }
                } else {
                    versionLabel.setText(String.format("(Older Version - ID: %d)", review.getId()));
                    versionLabel.setStyle("-fx-font-size: 11px; -fx-font-style: italic; -fx-text-fill: #aaaaaa;");
                    editButton.setDisable(true);
                }
                versionLabel.setVisible(true);
                
                // load target content
                loadTargetContent(review.getTargetType(), review.getTargetId(), targetContentLabel);
                
                // clear and rebuild buttons
                buttonBox.getChildren().clear();

                // Add View Feedback button (only if target is an answer)
                if ("answer".equalsIgnoreCase(review.getTargetType())) {
                    Button viewFeedbackBtn = new Button("View Feedback");
                    viewFeedbackBtn.setStyle("-fx-font-size: 11px; -fx-background-color: #6f42c1; -fx-text-fill: white;");
                    viewFeedbackBtn.setOnAction(e -> showFeedbackForAnswer(review.getTargetId(), currentUser));
                    buttonBox.getChildren().add(viewFeedbackBtn);
                }

                // Add other buttons
                buttonBox.getChildren().addAll(editButton, deleteButton, viewTargetButton);

                setGraphic(contentBox);
                setStyle(getIndex() % 2 == 0 ? "-fx-background-color: #2e2e2e;" : "-fx-background-color: #383838;");
            }
        }

        /**
         * Confirms with the user before deleting a review and its history from the database.
         *
         * @param review The {@code Review} object to delete.
         */
        private void confirmAndDeleteReviewHistory(Review review) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Deletion");
            confirm.setHeaderText("Delete Review History?");
            confirm.setContentText(String.format(
                "Are you sure you want to delete this review and ALL its previous versions for %s ID %d?\nThis action cannot be undone.",
                review.getTargetType(), review.getTargetId()));
            setupDarkDialogPane(confirm.getDialogPane());

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    boolean success = databaseHelper.deleteReviewAndHistory(review.getId());

                    if (success) {
                        showAlert("Success", "Review history deleted successfully.");
                        loadMyReviews(false); // Default: only latest
                    } else {
                        showAlert("Deletion Note", "Could not delete the specified review ID, or history was already deleted.");
                        loadMyReviews(false); // Default: only latest
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showAlert("Database Error", "Failed to delete review history: " + ex.getMessage());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showAlert("Error", "An unexpected error occurred during deletion: " + ex.getMessage());
                }
            }
        }

        /**
         * Loads the content of the target being reviewed (e.g., question title, answer snippet) from the database.
         *
         * @param targetType  The type of the target being reviewed (e.g., "question", "answer").
         * @param targetId    The ID of the target being reviewed.
         * @param contentLabel The {@code Label} to display the content in.
         */
        private void loadTargetContent(String targetType, int targetId, Label contentLabel) {
            contentLabel.setText("Loading target info...");
            contentLabel.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
            try {
                String textToShow = "Target not found or error loading.";
                String style = "-fx-text-fill: red;";

                if ("question".equalsIgnoreCase(targetType)) {
                     Question question = questionSystemManager.getQuestionById(targetId);
                    if (question != null) {
                        textToShow = "Question Title: " + question.getTitle();
                        style = "-fx-text-fill: lightblue; -fx-font-size: 12px; -fx-font-style: italic; -fx-wrap-text: true;";
                    } else {
                         textToShow = "Target Question (ID: " + targetId + ") not found.";
                    }
                } else if ("answer".equalsIgnoreCase(targetType)) {
                     Answer answer = questionSystemManager.getAnswerById(targetId);
                    if (answer != null) {
                        String snippet = answer.getContent();
                        if (snippet != null && snippet.length() > 90) snippet = snippet.substring(0, 87).replace("\n", " ") + "...";
                        else if (snippet != null) snippet = snippet.replace("\n", " ");
                        else snippet = "(No content)";
                        textToShow = "Answer Snippet: \"" + snippet + "\"";
                         style = "-fx-text-fill: lightblue; -fx-font-size: 12px; -fx-font-style: italic; -fx-wrap-text: true;";
                    } else {
                         textToShow = "Target Answer (ID: " + targetId + ") not found.";
                    }
                }
                final String finalText = textToShow;
                final String finalStyle = style;
                 javafx.application.Platform.runLater(() -> {
                     contentLabel.setText(finalText);
                     contentLabel.setStyle(finalStyle);
                 });


            } catch (SQLException e) {
                e.printStackTrace();
                 javafx.application.Platform.runLater(() -> {
                    contentLabel.setText("Error loading target content.");
                    contentLabel.setStyle("-fx-text-fill: orange; -fx-font-style: italic;");
                 });
            } catch (Exception e) {
                 e.printStackTrace();
                 javafx.application.Platform.runLater(() -> {
                    contentLabel.setText("Unexpected error loading target.");
                    contentLabel.setStyle("-fx-text-fill: orange; -fx-font-style: italic;");
                 });
            }
        }

    }

    /**
     * Shows a dialog displaying the details of a specific target (question or answer).
     *
     * @param targetType The type of the target to display details for (e.g., "question", "answer").
     * @param targetId   The ID of the target to display details for.
     */
    private void showTargetDetailsDialog(String targetType, int targetId) {
        try {
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("Details for " + targetType.substring(0, 1).toUpperCase() + targetType.substring(1) + " ID: " + targetId);

            VBox dialogVBox = new VBox(15);
            dialogVBox.setPadding(new Insets(20));
            dialogVBox.setStyle("-fx-background-color: #1e1e1e;");

            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: transparent; -fx-background: #1e1e1e; -fx-border-color: #444;");

            VBox contentContainer = new VBox(10);
            contentContainer.setStyle("-fx-background-color: #2e2e2e; -fx-padding: 15px; -fx-border-radius: 5;");

            boolean found = false;
            if ("question".equalsIgnoreCase(targetType)) {
                 Question question = questionSystemManager.getQuestionById(targetId);
                if (question != null) {
                    found = true;
                    Label qTitle = new Label("Title: " + question.getTitle());
                    qTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white; -fx-wrap-text: true;");
                    Label qAuthor = new Label("Author: " + question.getAuthor());
                    qAuthor.setStyle("-fx-font-size: 12px; -fx-text-fill: #cccccc;");
                     String tagsString = (question.getTags() == null || question.getTags().isEmpty() || question.getTags().stream().allMatch(String::isEmpty))
                                         ? "None"
                                         : String.join(", ", question.getTags());
                    Label qTags = new Label("Tags: " + tagsString);
                    qTags.setStyle("-fx-font-size: 12px; -fx-text-fill: #cccccc;");

                    TextArea qContent = new TextArea("Content:\n" + (question.getContent() != null ? question.getContent() : ""));
                    qContent.setEditable(false); qContent.setWrapText(true);
                    qContent.setStyle("-fx-control-inner-background: #3a3a3a; -fx-text-fill: white; -fx-border-color: transparent; -fx-font-family: monospace;");
                    qContent.setPrefRowCount(10);
                    contentContainer.getChildren().addAll(qTitle, qAuthor, qTags, new Separator(), qContent);
                }
            } else if ("answer".equalsIgnoreCase(targetType)) {
                 Answer answer = questionSystemManager.getAnswerById(targetId);
                if (answer != null) {
                    found = true;
                    Label aDetails = new Label(String.format("Answer to Q:%d | Author: %s", answer.getQuestionId(), answer.getAuthor()));
                    aDetails.setStyle("-fx-font-size: 12px; -fx-text-fill: #cccccc;");
                    TextArea aContent = new TextArea("Content:\n" + (answer.getContent() != null ? answer.getContent() : ""));
                    aContent.setEditable(false); aContent.setWrapText(true);
                    aContent.setStyle("-fx-control-inner-background: #3a3a3a; -fx-text-fill: white; -fx-border-color: transparent; -fx-font-family: monospace;");
                    aContent.setPrefRowCount(10);
                    contentContainer.getChildren().addAll(aDetails, new Separator(), aContent);
                }
            }

            if (!found) {
                 contentContainer.getChildren().add(new Label("Target " + targetType + " with ID " + targetId + " not found.") {{ setStyle("-fx-text-fill: red; -fx-font-weight: bold;"); }});
            }

            scrollPane.setContent(contentContainer);

            Button closeButton = new Button("Close");
            closeButton.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white;");
            closeButton.setOnAction(e -> dialogStage.close());
            HBox buttonPane = new HBox(closeButton);
            buttonPane.setAlignment(Pos.CENTER_RIGHT);
            buttonPane.setPadding(new Insets(10,0,0,0));

            dialogVBox.getChildren().addAll(scrollPane, buttonPane);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);

            Scene dialogScene = new Scene(dialogVBox, 650, 500);
            setupDarkScene(dialogScene);
            dialogStage.setScene(dialogScene);
            dialogStage.showAndWait();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Could not load target content: " + e.getMessage());
        } catch (Exception e) {
             e.printStackTrace();
             showAlert("Error", "An unexpected error occurred showing target details: " + e.getMessage());
        }
    }

    private void setupDarkDialogPane(DialogPane dialogPane) {
        try {
            String cssPath = getClass().getResource("dark-theme.css").toExternalForm();
             if (cssPath != null) {
                dialogPane.getStylesheets().add(cssPath);
                dialogPane.getStyleClass().add("dialog-pane");
             } else {
                 System.err.println("Could not find dark-theme.css for dialog pane. Applying basic fallback.");
                 applyBasicDarkStyles(dialogPane);
             }
        } catch (Exception e) {
            System.err.println("Error loading dark-theme.css for dialog pane: " + e.getMessage() + ". Applying fallback.");
            applyBasicDarkStyles(dialogPane);
        }
    }

     private void applyBasicDarkStyles(Region region) {
         region.setStyle("-fx-background-color: #1e1e1e; -fx-border-color: #444;");
         region.lookupAll(".label").forEach(node -> node.setStyle("-fx-text-fill: white;"));
         region.lookupAll(".button").forEach(node -> node.setStyle("-fx-base: #333; -fx-text-fill: white;"));
         region.lookupAll(".text-field, .text-area").forEach(node -> node.setStyle(
             "-fx-control-inner-background: #2e2e2e; -fx-text-fill: white; -fx-prompt-text-fill: gray; -fx-border-color: #555;"
         ));
     }

     private void setupDarkScene(Scene scene) {
        try {
             String cssPath = getClass().getResource("dark-theme.css").toExternalForm();
             if (cssPath != null) {
                scene.getStylesheets().add(cssPath);
             } else {
                 System.err.println("Could not find dark-theme.css for scene. Applying fallback.");
                 if (scene.getRoot() != null && scene.getRoot() instanceof Region) {
                     applyBasicDarkStyles((Region) scene.getRoot());
                 }
             }
         } catch (Exception ex) {
             System.err.println("Error loading dark-theme.css for scene: " + ex.getMessage() + ". Applying fallback.");
             if (scene.getRoot() != null && scene.getRoot() instanceof Region) {
                 applyBasicDarkStyles((Region) scene.getRoot());
             }
         }
    }

    private void showAlert(String title, String message) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            if (title.toLowerCase().contains("error") || title.toLowerCase().contains("failed")) {
                alert.setAlertType(Alert.AlertType.ERROR);
            } else if (title.toLowerCase().contains("warning") || title.toLowerCase().contains("missing")) {
                 alert.setAlertType(Alert.AlertType.WARNING);
            }

            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            setupDarkDialogPane(alert.getDialogPane());
            alert.showAndWait();
        });
    }
    
    // allows for students to view profile as read only
    public static void showReadOnly(String reviewerUsername, String currentUser, DatabaseHelper dbHelper) {
        Stage stage = new Stage();
        stage.setTitle("Reviews by " + reviewerUsername);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        layout.setStyle("-fx-background-color: #1e1e1e;");

        Label title = new Label("Reviews written by " + reviewerUsername);
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        layout.getChildren().add(title);

        ListView<Review> listView = new ListView<>();
        listView.setStyle("-fx-control-inner-background: #2e2e2e;");
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Review review, boolean empty) {
                super.updateItem(review, empty);
                if (empty || review == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (!review.isLatest()) {
                        // Don't display outdated versions in the main list
                        setGraphic(null);
                        return;
                    }

                    VBox box = new VBox(6);
                    box.setPadding(new Insets(8));
                    box.setStyle("-fx-background-color: #2e2e2e; -fx-border-color: #444; -fx-border-width: 0 0 1 0;");

                    Label header = new Label("★ " + review.getRating() + " stars   " +
                                             review.getTimestamp().toString().substring(0, 19));
                    header.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");

                    Label comment = new Label(review.getComment() != null ? review.getComment() : "(no comment)");
                    comment.setWrapText(true);
                    comment.setStyle("-fx-text-fill: #dddddd;");

                    Label targetLabel = new Label("Loading...");
                    targetLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-style: italic;");

                    box.getChildren().addAll(header, comment, targetLabel);

                    //  View History Button
                    Button viewHistoryBtn = new Button("View History");
                    viewHistoryBtn.setStyle("-fx-background-color: #444; -fx-text-fill: white;");
                    viewHistoryBtn.setOnAction(e -> showReviewHistoryPopup(review.getTargetId(), review.getReviewerUsername(), dbHelper));
                    box.getChildren().add(viewHistoryBtn);

                    // Load target type
                    try {
                        String targetType = review.getTargetType();
                        int targetId = review.getTargetId();
                        if ("question".equalsIgnoreCase(targetType)) {
                            Question q = dbHelper.getQuestionById(targetId);
                            if (q != null) {
                                targetLabel.setText("Question: " + q.getTitle());
                            }
                        } else if ("answer".equalsIgnoreCase(targetType)) {
                            Answer a = dbHelper.getAnswerById(targetId);
                            if (a != null) {
                                String snippet = a.getContent();
                                if (snippet.length() > 80) snippet = snippet.substring(0, 77) + "...";
                                targetLabel.setText("Answer Snippet: " + snippet.replaceAll("\n", " "));
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        targetLabel.setText("Could not load target.");
                    }

                    setGraphic(box);
                }
            }
        });

        try {
            List<Review> reviews = dbHelper.getAllReviewsByReviewer(reviewerUsername);
            listView.getItems().addAll(reviews);
        } catch (Exception e) {
            e.printStackTrace();
            Alert error = new Alert(Alert.AlertType.ERROR, "Failed to load reviews.");
            error.showAndWait();
        }

        layout.getChildren().add(listView);

        Button close = new Button("Close");
        close.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white;");
        close.setOnAction(e -> stage.close());
        layout.getChildren().add(close);

        Scene scene = new Scene(layout, 600, 400);
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.showAndWait();
    }
    
    // shows review history
    private static void showReviewHistoryPopup(int answerId, String reviewer, DatabaseHelper db) {
        Stage stage = new Stage();
        stage.setTitle("Review History for Answer ID: " + answerId);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        layout.setStyle("-fx-background-color: #1e1e1e;");

        Label title = new Label("Older Versions:");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        layout.getChildren().add(title);

        try {
            List<Review> history = db.getReviewHistoryForAnswer(reviewer, answerId);
            for (Review rev : history) {
                if (!rev.isLatest()) {
                    VBox box = new VBox(5);
                    box.setPadding(new Insets(10));
                    box.setStyle("-fx-background-color: #2a2a2a; -fx-border-color: #666666;");

                    Label meta = new Label("★ " + rev.getRating() + " | " + rev.getTimestamp().toString().substring(0, 19));
                    meta.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

                    Label comment = new Label(rev.getComment());
                    comment.setStyle("-fx-text-fill: white;");

                    box.getChildren().addAll(meta, comment);
                    layout.getChildren().add(box);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Button closeBtn = new Button("Close");
        closeBtn.setOnAction(e -> stage.close());
        layout.getChildren().add(closeBtn);

        stage.setScene(new Scene(layout));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.show();
    }
    private void showProfile() {
        Stage profileStage = new Stage();
        profileStage.setTitle("Reviewer Profile - " + currentUser);
        profileStage.initModality(Modality.WINDOW_MODAL);
        profileStage.initOwner(reviewListView.getScene().getWindow());

        // Fetch data from database
        double avgRating = 0.0;
        try {
            avgRating = databaseHelper.getAverageRatingForReviewer(currentUser);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        List<Message> feedbackMessages = new ArrayList<>();
        try {
            feedbackMessages = databaseHelper.getFeedbackMessagesForReviewer(currentUser);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Build UI components
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

     // Left panel (avatar)
        VBox leftPanel = new VBox(20);
        leftPanel.setPrefWidth(200);
        leftPanel.setStyle("-fx-border-color: black; -fx-border-width: 3px; -fx-background-color: #1e1e1e;");

        // Load avatar image
        Image avatarImage = new Image(getClass().getResourceAsStream("/application/default_avatar.png"));
        ImageView imageView = new ImageView(avatarImage);
        imageView.setFitWidth(160);
        imageView.setFitHeight(240);
        imageView.setPreserveRatio(true);

        // Center the image
        HBox imageContainer = new HBox(imageView);
        imageContainer.setAlignment(Pos.CENTER);

        leftPanel.getChildren().add(imageContainer);

        
        // Right panel (details)
        VBox rightPanel = new VBox(15);
        Label nameLabel = new Label("Name: " + currentUser);
        nameLabel.setStyle("-fx-font-weight: bold;");

        // Star rating
        HBox starBox = new HBox(5);
        starBox.setAlignment(Pos.CENTER_LEFT);

        int filledStars = (int) Math.round(avgRating);
        for (int i = 0; i < 5; i++) {
            String starText = i < filledStars ? "*" : "_";
            Label starLabel = new Label(starText);
            starLabel.setStyle(
                "-fx-font-family: 'Consolas', monospace; " + 
                "-fx-font-size: 24px; " +
                "-fx-text-fill: " + (i < filledStars ? "#FFD700" : "#AAAAAA") + ";"
            );
            starBox.getChildren().add(starLabel);
        }
        Label avgLabel = new Label(String.format("Average Rating: %.1f", avgRating));

        // About Me section
        TextArea aboutMe = new TextArea();
        aboutMe.setWrapText(true);         
        
        try {
            String bio = databaseHelper.getBio(currentUser);
            aboutMe.setText(bio != null ? bio : "");
        } catch (SQLException e) {
            aboutMe.setText("[Failed to load bio]");
            e.printStackTrace();
        }
        
        Button saveBioBtn = new Button("✔ Save Bio");
        saveBioBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        saveBioBtn.setOnAction(e -> {
            try {
                databaseHelper.updateBio(currentUser, aboutMe.getText());
                showAlert("Success", "Bio updated successfully!");
            } catch (SQLException ex) {
                showAlert("Error", "Failed to update bio: " + ex.getMessage());
            }
        });
     
        
        HBox bioContainer = new HBox(10, aboutMe);
        rightPanel.getChildren().addAll(new Label("About Me:"), bioContainer);
        
        // Student reviews
        ListView<Message> feedbackList = new ListView<>(FXCollections.observableArrayList(feedbackMessages));
        feedbackList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Message msg, boolean empty) {
                super.updateItem(msg, empty);
                if (empty || msg == null) return;
                setText(String.format("%s: %s", msg.getSender(), msg.getContent()));
            }
        });
        

        rightPanel.getChildren().addAll(
                nameLabel,          // Username
                starBox,            // Star rating
                avgLabel,           // Average rating text
                aboutMe,            // Bio text area
                feedbackList,         // Reviews list
                saveBioBtn          // Save button at the bottom
            );
 
        // Combine panels
        HBox mainContent = new HBox(20, leftPanel, rightPanel);
        root.getChildren().add(mainContent);
        

        Scene scene = new Scene(root, 600, 400);
        profileStage.setScene(scene);
        profileStage.show();
    }
}