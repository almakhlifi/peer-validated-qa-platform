/**
 * TrustedReviewerAdvancedTest
 * 
 * JUnit test class for advanced functionality related to trusted reviewers.
 */
package application;

import databasePart1.DatabaseHelper;
import org.junit.jupiter.api.*;
import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TrustedReviewerAdvancedTest {

    private static DatabaseHelper db;
    private final String student = "adwait_student";
    private final String reviewer = "trusted_reviewer";
    private final int answerId = 101;

    /**
     * Sets up the database connection and ensures the necessary tables exist before running tests.
     *
     * @throws SQLException If there is an issue connecting to the database or creating tables.
     */
    @BeforeAll
    public static void setupDB() throws SQLException {
        db = new DatabaseHelper();
        db.connectToDatabase();
        db.createTables();
    }

    /**
     * Cleans up the database state before each test to ensure consistent conditions.
     *
     * @throws SQLException If there is an issue clearing reviews, trusted reviewers, or messages.
     */
    @BeforeEach
    public void cleanState() throws SQLException {
        // Ensure users exist
        if (!db.doesUserExist(student)) {
            db.register(new User(student, "pass", List.of("student")));
        }
        if (!db.doesUserExist(reviewer)) {
            db.register(new User(reviewer, "pass", List.of("reviewer")));
        }

        // Remove trusted reviewer relationship
        db.removeTrustedReviewer(student, reviewer);

        // Clear any existing updates for the reviewer
        db.clearUpdateForReviewer(student, reviewer);

        // Clear all reviews and messages
        db.clearAllReviews();
        db.clearAllMessages();
    }

    /**
     * Tests whether adding a trusted reviewer with a default weight is successful.
     *
     * @throws SQLException If there is an issue interacting with the database.
     */
    @Test
    @Order(1)
    public void testAddTrustedReviewerWithDefaultWeight() throws SQLException {
        db.addOrUpdateTrustedReviewer(student, reviewer, 1);
        Map<String, Integer> trusted = db.getTrustedReviewersWithWeights(student);
        assertEquals(1, trusted.get(reviewer), "The default weight should be set correctly.");
    }

    /**
     * Tests whether updating a trusted reviewer's weight is successful.
     *
     * @throws SQLException If there is an issue interacting with the database.
     */
    @Test
    @Order(2)
    public void testUpdateTrustedReviewerWeight() throws SQLException {
        db.addOrUpdateTrustedReviewer(student, reviewer, 2);
        db.addOrUpdateTrustedReviewer(student, reviewer, 5); // Update weight
        Map<String, Integer> weights = db.getTrustedReviewersWithWeights(student);
        assertEquals(5, weights.get(reviewer), "The updated weight should reflect the latest value.");
    }

    /**
     * Tests whether a notification is shown for an updated review by a trusted reviewer.
     *
     * @throws SQLException If there is an issue interacting with the database.
     */
    @Test
    @Order(3)
    public void testShowNotificationForUpdatedReview() throws SQLException {
        db.saveReview(reviewer, answerId, 4, "Initial review");
        db.saveReview(reviewer, answerId, 5, "Updated review");

        db.addOrUpdateTrustedReviewer(student, reviewer, 3);

        Set<String> updated = db.getUpdatedTrustedReviewers(student);
        assertTrue(updated.contains(reviewer), "A notification should appear for the updated review.");
    }

    /**
     * Tests whether clearing an update notification for a trusted reviewer works as expected.
     *
     * @throws SQLException If there is an issue interacting with the database.
     */
    @Test
    @Order(4)
    public void testClearUpdateNotification() throws SQLException {
        db.saveReview(reviewer, answerId, 4, "Initial review");
        db.saveReview(reviewer, answerId, 5, "Updated review");

        db.addOrUpdateTrustedReviewer(student, reviewer, 3);
        db.clearUpdateForReviewer(student, reviewer);

        Set<String> updated = db.getUpdatedTrustedReviewers(student);
        assertFalse(updated.contains(reviewer), "The update notification should be cleared.");
    }

    /**
     * Tests whether viewing a trusted reviewer's profile loads their reviews correctly.
     *
     * @throws SQLException If there is an issue interacting with the database.
     */
    @Test
    @Order(5)
    public void testReviewerProfileViewLoadsReviews() throws SQLException {
        db.saveReview(reviewer, answerId, 3, "Profile test review");
        List<Review> reviews = db.getAllReviewsByReviewer(reviewer);

        assertNotNull(reviews, "The list of reviews should not be null.");
        assertFalse(reviews.isEmpty(), "The list of reviews should not be empty.");
        assertEquals(reviewer, reviews.get(0).getReviewerUsername(), "The reviews should belong to the specified reviewer.");
    }
}
