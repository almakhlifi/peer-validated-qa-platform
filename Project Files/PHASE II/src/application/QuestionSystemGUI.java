package application;

import databasePart1.DatabaseHelper;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class QuestionSystemGUI extends Application {
    private QuestionSystem manager = new QuestionSystem();
    private ObservableList<Question> questionList = FXCollections.observableArrayList();
    private String currentUser;
    private DatabaseHelper databaseHelper;
    private Stage previousStage;
    private String currentCategory = "All"; // Default to "All"

    private VBox questionDetailPanel;

    private TextField searchTextField;
    private Button searchButton;

    public QuestionSystemGUI(String user, DatabaseHelper databaseHelper, Stage previousStage) {
        this.currentUser = user;
        this.databaseHelper = databaseHelper;
        this.previousStage = previousStage;
        manager.databaseHelper = databaseHelper; //set databaseHelper here
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Discussion Board - Questions");
        BorderPane mainLayout = new BorderPane();

        HBox searchBar = new HBox(10);
        searchBar.setPadding(new Insets(10));
        searchBar.setAlignment(Pos.CENTER_LEFT);

        Label searchLabel = new Label("Search:");
        searchTextField = new TextField();
        searchTextField.setPromptText("Search questions...");
        searchButton = new Button("Search");

        searchBar.getChildren().addAll(searchLabel, searchTextField, searchButton);

        searchButton.setOnAction(e -> {
            String keyword = searchTextField.getText().trim();
            performSearch(keyword);
        });

        mainLayout.setTop(searchBar);

        ListView<Question> questionListView = createQuestionListView();
        VBox controlPanel = createControlPanel();

        questionDetailPanel = new VBox(10);
        questionDetailPanel.setPadding(new Insets(10));
        questionDetailPanel.setPrefWidth(400);

        questionListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                Question selected = questionListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showQuestionDetailInPanel(selected, questionDetailPanel);
                }
            }
        });

        Button backButton = new Button("Back to Home");
        backButton.setStyle("-fx-font-size: 14px; -fx-background-color: #2196F3; -fx-text-fill: white;");
        backButton.setOnAction(e -> {
            primaryStage.close();
            if (previousStage != null) {
                previousStage.show();
            } else {
                Stage studentHomeStage = new Stage();
                new StudentHomePage().show(studentHomeStage, databaseHelper, new User(currentUser, "", null));
            }
        });

        VBox bottomPanel = new VBox(10, backButton);
        bottomPanel.setPadding(new Insets(10));
        bottomPanel.setStyle("-fx-alignment: center;");

        HBox mainContent = new HBox(10, questionListView, questionDetailPanel);
        mainContent.setPadding(new Insets(10));

        mainLayout.setLeft(controlPanel);
        mainLayout.setCenter(mainContent);
        mainLayout.setBottom(bottomPanel);

        primaryStage.setScene(new Scene(mainLayout, 1000, 600));
        primaryStage.show();
        refreshQuestions();
    }

    private ListView<Question> createQuestionListView() {
        ListView<Question> listView = new ListView<>(questionList);
        listView.setCellFactory(lv -> new ListCell<Question>() {
            @Override
            protected void updateItem(Question question, boolean empty) {
                super.updateItem(question, empty);
                if (empty || question == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox tagsBox = new HBox(5);
                    for (String tag : question.getTags()) {
                        Label tagLabel = new Label(tag);
                        tagLabel.setStyle(getTagStyle(tag));
                        tagsBox.getChildren().add(tagLabel);
                    }

                    VBox cellContent = new VBox(2);
                    Label titleLabel = new Label(question.getTitle() + " by " + question.getAuthor());
                    cellContent.getChildren().addAll(titleLabel, tagsBox);

                    setGraphic(cellContent);
                    setText(null);
                }
            }
        });
        return listView;
    }

    private VBox createControlPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: #f9f9f9; -fx-padding: 15px; -fx-border-radius: 8px; -fx-border-color: #ddd;");

        Button newQuestionBtn = new Button("New Question");
        newQuestionBtn.setStyle("-fx-font-size: 16px; -fx-background-color: #007BFF; -fx-text-fill: white; -fx-font-weight: bold; "
                + "-fx-border-color: #0056b3; -fx-border-width: 1px; -fx-border-radius: 5px; -fx-padding: 8px;");
        newQuestionBtn.setMinWidth(180);

        Button refreshBtn = new Button("Refresh Questions");
        refreshBtn.setStyle("-fx-font-size: 14px; -fx-background-color: #ffffff; -fx-border-color: #ccc; "
                + "-fx-border-width: 1px; -fx-border-radius: 5px; -fx-padding: 5px;");
        refreshBtn.setMinWidth(180);

        Label filterLabel = new Label("CATEGORIES");
        filterLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333;");

        VBox categoryBox = new VBox(8);

        String[][] categories = {
                {"All", "#4a90e2"},
                {"Classwork", "#7ed321"},
                {"Exams", "#f5a623"},
                {"Project", "#d0021b"},
                {"Urgent", "#c0392b"},
                {"Meeting", "#8e44ad"},
                {"Question", "#2ecc71"}
        };

        for (String[] category : categories) {
            HBox categoryItem = new HBox(10);
            categoryItem.setAlignment(Pos.CENTER_LEFT);

            Label colorIndicator = new Label("  ");
            colorIndicator.setStyle("-fx-background-color: " + category[1] +
                    "; -fx-min-width: 12px; -fx-min-height: 12px; -fx-border-radius: 4px;");

            Label categoryLabel = new Label(category[0]);
            categoryLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333; -fx-padding: 4px 10px;");

            categoryLabel.setOnMouseClicked(e -> {
                currentCategory = category[0];
                filterQuestions(currentCategory);
                highlightSelectedCategory(categoryBox, categoryLabel);
            });

            categoryItem.getChildren().addAll(colorIndicator, categoryLabel);
            categoryBox.getChildren().add(categoryItem);
        }

        newQuestionBtn.setOnAction(e -> showCreateQuestionDialog());
        refreshBtn.setOnAction(e -> {
            refreshQuestions();
            filterQuestions(currentCategory);
        });

        panel.getChildren().addAll(newQuestionBtn, refreshBtn, filterLabel, categoryBox);
        return panel;
    }

    private void showCreateQuestionDialog() {
        Stage dialog = new Stage();
        dialog.setTitle("Create New Question");
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));

        TextField titleField = new TextField();
        TextArea contentArea = new TextArea();

        Button createBtn = new Button("Create Question");
        createBtn.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-color: #007bff; " +
                        "-fx-text-fill: white; " +
                        "-fx-border-color: black; " +
                        "-fx-border-width: 1px; " +
                        "-fx-padding: 8px 16px;"
        );

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-background-color: #ccc; " +
                        "-fx-text-fill: black; " +
                        "-fx-border-color: black; " +
                        "-fx-border-width: 1px; " +
                        "-fx-padding: 8px 16px;"
        );

        cancelBtn.setOnAction(e -> dialog.close());

        HBox buttonBox = new HBox(10, createBtn, cancelBtn);
        buttonBox.setAlignment(Pos.CENTER);

        Label tagsLabel = new Label("Tags:");
        FlowPane tagsButtonPane = new FlowPane(5, 5);
        tagsButtonPane.setPrefWrapLength(300);

        String[] availableTags = {"Classwork", "Exams", "Project", "Urgent", "Meeting", "Question"};
        ObservableList<String> selectedTags = FXCollections.observableArrayList();

        for (String tag : availableTags) {
            Button tagButton = new Button(tag);
            tagButton.setOnAction(evt -> {
                if (selectedTags.contains(tag)) {
                    selectedTags.remove(tag);
                    tagButton.setStyle("");
                } else {
                    selectedTags.add(tag);
                    tagButton.setStyle("-fx-background-color: lightblue;");
                }
            });
            tagsButtonPane.getChildren().add(tagButton);
        }

        VBox tagsInputArea = new VBox(5);
        tagsInputArea.getChildren().addAll(tagsLabel, tagsButtonPane);

        layout.getChildren().addAll(
                new Label("Title:"), titleField,
                new Label("Content:"), contentArea,
                tagsInputArea,
                buttonBox
        );

        createBtn.setOnAction(evt -> {
            String title = titleField.getText();
            String content = contentArea.getText();

            if (title == null || title.trim().isEmpty() || content == null || content.trim().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Missing Information");
                alert.setContentText("Please enter both a Title and Content for your question.");
                alert.showAndWait();
                return;
            }

            List<String> tags = new ArrayList<>(selectedTags);
            manager.createQuestion(title, content, currentUser, tags);
            refreshQuestions();
            dialog.close();
        });

        dialog.setScene(new Scene(layout, 400, 450));
        dialog.show();
    }


    private void showQuestionDetailInPanel(Question question, VBox questionDetailPanel) {
        questionDetailPanel.getChildren().clear();

        Label titleLabel = new Label("Title: " + question.getTitle());
        Label authorLabel = new Label("Author: " + question.getAuthor());
        TextArea contentArea = new TextArea(question.getContent());
        contentArea.setEditable(false);
        Label tagsLabel = new Label("Tags: " + String.join(", ", question.getTags()));

        // Answers section
        Label answersLabel = new Label("Answers:");
        ListView<Answer> answersListView = new ListView<>();
        answersListView.setPrefHeight(200); // Set a preferred height

        List<Answer> answers = manager.loadThreadedAnswersForQuestion(question.getId());
        answersListView.getItems().addAll(answers);

        answersListView.setCellFactory(lv -> new ListCell<Answer>() {
            @Override
            protected void updateItem(Answer answer, boolean empty) {
                super.updateItem(answer, empty);
                if (empty || answer == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox answerBox = createAnswerBox(answer, question.getId());
                    setGraphic(answerBox);
                }
            }
        });
        


        // Answer input section
        VBox answerSection = new VBox(5);
        Label answerLabel = new Label("Your Answer:");
        TextArea answerInput = new TextArea();
        answerInput.setPromptText("Write your answer...");
        answerInput.setPrefRowCount(3);
        Button submitAnswer = new Button("Post Answer");

        submitAnswer.setOnAction(e -> {
            String answerContent = answerInput.getText();
            if (answerContent != null && !answerContent.trim().isEmpty()) {
                Answer newAnswer = manager.createThreadedAnswer(answerContent, currentUser, question.getId(), null);
                List<Answer> updatedAnswers = manager.loadThreadedAnswersForQuestion(question.getId());
                answersListView.getItems().setAll(updatedAnswers);
                answerInput.clear();
            } else {
                // Show warning for empty answer
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Warning");
                alert.setHeaderText("Empty Answer");
                alert.setContentText("Please write an answer before submitting.");
                alert.showAndWait();
            }
        });

        answerSection.getChildren().addAll(answerLabel, answerInput, submitAnswer);
        
        HBox questionManagementBox = new HBox(10);
        if (question.getAuthor().equals(currentUser)) {
            Button editBtn = new Button("Edit Question");
            Button deleteBtn = new Button("Delete Question");

            editBtn.setOnAction(e -> showEditQuestionDialog(question));
            deleteBtn.setOnAction(e -> {
                Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
                confirmation.setTitle("Confirm Deletion");
                confirmation.setHeaderText("Are you sure you want to delete this question?");
                confirmation.setContentText("This action cannot be undone.");

                Optional<ButtonType> result = confirmation.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    boolean success = databaseHelper.deleteQuestion(question.getId());
                    if (success) {
                        questionList.removeIf(q -> q.getId() == question.getId());
                        refreshQuestions();
                        questionDetailPanel.getChildren().clear();
                    } else {
                        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                        errorAlert.setTitle("Error");
                        errorAlert.setHeaderText("Deletion Failed");
                        errorAlert.setContentText("Could not delete the question. Please try again.");
                        errorAlert.showAndWait();
                    }
                }
            });
            
            questionManagementBox.getChildren().addAll(editBtn, deleteBtn);
        }

        questionDetailPanel.getChildren().addAll(
                titleLabel, authorLabel, contentArea, tagsLabel,
                answersLabel, answersListView,
                questionManagementBox, answerSection
            );
    }

    
    
    // for updating answer dialog 
    
    private void showEditAnswerDialog(Answer answer, int questionId) {
        Stage dialog = new Stage();
        dialog.setTitle("Edit Answer");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));

        TextArea answerInput = new TextArea(answer.getContent());
        answerInput.setPrefRowCount(3);

        Button saveButton = new Button("Save Changes");
        Button cancelButton = new Button("Cancel");

        saveButton.setOnAction(e -> {
            String newContent = answerInput.getText().trim();
            if (!newContent.isEmpty()) {
                try {
                    boolean success = manager.updateAnswer(answer.getId(), newContent);
                    if (success) {
                        dialog.close();
                        refreshAnswers(questionId); // ✅ Refresh UI after edit
                    } else {
                        showAlert("Update Failed", "Could not update the answer. Please try again.");
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showAlert("Database Error", "An error occurred while updating the answer.");
                }
            } else {
                showAlert("Empty Answer", "Answer cannot be empty.");
            }
        });

        cancelButton.setOnAction(e -> dialog.close());

        HBox buttonBox = new HBox(10, saveButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        layout.getChildren().addAll(new Label("Edit Your Answer:"), answerInput, buttonBox);

        dialog.setScene(new Scene(layout, 400, 200));
        dialog.show();
    }

    
    
    
 // This method displays an alert box with a title and message.
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null); // No header text
        alert.setContentText(message);
        alert.showAndWait();
    }

    
    public List<Answer> loadThreadedAnswersForQuestion(int questionId) {
        try {
            return databaseHelper.loadThreadedAnswersForQuestion(questionId);
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }


    private void displayAnswers(VBox container, List<Answer> answers, int questionId) {
        container.getChildren().clear(); // Clear previous answers

        for (Answer answer : answers) {
            VBox answerBox = createAnswerBox(answer, questionId);
            container.getChildren().add(answerBox);
        }
    }

    private VBox createAnswerBox(Answer answer, int questionId) {
        VBox answerBox = new VBox(5);
        answerBox.setStyle("-fx-border-width: 1px; -fx-border-color: lightgray; -fx-padding: 5px; -fx-margin: 5px;");

        Label answerLabel = new Label(answer.getAuthor() + ": " + answer.getContent());
        answerLabel.setWrapText(true);

        Button replyButton = new Button("Reply");
        replyButton.setOnAction(e -> showReplyDialog(answer, questionId));

        Button editButton = new Button("Edit");
        
        // Only allow editing if the current user is the answer's author
        if (answer.getAuthor().equals(currentUser)) {
            editButton.setOnAction(e -> showEditAnswerDialog(answer, questionId));
        } else {
            editButton.setDisable(true);
        }

        HBox actionBox = new HBox(5, replyButton, editButton);
        actionBox.setAlignment(Pos.CENTER_LEFT);

        answerBox.getChildren().addAll(answerLabel, actionBox);

        // Add replies if any
        if (!answer.getReplies().isEmpty()) {
            ListView<Answer> repliesListView = new ListView<>();
            repliesListView.getItems().addAll(answer.getReplies());
            repliesListView.setCellFactory(lv -> new ListCell<Answer>() {
                @Override
                protected void updateItem(Answer reply, boolean empty) {
                    super.updateItem(reply, empty);
                    if (empty || reply == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        VBox replyBox = createAnswerBox(reply, questionId);
                        setGraphic(replyBox);
                    }
                }
            });
            answerBox.getChildren().add(repliesListView);
        }

        return answerBox;
    }



    private void showReplyDialog(Answer parentAnswer, int questionId) {
        Stage dialog = new Stage();
        dialog.setTitle("Reply to Answer");
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));

        TextArea replyInput = new TextArea();
        replyInput.setPromptText("Write your reply...");
        replyInput.setPrefRowCount(3);

        Button submitReply = new Button("Submit Reply");
        submitReply.setOnAction(e -> {
            String replyContent = replyInput.getText();
            if (replyContent != null && !replyContent.trim().isEmpty()) {
                Answer newReply = manager.createThreadedAnswer(replyContent, currentUser, questionId, parentAnswer.getId());
                parentAnswer.addReply(newReply);
                dialog.close();
                showQuestionDetailInPanel(questionList.stream()
                        .filter(q -> q.getId() == questionId)
                        .findFirst()
                        .orElse(null), questionDetailPanel);
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Warning");
                alert.setHeaderText("Empty Reply");
                alert.setContentText("Please write a reply before submitting.");
                alert.showAndWait();
            }
        });

        layout.getChildren().addAll(new Label("Your Reply:"), replyInput, submitReply);
        dialog.setScene(new Scene(layout, 300, 200));
        dialog.show();
    }


    private void showEditQuestionDialog(Question question) {
        Stage dialog = new Stage();
        dialog.setTitle("Edit Question");
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));

        TextField titleField = new TextField(question.getTitle());
        TextArea contentArea = new TextArea(question.getContent());

        Button saveBtn = new Button("Save Changes");
        Button cancelBtn = new Button("Cancel");

        saveBtn.setOnAction(e -> {
            String title = titleField.getText();
            String content = contentArea.getText();

            if (title == null || title.trim().isEmpty() || content == null || content.trim().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Missing Information");
                alert.setContentText("Please enter both a Title and Content for your question.");
                alert.showAndWait();
                return;
            }

            boolean success = manager.updateQuestion(question.getId(), title, content);
            if (success) {
                refreshQuestions();
                try {
                    Question updatedQuestion = databaseHelper.getQuestionById(question.getId());
                    if (questionDetailPanel != null && updatedQuestion != null) {
                        showQuestionDetailInPanel(updatedQuestion, questionDetailPanel);
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Update Failed");
                alert.setContentText("Could not update the question. Please try again.");
                alert.showAndWait();
            }
            dialog.close();
        });

        cancelBtn.setOnAction(e -> dialog.close());

        HBox buttonBox = new HBox(10, saveBtn, cancelBtn);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        layout.getChildren().addAll(
                new Label("Title:"), titleField,
                new Label("Content:"), contentArea,
                buttonBox
        );

        dialog.setScene(new Scene(layout, 400, 300));
        dialog.show();
    }

    private String getTagStyle(String tag) {
        return "-fx-background-color: " + getCategoryColor(tag) + "; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 12px; " +
                "-fx-padding: 2px 4px; " +
                "-fx-border-radius: 3px; " +
                "-fx-font-weight: normal;";
    }

    private void highlightSelectedCategory(VBox categoryBox, Label selectedLabel) {
        for (javafx.scene.Node node : categoryBox.getChildren()) {
            if (node instanceof HBox) {
                HBox hbox = (HBox) node;
                Label colorIndicator = (Label) hbox.getChildren().get(0);
                Label categoryLabel = (Label) hbox.getChildren().get(1);
                categoryLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333; -fx-padding: 4px 10px; -fx-border-color: transparent;");
                colorIndicator.setStyle("-fx-min-width: 12px; -fx-min-height: 12px; -fx-border-radius: 4px; "
                        + "-fx-background-color: " + getCategoryColor(categoryLabel.getText()) + ";");
            }
        }

        selectedLabel.setStyle(
                "-fx-font-size: 14px; -fx-text-fill: #333; -fx-padding: 4px 10px; " +
                        "-fx-border-color: #007bff; -fx-border-width: 2px; -fx-border-radius: 5px; " +
                        "-fx-background-color: rgba(0, 123, 255, 0.1);"
        );

        HBox selectedHBox = (HBox) selectedLabel.getParent();
        Label selectedColorIndicator = (Label) selectedHBox.getChildren().get(0);
        selectedColorIndicator.setStyle("-fx-min-width: 12px; -fx-min-height: 12px; -fx-border-radius: 4px; "
                + "-fx-background-color: " + getCategoryColor(selectedLabel.getText()) + ";");
    }

    private String getCategoryColor(String category) {
        switch (category.toLowerCase()) {
            case "all":
                return "#4a90e2";
            case "classwork":
                return "#7ed321";
            case "exams":
                return "#f5a623";
            case "project":
                return "#d0021b";
            case "urgent":
                return "#c0392b";
            case "meeting":
                return "#8e44ad";
            case "question":
                return "#2ecc71";
            default:
                return "#ccc";
        }
    }

    private void refreshQuestions() {
        try {
            questionList.setAll(manager.loadQuestions());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Update Answers 
    
    private void refreshAnswers(int questionId) {
        List<Answer> updatedAnswers = manager.loadThreadedAnswersForQuestion(questionId);
        showQuestionDetailInPanel(questionList.stream()
            .filter(q -> q.getId() == questionId)
            .findFirst()
            .orElse(null), questionDetailPanel);
    }


    private void filterQuestions(String tag) {
        try {
            List<Question> filteredQuestions = tag.equals("All")
                    ? manager.loadQuestions()
                    : manager.filterQuestionsByTag(tag);
            questionList.setAll(filteredQuestions);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ADDED CODE START: performSearch now calls the updated search method (which includes answers)
    private void performSearch(String keyword) {
        try {
            if (keyword.isEmpty()) {
                // If no search input, just load all questions
                questionList.setAll(manager.loadQuestions());
            } else {
                List<Question> results = databaseHelper.searchQuestions(keyword);
                questionList.setAll(results);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    // ADDED CODE END
}
