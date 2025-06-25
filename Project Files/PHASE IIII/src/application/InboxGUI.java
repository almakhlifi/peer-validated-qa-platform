package application;

import databasePart1.DatabaseHelper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.stage.Modality;
import java.time.LocalDateTime;

/**
 * Represents a custom TreeItem to hold message thread information.
 */
class MessageTreeItem extends TreeItem<String> {
    private String threadKey;

    /**
     * Constructs a new MessageTreeItem.
     * @param label The text label for the tree item.
     * @param graphic The graphic node to display with the label.
     * @param threadKey The unique key identifying the message thread.
     */
    public MessageTreeItem(String label, Label graphic, String threadKey) {
        super(label, graphic);
        this.threadKey = threadKey;
    }

    /**
     * Returns the thread key associated with this tree item.
     * @return The thread key.
     */
    public String getThreadKey() {
        return threadKey;
    }
}

/**
 * Represents the graphical user interface for the inbox.
 */
public class InboxGUI {
    /**
     * Displays the inbox window for the specified user.
     * @param currentUser The username of the currently logged-in user.
     * @param dbHelper The database helper instance to interact with the database.
     * @param previousStage The previous stage to return to when the inbox is closed.
     */
    public static void show(String currentUser, DatabaseHelper dbHelper, Stage previousStage) {
        Stage inboxStage = new Stage();
        inboxStage.setTitle("Inbox - " + currentUser);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1e1e1e;");

        TreeView<String> conversationTree = new TreeView<>();
        conversationTree.setStyle("-fx-control-inner-background: #2e2e2e; -fx-text-fill: white;");

        // chat title
        Label chatTitle = new Label("Select a conversation to begin");
        chatTitle.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        // link to question
        Hyperlink questionLink = new Hyperlink();
        questionLink.setStyle("-fx-text-fill: #6c63ff; -fx-font-size: 13px;");
        questionLink.setVisible(false);

        // chat header
        HBox chatHeader = new HBox(10, chatTitle, questionLink);
        chatHeader.setPadding(new Insets(10, 0, 5, 10));
        chatHeader.setAlignment(Pos.CENTER_LEFT);

        // message area
        ScrollPane messageScroll = new ScrollPane();
        messageScroll.setStyle("-fx-background: #1e1e1e;");
        messageScroll.setFitToWidth(true);
        messageScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        messageScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        VBox messageBox = new VBox(8);
        messageBox.setPadding(new Insets(10));
        messageBox.setStyle("-fx-background-color: #1e1e1e;");
        messageScroll.setContent(messageBox);

        // input instruction label
        Label inputInstruction = new Label("Type your message below");
        inputInstruction.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12px;");

        // input area
        TextArea inputArea = new TextArea();
        inputArea.setStyle("-fx-control-inner-background: #2e2e2e; -fx-text-fill: white;");
        inputArea.setPromptText("Write your message...");
        inputArea.setPrefRowCount(3);

        // send button and styling
        Button sendBtn = new Button("Send");
        sendBtn.setStyle("-fx-background-color: #6c63ff; -fx-text-fill: white; -fx-font-weight: bold;");

        // back button and styling
        Button backBtn = new Button("Back");
        backBtn.setStyle("-fx-background-color: #555555; -fx-text-fill: white;");

        HBox buttonBar = new HBox(10, sendBtn, backBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        VBox rightPane = new VBox(10, chatHeader, messageScroll, inputInstruction, inputArea, buttonBar);
        rightPane.setPadding(new Insets(10));

        Map<String, List<Message>> groupedMessages = new HashMap<>();

        final String[] activeThreadKey = new String[1];

        try {
            List<Message> allMessages = dbHelper.getAllMessagesForUser(currentUser);
            for (Message msg : allMessages) {

                int questionId = msg.getQuestionId(); // could be -1 if unrelated to a question

                String normalizedUsers = Stream.of(msg.getSender(), msg.getRecipient())
                                                        .sorted()
                                                        .collect(Collectors.joining("_"));

                // check message type (question or review feedback)
                String messageType = msg.getMessageType();
                String threadKey = normalizedUsers + "::" + questionId;

                String partner = msg.getSender().equals(currentUser) ? msg.getRecipient() : msg.getSender();

                groupedMessages.computeIfAbsent(threadKey, k -> new ArrayList<>()).add(msg);
            }

            List<String> sortedKeys = groupedMessages.keySet().stream()
                    .sorted((a, b) -> {
                        List<Message> aMsgs = groupedMessages.get(a);
                        List<Message> bMsgs = groupedMessages.get(b);
                        return bMsgs.get(bMsgs.size() - 1).getTimestamp().compareTo(aMsgs.get(aMsgs.size() - 1).getTimestamp());
                    })
                    .collect(Collectors.toList());

                TreeItem<String> rootItem = new TreeItem<>("Conversations");
                rootItem.setExpanded(true);
                Map<String, TreeItem<String>> userNodes = new HashMap<>();
                Set<String> usersWithUnread = new HashSet<>();

                for (String threadKey : sortedKeys) {
                    List<Message> msgs = groupedMessages.get(threadKey);
                    String[] parts = threadKey.split("::");
                    String partner = parts[0];
                    int questionId = Integer.parseInt(parts[1]);

                    if (questionId == -1) {
                        continue; // Don't show threads without a linked question
                    }

                    String questionTitle = "";
                    if (questionId != -1) {
                        try {
                            questionTitle = dbHelper.getQuestionById(questionId).getTitle();
                        } catch (Exception ignored) {}
                    }

                    boolean hasUnread = msgs.stream().anyMatch(msg ->
                            msg.getRecipient().equals(currentUser) && !msg.isRead());

                    String threadLabel = questionTitle.isEmpty() ? "(No linked question)" : questionTitle;
                    Label threadLabelNode = new Label(threadLabel);

                    if (hasUnread) {
                        threadLabelNode.setGraphic(new Label("🔵"));
                        usersWithUnread.add(partner);
                    }

                    MessageTreeItem threadItem = new MessageTreeItem("", threadLabelNode, threadKey);
                    threadItem.setExpanded(false);

                    TreeItem<String> userNode = userNodes.computeIfAbsent(partner, k -> {
                        // allows conversation list to only show other user's name
                        String displayName = Arrays.stream(k.split("_"))
                                                .filter(name -> !name.equals(currentUser))
                                                .findFirst()
                                                .orElse(k); // fallback if something's wrong
                        Label userLabelNode = new Label(displayName);


                        TreeItem<String> node = new TreeItem<>("", userLabelNode);
                        node.setExpanded(true);
                        rootItem.getChildren().add(node);
                        return node;
                    });

                    userNode.getChildren().add(threadItem);
                }

                // Show unread indicator at user level if needed
                for (String user : usersWithUnread) {
                    TreeItem<String> node = userNodes.get(user);
                    if (node != null) {
                        ((Label) node.getGraphic()).setGraphic(new Label("🟣"));
                    }
                }

                conversationTree.setRoot(rootItem);
                conversationTree.setShowRoot(false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // conversation list with collapsing threads
        conversationTree.setOnMouseClicked(event -> {
            TreeItem<String> selectedNode = conversationTree.getSelectionModel().getSelectedItem();
            if (selectedNode == null || selectedNode.getParent() == null) return;

            if (!(selectedNode instanceof MessageTreeItem)) return;
            String threadKey = ((MessageTreeItem) selectedNode).getThreadKey();
            activeThreadKey[0] = threadKey;

            String[] parts = threadKey.split("::");
            String partner = parts[0];
            int questionId = Integer.parseInt(parts[1]);

            List<Message> thread = groupedMessages.get(threadKey);
            if (thread == null) return;

            // Set chat header
            String labelPrefix = "Chat with ";
            String messageType = thread.get(0).getMessageType();
            if ("review-feedback".equalsIgnoreCase(messageType)) {
                labelPrefix = "Feedback with ";
            }
            chatTitle.setText(labelPrefix + partner);
            if (questionId != -1) {
                try {
                    String context = dbHelper.getQuestionById(questionId).getTitle();
                    questionLink.setText("🔗 View Question: \"" + context + "\"");
                    questionLink.setVisible(true);
                    questionLink.setOnAction(evt -> {
                        try {
                            Question q = dbHelper.getQuestionById(questionId);
                            Stage popup = new Stage();
                            popup.setTitle("Question Preview");

                            Label title = new Label(q.getTitle());
                            title.setStyle("-fx-font-size: 16px; -fx-text-fill: white; -fx-font-weight: bold;");

                            Label desc = new Label(q.getContent());
                            desc.setWrapText(true);
                            desc.setStyle("-fx-text-fill: white;");

                            VBox layout = new VBox(10, title, desc);
                            layout.setPadding(new Insets(15));
                            layout.setStyle("-fx-background-color: #1e1e1e;");

                            popup.setScene(new Scene(layout, 500, 250));
                            popup.show();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    questionLink.setVisible(false);
                }
            } else {
                questionLink.setVisible(false);
            }

            // Display messages
            thread.sort(Comparator.comparing(Message::getTimestamp));
            messageBox.getChildren().clear();
            for (Message msg : thread) {
                Label bubble = new Label(msg.getContent());
                bubble.setWrapText(true);
                bubble.setMaxWidth(300);
                bubble.setPadding(new Insets(8));
                bubble.setStyle("-fx-background-radius: 8; -fx-font-size: 13px;");

                HBox wrapper = new HBox(bubble);
                if (msg.getSender().equals(currentUser)) {
                    wrapper.setAlignment(Pos.CENTER_RIGHT);
                    bubble.setStyle(bubble.getStyle() + "-fx-background-color: #6c63ff; -fx-text-fill: white;");
                } else {
                    wrapper.setAlignment(Pos.CENTER_LEFT);
                    bubble.setStyle(bubble.getStyle() + "-fx-background-color: #2e2e2e; -fx-text-fill: white;");
                }

                messageBox.getChildren().add(wrapper);
            }

            messageBox.heightProperty().addListener((obs, oldVal, newVal) -> messageScroll.setVvalue(1.0));

            try {
                boolean updated = false;
                for (Message msg : thread) {
                    if (msg.getRecipient().equals(currentUser) && !msg.isRead()) {
                        try {
                            dbHelper.markMessagesAsRead(currentUser, msg.getQuestionId(), msg.getAnswerId());
                            msg.setRead(true);  // Update in-memory
                            updated = true;
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
                if (updated) {
                    rebuildConversationTree(conversationTree, groupedMessages, currentUser, dbHelper);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // send button functionality
        sendBtn.setOnAction(e -> {
            String content = inputArea.getText().trim();
            if (content.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Empty Message");
                alert.setHeaderText(null);
                alert.setContentText("Cannot send an empty message.");
                alert.showAndWait();
                return;
            }

            if (activeThreadKey[0] != null) {
                String[] parts = activeThreadKey[0].split("::")[0].split("_");

                String recipient = null;
                for (String name : parts) {
                    if (!name.equals(currentUser)) {
                        recipient = name;
                        break;
                    }
                }

                try {
                    int questionId = -1;
                    if (activeThreadKey[0] != null) {
                        questionId = Integer.parseInt(activeThreadKey[0].split("::")[1]);
                    }
                    dbHelper.sendMessage(currentUser, recipient, questionId, -1, content, "question");

                    String key = activeThreadKey[0];
                    if (key != null) {
                        groupedMessages.computeIfAbsent(key, k -> new ArrayList<>())
                            .add(new Message(-1, currentUser, recipient, questionId, -1, content,
                                    new java.sql.Timestamp(System.currentTimeMillis()), false, "question"));
                    }

                    Label bubble = new Label(content);
                    bubble.setWrapText(true);
                    bubble.setMaxWidth(300);
                    bubble.setPadding(new Insets(8));
                    bubble.setStyle("-fx-background-radius: 8; -fx-background-color: #6c63ff; -fx-text-fill: white; -fx-font-size: 13px;");
                    HBox wrapper = new HBox(bubble);
                    wrapper.setAlignment(Pos.CENTER_RIGHT);

                    messageBox.getChildren().add(wrapper);
                    inputArea.clear();
                    messageBox.heightProperty().addListener((obs, oldVal, newVal) -> messageScroll.setVvalue(1.0));
                    rebuildConversationTree(conversationTree, groupedMessages, currentUser, dbHelper);

                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });

        // back button functionality
        backBtn.setOnAction(e -> {
            inboxStage.close();
            if (previousStage != null) {
                previousStage.show();
            }
        });

        root.setLeft(conversationTree);
        root.setCenter(rightPane);

        Scene scene = new Scene(root, 700, 400);
        scene.getStylesheets().add(InboxGUI.class.getResource("dark-theme.css").toExternalForm());
        inboxStage.setScene(scene);
        inboxStage.show();
    }

    /**
     * Helper function for rebuilding the conversation list in the TreeView.
     * @param conversationTree The TreeView to rebuild.
     * @param groupedMessages The map of message threads.
     * @param currentUser The current user's username.
     * @param dbHelper The database helper instance.
     */
    private static void rebuildConversationTree(TreeView<String> conversationTree,
                                                Map<String, List<Message>> groupedMessages,
                                                String currentUser,
                                                DatabaseHelper dbHelper) {
        List<String> sortedKeys = groupedMessages.keySet().stream()
            .sorted((a, b) -> {
                List<Message> aMsgs = groupedMessages.get(a);
                List<Message> bMsgs = groupedMessages.get(b);
                return bMsgs.get(bMsgs.size() - 1).getTimestamp().compareTo(aMsgs.get(aMsgs.size() - 1).getTimestamp());
            })
            .collect(Collectors.toList());

        TreeItem<String> rootItem = new TreeItem<>("Conversations");
        rootItem.setExpanded(true);
        Map<String, TreeItem<String>> userNodes = new HashMap<>();
        Set<String> usersWithUnread = new HashSet<>();

        for (String threadKey : sortedKeys) {
            List<Message> msgs = groupedMessages.get(threadKey);
            String[] parts = threadKey.split("::");
            String normalizedUsers = parts[0];
            int questionId = Integer.parseInt(parts[1]);

            if (questionId == -1) {
                continue; // Skip unlinked threads
            }

            String questionTitle = "";
            if (questionId != -1) {
                try {
                    questionTitle = dbHelper.getQuestionById(questionId).getTitle();
                } catch (Exception ignored) {}
            }

            boolean hasUnread = msgs.stream().anyMatch(msg ->
                msg.getRecipient().equals(currentUser) && !msg.isRead());

            String messageType = msgs.get(0).getMessageType();

            String threadLabel = questionTitle.isEmpty() ? "(No linked question)" : questionTitle;

            Label threadLabelNode = new Label(threadLabel);

            if (hasUnread) {
                threadLabelNode.setGraphic(new Label("🔵"));
                usersWithUnread.add(normalizedUsers);
            }

            MessageTreeItem threadItem = new MessageTreeItem("", threadLabelNode, threadKey);
            threadItem.setExpanded(false);

            TreeItem<String> userNode = userNodes.computeIfAbsent(normalizedUsers, k -> {
                // allows conversation list to only show other person's name
                String displayName = Arrays.stream(k.split("_"))
                                        .filter(name -> !name.equals(currentUser))
                                        .findFirst()
                                        .orElse(k); // fallback if something's wrong
                Label userLabelNode = new Label(displayName);

                TreeItem<String> node = new TreeItem<>("", userLabelNode);
                node.setExpanded(true);
                rootItem.getChildren().add(node);
                return node;
            });

            userNode.getChildren().add(threadItem);
        }

        for (String user : usersWithUnread) {
            TreeItem<String> node = userNodes.get(user);
            if (node != null) {
                ((Label) node.getGraphic()).setGraphic(new Label("🟣"));
            }
        }

        conversationTree.setRoot(rootItem);
        conversationTree.setShowRoot(false);
    }
    
    /**
     * Opens a pop-up dialog for submitting a flag on a question, answer, or message.
     * Prompts the user for a reason and inserts the flag into the database.
     *
     * @param type The type of item being flagged (QUESTION, ANSWER, MESSAGE).
     * @param itemId The unique ID of the item being flagged.
     * @param label A user-friendly label used in the popup (e.g., "question", "answer").
     * @param currentUser The username of the person submitting the flag.
     * @param db Reference to the DatabaseHelper for inserting the flag.
     * @param onCloseCallback A callback that runs after the popup closes (for refresh purposes).
     */
    private static void showFlagDialog(Flag.FlagType type, String itemId, String label, String currentUser, DatabaseHelper db, Runnable onCloseCallback) {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setTitle("Flag This " + label.substring(0, 1).toUpperCase() + label.substring(1));

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        layout.setStyle("-fx-background-color: #1e1e1e;");
        
        // prompt for providing a reason
        Label instruction = new Label("Provide a reason for flagging this " + label + ":");
        instruction.setStyle("-fx-text-fill: white;");
        
        // comment writing area
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
            	db.insertFlag(flag);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, label + " has been flagged.", ButtonType.OK);
                alert.showAndWait();
                popupStage.close();
                
                // Refresh after closing
                if (onCloseCallback != null) onCloseCallback.run();  
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Please provide a reason before submitting.", ButtonType.OK);
                alert.showAndWait();
            }
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #555; -fx-text-fill: white;");
        cancelBtn.setOnAction(e -> popupStage.close());

        HBox btnBox = new HBox(10, submitBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        layout.getChildren().addAll(instruction, reasonArea, btnBox);
        popupStage.setScene(new Scene(layout, 400, 230));
        popupStage.show();
    }
    
    /**
     * Displays the full private message thread associated with a given question.
     * Used by staff and instructors to monitor conversations and submit flags.
     *
     * @param viewer The username of the person viewing the thread (used for context).
     * @param db Reference to the database for retrieving messages.
     * @param parentStage The stage to return to when this window is closed.
     * @param questionId The ID of the question whose thread should be displayed.
     */
    public static void showThreadForQuestion(String viewer, DatabaseHelper db, Stage parentStage, int questionId) {
        Stage stage = new Stage();
        stage.setTitle("Messages for Question ID " + questionId);

        List<Message> thread = db.getMessagesForQuestion(questionId);
        
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #1e1e1e;");
        
        // load messages
        refreshMessageThread(root, questionId, db, parentStage, viewer);
        
        // scroll plane
        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #1e1e1e; -fx-control-inner-background: #1e1e1e;");
        
        // Back button
        Button backBtn = new Button("Back");
        backBtn.setStyle("-fx-background-color: #555; -fx-text-fill: white;");
        backBtn.setOnAction(e -> {
            stage.close();
            if (parentStage != null) {
                parentStage.show();
            }
        });
        
        VBox layoutWithBack = new VBox(10, scrollPane, backBtn);
        layoutWithBack.setPadding(new Insets(10));
        layoutWithBack.setStyle("-fx-background-color: #1e1e1e;");

        Scene scene = new Scene(layoutWithBack, 600, 450);
        scene.getStylesheets().add(InboxGUI.class.getResource("dark-theme.css").toExternalForm());
        stage.setScene(scene);
        stage.initOwner(parentStage);
        stage.show();
    }
    
    /**
     * Reloads and displays all messages associated with a question.
     * Also enables flagging of messages if they have not yet been flagged.
     *
     * @param container The VBox container used to hold message elements.
     * @param questionId The ID of the associated question.
     * @param db Reference to the database for message and flag data.
     * @param parentStage The parent window (used for navigation).
     * @param currentUser The currently logged-in user for flag attribution.
     */
    private static void refreshMessageThread(VBox container, int questionId, DatabaseHelper db, Stage parentStage, String currentUser) {
        container.getChildren().clear();
        List<Message> thread = db.getMessagesForQuestion(questionId);

        if (thread.isEmpty()) {
            Label noMessages = new Label("No private messages for this question.");
            noMessages.setStyle("-fx-text-fill: #aaa; -fx-font-size: 14px; -fx-font-style: italic;");
            container.getChildren().add(noMessages);
        } else {
            for (Message msg : thread) {
                VBox messageBox = new VBox(5);
                messageBox.setStyle("-fx-background-color: #333333; -fx-padding: 10; -fx-border-radius: 6; -fx-background-radius: 6;");

                Label msgLabel = new Label("From: " + msg.getSender() + "\nTo: " + msg.getRecipient() + "\n\n" + msg.getContent());
                msgLabel.setWrapText(true);
                msgLabel.setStyle("-fx-text-fill: white;");
                
                List<Flag> messageFlags = FlagSystem.getFlagsByItemId(String.valueOf(msg.getId()));
                boolean unresolvedFlag = messageFlags.stream()
                    .anyMatch(f -> f.getType() == Flag.FlagType.MESSAGE &&
                                   !db.isFlagResolved(f.getItemId(), f.getType()));

                Button flagBtn = new Button(unresolvedFlag ? "🚩 Flagged" : "🚩 Flag");
                flagBtn.setDisable(unresolvedFlag);
                flagBtn.setStyle("-fx-background-color: #ac280c; -fx-text-fill: white; -fx-font-size: 12px;");
                
                // if not flagged, then flag message and prompt for reason
                if (!unresolvedFlag) {
                	flagBtn.setOnAction(e -> {
                	    showFlagDialog(Flag.FlagType.MESSAGE, String.valueOf(msg.getId()), "message", currentUser, db, () -> {
                	        refreshMessageThread(container, questionId, db, parentStage, currentUser);
                	    });
                	});
                }

                messageBox.getChildren().addAll(msgLabel, flagBtn);
                container.getChildren().add(messageBox);
            }
        }
    }
    
    /**
     * Opens the full message thread for a flagged message, allowing staff to view the
     * surrounding context of the flagged content.
     *
     * @param messageId The ID of the flagged message.
     * @param parentStage The stage to return to after closing this thread view.
     */
    public static void showThreadForMessageFlag(int messageId, Stage parentStage) {
        try {
            DatabaseHelper db = new DatabaseHelper();
            Message msg = db.getMessageById(messageId);
            showThreadForQuestion("staff", db, parentStage, msg.getQuestionId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Opens the internal alert viewer which displays private alerts or messages
     * sent to users with staff or instructor roles.
     *
     * @param db Reference to the shared database.
     * @param parentStage The current stage from which this was triggered.
     * @param currentUser The username of the user viewing the alerts.
     */
    public static void showStaffAlerts(DatabaseHelper db, Stage parentStage, String currentUser) {
        InternalAlertViewer.show(db, parentStage, new User(currentUser));
    }
}