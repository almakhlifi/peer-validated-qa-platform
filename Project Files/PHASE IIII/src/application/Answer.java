package application;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an answer to a question, which can also be a reply to another answer.
 */
public class Answer {
    /**
     * Static counter to generate unique IDs for new answers not yet saved to the database.
     * Note: This is a simple in-memory counter and might not be suitable for production environments
     * without proper synchronization or database sequence usage.
     */
    private static int nextId = 1;
    /**
     * The unique identifier for this answer.
     */
    private int id;
    /**
     * The textual content of the answer.
     */
    private String content;
    /**
     * The username of the user who authored the answer.
     */
    private final String author;
    /**
     * The ID of the question this answer belongs to.
     */
    private final int questionId; // Link to Question
    /**
     * The ID of the parent answer if this is a reply, otherwise null for top-level answers.
     */
    private Integer parentAnswerId; // Null if it's a top-level answer
    /**
     * A list of answers that are direct replies to this answer.
     */
    private List<Answer> replies; // List of replies

    /**
     * Constructor for creating a new top-level answer (not a reply).
     * Uses the static counter for the initial ID.
     *
     * @param content    The content of the answer.
     * @param author     The username of the author.
     * @param questionId The ID of the question being answered.
     */
    public Answer(String content, String author, int questionId) {
        this.id = nextId++; // Assign temporary ID
        this.content = content;
        this.author = author;
        this.questionId = questionId;
        this.parentAnswerId = null; // Explicitly null for top-level
        this.replies = new ArrayList<>();
    }

    /**
     * Constructor for creating a new reply to an existing answer.
     * Uses the static counter for the initial ID.
     *
     * @param content        The content of the reply.
     * @param author         The username of the author.
     * @param questionId     The ID of the original question.
     * @param parentAnswerId The ID of the answer this is replying to.
     */
    public Answer(String content, String author, int questionId, int parentAnswerId) {
        this.id = nextId++; // Assign temporary ID
        this.content = content;
        this.author = author;
        this.questionId = questionId;
        this.parentAnswerId = parentAnswerId;
        this.replies = new ArrayList<>();
    }

    /**
     * Constructor used when loading an answer from the database.
     * Assigns the ID provided from the database record.
     *
     * @param id             The database ID of the answer.
     * @param content        The content of the answer.
     * @param author         The username of the author.
     * @param questionId     The ID of the associated question.
     * @param parentAnswerId The ID of the parent answer, or null if it's a top-level answer.
     */
    public Answer(int id, String content, String author, int questionId, Integer parentAnswerId) {
        this.id = id; // Assign database ID
        this.content = content;
        this.author = author;
        this.questionId = questionId;
        this.parentAnswerId = parentAnswerId;
        this.replies = new ArrayList<>();
        // Potentially update nextId if loading from DB to avoid collisions, though risky.
        // Consider using UUIDs or database sequences for robust ID generation.
        if (id >= nextId) {
             nextId = id + 1; // Simple strategy, might have gaps or issues in concurrent scenarios
        }
    }

    /**
     * Sets the ID of the answer, typically used after saving to the database
     * and retrieving the auto-generated ID.
     *
     * @param id The new ID for the answer.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Gets the unique ID of the answer.
     * @return The answer ID.
     */
    public int getId() { return this.id; }

    /**
     * Gets the content of the answer.
     * @return The answer content.
     */
    public String getContent() { return content; }

    /**
     * Gets the username of the author.
     * @return The author's username.
     */
    public String getAuthor() { return author; }

    /**
     * Gets the ID of the associated question.
     * @return The question ID.
     */
    public int getQuestionId() { return questionId; }

    /**
     * Gets the ID of the parent answer.
     * @return The parent answer ID, or null if this is a top-level answer.
     */
    public Integer getParentAnswerId() { return parentAnswerId; }

    /**
     * Gets the list of replies to this answer.
     * @return A list of {@link Answer} objects representing replies.
     */
    public List<Answer> getReplies() { return replies; }

    /**
     * Sets the content of the answer.
     * @param content The new content for the answer.
     */
    public void setContent(String content) { this.content = content; }

    /**
     * Adds a reply to this answer's list of replies.
     *
     * @param reply The {@link Answer} object representing the reply to add.
     */
    public void addReply(Answer reply) {
        if (reply != null) {
            replies.add(reply);
        }
    }
}