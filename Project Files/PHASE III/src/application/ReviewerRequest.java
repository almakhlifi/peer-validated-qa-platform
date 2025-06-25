/**
 * ReviewerRequest
 * 
 * Represents a request made by a reviewer within the application. Each request includes the reviewer's username,
 * the status of the request, and the timestamp when the request was created.
 */
package application;

import java.sql.Timestamp;

public class ReviewerRequest {
    private String username;
    private String status;
    private Timestamp timestamp;

    /**
     * Constructs a new ReviewerRequest object with the specified username, status, and timestamp.
     *
     * @param username The username of the reviewer.
     * @param status   The status of the request (e.g., "Pending", "Approved", "Rejected").
     * @param timestamp The timestamp when the request was created.
     * @throws IllegalArgumentException If the username is null or empty, or if the timestamp is null.
     */
    public ReviewerRequest(String username, String status, Timestamp timestamp) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Reviewer username cannot be null or empty.");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null.");
        }
        this.username = username;
        this.status = status;
        this.timestamp = timestamp;
    }

    /**
     * Gets the username of the reviewer.
     *
     * @return The reviewer's username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the status of the request.
     *
     * @return The status of the request.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Gets the timestamp when the request was created.
     *
     * @return The timestamp.
     */
    public Timestamp getTimestamp() {
        return timestamp;
    }

    /**
     * Provides a string representation of the ReviewerRequest object.
     *
     * @return A formatted string representing the reviewer's username, status, and timestamp.
     */
    @Override
    public String toString() {
        return username + " (" + status + " at " + timestamp + ")";
    }
}