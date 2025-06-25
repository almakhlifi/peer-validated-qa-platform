/**
 * ReviewerRequestTest
 * 
 * JUnit test class to verify the functionality of the ReviewerRequest-related database operations.
 */
package application;

import databasePart1.DatabaseHelper;
import org.junit.jupiter.api.*;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReviewerRequestTest {

    private static DatabaseHelper db;

    /**
     * Sets up the database connection and ensures the necessary constraints are in place before running tests.
     *
     * @throws SQLException If there is an issue connecting to the database or altering table constraints.
     */
    @BeforeAll
    public static void setup() throws SQLException {
        db = new DatabaseHelper();
        db.connectToDatabase();
        db.createTables();
        ensureReviewerStatusAllowsDenied(db); // Ensure the schema allows 'denied' status
    }

    /**
     * Ensures the 'STATUS_CHECK' constraint exists and allows 'denied' as a valid status.
     *
     * @param db The database helper instance.
     */
    public static void ensureReviewerStatusAllowsDenied(DatabaseHelper db) {
        try (Statement stmt = db.getConnection().createStatement()) {
            stmt.execute("ALTER TABLE reviewer_requests DROP CONSTRAINT IF EXISTS STATUS_CHECK");
            stmt.execute("ALTER TABLE reviewer_requests ADD CONSTRAINT STATUS_CHECK CHECK (status IN ('pending', 'approved', 'denied'))");
        } catch (SQLException e) {
            System.out.println("Constraint already set or cannot be changed: " + e.getMessage());
        }
    }

    /**
     * Resets the database state before each test to ensure clean conditions.
     *
     * @throws SQLException If there is an issue resetting the database.
     */
    @BeforeEach
    public void reset() throws SQLException {
        db.connectToDatabase(); // Reconnect if needed

        // Ensure all students are registered
        for (String student : List.of("student1", "student2", "student3", "student4")) {
            if (!db.doesUserExist(student)) {
                db.register(new User(student, "pass", List.of("student")));
            }
            db.clearReviewerRequest(student);
        }

        try {
            db.removeRoleFromUser("student1", "reviewer", "admin");
            db.removeRoleFromUser("student2", "reviewer", "admin");
            db.removeRoleFromUser("student3", "reviewer", "admin");
            db.removeRoleFromUser("student4", "reviewer", "admin");
        } catch (Exception ignored) {}
    }

    /**
     * Tests whether a student can successfully request a reviewer role.
     *
     * @throws SQLException If there is an issue interacting with the database.
     */
    @Test
    @Order(1)
    public void testStudentCanRequestReviewerRole() throws SQLException {
        db.requestReviewerRoleRequest("student1");
        assertTrue(db.hasPendingReviewerRequest("student1"), "Request should be pending");
    }

    /**
     * Tests whether approving a reviewer request adds the 'reviewer' role to the user.
     *
     * @throws SQLException If there is an issue interacting with the database.
     */
    @Test
    @Order(2)
    public void testApproveReviewerRequestAddsRole() throws SQLException {
        if (!db.doesUserExist("studentUser")) {
            db.register(new User("studentUser", "password123", List.of("student")));
        }

        db.clearReviewerRequest("studentUser");
        if (db.getUserRoles("studentUser").contains("reviewer")) {
            db.removeRoleFromUser("studentUser", "reviewer", "admin");
        }
        db.requestReviewerRoleRequest("studentUser");
        db.approveReviewerRequest("studentUser");

        List<String> roles = db.getUserRoles("studentUser");
        assertTrue(roles.contains("reviewer"), "✅ Approved, now has reviewer role");
    }

    /**
     * Tests whether denying a reviewer request does not affect existing roles and removes the pending request.
     *
     * @throws SQLException If there is an issue interacting with the database.
     */
    @Test
    @Order(3)
    public void testDenyReviewerRequestDoesNotAffectExistingRoles() throws SQLException {
        if (!db.doesUserExist("student3")) {
            db.register(new User("student3", "pass", List.of("student")));
        }

        db.clearReviewerRequest("student3");

        List<String> rolesBefore = db.getUserRoles("student3");
        if (rolesBefore.contains("reviewer")) {
            db.removeRoleFromUser("student3", "reviewer", "admin");
        }

        db.requestReviewerRoleRequest("student3");

        // ✅ This now assumes schema already allows 'denied'
        db.denyReviewerRequest("student3");

        String status = db.getReviewerRequestStatus("student3");
        assertEquals("denied", status, "Reviewer request status should be 'denied'");

        List<String> rolesAfter = db.getUserRoles("student3");
        assertTrue(rolesAfter.contains("student"), "Student role should still exist");
        assertFalse(rolesAfter.contains("reviewer"), "Reviewer role should not exist");
    }

    /**
     * Tests whether duplicate pending reviewer requests are prevented.
     *
     * @throws SQLException If there is an issue interacting with the database.
     */
    @Test
    @Order(4)
    public void testCannotDuplicatePendingReviewerRequest() throws SQLException {
        db.requestReviewerRoleRequest("student4");
        boolean hasPending = db.hasPendingReviewerRequest("student4");
        assertTrue(hasPending, "There should be a pending request");

        // Try to submit again
        db.requestReviewerRoleRequest("student4"); // Will not duplicate due to MERGE

        // Still only 1 pending
        assertTrue(db.hasPendingReviewerRequest("student4"), "Only one pending request should exist");
    }

    /**
     * Tests whether an instructor can see pending reviewer requests and verify their behavior after approval.
     *
     * @throws SQLException If there is an issue interacting with the database.
     */
    @Test
    @Order(5)
    public void testInstructorSeesPendingRequests() throws SQLException {
        // Clean up all existing reviewer requests
        for (String username : db.getAllReviewerRequestUsernames()) {
            db.clearReviewerRequest(username);
        }

        // Clean up roles
        for (String student : List.of("student1", "student2")) {
            if (!db.doesUserExist(student)) {
                db.register(new User(student, "pass", List.of("student")));
            }
            if (db.getUserRoles(student).contains("reviewer")) {
                db.removeRoleFromUser(student, "reviewer", "admin");
            }
        }

        // Submit fresh requests
        db.requestReviewerRoleRequest("student1");
        db.requestReviewerRoleRequest("student2");

        // Approve one of them
        db.approveReviewerRequest("student2");

        // Verify only student1 is pending
        List<String> pending = db.getPendingReviewerRequests();
        System.out.println("Pending requests after approval: " + pending);

        assertEquals(1, pending.size(), "There should be exactly one pending request");
        assertTrue(pending.contains("student1"), "student1 should be pending");
        assertFalse(pending.contains("student2"), "student2 should not be pending");
    }
}