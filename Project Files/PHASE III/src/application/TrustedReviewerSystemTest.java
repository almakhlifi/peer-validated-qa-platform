/**
 * TrustedReviewerSystemTest
 * 
 * JUnit test class for verifying the functionality of the trusted reviewer system.
 */
package application;

import databasePart1.DatabaseHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TrustedReviewerSystemTest {

    private DatabaseHelper db;
    private final String student = "maan";
    private final String reviewer = "alex";
    private final int answerId = 123;

    /**
     * Sets up the database helper before each test and ensures the necessary tables exist.
     *
     * @throws Exception If there is an issue connecting to the database or creating tables.
     */
    @BeforeEach
    public void setup() throws Exception {
        db = new DatabaseHelper();
        db.connectToDatabase();
        db.createTables();

        // Patch missing column in older databases
        try (Statement stmt = db.getConnection().createStatement()) {
            stmt.execute("ALTER TABLE messages ADD COLUMN IF NOT EXISTS message_type VARCHAR(30) DEFAULT 'question'");
        }

        // Clean up any existing reviews or messages
        db.clearAllReviews();
        db.clearAllMessages();
        db.removeTrustedReviewer(student, reviewer);
    }

    /**
     * Tests whether a student can view the latest reviews for a specific answer.
     *
     * @throws Exception If there is an issue interacting with the database.
     */
    @Test
    public void testStudentCanViewAnswerReviews() throws Exception {
        Review review = new Review(0, reviewer, "answer", answerId, 5, "Excellent answer", new Timestamp(System.currentTimeMillis()), null, true);
        db.saveReview(review);

        List<Review> reviews = db.getLatestReviewsForAnswer(answerId);
        assertFalse(reviews.isEmpty(), "The reviews list should not be empty.");
        assertEquals("Excellent answer", reviews.get(0).getComment(), "The comment should match what was saved.");
    }

    /**
     * Tests whether a student can successfully add a reviewer to their trusted list.
     *
     * @throws Exception If there is an issue interacting with the database.
     */
    @Test
    public void testStudentCanAddToTrustedReviewers() throws Exception {
        db.addOrUpdateTrustedReviewer(student, reviewer, 4);

        Map<String, Integer> trusted = db.getTrustedReviewersWithWeights(student);
        assertTrue(trusted.containsKey(reviewer), "The trusted reviewers map should contain the reviewer.");
        assertEquals(4, trusted.get(reviewer), "The weight should be set correctly.");
    }

    /**
     * Tests whether a student can view their trusted reviewers.
     *
     * @throws Exception If there is an issue interacting with the database.
     */
    @Test
    public void testStudentCanViewTrustedReviewers() throws Exception {
        db.addOrUpdateTrustedReviewer(student, reviewer, 3);

        Map<String, Integer> trusted = db.getTrustedReviewersWithWeights(student);
        assertNotNull(trusted, "The trusted reviewers map should not be null.");
        assertTrue(trusted.containsKey(reviewer), "The trusted reviewers map should contain the reviewer.");
    }

    /**
     * Tests whether a student can send feedback to a reviewer.
     *
     * @throws Exception If there is an issue interacting with the database.
     */
    @Test
    public void testStudentCanSendFeedbackToReviewer() throws Exception {
        String message = "Thanks for the review!";
        db.sendMessage(student, reviewer, -1, answerId, message, "review-feedback");

        List<Message> feedback = db.getFeedbackMessagesForAnswer(reviewer, answerId);
        assertEquals(1, feedback.size(), "There should be one feedback message.");
        assertEquals(message, feedback.get(0).getContent(), "The feedback message content should match what was sent.");
    }

    /**
     * Tests whether a reviewer's profile can be opened from the trusted list.
     *
     * @throws Exception If there is an issue interacting with the database.
     */
    @Test
    public void testReviewerProfileOpensFromTrustedList() throws Exception {
        db.addOrUpdateTrustedReviewer(student, reviewer, 5);
        Set<String> updated = db.getUpdatedTrustedReviewers(student);

        // This implies that clicking opens the profile/dashboard
        // In real GUI, this would trigger dashboard loading logic
        assertTrue(updated.contains(reviewer) || db.getTrustedReviewersWithWeights(student).containsKey(reviewer), 
                   "The reviewer should be listed as trusted or updated.");
    }
}