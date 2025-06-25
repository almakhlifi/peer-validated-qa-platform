package application;

import databasePart1.DatabaseHelper;
import application.Message;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Manages the system for creating, retrieving, updating, and deleting questions and answers.
 * Also handles private messaging and review functionalities.
 */


public class QuestionSystem {
    private final List<Question> questions = new ArrayList<>();
    DatabaseHelper databaseHelper = new DatabaseHelper();

    
    /**
     * Constructs a QuestionSystem with a shared DatabaseHelper instance.
     * @param dbHelper The DatabaseHelper instance to use for database operations.
     */

    public void setDatabaseHelper(DatabaseHelper dbHelper) {
        Objects.requireNonNull(dbHelper, "DatabaseHelper cannot be null when setting");
        this.databaseHelper = dbHelper;
    }

    
    /**
     * Creates a new question and saves it to the database.
     * @param title The title of the question.
     * @param content The content of the question.
     * @param author The author of the question.
     * @param tags A list of tags associated with the question.
     * @return The newly created Question object.
     */
    public Question createQuestion(String title, String content, String author, List<String> tags) {
        Question question = new Question(title, content, author, tags);
        try {
            databaseHelper.saveQuestion(question); // Save to database
        } catch (SQLException e) {
            e.printStackTrace();
        }
        questions.add(question);
        return question;
    }

    /**
     * Loads all questions from the database into the system.
     * @return A new list containing all loaded Question objects.
     * @throws SQLException If a database error occurs.
     */
    public List<Question> loadQuestions() throws SQLException {
        List<Question> loadedQuestions = databaseHelper.loadQuestions();
        questions.clear();
        questions.addAll(loadedQuestions);
        return new ArrayList<>(loadedQuestions);
    }

    /**
     * Updates an existing question in the database and the local list.
     * @param questionId The ID of the question to update.
     * @param newTitle The new title for the question.
     * @param newContent The new content for the question.
     * @return True if the question was successfully updated, false otherwise.
     */
    public boolean updateQuestion(int questionId, String newTitle, String newContent) {
        boolean success = databaseHelper.updateQuestion(questionId, newTitle, newContent);
        if (success) {
            questions.stream()
                    .filter(q -> q.getId() == questionId)
                    .findFirst()
                    .ifPresent(q -> {
                        q.setTitle(newTitle);
                        q.setContent(newContent);
                    });
        }
        return success;
    }

    /**
     * Marks a specific answer as accepted for a given question, provided the user is the author of the question.
     * Updates both the database and the local Question object.
     * @param questionId The ID of the question.
     * @param answerId The ID of the answer to mark as accepted.
     * @param author The username of the user attempting to accept the answer (must be the question's author).
     * @return True if the answer was successfully marked as accepted, false otherwise.
     * @throws SQLException If a database error occurs.
     */
    public boolean markAnswerAsAccepted(int questionId, int answerId, String author) throws SQLException {
        Question question = getQuestionById(questionId); // pull from database

        if (question != null && question.getAuthor().equals(author)) {
            boolean success = databaseHelper.markAnswerAsAccepted(questionId, answerId);
            if (success) {
                question.setAcceptedAnswerId(answerId);
            }
            return success;
        }
        return false;
    }

    /**
     * Updates the content of an existing answer in the database.
     * @param answerId The ID of the answer to update.
     * @param newContent The new content for the answer.
     * @return True if the answer was successfully updated, false otherwise.
     * @throws SQLException If a database error occurs.
     */
    public boolean updateAnswer(int answerId, String newContent) throws SQLException {
        return databaseHelper.updateAnswer(answerId, newContent);
    }

    /**
     * Deletes a question from the database and the local list, provided the user is the author.
     * @param questionId The ID of the question to delete.
     * @param author The username of the user attempting to delete the question (must be the question's author).
     * @return True if the question was successfully deleted, false otherwise.
     */
    public boolean deleteQuestion(int questionId, String author) {
        try {
            boolean success = databaseHelper.deleteQuestion(questionId);
            if (success) {
                questions.removeIf(q -> q.getId() == questionId && q.getAuthor().equals(author));
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Filters questions based on a specific tag by querying the database.
     * @param tag The tag to filter questions by.
     * @return A list of Question objects that have the specified tag.
     */
    public List<Question> filterQuestionsByTag(String tag) {
        try {
            return databaseHelper.getQuestionsByTag(tag); // Query DB for filtered results
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Returns a list of all questions currently loaded in the system.
     * @return A new list containing all Question objects.
     */
    public List<Question> getAllQuestions() {
        return new ArrayList<>(questions);
    }

    /**
     * Creates a new answer for a specific question and saves it to the database.
     * @param content The content of the answer.
     * @param author The author of the answer.
     * @param questionId The ID of the question this answer belongs to.
     * @return The newly created Answer object.
     */
    public Answer createAnswer(String content, String author, int questionId) {
        Answer answer = new Answer(content, author, questionId);

        try {
            databaseHelper.saveAnswer(answer);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Question question = questions.stream()
                .filter(q -> q.getId() == questionId)
                .findFirst()
                .orElse(null);

        if (question != null) {
            question.addAnswer(answer);
        }

        return answer;
    }

    /**
     * Retrieves all answers for a specific question from the locally loaded questions.
     * @param questionId The ID of the question.
     * @return A list of Answer objects for the given question.
     */
    public List<Answer> getAllAnswersForQuestion(int questionId) { // New method to get answers for a specific question
        return questions.stream()
                .filter(q -> q.getId() == questionId)
                .findFirst()
                .map(Question::getAnswers)
                .orElse(new ArrayList<>());
    }

    /**
     * Creates a new threaded answer (either top-level or a reply to another answer)
     * and saves it to the database.
     * @param content The content of the answer.
     * @param author The author of the answer.
     * @param questionId The ID of the question this answer belongs to.
     * @param parentAnswerId The ID of the parent answer if this is a reply, or null for a top-level answer.
     * @return The newly created Answer object.
     */
    public Answer createThreadedAnswer(String content, String author, int questionId, Integer parentAnswerId) {
        // Generate a new unique ID for this answer
        int newAnswerId = generateUniqueAnswerId();

        Answer answer = new Answer(newAnswerId, content, author, questionId, parentAnswerId);
        try {
            databaseHelper.saveThreadedAnswer(answer);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }

        Question question = questions.stream()
                .filter(q -> q.getId() == questionId)
                .findFirst()
                .orElse(null);

        if (question != null) {
            if (parentAnswerId == null) {
                question.addAnswer(answer);
            } else {
                Answer parentAnswer = findAnswerById(question, parentAnswerId);
                if (parentAnswer != null) {
                    parentAnswer.addReply(answer);
                }
            }
        }
        return answer;
    }

    /**
     * Loads all threaded answers for a specific question from the database.
     * @param questionId The ID of the question.
     * @return A list of Answer objects representing the threaded answers.
     */
    public List<Answer> loadThreadedAnswersForQuestion(int questionId) {
        try {
            return databaseHelper.loadThreadedAnswersForQuestion(questionId);
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Generates a unique ID for a new answer.
     * @return A unique integer ID.
     */
    private int generateUniqueAnswerId() {
        // Implement a method to generate a unique ID for each answer
        // This could be based on the current timestamp, a database sequence, or any other method
        // For simplicity, let's use the current timestamp in milliseconds
        return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
    }

    /**
     * Recursively finds an answer by its ID within a question's answer hierarchy.
     * @param question The question containing the answers.
     * @param answerId The ID of the answer to find.
     * @return The Answer object if found, null otherwise.
     */
    private Answer findAnswerById(Question question, int answerId) {
        return findAnswerRecursive(question.getAnswers(), answerId);
    }

    /**
     * Helper method to recursively search for an answer by its ID within a list of answers and their replies.
     * @param answers The list of answers to search within.
     * @param answerId The ID of the answer to find.
     * @return The Answer object if found, null otherwise.
     */
    private Answer findAnswerRecursive(List<Answer> answers, int answerId) {
        for (Answer answer : answers) {
            if (answer.getId() == answerId) {
                return answer;
            }
            Answer found = findAnswerRecursive(answer.getReplies(), answerId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * Creates a reply to an existing answer for a specific question and saves it to the database.
     * @param content The content of the reply.
     * @param author The author of the reply.
     * @param questionId The ID of the question the answer belongs to.
     * @param answerId The ID of the answer being replied to.
     * @return The newly created Answer object representing the reply.
     */
    public Answer replyToAnswer(String content, String author, int questionId, int answerId) {
        Answer reply = new Answer(content, author, questionId);

        try {
            databaseHelper.saveAnswer(reply); // Save reply to database
        } catch (SQLException e) {
            e.printStackTrace();
        }

        questions.stream()
                .filter(q -> q.getId() == questionId)
                .findFirst()
                .ifPresent(question -> {
                    question.getAnswers().stream()
                            .filter(a -> a.getId() == answerId)
                            .findFirst()
                            .ifPresent(answer -> answer.addReply(reply));
                });

        return reply;
    }

    /**
     * Retrieves all replies for a specific answer within a question.
     * @param questionId The ID of the question.
     * @param answerId The ID of the answer to get replies for.
     * @return A list of Answer objects that are replies to the specified answer.
     */
    @SuppressWarnings("unchecked")
    public List<Answer> getRepliesForAnswer(int questionId, int answerId) {
        return (List<Answer>) questions.stream()
                .filter(q -> q.getId() == questionId)
                .findFirst()
                .flatMap(question -> question.getAnswers().stream()
                        .filter(a -> a.getId() == answerId)
                        .findFirst())
                .map(Answer::getReplies)
                .orElse(new ArrayList<>());
    }

    /**
     * Sends a private message between two users.
     * @param sender The username of the sender.
     * @param recipient The username of the recipient.
     * @param questionId The ID of the related question (-1 if not related).
     * @param answerId The ID of the related answer (-1 if not related).
     * @param content The content of the message.
     * @param messageType The type of the message.
     */
    public void sendMessage(String sender, String recipient, int questionId, int answerId, String content, String messageType) {
        try {
            databaseHelper.sendMessage(sender, recipient, questionId, answerId, content, messageType);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the message history between two users, optionally filtered by a specific question and answer.
     * @param user1 The username of the first user.
     * @param user2 The username of the second user.
     * @param questionId The ID of the related question (-1 for all).
     * @param answerId The ID of the related answer (-1 for all).
     * @return A list of Message objects exchanged between the users.
     */
    public List<Message> getMessagesBetween(String user1, String user2, int questionId, int answerId) {
        try {
            return databaseHelper.getMessagesBetween(user1, user2, questionId, answerId);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Gets the count of unread messages for a specific recipient, optionally filtered by a question and answer.
     * @param recipient The username of the recipient.
     * @param questionId The ID of the related question (-1 for all).
     * @param answerId The ID of the related answer (-1 for all).
     * @return The number of unread messages.
     */
    public int getUnreadMessageCount(String recipient, int questionId, int answerId) {
        try {
            return databaseHelper.getUnreadMessageCount(recipient, questionId, answerId);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Marks messages as read for a specific recipient, optionally filtered by a question and answer.
     * @param recipient The username of the recipient.
     * @param questionId The ID of the related question (-1 for all).
     * @param answerId The ID of the related answer (-1 for all).
     */
    public void markMessagesAsRead(String recipient, int questionId, int answerId) {
        try {
            databaseHelper.markMessagesAsRead(recipient, questionId, answerId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new review and saves it to the database.
     * @param review The Review object to save.
     * @throws SQLException If a database error occurs.
     */
    public void createReview(Review review) throws SQLException {
        databaseHelper.saveReview(review);

    }

    /**
     * Updates an existing review by creating a new review that links to the old one.
     * @param newVersionReview The new version of the review to save. Must have a previousReviewId set.
     * @throws SQLException If a database error occurs.
     * @throws IllegalArgumentException If the provided review does not have a previousReviewId.
     */
    public void updateReview(Review newVersionReview) throws SQLException {
        if (newVersionReview.getPreviousReviewId() == null) {
            throw new IllegalArgumentException("Review must have a previousReviewId to be considered an update.");
        }
        databaseHelper.saveReview(newVersionReview);

    }

    /**
     * Deletes a review from the database.
     * @param reviewId The ID of the review to delete.
     * @return True if the review was successfully deleted, false otherwise.
     * @throws SQLException If a database error occurs.
     */
    public boolean deleteReview(int reviewId) throws SQLException {
        return databaseHelper.deleteReview(reviewId);
    }

    /**
     * Retrieves the latest reviews for a specific question.
     * @param questionId The ID of the question.
     * @return A list of the latest Review objects for the question.
     * @throws SQLException If a database error occurs.
     */
    public List<Review> getLatestReviewsForQuestion(int questionId) throws SQLException {
        return databaseHelper.getLatestReviewsForQuestion(questionId);
    }

    /**
     * Retrieves the latest reviews for a specific answer.
     * @param answerId The ID of the answer.
     * @return A list of the latest Review objects for the answer.
     * @throws SQLException If a database error occurs.
     */
    public List<Review> getLatestReviewsForAnswer(int answerId) throws SQLException {
        return databaseHelper.getLatestReviewsForAnswer(answerId);
    }

    /**
     * Retrieves all reviews created by a specific reviewer.
     * @param reviewerUsername The username of the reviewer.
     * @return A list of all Review objects created by the reviewer.
     * @throws SQLException If a database error occurs.
     */
    public List<Review> getAllReviewsByReviewer(String reviewerUsername) throws SQLException {
        return databaseHelper.getAllReviewsByReviewer(reviewerUsername);
    }

    /**
     * Retrieves a specific review by its ID.
     * @param reviewId The ID of the review.
     * @return The Review object with the given ID, or null if not found.
     * @throws SQLException If a database error occurs.
     */
    public Review getReviewById(int reviewId) throws SQLException {
        return databaseHelper.getReviewById(reviewId);
    }

    /**
     * Retrieves the latest review submitted by a specific user for a specific target (question or answer).
     * @param reviewerUsername The username of the reviewer.
     * @param targetType The type of the target ("question" or "answer").
     * @param targetId The ID of the target.
     * @return The latest Review object submitted by the user for the target, or null if none found.
     * @throws SQLException If a database error occurs.
     */
    public Review getLatestReviewByUserForTarget(String reviewerUsername, String targetType, int targetId) throws SQLException {
        return databaseHelper.getLatestReviewByUserForTarget(reviewerUsername, targetType, targetId);
    }

    /**
     * Checks if a given user has the 'reviewer' role.
     * @param username The username to check.
     * @return True if the user has the 'reviewer' role, false otherwise.
     */
    public boolean isCurrentUserReviewer(String username) {
        try {
            List<String> roles = databaseHelper.getUserRoles(username);
            return roles != null && roles.contains("reviewer");
        } catch (SQLException e) {
            System.err.println("Error checking user roles: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves a specific question by its ID from the database.
     * @param questionId The ID of the question to retrieve.
     * @return The Question object with the given ID, or null if not found.
     * @throws SQLException If a database error occurs.
     */
    public Question getQuestionById(int questionId) throws SQLException {
        return databaseHelper.getQuestionById(questionId);
    }

    /**
     * Retrieves a specific answer by its ID from the database.
     * @param answerId The ID of the answer to retrieve.
     * @return The Answer object with the given ID, or null if not found.
     * @throws SQLException If a database error occurs.
     */
    public Answer getAnswerById(int answerId) throws SQLException {
        return databaseHelper.getAnswerById(answerId);

    }

    /**
     * Retrieves all answers from the database.
     * @return A list of all Answer objects in the database.
     * @throws SQLException If a database error occurs.
     */
    public List<Answer> getAllAnswers() throws SQLException {
        return databaseHelper.getAllAnswers();
    }
}