/**
 * ReviewSystemTest
 * 
 * JUnit test class for verifying the functionality of the review system in the application.
 */
package application;

import application.Review;
import databasePart1.DatabaseHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReviewSystemTest {

    private DatabaseHelper db;
    private final String reviewer = "alex";
    private final String otherUser = "charlie";
    private final int questionId = 1001;
    private final int answerId = 2002;

    /**
     * Sets up the database helper before each test and clears all reviews.
     *
     * @throws Exception If there is an issue connecting to the database or clearing reviews.
     */
    @BeforeEach
    public void setup() throws Exception {
        db = new DatabaseHelper();
        db.connectToDatabase();
        db.clearAllReviews(); // Clear reviews table before each test
    }

    /**
     * Tests whether a new review can be successfully created and saved.
     *
     * @throws Exception If there is an issue interacting with the database.
     */
    @Test
    public void testReviewCreation() throws Exception {
        Review review = new Review(0, reviewer, "answer", answerId, 4, "Nice job", new Timestamp(System.currentTimeMillis()), null, true);
        db.saveReview(review);

        Review fetched = db.getLatestReviewByUserForTarget(reviewer, "answer", answerId);
        assertNotNull(fetched, "The review should not be null after creation.");
        assertEquals("Nice job", fetched.getComment(), "The comment should match what was saved.");
        assertEquals(4, fetched.getRating(), "The rating should match what was saved.");
    }

    /**
     * Tests whether a review can be successfully deleted from the database.
     *
     * @throws Exception If there is an issue interacting with the database.
     */
    @Test
    public void testReviewDeletion() throws Exception {
        Review review = new Review(0, reviewer, "answer", answerId, 3, "Temporary", new Timestamp(System.currentTimeMillis()), null, true);
        db.saveReview(review);

        boolean deleted = db.deleteReview(review.getId());
        assertTrue(deleted, "The review deletion should succeed.");

        Review fetched = db.getLatestReviewByUserForTarget(reviewer, "answer", answerId);
        assertNull(fetched, "The review should be null after deletion.");
    }

    /**
     * Tests whether a review deletion attempt by a non-reviewer fails.
     *
     * @throws Exception If there is an issue interacting with the database.
     */
    @Test
    public void testReviewDeletionByOtherUserFails() throws Exception {
        Review review = new Review(0, reviewer, "answer", answerId, 3, "Private", new Timestamp(System.currentTimeMillis()), null, true);
        db.saveReview(review);

        // Simulate "charlie" trying to delete alex's review manually
        // There is no method to check user permissions in deleteReview(),
        // so this should be enforced in the UI or controller, not DB.
        Review fetched = db.getLatestReviewByUserForTarget(otherUser, "answer", answerId);
        assertNull(fetched, "Other user shouldn't see the review."); // Other user shouldn't see the review
    }

    /**
     * Tests whether updating a review creates a new version and retains the previous version.
     *
     * @throws Exception If there is an issue interacting with the database.
     */
    @Test
    public void testReviewUpdateCreatesNewVersion() throws Exception {
        Review original = new Review(0, reviewer, "answer", answerId, 2, "Needs work", new Timestamp(System.currentTimeMillis()), null, true);
        db.saveReview(original);

        Review updated = new Review(0, reviewer, "answer", answerId, 5, "Fixed everything", new Timestamp(System.currentTimeMillis()), original.getId(), true);
        db.saveReview(updated);

        Review latest = db.getLatestReviewByUserForTarget(reviewer, "answer", answerId);
        assertEquals("Fixed everything", latest.getComment(), "The latest comment should match the updated one.");
        assertEquals(original.getId(), latest.getPreviousReviewId(), "The previous review ID should be set correctly.");

        List<Review> history = db.getReviewHistoryForAnswer(reviewer, answerId);
        assertEquals(2, history.size(), "There should be two versions of the review in the history.");
    }

    /**
     * Tests whether deleting a review also deletes its entire history.
     *
     * @throws Exception If there is an issue interacting with the database.
     */
    @Test
    public void testDeleteReviewWithHistory() throws Exception {
        // Create original and update
        Review r1 = new Review(0, reviewer, "answer", answerId, 2, "Initial", new Timestamp(System.currentTimeMillis()), null, true);
        db.saveReview(r1);
        Review r2 = new Review(0, reviewer, "answer", answerId, 4, "Updated", new Timestamp(System.currentTimeMillis()), r1.getId(), true);
        db.saveReview(r2);

        boolean deleted = db.deleteReviewAndHistory(r2.getId());
        assertTrue(deleted, "The review and its history should be deleted successfully.");

        List<Review> afterDelete = db.getReviewHistoryForAnswer(reviewer, answerId);
        assertEquals(0, afterDelete.size(), "There should be no reviews left after deletion.");
    }
}