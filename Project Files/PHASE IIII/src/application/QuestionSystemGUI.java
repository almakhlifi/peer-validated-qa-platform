package application;

import databasePart1.DatabaseHelper;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.sql.SQLException;
import java.util.*;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * QuestionSystemGUI
 * 
 * Class is responsible for displaying questions and answers, editing questions and answers, filtering
 * questions, searching questions, creating new questions and answers, and interactions between reviewers and students.
 */
public class QuestionSystemGUI extends Application {

    private QuestionSystem manager = new QuestionSystem();
    private ObservableList<Question> questionList = FXCollections.observableArrayList();
    private String currentUser;
    private DatabaseHelper databaseHelper;
    private Stage previousStage;
    private String currentCategory = "All";
    private String activeRole; // checks the current role a user has selected
    private Stage mainStage;
    private VBox questionDetailPanel;
    private TextField searchTextField;
    private Button searchButton;
    private ComboBox<String> statusComboBox; // filtering for answered/unanswered questions

    private static final Set<String> trustedReviewers = new HashSet<>();
    
    // constructor
    public QuestionSystemGUI(String user, String activeRole, DatabaseHelper databaseHelper, Stage previousStage) {
        this.currentUser = user;
        this.activeRole = activeRole;
        this.databaseHelper = databaseHelper;
        this.previousStage = previousStage;
        manager.databaseHelper = databaseHelper;
    }

    @Override
    public void start(Stage primaryStage) {
    	this.mainStage = primaryStage;
    	
    	primaryStage.setTitle("Discussion Board - Questions");
        BorderPane mainLayout = new BorderPane();

        // Dark mode styling
        Scene scene = new Scene(mainLayout, 1000, 600);
        scene.getStylesheets().add(getClass().getResource("dark-theme.css").toExternalForm());
        primaryStage.setScene(scene);

        // Top Bar: includes search on left, "Trusted Reviewers" on right
        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(10));
        topBar.setAlignment(Pos.CENTER_LEFT);

        // Left side: Search bar
        Label searchLabel = new Label("Search:");
        searchTextField = new TextField();
        searchTextField.setPromptText("Search questions...");
        searchButton = new Button("Search");
        searchButton.setOnAction(e -> {
            String keyword = searchTextField.getText().trim();
            performSearch(keyword);
        });
        
        // Status filter dropdown
        Label statusLabel = new Label("Status:");
        statusComboBox = new ComboBox<>();
        statusComboBox.getItems().addAll("All", "Answered", "Unanswered");
        statusComboBox.setValue("All");
        statusComboBox.setStyle("-fx-background-color: #2e2e2e; -fx-text-fill: white;");

        // Trigger re-filtering when changed
        statusComboBox.setOnAction(e -> filterQuestions(currentCategory, statusComboBox.getValue()));

        // Filler region to push the trustedReviewersBtn to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Right side: "Trusted Reviewers" button
        Button trustedReviewersBtn = new Button("Trusted Reviewers");
        // Make it gold colored
        trustedReviewersBtn.setStyle("-fx-background-color: gold; -fx-text-fill: black; -fx-font-weight: bold;");
        trustedReviewersBtn.setOnAction(e -> {
            // Show a simple pop-up listing all trusted reviewers
        	new TrustedReviewersGUI(trustedReviewers, currentUser, databaseHelper).show();
        });
        
        // View all reviewers button
        Button allReviewersBtn = new Button("All Reviewers");
        allReviewersBtn.setStyle("-fx-background-color: darkgray; -fx-text-fill: black; -fx-font-weight: bold;");
        allReviewersBtn.setOnAction(e -> {
            ReviewerDirectoryGUI reviewerDirectory = new ReviewerDirectoryGUI(currentUser, databaseHelper);
            reviewerDirectory.show();
        });
        
        // only allow students to have trusted reviewer list
        topBar.getChildren().addAll(searchLabel, searchTextField, searchButton, statusLabel, statusComboBox, spacer);

        if ("student".equalsIgnoreCase(activeRole)) {
        	topBar.getChildren().addAll(trustedReviewersBtn, allReviewersBtn); // adds buttons if student role selected
        }
        
        mainLayout.setTop(topBar);

        // Left: List of questions and control panel
        ListView<Question> questionListView = createQuestionListView();
        VBox controlPanel = createControlPanel();

        // Right: Question detail panel
        questionDetailPanel = new VBox(10);
        questionDetailPanel.setPadding(new Insets(10));
        questionDetailPanel.setStyle("-fx-background-color: #1e1e1e;");
        questionDetailPanel.setPrefWidth(400);

        // Placeholder
        VBox placeholderBox = new VBox(10);
        placeholderBox.setAlignment(Pos.CENTER);
        placeholderBox.setPrefHeight(400);
        placeholderBox.setStyle("-fx-padding: 100 0 0 0;");
        Label iconLabel = new Label("💬");
        iconLabel.setStyle("-fx-font-size: 40px; -fx-text-fill: #888;");
        Label textLabel = new Label("Select a question");
        textLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #888;");
        placeholderBox.getChildren().addAll(iconLabel, textLabel);
        questionDetailPanel.getChildren().add(placeholderBox);

        // When a question is clicked, display details
        questionListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                Question selected = questionListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showQuestionDetailInPanel(selected, questionDetailPanel);
                }
            }
        });

        // Back button at the bottom
        Button backButton = new Button("Back");
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

        // Main layout
        HBox mainContent = new HBox(10, questionListView, questionDetailPanel);
        mainContent.setPadding(new Insets(10));
        mainLayout.setLeft(controlPanel);
        mainLayout.setCenter(mainContent);
        mainLayout.setBottom(bottomPanel);

        primaryStage.show();
        filterQuestions("All", "All");
    }
    
    /*
     * These two methods are responsible for displaying the question list, the left 
     * side control panel with categories, the new question button, and the create question pop up
     */

    // Create a ListView for questions
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
                        // Use the same color scheme as the category panel
                        tagLabel.setStyle(getTagStyle(tag));
                        tagsBox.getChildren().add(tagLabel);
                    }
                    VBox cellContent = new VBox(2);
                    Label titleLabel = new Label(question.getTitle() + " by " + question.getAuthor());
                    titleLabel.setStyle("-fx-text-fill: white;");
                    titleLabel.setMaxWidth(240);
                    titleLabel.setEllipsisString("...");
                    titleLabel.setWrapText(false);
                    VBox titleContainer = new VBox(2);
                    titleContainer.getChildren().add(titleLabel);
                    
                    // Show unread count as bell notification if current user is author
                    if (question.getAuthor().equals(currentUser)) {
                        try {
                            int unreadCount = databaseHelper.getUnreadMessageCount(currentUser, question.getId(), -1);
                            if (unreadCount > 0) {
                                Label unreadLabel = new Label("🔔 " + unreadCount);
                                unreadLabel.setStyle("-fx-text-fill: orange; -fx-font-size: 12px; -fx-font-weight: bold;");
                                titleContainer.getChildren().add(unreadLabel);
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    
                    cellContent.getChildren().addAll(titleContainer, tagsBox);
                    setGraphic(cellContent);
                    setText(null);
                }
            }
        });
        return listView;
    }

    // Left control panel with categories and new question button
    private VBox createControlPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: #1e1e1e; -fx-padding: 15px; -fx-border-radius: 8px; -fx-border-color: #444;");
        
        // new question button
        Button newQuestionBtn = new Button("New Question");
        newQuestionBtn.setStyle("-fx-font-size: 16px; -fx-background-color: #007BFF; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-color: #0056b3; -fx-border-width: 1px; -fx-border-radius: 5px; -fx-padding: 8px;");
        newQuestionBtn.setMinWidth(180);
        
        // categories label
        Label filterLabel = new Label("CATEGORIES");
        filterLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        // box for each category for filtering
        VBox categoryBox = new VBox(8);
        String[][] categories = {
            {"All", "#4a90e2"},
            {"Classwork", "#7ed321"},
            {"Exams", "#f5a623"},
            {"Project", "#d0021b"},
            {"Urgent", "#c0392b"},
            {"Meeting", "#8e44ad"},
            {"Question", "#151ec0"}
        };

        for (String[] category : categories) {
            HBox categoryItem = new HBox(10);
            categoryItem.setAlignment(Pos.CENTER_LEFT);
            
            // color for category
            Label colorIndicator = new Label("  ");
            colorIndicator.setStyle("-fx-background-color: " + category[1]
                    + "; -fx-min-width: 12px; -fx-min-height: 12px; -fx-border-radius: 4px;");
            
            // name of category
            Label categoryLabel = new Label(category[0]);
            categoryLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: white; -fx-padding: 4px 10px;");
            
            // filters questions after clicking on category
            categoryLabel.setOnMouseClicked(e -> {
                currentCategory = category[0];
                String selectedStatus = statusComboBox.getValue();
                filterQuestions(currentCategory, selectedStatus);
                highlightSelectedCategory(categoryBox, categoryLabel);
            });

            categoryItem.getChildren().addAll(colorIndicator, categoryLabel);
            categoryBox.getChildren().add(categoryItem);
        }
        
        // new question button opens pop up
        newQuestionBtn.setOnAction(e -> showCreateQuestionDialog());

        panel.getChildren().addAll(newQuestionBtn, filterLabel, categoryBox);
        return panel;
    }

    // Create new question pop up
    private void showCreateQuestionDialog() {
        Stage dialog = new Stage();
        dialog.setTitle("Create New Question");
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        layout.setStyle("-fx-background-color: #1e1e1e;");
        
        // title area for new question
        Label titleLabel = new Label("Title:");
        titleLabel.setStyle("-fx-text-fill: white;");
        TextField titleField = new TextField();
        titleField.setStyle("-fx-control-inner-background: #2e2e2e; -fx-text-fill: white; -fx-border-color: #444;");
        
        // content area for new question contenet
        Label contentLabel = new Label("Content:");
        contentLabel.setStyle("-fx-text-fill: white;");
        TextArea contentArea = new TextArea();
        contentArea.setPrefRowCount(10);
        contentArea.setStyle("-fx-control-inner-background: #2e2e2e; -fx-text-fill: white; -fx-border-color: #444;");
        
        // tag selection
        Label tagsLabel = new Label("Tags:");
        tagsLabel.setStyle("-fx-text-fill: white;");
        HBox tagButtons = new HBox(5);
        Set<String> selectedTags = new HashSet<>();
        String[] tagNames = {"Classwork", "Exams", "Project", "Urgent", "Meeting", "Question"};
        for (String tag : tagNames) {
            Button tagButton = new Button(tag);
            tagButton.setStyle("-fx-background-color: #dddddd; -fx-font-weight: bold;");
            tagButton.setOnAction(e -> {
                if (selectedTags.contains(tag)) {
                    selectedTags.remove(tag);
                    tagButton.setStyle("-fx-background-color: #dddddd; -fx-font-weight: bold;");
                } else {
                    selectedTags.add(tag);
                    tagButton.setStyle("-fx-background-color: #6c63ff; -fx-text-fill: white; -fx-font-weight: bold;");
                }
            });
            tagButtons.getChildren().add(tagButton);
        }
        
        // create button
        Button createButton = new Button("Create Question");
        createButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        createButton.setOnAction(e -> {
            String title = titleField.getText().trim();
            String content = contentArea.getText().trim();
            if (!title.isEmpty() && !content.isEmpty()) {
                Question newQuestion = new Question(title, content, currentUser, new ArrayList<>(selectedTags));
                try {
                    databaseHelper.saveQuestion(newQuestion);
                    refreshQuestions();
                    dialog.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showAlert("Database Error", "Failed to save the new question.");
                }
            } else {
                showAlert("Missing Info", "Please fill in both title and content.");
            }
        });
        
        // cancel button
        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle("-fx-background-color: #555555; -fx-text-fill: white;");
        cancelButton.setOnAction(e -> dialog.close());
        
        // box for both buttons
        HBox buttonBox = new HBox(10, createButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        layout.getChildren().addAll(titleLabel, titleField, contentLabel, contentArea, tagsLabel, tagButtons, buttonBox);

        Scene scene = new Scene(layout, 520, 450);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    
    /*
     * Important method for showing question details upon selecting in the question list.
     * Responsible for showing question title, author, content, send feedback button, answer
     * area, and send answer button.
     * 
     * If the current user is the author, they are allowed edit question button, delete question, and
     * check inbox. 
     */

    // Show question detail on the right panel
    private void showQuestionDetailInPanel(Question question, VBox questionDetailPanel) {
        questionDetailPanel.getChildren().clear();

        // Title
    	Label titleLabel = new Label(question.getTitle());
    	titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
    	
    	// author and send feedback button (if not author)
        HBox authorBox = new HBox(8);
        Label authorLabel = new Label("by " + question.getAuthor());
        authorLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #cccccc;");

        // If current user != author, show "Send Feedback" button
        if (!currentUser.equals(question.getAuthor()) && !"staff".equalsIgnoreCase(activeRole)) {
            Button msgBtn = new Button("💬 Send Feedback");
            msgBtn.setStyle("-fx-background-color: #6c63ff; -fx-text-fill: white; -fx-font-weight: bold;");
            msgBtn.setTooltip(new Tooltip("Send private feedback to " + question.getAuthor()));
            
            msgBtn.setOnAction(e -> {
                showFeedbackPopup(question.getAuthor(), question.getId(), -1);
            });
            
            authorBox.getChildren().addAll(authorLabel, msgBtn);
        } else {
            authorBox.getChildren().add(authorLabel);
        }

        questionDetailPanel.getChildren().addAll(titleLabel, authorBox);
        
        // question content area
        TextArea contentArea = new TextArea(question.getContent());
        contentArea.setEditable(false);
        contentArea.setWrapText(true);
        contentArea.setStyle("-fx-control-inner-background: #2e2e2e; -fx-text-fill: white;"
                + " -fx-border-color: #444; -fx-border-radius: 4; -fx-font-size: 13px;");

        HBox tagBox = new HBox(5);
        for (String tag : question.getTags()) {
            Label tagLabel = new Label(tag);
            tagLabel.setStyle("-fx-background-color: " + getColorForTag(tag)
                    + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 2 6 2 6; -fx-background-radius: 4;");
            tagBox.getChildren().add(tagLabel);
        }

        Label answersLabel = new Label("Answers:");
        answersLabel.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");

        // Slightly taller for answer box
        ListView<Answer> answersListView = new ListView<>();
        answersListView.setStyle("-fx-control-inner-background: #1e1e1e; -fx-background-color: #1e1e1e;");
        answersListView.setPrefHeight(210);

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
        
        // answer area
        VBox answerSection = new VBox(5);
        Label answerLabel = new Label("Your Answer:");
        answerLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        // field for writing answer
        TextArea answerInput = new TextArea();
        answerInput.setPromptText("Write your answer...");
        answerInput.setStyle("-fx-control-inner-background: #2e2e2e; -fx-text-fill: white;"
                + " -fx-border-color: #555; -fx-border-radius: 4;"
                + " -fx-prompt-text-fill: #ffffff;");

        answerInput.setPrefRowCount(3);
        
        // submit answer button
        Button submitAnswer = new Button("Post Answer");
        submitAnswer.setStyle("-fx-background-color: #6c63ff; -fx-text-fill: white;");
        submitAnswer.setOnAction(e -> {
            String answerContent = answerInput.getText();
            if (answerContent != null && !answerContent.trim().isEmpty()) {
                Answer newAnswer = manager.createThreadedAnswer(answerContent, currentUser, question.getId(), null);
                // Refresh the question detail panel after posting
                try {
                    Question updatedQ = databaseHelper.getQuestionById(question.getId());
                    showQuestionDetailInPanel(updatedQ, questionDetailPanel);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                answerInput.clear();
            } else {
                showAlert("Warning", "Please write an answer before submitting.");
            }
        });
        
        // hide answer label, input, and submit answer button for staff
        if (!"staff".equalsIgnoreCase(activeRole)) {
            answerSection.getChildren().addAll(answerLabel, answerInput, submitAnswer);
        }

        // Management box for question
        HBox questionManagementBox = new HBox(10);
        if (question.getAuthor().equals(currentUser) && !"staff".equalsIgnoreCase(activeRole)) {
            // edit question button
        	Button editBtn = new Button("Edit Question");
            editBtn.setStyle("-fx-background-color: white; -fx-text-fill: black;");
            editBtn.setOnAction(e -> showEditQuestionDialog(question));
            
            // edit question button
            Button deleteBtn = new Button("Delete Question");
            deleteBtn.setStyle("-fx-background-color: white; -fx-text-fill: black;");
            deleteBtn.setOnAction(e -> {
                Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
                // messages
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
                        showAlert("Error", "Could not delete the question. Please try again.");
                    }
                }
            });
            
            // inbox button
            Button inboxBtn = new Button("🔔 Check Inbox");
            inboxBtn.setStyle("-fx-background-color: orange; -fx-text-fill: white;");
            inboxBtn.setOnAction(e -> {
                InboxGUI.show(currentUser, databaseHelper, mainStage);
            });

            questionManagementBox.getChildren().addAll(inboxBtn, editBtn, deleteBtn);
        }

        // Flag Question button (only shown for staff)
        if ("staff".equalsIgnoreCase(activeRole)) {
        	List<Flag> existingFlags = databaseHelper.getAllFlags().stream()
        		    .filter(f -> f.getItemId().equals(String.valueOf(question.getId())) &&
        		                 f.getType() == Flag.FlagType.QUESTION)
        		    .collect(Collectors.toList());

            // Check if there’s an unresolved flag for this question
            boolean unresolvedFlag = existingFlags.stream()
                .anyMatch(f -> f.getType() == Flag.FlagType.QUESTION &&
                               !databaseHelper.isFlagResolved(f.getItemId(), f.getType()));
            
            // flag question button
            Button flagBtn = new Button(unresolvedFlag ? "🚩 Flagged" : "🚩 Flag Question");
            flagBtn.setStyle("-fx-background-color: #ac280c; -fx-text-fill: white;");
            flagBtn.setDisable(unresolvedFlag);

            if (!unresolvedFlag) {
                flagBtn.setOnAction(e -> {
                    showFlagDialog(Flag.FlagType.QUESTION, String.valueOf(question.getId()), "question");
                    // Force UI to update after flagging
                    try {
                        Question updated = databaseHelper.getQuestionById(question.getId());
                        showQuestionDetailInPanel(updated, questionDetailPanel);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }

            questionManagementBox.getChildren().add(flagBtn);
        }
        
        // view message thread for staff
        if ("staff".equalsIgnoreCase(activeRole)) {
            Button viewMessagesBtn = new Button("📩 View Message Thread");
            viewMessagesBtn.setStyle("-fx-background-color: #00aaff; -fx-text-fill: white; -fx-font-weight: bold;");
            viewMessagesBtn.setTooltip(new Tooltip("View all private messages about this question"));

            viewMessagesBtn.setOnAction(e -> {
                InboxGUI.showThreadForQuestion(currentUser, databaseHelper, mainStage, question.getId());
            });

            questionManagementBox.getChildren().add(viewMessagesBtn);
        }
        
        // check for staff to hide answer section
        questionDetailPanel.getChildren().addAll(
        	contentArea, tagBox, questionManagementBox, answersLabel, answersListView 
        );
        if (!"staff".equalsIgnoreCase(activeRole)) {
        	questionDetailPanel.getChildren().add(answerSection);
        }
    }
    
    /*
     * These two methods are responsible for showing answers, reply, edit, mark as accepted,
     * view reviews, and write reviews buttons, and pop up window for showing reviews
     */

    // Create the UI for an individual answer
    private VBox createAnswerBox(Answer answer, int questionId) {
        VBox answerBox = new VBox(5);

        // default box styling
        answerBox.setStyle("-fx-border-width: 1px; -fx-border-color: #444;"
                + " -fx-padding: 8px; -fx-background-color: #2e2e2e;"
                + " -fx-background-radius: 5;");
        
        // shows answer author
        Label authorLabel = new Label(answer.getAuthor());
        authorLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");

        HBox authorRow = new HBox(6);
        authorRow.setAlignment(Pos.CENTER_LEFT);
        authorRow.getChildren().add(authorLabel);
        
        // shows answer content
        Label contentLabel = new Label(answer.getContent());
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-text-fill: white;");
        
        // reply button
        Button replyButton = new Button("Reply");
        replyButton.setOnAction(e -> showReplyDialog(answer, questionId));
        
        // edit button
        Button editButton = new Button("Edit");
        if (answer.getAuthor().equals(currentUser)) {
            editButton.setOnAction(e -> showEditAnswerDialog(answer, questionId));
        } else {
            editButton.setDisable(true);
        }

        // Mark as Correct button
        Button markAsAcceptedButton = new Button("✔ Mark as Correct");
        Question relatedQuestion;
        try {
            relatedQuestion = databaseHelper.getQuestionById(questionId);
        } catch (SQLException e) {
            e.printStackTrace();
            relatedQuestion = null;
        }

        if (relatedQuestion != null && relatedQuestion.getAuthor().equals(currentUser)) {
        	final int qId = questionId;
        	final int aId = answer.getId();

        	markAsAcceptedButton.setOnAction(e -> {
        	    try {
        	        boolean success = manager.markAnswerAsAccepted(qId, aId, currentUser);

                    if (success) {
                        refreshAnswers(questionId);
                    } else {
                        showAlert("Error", "Failed to mark answer as correct.");
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showAlert("Database Error", "An error occurred while updating the accepted answer.");
                }
            });
        } else {
            markAsAcceptedButton.setVisible(false);
        }

        // View reviews button (and write reviews if reviewer role selected)
        HBox reviewBox = new HBox(5);
        
        // view reviews button (hidden for staff)
        if (!"staff".equalsIgnoreCase(activeRole)) {
            Button viewReviewsBtn = new Button("View Reviews");
            viewReviewsBtn.setOnAction(e -> showReviewsForAnswer(answer));
            reviewBox.getChildren().add(viewReviewsBtn);
        }
        
        //checks if the user's active role is reviewer
        if ("reviewer".equalsIgnoreCase(activeRole)) {
            Button writeReviewBtn = new Button("Write Review");
            writeReviewBtn.setStyle("-fx-background-color: green; -fx-text-fill: white;");
            writeReviewBtn.setOnAction(e -> showWriteReviewDialog(answer));
            reviewBox.getChildren().add(writeReviewBtn);
        }

        reviewBox.setAlignment(Pos.CENTER_LEFT);
        
        // box for reply, edit, and mark as accepted button (empty for staff role)
        HBox actionBox;
        if (!"staff".equalsIgnoreCase(activeRole)) {
            actionBox = new HBox(5, replyButton, editButton, markAsAcceptedButton);
        } else {
            actionBox = new HBox(); // empty for staff
        }
        actionBox.setAlignment(Pos.CENTER_LEFT);
        
        // checks if answer is marked as accepted
        boolean isAccepted = relatedQuestion != null 
                && relatedQuestion.getAcceptedAnswerId() != null 
                && relatedQuestion.getAcceptedAnswerId().equals(answer.getId());

        if (isAccepted) {
            answerBox.setStyle("-fx-border-color: green; -fx-border-width: 2px; -fx-background-color: #224422;"); // darker green
        }
        
        // add check mark if accepted
        if (isAccepted) {
            Label checkmark = new Label("✔");
            checkmark.setStyle("-fx-text-fill: limegreen; -fx-font-size: 14px; -fx-font-weight: bold;");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            authorRow.getChildren().addAll(spacer, checkmark);
        }
        
        // button for flagging answer (only for staff)
        if ("staff".equalsIgnoreCase(activeRole)) {
        	List<Flag> existingFlags = databaseHelper.getAllFlags().stream()
        		    .filter(f -> f.getItemId().equals(String.valueOf(answer.getId())) &&
        		                 f.getType() == Flag.FlagType.ANSWER)
        		    .collect(Collectors.toList());

            // Only consider unresolved flags
            boolean unresolvedFlag = existingFlags.stream()
                .anyMatch(f -> f.getType() == Flag.FlagType.ANSWER &&
                               !databaseHelper.isFlagResolved(f.getItemId(), f.getType()));
            
            // flag answer button
            Button flagAnswerBtn = new Button(unresolvedFlag ? "🚩 Flagged" : "🚩 Flag Answer");
            flagAnswerBtn.setStyle("-fx-background-color: #ac280c; -fx-text-fill: white;");
            flagAnswerBtn.setDisable(unresolvedFlag);

            if (!unresolvedFlag) {
                flagAnswerBtn.setOnAction(e -> {
                    showFlagDialog(Flag.FlagType.ANSWER, String.valueOf(answer.getId()), "answer");
                    try {
                        Question updated = databaseHelper.getQuestionById(answer.getQuestionId());
                        showQuestionDetailInPanel(updated, questionDetailPanel);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }

            answerBox.getChildren().add(flagAnswerBtn);
        }

        answerBox.getChildren().addAll(authorRow, contentLabel, actionBox, reviewBox);

        // Shows threaded replies
        if (!answer.getReplies().isEmpty()) {
            ListView<Answer> repliesListView = new ListView<>();
            repliesListView.getItems().addAll(answer.getReplies());
            repliesListView.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Answer reply, boolean empty) {
                    super.updateItem(reply, empty);
                    if (empty || reply == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setGraphic(createAnswerBox(reply, questionId));
                    }
                }
            });
            answerBox.getChildren().add(repliesListView);
        }

        return answerBox;
    }

    // Show reviews for an answer on button click
    private void showReviewsForAnswer(Answer answer) {
        Stage reviewStage = new Stage();
        reviewStage.setTitle("Reviews for Answer by " + answer.getAuthor());

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        layout.setStyle("-fx-background-color: #1e1e1e;");
        
        // header
        Label header = new Label("Reviews for this answer:");
        header.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        layout.getChildren().add(header);

        List<Review> reviews;
        try {
            reviews = databaseHelper.getLatestReviewsForAnswer(answer.getId());
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load reviews.");
            return;
        }

        if (reviews.isEmpty()) {
            Label noReviewsLabel = new Label("No reviews found for this answer.");
            noReviewsLabel.setStyle("-fx-text-fill: white;");
            layout.getChildren().add(noReviewsLabel);
        } else {
            for (Review review : reviews) {
                VBox reviewBox = new VBox(5);
                reviewBox.setStyle("-fx-background-color: #2e2e2e; -fx-border-color: #444; -fx-border-width: 1px; -fx-padding: 5px;");
                
                // shows reviewer
                Label reviewerLabel = new Label("Reviewer: " + review.getReviewerUsername());
                reviewerLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                
                // shows rating of review
                Label ratingLabel = new Label("Rating: " + review.getRating() + "★");
                ratingLabel.setStyle("-fx-text-fill: white;");
                
                // shows comment of review
                Label commentLabel = new Label("Comment: " + review.getComment());
                commentLabel.setWrapText(true);
                commentLabel.setStyle("-fx-text-fill: white;");

                HBox buttonBox = new HBox(10);

                // Add to trusted reviewer button (only for students)
                if ("student".equalsIgnoreCase(activeRole)) {
                	
                	// add to trusted button
                	Button addTrustedBtn = new Button("Add to Trusted");
                	addTrustedBtn.setStyle("-fx-background-color: gold; -fx-text-fill: black; -fx-font-weight: bold;");
                	addTrustedBtn.setOnAction(e -> {
                	    try {
                	        databaseHelper.addOrUpdateTrustedReviewer(currentUser, review.getReviewerUsername(), 1); // default weight
                	        showAlert("Trusted Reviewer", "Reviewer " + review.getReviewerUsername() + " added to your trusted list!");
                	    } catch (SQLException ex) {
                	        ex.printStackTrace();
                	        showAlert("Error", "Failed to add reviewer to trusted list.");
                	    }
                	});
                	
                 	buttonBox.getChildren().add(addTrustedBtn);
             	}
                
                // Send feedback button (only for students to reviewers)
                if ("student".equalsIgnoreCase(activeRole)) {
                    Button feedbackBtn = new Button("Send Feedback");
                    feedbackBtn.setStyle("-fx-background-color: #6c63ff; -fx-text-fill: white;");
                    feedbackBtn.setOnAction(ev -> {
                        showFeedbackPopup(review.getReviewerUsername(), answer.getQuestionId(), answer.getId());
                    });
                    buttonBox.getChildren().add(feedbackBtn);
                }

                reviewBox.getChildren().addAll(reviewerLabel, ratingLabel, commentLabel, buttonBox);
                layout.getChildren().add(reviewBox);
            }
        }
        
        // close button
        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #6c63ff; -fx-text-fill: white;");
        closeBtn.setOnAction(e -> reviewStage.close());
        layout.getChildren().add(closeBtn);

        Scene scene = new Scene(layout, 400, 400);
        scene.getStylesheets().add(getClass().getResource("dark-theme.css").toExternalForm());
        reviewStage.setScene(scene);
        reviewStage.initModality(Modality.APPLICATION_MODAL);
        reviewStage.show();
    }
    
    /*
     * These methods are pop ups for sending replies, reviews, and
     * feedback to other users
     */
    
    // Method for writing reviews on potential answers
    private void showWriteReviewDialog(Answer answer) {
        Stage dialog = new Stage();
        dialog.setTitle("Write Review for Answer by " + answer.getAuthor());
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        layout.setStyle("-fx-background-color: #1e1e1e;");
        
        // rating drop down
        Label ratingLabel = new Label("Rating (1–5):");
        ratingLabel.setStyle("-fx-text-fill: white;");
        Spinner<Integer> ratingSpinner = new Spinner<>(1, 5, 5);
        ratingSpinner.setStyle("-fx-background-color: #2e2e2e; -fx-text-fill: white;");
        
        // comment area
        Label commentLabel = new Label("Comment:");
        commentLabel.setStyle("-fx-text-fill: white;");
        TextArea commentArea = new TextArea();
        commentArea.setPromptText("Write your review...");
        commentArea.setWrapText(true);
        commentArea.setStyle("-fx-control-inner-background: #2e2e2e; -fx-text-fill: white;"
                + " -fx-prompt-text-fill: #cccccc;");
        
        // submit button
        Button submitBtn = new Button("Submit Review");
        submitBtn.setStyle("-fx-background-color: green; -fx-text-fill: white;");
        submitBtn.setOnAction(e -> {
            int rating = ratingSpinner.getValue();
            String comment = commentArea.getText().trim();
            if (comment.isEmpty()) {
                showAlert("Missing Comment", "Please write a comment before submitting.");
                return;
            }
            try {
                databaseHelper.saveReview(currentUser, answer.getId(), rating, comment);
                showAlert("Review Saved", "Your review has been submitted.");
                dialog.close();

                // Refresh question to show updated state
                Question updatedQ = databaseHelper.getQuestionById(answer.getQuestionId());
                showQuestionDetailInPanel(updatedQ, questionDetailPanel);

            } catch (SQLException ex) {
                ex.printStackTrace();
                showAlert("Error", "Failed to save review.");
            }
        });
        
        // cancel button
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #555555; -fx-text-fill: white;");
        cancelBtn.setOnAction(e -> dialog.close());
        
        // box for both buttons
        HBox btnBox = new HBox(10, submitBtn, cancelBtn);
        layout.getChildren().addAll(ratingLabel, ratingSpinner, commentLabel, commentArea, btnBox);

        Scene scene = new Scene(layout, 400, 300);
        scene.getStylesheets().add(getClass().getResource("dark-theme.css").toExternalForm());
        dialog.setScene(scene);
        dialog.show();
    }

    // Method for writing replies on potential answers
    private void showReplyDialog(Answer answer, int questionId) {
        Stage replyStage = new Stage();
        replyStage.setTitle("Reply to Answer");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        layout.setStyle("-fx-background-color: #1e1e1e;");
        
        // prompt for replying
        Label prompt = new Label("Enter your reply:");
        prompt.setStyle("-fx-text-fill: white;");
        
        // text area for editing replies
        TextArea replyArea = new TextArea();
        replyArea.setPromptText("Type your reply here...");
        replyArea.setWrapText(true);
        replyArea.setStyle("-fx-control-inner-background: #2e2e2e; -fx-text-fill: white;"
                + " -fx-prompt-text-fill: #ffffff;");
        
        // send reply button
        Button replyBtn = new Button("Send Reply");
        replyBtn.setStyle("-fx-background-color: #6c63ff; -fx-text-fill: white;");
        replyBtn.setOnAction(e -> {
            String replyContent = replyArea.getText().trim();
            if (replyContent.isEmpty()) {
                showAlert("Empty Reply", "Please type your reply before sending.");
                return;
            }
            Answer replyAnswer = manager.createThreadedAnswer(replyContent, currentUser, questionId, answer.getId());
            if (replyAnswer == null) {
                showAlert("Error", "Failed to send reply.");
            } else {
                showAlert("Reply Sent", "Your reply has been posted.");
                replyStage.close();
                // Refresh question detail
                try {
                    Question updatedQ = databaseHelper.getQuestionById(questionId);
                    showQuestionDetailInPanel(updatedQ, questionDetailPanel);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });
        
        // cancel button
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #555555; -fx-text-fill: white;");
        cancelBtn.setOnAction(e -> replyStage.close());
        
        // box for both buttons
        HBox btnBox = new HBox(10, replyBtn, cancelBtn);
        layout.getChildren().addAll(prompt, replyArea, btnBox);

        Scene scene = new Scene(layout, 400, 250);
        scene.getStylesheets().add(getClass().getResource("dark-theme.css").toExternalForm());
        replyStage.setScene(scene);
        replyStage.initModality(Modality.APPLICATION_MODAL);
        replyStage.show();
    }
    
    // Method for writing feedback
    private void showFeedbackPopup(String recipient, int questionId, int answerId) {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setTitle("Send Feedback to " + recipient);

        Label instruction = new Label("Write your private feedback below:");
        instruction.setStyle("-fx-text-fill: white;");
        
        // text area for feedback
        TextArea feedbackArea = new TextArea();
        feedbackArea.setWrapText(true);
        feedbackArea.setPromptText("Type your message here...");
        feedbackArea.setStyle("-fx-control-inner-background: #2e2e2e; -fx-text-fill: white;");
        
        // send button
        Button sendButton = new Button("Send");
        sendButton.setStyle("-fx-background-color: #6c63ff; -fx-text-fill: white;");
        sendButton.setOnAction(e -> {
            String message = feedbackArea.getText().trim();
            if (!message.isEmpty()) {
                try {
                	databaseHelper.sendMessage(currentUser, recipient, questionId, answerId, message, "review-feedback");
                    popupStage.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Empty Message");
                alert.setHeaderText(null);
                alert.setContentText("Please write something before sending.");
                alert.showAndWait();
            }
        });
        
        // cancel button
        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle("-fx-background-color: #555; -fx-text-fill: white;");
        cancelButton.setOnAction(e -> popupStage.close());
        
        // row for both buttons
        HBox buttonRow = new HBox(10, sendButton, cancelButton);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);
        
        VBox layout = new VBox(10, instruction, feedbackArea, buttonRow);
        layout.setPadding(new Insets(15));
        layout.setStyle("-fx-background-color: #1e1e1e;");
        popupStage.setScene(new Scene(layout, 400, 250));
        popupStage.showAndWait();
    }    
    
    /*
     * These methods are responsible for displaying pop ups for editing
     * questions and answers
     */

    // Method for editing question and title with pop up
    private void showEditQuestionDialog(Question question) {
        Stage editStage = new Stage();
        editStage.setTitle("Edit Question");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        layout.setStyle("-fx-background-color: #1e1e1e;");
        
        // title field for editing
        Label titleLabel = new Label("Edit Title:");
        titleLabel.setStyle("-fx-text-fill: white;");
        TextField titleField = new TextField(question.getTitle());
        titleField.setStyle("-fx-control-inner-background: #2e2e2e; -fx-text-fill: white;");
        
        // content field for editing
        Label contentLabel = new Label("Edit Content:");
        contentLabel.setStyle("-fx-text-fill: white;");
        TextArea contentArea = new TextArea(question.getContent());
        contentArea.setWrapText(true);
        contentArea.setStyle("-fx-control-inner-background: #2e2e2e; -fx-text-fill: white;");
        
        // save button
        Button saveBtn = new Button("Save Changes");
        saveBtn.setStyle("-fx-background-color: white; -fx-text-fill: black;");
        saveBtn.setOnAction(e -> {
            String newTitle = titleField.getText().trim();
            String newContent = contentArea.getText().trim();
            if (newTitle.isEmpty() || newContent.isEmpty()) {
                showAlert("Error", "Title and Content cannot be empty.");
                return;
            }
            boolean success = databaseHelper.updateQuestion(question.getId(), newTitle, newContent);
            if (success) {
                showAlert("Success", "Question updated successfully.");
                refreshQuestions();
                editStage.close();
            } else {
                showAlert("Error", "Failed to update question.");
            }
        });
        
        // cancel button
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #555555; -fx-text-fill: white;");
        cancelBtn.setOnAction(e -> editStage.close());
        
        // box for both buttons
        HBox btnBox = new HBox(10, saveBtn, cancelBtn);
        layout.getChildren().addAll(titleLabel, titleField, contentLabel, contentArea, btnBox);

        Scene scene = new Scene(layout, 400, 300);
        scene.getStylesheets().add(getClass().getResource("dark-theme.css").toExternalForm());
        editStage.setScene(scene);
        editStage.initModality(Modality.APPLICATION_MODAL);
        editStage.show();
    }
    
    // Method for editing answer with pop up
    private void showEditAnswerDialog(Answer answer, int questionId) {
        Stage dialog = new Stage();
        dialog.setTitle("Edit Your Answer");
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        layout.setStyle("-fx-background-color: #1e1e1e;");

        Label prompt = new Label("Edit your answer:");
        prompt.setStyle("-fx-text-fill: white;");
        
        // area for editing answer
        TextArea answerArea = new TextArea(answer.getContent());
        answerArea.setWrapText(true);
        answerArea.setStyle("-fx-control-inner-background: #2e2e2e; -fx-text-fill: white;");
        
        // save button
        Button saveBtn = new Button("Save");
        saveBtn.setStyle("-fx-background-color: #6c63ff; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            String newContent = answerArea.getText().trim();
            if (!newContent.isEmpty()) {
                try {
                    boolean updated = databaseHelper.updateAnswer(answer.getId(), newContent);
                    if (updated) {
                        showAlert("Success", "Your answer has been updated.");
                        dialog.close();
                        try {
                            Question updatedQ = databaseHelper.getQuestionById(questionId);
                            showQuestionDetailInPanel(updatedQ, questionDetailPanel);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        showAlert("Error", "Failed to update the answer.");
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    showAlert("Error", "Database error occurred while updating your answer.");
                }
            } else {
                showAlert("Warning", "Answer cannot be empty.");
            }
        });
        
        // cancel button
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #555555; -fx-text-fill: white;");
        cancelBtn.setOnAction(e -> dialog.close());
        
        // box for both buttons
        HBox buttonBox = new HBox(10, saveBtn, cancelBtn);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        layout.getChildren().addAll(prompt, answerArea, buttonBox);

        Scene scene = new Scene(layout, 400, 250);
        scene.getStylesheets().add(getClass().getResource("dark-theme.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    
    /*
     * These methods are helper methods for refreshing questions and answers, showing
     * alert pop ups, and performing search
     */
    
    // Refresh the question list from database
    private void refreshQuestions() {
        try {
            List<Question> questions = databaseHelper.loadQuestions();
            questionList.setAll(questions);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load questions.");
        }
    }
    
    // This method updates Answers 
    private void refreshAnswers(int questionId) {
        try {
            Question updated = databaseHelper.getQuestionById(questionId);
            showQuestionDetailInPanel(updated, questionDetailPanel);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to refresh answer view.");
        }
    }
    
    // Method for alert messages
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }    

    // Searching for questions
    private void performSearch(String keyword) {
        try {
            List<Question> results = databaseHelper.searchQuestions(keyword);
            questionList.setAll(results);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Search Error", "Failed to search questions.");
        }
    }
    
    /*
     * Method for showing flag pop up.
     * Prompts for reason and has submit and cancel button
     */
    
    private void showFlagDialog(Flag.FlagType type, String itemId, String label) {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setTitle("Flag This " + label.substring(0, 1).toUpperCase() + label.substring(1));

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        layout.setStyle("-fx-background-color: #1e1e1e;");

        Label instruction = new Label("Provide a reason for flagging this " + label + ":");
        instruction.setStyle("-fx-text-fill: white;");

        TextArea reasonArea = new TextArea();
        reasonArea.setWrapText(true);
        reasonArea.setPromptText("Reason...");
        reasonArea.setStyle("-fx-control-inner-background: #2e2e2e; -fx-text-fill: white;");

        Button submitBtn = new Button("Submit Flag");
        submitBtn.setStyle("-fx-background-color: #ac280c; -fx-text-fill: white;");
        submitBtn.setOnAction(e -> {
            String reason = reasonArea.getText().trim();
            if (!reason.isEmpty()) {
            	Flag flag = new Flag(type, itemId, currentUser, reason, LocalDateTime.now());
                FlagSystem.addFlag(flag);
                showAlert("Flag Submitted", label + " has been flagged for review.");
                popupStage.close();

                try {
                    if (type == Flag.FlagType.QUESTION) {
                        Question updated = databaseHelper.getQuestionById(Integer.parseInt(itemId));
                        if (updated != null) {
                            showQuestionDetailInPanel(updated, questionDetailPanel);
                        } else {
                            showAlert("Error", "Could not reload the question after flagging.");
                        }
                    } else if (type == Flag.FlagType.ANSWER) {
                        // Fetch the answer and then its question
                        Answer flaggedAnswer = databaseHelper.getAnswerById(Integer.parseInt(itemId));
                        if (flaggedAnswer != null) {
                            Question parentQ = databaseHelper.getQuestionById(flaggedAnswer.getQuestionId());
                            if (parentQ != null) {
                                showQuestionDetailInPanel(parentQ, questionDetailPanel);
                            } else {
                                showAlert("Error", "Could not find the parent question for this answer.");
                            }
                        } else {
                            showAlert("Error", "Flagged answer not found.");
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showAlert("Error", "An unexpected error occurred while refreshing.");
                }
            } else {
                showAlert("Missing Reason", "Please provide a reason.");
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #555555; -fx-text-fill: white;");
        cancelBtn.setOnAction(e -> popupStage.close());

        HBox btnBox = new HBox(10, submitBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        layout.getChildren().addAll(instruction, reasonArea, btnBox);

        popupStage.setScene(new Scene(layout, 400, 230));
        popupStage.show();
    }
    
    /*
     * These two methods are helper methods for displaying the contextual link content
     * that is called in the flagged content dashboard
     */
    
    // method for small question detail pop up
    public static void viewQuestionDetails(int questionId, Stage parentStage) {
    	DatabaseHelper db = new DatabaseHelper();
    	try {
    	    Question question = db.getQuestionById(questionId);
    	    Stage popup = new Stage();
    	    QuestionSystemGUI gui = new QuestionSystemGUI(question.getAuthor(), "staff", db, parentStage);
    	    gui.start(popup);
    	} catch (Exception e) {
    	    e.printStackTrace();
    	}
    }
    
    // method for seeing question given the answer
    public static void viewQuestionContainingAnswer(int answerId, Stage parentStage) {
        DatabaseHelper db = new DatabaseHelper();
        try {
            Answer answer = db.getAnswerById(answerId);
            int questionId = answer.getQuestionId();
            viewQuestionDetails(questionId, parentStage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    /*
     * These methods are responsible for filtering categories and
     * adding color and style to tags
     */
    
    // Method to filter question by category
    private void filterQuestions(String category, String status) {
        try {
            List<Question> allQuestions = databaseHelper.loadQuestions();
            List<Question> filtered = allQuestions.stream()
                .filter(q -> "All".equals(category) || q.getTags().contains(category))
                .filter(q -> {
                    if ("Answered".equals(status)) return q.isAnswered();
                    if ("Unanswered".equals(status)) return !q.isAnswered();
                    return true;
                })
                .collect(Collectors.toList());

            questionList.setAll(filtered);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to filter questions.");
        }
    }
    
    // Method for styling filter selection
    private void highlightSelectedCategory(VBox categoryBox, Label selectedLabel) {
        for (Node node : categoryBox.getChildren()) {
            if (node instanceof HBox) {
                HBox hbox = (HBox) node;
                Label colorIndicator = (Label) hbox.getChildren().get(0);
                Label categoryLabel = (Label) hbox.getChildren().get(1);
                categoryLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: white; -fx-padding: 4px 10px; -fx-border-color: transparent;");
                colorIndicator.setStyle("-fx-min-width: 12px; -fx-min-height: 12px; -fx-border-radius: 4px; "
                        + "-fx-background-color: " + getCategoryColor(categoryLabel.getText()) + ";");
            }
        }
        
        // style
        selectedLabel.setStyle(
            "-fx-font-size: 14px; -fx-text-fill: white; -fx-padding: 4px 10px; " +
            "-fx-border-color: #007bff; -fx-border-width: 2px; -fx-border-radius: 5px; " +
            "-fx-background-color: rgba(0, 123, 255, 0.1);"
        );

        HBox selectedHBox = (HBox) selectedLabel.getParent();
        Label selectedColorIndicator = (Label) selectedHBox.getChildren().get(0);
        selectedColorIndicator.setStyle("-fx-min-width: 12px; -fx-min-height: 12px; -fx-border-radius: 4px; "
                + "-fx-background-color: " + getCategoryColor(selectedLabel.getText()) + ";");
    }
    
    // Category filter color
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
                return "#151ec0";
            default:
                return "#ccc";
        }
    }

    // Return a style that uses the same color as the left category panel
    private String getTagStyle(String tag) {
        // Re-use the same color from getColorForTag(...)
        String color = getColorForTag(tag);
        return "-fx-background-color: " + color + "; -fx-text-fill: white; "
             + "-fx-padding: 2px 4px; -fx-background-radius: 4px;";
    }

    // Return color for a tag (matches the category color)
    private String getColorForTag(String tag) {
        switch (tag.toLowerCase()) {
            case "classwork": return "#7ed321";
            case "exams": return "#f5a623";
            case "project": return "#d0021b";
            case "urgent": return "#c0392b";
            case "meeting": return "#8e44ad";
            case "question": return "#151ec0";
            default: return "#4a90e2";
        }
    }
}

/**
 * TrustedReviewersGUI
 * 
 * Simple pop-up to display the student's trusted reviewers.
 */
class TrustedReviewersGUI {
	private final Map<String, Integer> trustedReviewersWithWeights;
    private final String currentUser;

    private final DatabaseHelper databaseHelper;

    public TrustedReviewersGUI(Set<String> trustedReviewers, String currentUser, DatabaseHelper databaseHelper) {
        this.trustedReviewersWithWeights = new HashMap<>();
        this.currentUser = currentUser;
        this.databaseHelper = databaseHelper;

        try {
            Map<String, Integer> loaded = databaseHelper.getTrustedReviewersWithWeights(currentUser);
            trustedReviewersWithWeights.putAll(loaded);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to load trusted reviewers.");
        }
    }

    public void show() {
        Stage stage = new Stage();
        stage.setTitle("Trusted Reviewers for " + currentUser);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        layout.setStyle("-fx-background-color: #1e1e1e;");
        
        Set<String> updatedReviewers = new HashSet<>();
        try {
            updatedReviewers = databaseHelper.getUpdatedTrustedReviewers(currentUser);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Could not load update notifications.");
        }

        Label title = new Label("Your Trusted Reviewers:");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        layout.getChildren().add(title);

        if (trustedReviewersWithWeights.isEmpty()) {
            Label noneLabel = new Label("No trusted reviewers yet.");
            noneLabel.setStyle("-fx-text-fill: white;");
            layout.getChildren().add(noneLabel);
        } else {
            for (Map.Entry<String, Integer> entry : trustedReviewersWithWeights.entrySet()) {
                String reviewer = entry.getKey();
                Integer weight = entry.getValue();

                String labelText = reviewer + " (Weight: " + weight + ")";
                if (updatedReviewers.contains(reviewer)) {
                    labelText += " 🔔";
                }
                Label reviewerLabel = new Label(labelText);
                reviewerLabel.setStyle("-fx-text-fill: white;");

                Button changeWeight = new Button("Set Weight");
                changeWeight.setOnAction(e -> {
                    TextInputDialog dialog = new TextInputDialog(weight.toString());
                    dialog.setTitle("Change Weight");
                    dialog.setHeaderText("Assign weight to " + reviewer);
                    dialog.setContentText("Enter a weight (1–5):");
                    Optional<String> result = dialog.showAndWait();
                    result.ifPresent(input -> {
                        try {
                            int newWeight = Integer.parseInt(input);
                            if (newWeight >= 1 && newWeight <= 5) {
                            	trustedReviewersWithWeights.put(reviewer, newWeight);
                            	
                            	try {
                                    databaseHelper.addOrUpdateTrustedReviewer(currentUser, reviewer, newWeight);
                                } catch (SQLException ex) {
                                    ex.printStackTrace();
                                    showAlert("Database Error", "Failed to update reviewer weight in the database.");
                                }
                            	
                            	// Refresh UI
                            	refreshUI(layout);
                            } else {
                                showAlert("Invalid", "Weight must be between 1 and 5.");
                            }
                        } catch (NumberFormatException ex) {
                            showAlert("Invalid", "Please enter a number.");
                        }
                    });
                });
                
                // view reviewer profile button
                Button viewProfile = new Button("View Reviews");
                viewProfile.setOnAction(e -> {
                	try {
                	    databaseHelper.clearUpdateForReviewer(reviewer, currentUser);
                	} catch (SQLException ex) {
                	    ex.printStackTrace();
                	}
                	
                	ReviewerDashboardGUI.show(reviewer, databaseHelper, true);
                });

                HBox reviewerBox = new HBox(10, reviewerLabel, changeWeight, viewProfile);
                reviewerBox.setAlignment(Pos.CENTER_LEFT);
                layout.getChildren().add(reviewerBox);
            }
        }

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #6c63ff; -fx-text-fill: white;");
        closeBtn.setOnAction(e -> stage.close());
        layout.getChildren().add(closeBtn);

        Scene scene = new Scene(layout, 300, 200);
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.show();
    }
    
    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
    
    // helper method to refresh
    private void refreshUI(VBox layout) {
        layout.getChildren().clear();
        Label title = new Label("Your Trusted Reviewers:");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        layout.getChildren().add(title);

        Set<String> updatedReviewers;
        try {
            updatedReviewers = databaseHelper.getUpdatedTrustedReviewers(currentUser);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Could not load update notifications.");
            return;
        }

        if (trustedReviewersWithWeights.isEmpty()) {
            Label noneLabel = new Label("No trusted reviewers yet.");
            noneLabel.setStyle("-fx-text-fill: white;");
            layout.getChildren().add(noneLabel);
        } else {
            for (Map.Entry<String, Integer> entry : trustedReviewersWithWeights.entrySet()) {
                String reviewer = entry.getKey();
                Integer weight = entry.getValue();

                String labelText = reviewer + " (Weight: " + weight + ")";
                if (updatedReviewers.contains(reviewer)) {
                    labelText += " 🔔";
                }

                Label reviewerLabel = new Label(labelText);
                reviewerLabel.setStyle("-fx-text-fill: white;");

                Button changeWeight = new Button("Set Weight");
                changeWeight.setOnAction(e -> {
                    TextInputDialog dialog = new TextInputDialog(weight.toString());
                    dialog.setTitle("Change Weight");
                    dialog.setHeaderText("Assign weight to " + reviewer);
                    dialog.setContentText("Enter a weight (1–5):");
                    Optional<String> result = dialog.showAndWait();
                    result.ifPresent(input -> {
                        try {
                            int newWeight = Integer.parseInt(input);
                            if (newWeight >= 1 && newWeight <= 5) {
                                trustedReviewersWithWeights.put(reviewer, newWeight);
                                databaseHelper.addOrUpdateTrustedReviewer(currentUser, reviewer, newWeight);
                                refreshUI(layout);
                            } else {
                                showAlert("Invalid", "Weight must be between 1 and 5.");
                            }
                        } catch (NumberFormatException | SQLException ex) {
                            ex.printStackTrace();
                            showAlert("Error", "Could not update weight.");
                        }
                    });
                });

                Button viewProfile = new Button("View Reviews");
                viewProfile.setOnAction(e -> {
                    try {
                        databaseHelper.clearUpdateForReviewer(reviewer, currentUser);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                    ReviewerDashboardGUI.showReadOnly(reviewer, currentUser, databaseHelper);
                });

                HBox reviewerBox = new HBox(10, reviewerLabel, changeWeight, viewProfile);
                reviewerBox.setAlignment(Pos.CENTER_LEFT);
                layout.getChildren().add(reviewerBox);
            }
        }

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #6c63ff; -fx-text-fill: white;");
        closeBtn.setOnAction(e -> ((Stage) layout.getScene().getWindow()).close());
        layout.getChildren().add(closeBtn);
    }
}
