package application;

import java.time.LocalDateTime;


/**
 * Represents a flag created by a staff member on a question, answer, or feedback.
 * Used to report potential issues or inappropriate content in the system.
 */
public class Flag {
    
	/**
     * Types of items that can be flagged.
     */
	public enum FlagType {
        QUESTION,
        ANSWER,
        MESSAGE
    }
	
    private FlagType type;
    private String itemId;      // ID of the question, answer, or feedback
    private String flaggedBy;   // Staff member who flagged
    private String reason;
    private LocalDateTime timestamp;
    
    /**
     * Constructs a Flag with a specific timestamp (used when loading from the database).
     *
     * @param type The type of item being flagged (e.g., QUESTION, ANSWER, MESSAGE).
     * @param itemId The ID of the item being flagged.
     * @param flaggedBy The username of the person who flagged the item.
     * @param reason The reason the item was flagged.
     * @param timestamp The date and time when the flag was originally submitted.
     */
    public Flag(FlagType type, String itemId, String flaggedBy, String reason, LocalDateTime timestamp) {
        this.type = type;
        this.itemId = itemId;
        this.flaggedBy = flaggedBy;
        this.reason = reason;
        this.timestamp = timestamp;
    }
    
    /**
     * @return the type of the flagged item
     */
    public FlagType getType() {
        return type;
    }
    
    /**
     * @return the ID of the item that was flagged
     */
    public String getItemId() {
        return itemId;
    }
    
    /**
     * @return the username or ID of the staff who flagged the item
     */
    public String getFlaggedBy() {
        return flaggedBy;
    }
    
    /**
     * @return the reason or explanation provided for the flag
     */
    public String getReason() {
        return reason;
    }
    
    /**
     * @return the timestamp when the flag was created
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * Returns a readable string summarizing the flag.
     *
     * @return a formatted string with type, author, time, and reason
     */
    @Override
    public String toString() {
        return "[" + type + "] flagged by " + flaggedBy + " at " + timestamp + ": " + reason;
    }
}