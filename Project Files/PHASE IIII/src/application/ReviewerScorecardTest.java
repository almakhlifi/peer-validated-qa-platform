package application;

import application.Review;
import databasePart1.DatabaseHelper;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReviewerScorecardTest {

    private DatabaseHelper db;

    @BeforeAll
    public void setupDatabase() throws SQLException {
        db = new DatabaseHelper();
        db.connectToDatabase();
        db.clearAllReviews();
    }

    @BeforeEach
    public void seedTestData() throws SQLException {
        db.clearAllReviews();

        // Create test reviewers manually
        if (!db.doesUserExist("alice")) {
            db.register(new application.User("alice", "pass", List.of("reviewer")));
        }
        if (!db.doesUserExist("bob")) {
            db.register(new application.User("bob", "pass", List.of("reviewer")));
        }

        // Insert some reviews for alice
        db.saveReview(new Review("alice", "answer", 101, 5, "Excellent"));
        db.saveReview(new Review("alice", "answer", 102, 4, "Nice work"));

        // Insert one review for bob
        db.saveReview(new Review("bob", "question", 201, 3, "Okay"));
    }

    @Test
    public void testReviewerMetrics() throws SQLException {
        List<String> reviewers = db.getUsersByRole("reviewer");
        assertTrue(reviewers.contains("alice"));
        assertTrue(reviewers.contains("bob"));

        Map<String, Object> aliceData = computeScorecard("alice");
        assertEquals("alice", aliceData.get("name"));
        assertEquals(2, aliceData.get("count"));
        assertEquals(4.5, (double) aliceData.get("avg"), 0.001);

        Map<String, Object> bobData = computeScorecard("bob");
        assertEquals("bob", bobData.get("name"));
        assertEquals(1, bobData.get("count"));
        assertEquals(3.0, (double) bobData.get("avg"), 0.001);
    }

    private Map<String, Object> computeScorecard(String reviewer) throws SQLException {
        List<Review> reviews = db.getAllReviewsByReviewer(reviewer).stream()
                .filter(Review::isLatest)
                .toList();

        double avgRating = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        int feedbackTotal = reviews.stream()
                .filter(r -> r.getTargetType().equals("answer"))
                .mapToInt(r -> {
                    try {
                        return db.getFeedbackCountForAnswer(reviewer, r.getTargetId());
                    } catch (SQLException e) {
                        return 0;
                    }
                }).sum();

        Map<String, Object> row = new HashMap<>();
        row.put("name", reviewer);
        row.put("avg", avgRating);
        row.put("count", reviews.size());
        row.put("feedback", feedbackTotal);
        return row;
    }
}
