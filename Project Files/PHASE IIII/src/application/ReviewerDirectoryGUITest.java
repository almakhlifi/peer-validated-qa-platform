package application;
import databasePart1.DatabaseHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ReviewerDirectoryGUITest {

    private DatabaseHelper db;
    private final String student = "test_student";
    private final String reviewer = "test_reviewer";

    @BeforeEach
    public void setup() throws SQLException {
        db = new DatabaseHelper();
        db.connectToDatabase();

        if (!db.doesUserExist(student))
            db.register(new application.User(student, "pass", List.of("student")));

        if (!db.doesUserExist(reviewer))
            db.register(new application.User(reviewer, "pass", List.of("reviewer")));

        db.addOrUpdateTrustedReviewer(student, reviewer, 1);
    }

    @Test
    public void testReviewerAddedToTrustedList() throws SQLException {
        Map<String, Integer> trusted = db.getTrustedReviewersWithWeights(student);
        assertTrue(trusted.containsKey(reviewer));
        assertEquals(1, (int) trusted.get(reviewer));
    }
}
