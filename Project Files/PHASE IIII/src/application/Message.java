package application;

import java.sql.Timestamp;

/**
 * Represents a message exchanged between users in the application.
 */
public class Message {
    private int id;
    private String sender;
    private String recipient;
    private int questionId;
    private int answerId;
    private String content;
    private Timestamp timestamp;
    private boolean isRead;
    private String messageType;

    /**
     * Constructs a new Message object.
     * @param id The unique identifier for the message.
     * @param sender The username of the sender.
     * @param recipient The username of the recipient.
     * @param questionId The ID of the question related to the message (-1 if not related).
     * @param answerId The ID of the answer related to the message (-1 if not related).
     * @param content The content of the message.
     * @param timestamp The timestamp when the message was sent.
     * @param isRead Indicates whether the message has been read by the recipient.
     * @param messageType The type of the message (e.g., "question", "review-feedback").
     */
    public Message(int id, String sender, String recipient, int questionId, int answerId,
            String content, Timestamp timestamp, boolean isRead, String messageType) {
    	this.id = id;
    	this.sender = sender;
    	this.recipient = recipient;
    	this.questionId = questionId;
    	this.answerId = answerId;
    	this.content = content;
    	this.timestamp = timestamp;
    	this.isRead = isRead;
    	this.messageType = messageType;
	}

    /**
     * Returns the unique identifier of the message.
     * @return The message ID.
     */
    public int getId() { return id; }

    /**
     * Returns the username of the sender.
     * @return The sender's username.
     */
    public String getSender() { return sender; }

    /**
     * Returns the username of the recipient.
     * @return The recipient's username.
     */
    public String getRecipient() { return recipient; }

    /**
     * Returns the ID of the question related to the message.
     * @return The question ID, or -1 if not related to a question.
     */
    public int getQuestionId() { return questionId; }

    /**
     * Returns the ID of the answer related to the message.
     * @return The answer ID, or -1 if not related to an answer.
     */
    public int getAnswerId() { return answerId; }

    /**
     * Returns the content of the message.
     * @return The message content.
     */
    public String getContent() { return content; }

    /**
     * Returns the timestamp when the message was sent.
     * @return The timestamp of the message.
     */
    public Timestamp getTimestamp() { return timestamp; }

    /**
     * Indicates whether the message has been read by the recipient.
     * @return True if the message has been read, false otherwise.
     */
    public boolean isRead() { return isRead; }

    /**
     * Returns the type of the message.
     * @return The message type (e.g., "question", "review-feedback").
     */
    public String getMessageType() {
        return messageType;
    }

    /**
     * Sets the read status of the message.
     * @param read True to mark the message as read, false otherwise.
     */
    public void setRead(boolean read) {
        this.isRead = read;
    }
}