package application;

import java.sql.Timestamp;
import java.time.Instant; // Use Instant for default timestamp
import java.util.Objects; // For equals/hashCode

/**
 * Review
 * 
 * Represents a review entity for either a question or an answer. Each review can have multiple versions, 
 * with only the latest version being considered the current review for a particular user-target combination.
 */
public class Review {
    private int id; // Unique ID for this specific version of the review
    private String reviewerUsername;
    private String targetType; // "question" or "answer"
    private int targetId;      // ID of the question or answer being reviewed
    private int rating;        // 1 to 5
    private String comment;    // Optional comment
    private Timestamp timestamp; // When this review version was created/saved
    private Integer previousReviewId; // ID of the review this one replaces (null if first)
    private boolean isLatest;         // True if this is the current/active review for the target by the user

    // Constructor for loading a complete Review record from the Database
    /**
     * Constructs a Review object from a fully loaded database record.
     *
     * @param id                The unique identifier for the review.
     * @param reviewerUsername  The username of the reviewer.
     * @param targetType        The type of target being reviewed ("question" or "answer").
     * @param targetId          The ID of the target being reviewed.
     * @param rating            The rating given by the reviewer (1-5).
     * @param comment           An optional comment provided by the reviewer.
     * @param timestamp         The timestamp when the review was created.
     * @param previousReviewId  The ID of the previous review this one replaces (null if first).
     * @param isLatest          Whether this is the latest version of the review for the target.
     * @throws IllegalArgumentException If required fields are missing or invalid.
     */
    public Review(int id, String reviewerUsername, String targetType, int targetId, int rating, String comment, Timestamp timestamp, Integer previousReviewId, boolean isLatest) {
        // Basic validation for required fields
        if (reviewerUsername == null || reviewerUsername.trim().isEmpty() ||
            targetType == null || (!targetType.equals("question") && !targetType.equals("answer")) ||
            rating < 1 || rating > 5 || timestamp == null) {
            throw new IllegalArgumentException("Invalid arguments for Review constructor (DB load): Missing required fields or invalid rating/type.");
        }
        this.id = id;
        this.reviewerUsername = reviewerUsername.trim(); // Trim username
        this.targetType = targetType;
        this.targetId = targetId;
        this.rating = rating;
        this.comment = comment; // Comment can be null or empty
        this.timestamp = timestamp;
        this.previousReviewId = previousReviewId; // Can be null
        this.isLatest = isLatest;
    }

    // Constructor for creating a brand NEW review (first version) in the application
    // ID will be set later by the database. Timestamp set now.
    /**
     * Constructs a new review to be saved into the database.
     *
     * @param reviewerUsername  The username of the reviewer.
     * @param targetType        The type of target being reviewed ("question" or "answer").
     * @param targetId          The ID of the target being reviewed.
     * @param rating            The rating given by the reviewer (1-5).
     * @param comment           An optional comment provided by the reviewer.
     */
    public Review(String reviewerUsername, String targetType, int targetId, int rating, String comment) {
         this(0, // ID is 0 initially, will be set by DB upon insertion
              reviewerUsername,
              targetType,
              targetId,
              rating,
              comment,
              Timestamp.from(Instant.now()), // Set current timestamp
              null,  // No previous ID for a brand new review chain
              true); // A newly created review is always the latest initially
    }

    // Constructor for creating an UPDATED review (a new version replacing an old one) in the application.
    // Takes the previous review's ID. ID is 0 initially, Timestamp set now.
    /**
     * Constructs an updated review version to replace an existing one.
     *
     * @param reviewerUsername  The username of the reviewer.
     * @param targetType        The type of target being reviewed ("question" or "answer").
     * @param targetId          The ID of the target being reviewed.
     * @param rating            The rating given by the reviewer (1-5).
     * @param comment           An optional comment provided by the reviewer.
     * @param previousReviewId  The ID of the previous review being replaced.
     * @throws IllegalArgumentException If the previous review ID is invalid.
     */
    public Review(String reviewerUsername, String targetType, int targetId, int rating, String comment, int previousReviewId) {
         this(0, // ID is 0 initially, will be set by DB
              reviewerUsername,
              targetType,
              targetId,
              rating,
              comment,
              Timestamp.from(Instant.now()), // Set current timestamp for the new version
              previousReviewId, // Link to the review being replaced
              true); // This new version becomes the latest
          // Add validation specific to updates
          if (previousReviewId <= 0) {
               throw new IllegalArgumentException("Previous Review ID must be a positive value for an update.");
          }
    }

    // --- Getters ---
    /**
     * Gets the unique ID for this review version.
     *
     * @return The ID of the review.
     */
    public int getId() { return id; }

    /**
     * Gets the username of the reviewer.
     *
     * @return The reviewer's username.
     */
    public String getReviewerUsername() { return reviewerUsername; }

    /**
     * Gets the type of the target being reviewed.
     *
     * @return The type of the target ("question" or "answer").
     */
    public String getTargetType() { return targetType; }

    /**
     * Gets the ID of the target being reviewed.
     *
     * @return The ID of the target.
     */
    public int getTargetId() { return targetId; }

    /**
     * Gets the rating given by the reviewer.
     *
     * @return The rating (1-5).
     */
    public int getRating() { return rating; }

    /**
     * Gets the comment provided by the reviewer.
     *
     * @return The comment, or null if no comment was provided.
     */
    public String getComment() { return comment; }

    /**
     * Gets the timestamp when this review version was created.
     *
     * @return The timestamp.
     */
    public Timestamp getTimestamp() { return timestamp; }

    /**
     * Gets the ID of the previous review version this one replaces.
     *
     * @return The ID of the previous review, or null if this is the first version.
     */
    public Integer getPreviousReviewId() { return previousReviewId; }

    /**
     * Checks if this is the latest version of the review for the target.
     *
     * @return True if this is the latest version, false otherwise.
     */
    public boolean isLatest() { return isLatest; }

    // --- Setters ---
    /**
     * Sets the unique ID for this review version, usually after it has been saved to the database.
     *
     * @param id The positive ID assigned by the database.
     * @throws IllegalArgumentException If the ID is non-positive.
     */
    public void setId(int id) {
        if (id <= 0) throw new IllegalArgumentException("Review ID must be positive.");
        this.id = id;
    }

    /**
     * Sets the 'isLatest' status. This is typically managed by the database logic
     * when a newer version is created.
     *
     * @param latest True if this is the current version, false otherwise.
     */
    public void setLatest(boolean latest) {
        isLatest = latest;
    }

    /**
     * Allows setting the timestamp externally, e.g., if creating object before immediate saving.
     *
     * @param timestamp The timestamp for the review version. Cannot be null.
     * @throws IllegalArgumentException If the timestamp is null.
     */
    public void setTimestamp(Timestamp timestamp) {
        if (timestamp == null) throw new IllegalArgumentException("Timestamp cannot be null.");
        this.timestamp = timestamp;
    }

    /**
     * Sets the ID of the review this version replaces. Should generally only be set
     * via the constructor for updated reviews.
     *
     * @param previousReviewId The ID of the previous version, or null if it's the first version.
     * @throws IllegalArgumentException If the previous review ID is non-positive.
     */
    public void setPreviousReviewId(Integer previousReviewId) {
         if (previousReviewId != null && previousReviewId <= 0) {
             throw new IllegalArgumentException("Previous Review ID must be positive if set.");
         }
        this.previousReviewId = previousReviewId;
    }

    // --- Overrides ---

    /**
     * Provides a concise string representation for debugging or simple list display.
     *
     * @return A formatted string representing the review.
     */
    @Override
    public String toString() {
        // Provides a concise string representation for debugging or simple list display
        String targetInfo = String.format("%s:%d", targetType, targetId);
        String versionInfo = isLatest ? "Latest" : "Old(Prev:" + (previousReviewId != null ? previousReviewId : "None") + ")";
        String timeStr = (timestamp != null) ? timestamp.toString().substring(0, 19) : "N/A"; // Format YYYY-MM-DD HH:MM:SS

        return String.format("Review ID=%d | By=%s | Target=%s | Rating=%d | %s | Time=%s",
                id, reviewerUsername, targetInfo, rating, versionInfo, timeStr);
    }

    /**
     * Compares this Review object to another for equality. Two reviews are equal if their IDs match
     * (once saved to the database) or if their content fields match (before saving).
     *
     * @param o The object to compare.
     * @return True if the objects are equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Review review = (Review) o;
        // If IDs are assigned (from DB), they are the primary key for equality.
        if (id > 0 && review.id > 0) {
            return id == review.id;
        }
        // If IDs aren't set (e.g., before saving), compare content fields for potential equality.
        // This might not be strictly necessary depending on usage.
        return targetId == review.targetId &&
               rating == review.rating &&
               isLatest == review.isLatest && // Include isLatest? Maybe not if only comparing content.
               Objects.equals(reviewerUsername, review.reviewerUsername) &&
               Objects.equals(targetType, review.targetType) &&
               Objects.equals(comment, review.comment) &&
               Objects.equals(timestamp, review.timestamp) && // Timestamps might differ slightly
               Objects.equals(previousReviewId, review.previousReviewId);
    }

    /**
     * Generates a hash code for this Review object. Uses the ID if available, otherwise hashes relevant content fields.
     *
     * @return The hash code.
     */
    @Override
    public int hashCode() {
        // Hash based on ID if available, otherwise hash relevant content fields.
        if (id > 0) {
            return Objects.hash(id);
        }
        return Objects.hash(reviewerUsername, targetType, targetId, rating, comment, timestamp, previousReviewId, isLatest);
    }
}
