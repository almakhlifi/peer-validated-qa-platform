package databasePart1;

import application.Message;
import application.Answer;
import application.Question;
import application.QuestionSystem;
import application.Review;

import java.sql.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.stream.Collectors;
import application.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet; 
import java.util.List;
import java.util.Map;
import java.util.Set; 
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

/*
   The DatabaseHelper class is responsible for managing the connection to the database,
   performing operations such as user registration, login validation, and handling invitation codes.
*/
public class DatabaseHelper {

    // JDBC driver name and database URL 
    static final String JDBC_DRIVER = "org.h2.Driver";   
    static final String DB_URL = "jdbc:h2:~/FoundationDatabase";  

    // Database credentials 
    static final String USER = "sa"; 
    static final String PASS = ""; 

    private Connection connection = null;
    private Statement statement = null; 

    public void connectToDatabase() throws SQLException {
        try {
            Class.forName(JDBC_DRIVER); // Load the JDBC driver
            System.out.println("Connecting to database...");
            connection = DriverManager.getConnection(DB_URL, USER, PASS);
            statement = connection.createStatement(); 
            // You can use this command to clear the database and restart from fresh.
            //statement.execute("DROP ALL OBJECTS");
            
            // Run the table reset once... TEMPORARY: Drop and recreate Invitations table
            //resetInvitationsTable();
            
            // Run this to reset the messages table... comment again when done
            //clearAllMessages();
            
            // Run this to reset the reviews table... comment again when done
            //clearAllReviews();

            createTables();  // Create the necessary tables if they don't exist
        } catch (ClassNotFoundException e) {
            System.err.println("JDBC Driver not found: " + e.getMessage());
        }
    }

    
    // Method to check if the connection is closed
    public boolean isConnectionClosed() {
        try {
            return connection == null || connection.isClosed();
        } catch (SQLException e) {
            e.printStackTrace();
            return true; // Assume connection is closed if an error occurs
        }
    }

    // Reconnect to the database if needed
    public void ensureConnected() throws SQLException {
        if (isConnectionClosed()) {
            connectToDatabase();
        }
    }
    
    // Delete user function
    public void deleteUser(String username) throws SQLException {
        String deleteUserQuery = "DELETE FROM cse360users WHERE userName = ?";
        String deleteRolesQuery = "DELETE FROM user_roles WHERE userName = ?";

        try {
            // Start transaction
            connection.setAutoCommit(false);

            // Delete user roles first (to maintain referential integrity)
            try (PreparedStatement pstmt = connection.prepareStatement(deleteRolesQuery)) {
                pstmt.setString(1, username);
                pstmt.executeUpdate();
            }

            // Delete user
            try (PreparedStatement pstmt = connection.prepareStatement(deleteUserQuery)) {
                pstmt.setString(1, username);
                pstmt.executeUpdate();
            }

            // Commit transaction
            connection.commit();
        } catch (SQLException e) {
            // Rollback in case of error
            connection.rollback();
            throw e;
        } finally {
            // Reset auto-commit to true
            connection.setAutoCommit(true);
        }
    }
    
    // Add role to user function
    public void addRoleToUser(String username, String role) throws SQLException {
        // Check if the user already has the role
        String checkQuery = "SELECT COUNT(*) FROM user_roles WHERE userName = ? AND role = ?";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
            checkStmt.setString(1, username);
            checkStmt.setString(2, role);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next() && rs.getInt(1) > 0) {
                throw new SQLException("User already has the role: " + role);
            }
        }

        // If role does not exist, add it
        String insertQuery = "INSERT INTO user_roles (userName, role) VALUES (?, ?)";
        try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
            insertStmt.setString(1, username);
            insertStmt.setString(2, role);
            insertStmt.executeUpdate();
        }
    }
    
    // Remove role from user function
    public void removeRoleFromUser(String username, String role, String currentAdmin) throws SQLException {
        // Prevent removing the last admin role
        if (role.equals("admin")) {
            int adminCount = getAdminCount();

            // Prevent removing the last remaining admin
            if (adminCount <= 1) {
                throw new SQLException("Cannot remove the last admin role. At least one admin is required.");
            }

            // Prevent an admin from removing their own "admin" role
            if (username.equals(currentAdmin)) {
                throw new SQLException("You cannot remove your own admin role.");
            }
        }

        // Check if the user actually has the role
        String checkQuery = "SELECT COUNT(*) FROM user_roles WHERE userName = ? AND role = ?";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
            checkStmt.setString(1, username);
            checkStmt.setString(2, role);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next() && rs.getInt(1) == 0) {
                throw new SQLException("User does not have the role: " + role);
            }
        }

        // Proceed with role removal if the user has it
        String query = "DELETE FROM user_roles WHERE userName = ? AND role = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, role);
            pstmt.executeUpdate();
        }
    }
    
    // Helper function to keep track of number of Admins in system
    public int getAdminCount() throws SQLException {
        String query = "SELECT COUNT(*) FROM user_roles WHERE role = 'admin'";
        try (PreparedStatement pstmt = connection.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
    
    // Retrieves the user and their roles or no roles
    public List<String> getUsersWithRolesIncludingNoRole() throws SQLException {
        List<String> usersWithRoles = new ArrayList<>();
        String query = "SELECT u.userName, GROUP_CONCAT(r.role) AS roles " +
                       "FROM cse360users u " +
                       "LEFT JOIN user_roles r ON u.userName = r.userName " +
                       "GROUP BY u.userName";

        try (PreparedStatement pstmt = connection.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String userName = rs.getString("userName");
                String roles = rs.getString("roles");
                if (roles == null || roles.isEmpty()) {
                    usersWithRoles.add(userName + " - No roles");
                } else {
                    usersWithRoles.add(userName + " - " + roles);
                }
            }
        }
        return usersWithRoles;
    }
    
    public Connection getConnection() {
        return this.connection;
    }

    
    // Table creation for storing accounts
    public void createTables() throws SQLException {
        // Ensure database connection is open
        ensureConnected();

        // User Table
        String userTable = "CREATE TABLE IF NOT EXISTS cse360users ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "userName VARCHAR(255) UNIQUE, "
                + "password VARCHAR(255), "
                + "oneTimePassword VARCHAR(255))"; 
        statement.execute(userTable);

        // User Roles Table
        String rolesTable = "CREATE TABLE IF NOT EXISTS user_roles ("
                + "userName VARCHAR(255), "
                + "role VARCHAR(20) CHECK (role IN ('admin', 'student', 'instructor', 'staff', 'reviewer')), "
                + "FOREIGN KEY (userName) REFERENCES cse360users(userName) ON DELETE CASCADE)";
        statement.execute(rolesTable);

        //**Create Invitations Table**
        String invitationTable = "CREATE TABLE IF NOT EXISTS Invitations ("
                + "invite_code VARCHAR(10) PRIMARY KEY, "
                + "role VARCHAR(50), "
                + "expiration_timestamp TIMESTAMP)";
        statement.execute(invitationTable);
        
        // Create Questions Table
        String questionTable = "CREATE TABLE IF NOT EXISTS questions ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "title VARCHAR(255), "
                + "content TEXT, "
                + "author VARCHAR(255), "
                + "tags VARCHAR(255),"
                + "acceptedAnswerId INT NULL)"; 
        statement.execute(questionTable);

        // Create Answers Table
        String answerTable = "CREATE TABLE IF NOT EXISTS answers ("
        	    + "id INT AUTO_INCREMENT PRIMARY KEY, "
        	    + "question_id INT, "
        	    + "content TEXT, "
        	    + "author VARCHAR(255), "
        	    + "parent_answer_id INT, "
        	    + "FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE, "
        	    + "FOREIGN KEY (parent_answer_id) REFERENCES answers(id) ON DELETE CASCADE)";
        	statement.execute(answerTable);
        
        // Create messages table	
    	String messagesTable = "CREATE TABLE IF NOT EXISTS messages (" +
    	        "id INT AUTO_INCREMENT PRIMARY KEY, " +
    	        "sender VARCHAR(255), " +
    	        "recipient VARCHAR(255), " +
    	        "question_id INT, " +
    	        "answer_id INT, " +
    	        "content TEXT, " +
    	        "message_type VARCHAR(30) DEFAULT 'question', " +
    	        "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
    	        "is_read BOOLEAN DEFAULT FALSE)";
    	statement.execute(messagesTable);
    	
    	//create review
    	String reviewTable = "CREATE TABLE IF NOT EXISTS reviews ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "reviewer_username VARCHAR(255) NOT NULL, "
                + "target_type VARCHAR(10) NOT NULL CHECK (target_type IN ('question', 'answer')), "
                + "target_id INT NOT NULL, "
                + "rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5), "
                + "comment TEXT, "
                + "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "previous_review_id INT NULL, "
                + "is_latest BOOLEAN DEFAULT TRUE, "
                + "FOREIGN KEY (previous_review_id) REFERENCES reviews(id) ON DELETE SET NULL)"; 
                statement.execute(reviewTable);
        
        // reviewer request table
                String createReviewerRequests = "CREATE TABLE IF NOT EXISTS reviewer_requests (" +
                	    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                	    "username VARCHAR(255) NOT NULL UNIQUE, " +
                	    "status VARCHAR(20) DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'denied')), " +
                	    "FOREIGN KEY (username) REFERENCES cse360users(userName) ON DELETE CASCADE)";

        statement.execute(createReviewerRequests);
        
        // trusted reviewers table
        String trustedReviewersTable = "CREATE TABLE IF NOT EXISTS trusted_reviewers (" +
        	    "student_username VARCHAR(255) NOT NULL, " +
        	    "reviewer_username VARCHAR(255) NOT NULL, " +
        	    "weight INT DEFAULT 1 CHECK (weight >= 1 AND weight <= 5), " +
        	    "PRIMARY KEY (student_username, reviewer_username), " +
        	    "FOREIGN KEY (student_username) REFERENCES cse360users(userName) ON DELETE CASCADE, " +
        	    "FOREIGN KEY (reviewer_username) REFERENCES cse360users(userName) ON DELETE CASCADE)";
        statement.execute(trustedReviewersTable);
        
        // table for review update notifications
        String reviewUpdatesTable = "CREATE TABLE IF NOT EXISTS review_updates (" +
        	    "reviewer_username VARCHAR(255) NOT NULL, " +
        	    "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        	statement.execute(reviewUpdatesTable);

        // Add PARENT_ANSWER_ID column to answers table
        String alterAnswerTable = "ALTER TABLE answers ADD COLUMN IF NOT EXISTS PARENT_ANSWER_ID INT";
        statement.execute(alterAnswerTable);

    }
    
    // clearing notifications for updated reviews
    public void clearUpdateForReviewer(String studentUsername, String reviewerUsername) throws SQLException {
        ensureConnected();
        String sql = "DELETE FROM review_updates WHERE reviewer_username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, reviewerUsername);
            stmt.executeUpdate();
        }
    }
    
    // method for adding or updating trusted reviewer
    public void addOrUpdateTrustedReviewer(String studentUsername, String reviewerUsername, int weight) throws SQLException {
        ensureConnected();
        String sql = "MERGE INTO trusted_reviewers (student_username, reviewer_username, weight) KEY(student_username, reviewer_username) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, studentUsername);
            stmt.setString(2, reviewerUsername);
            stmt.setInt(3, weight);
            stmt.executeUpdate();
        }
    }
    
    // method for getting review history
    public List<Review> getReviewHistoryForAnswer(String reviewer, int answerId) throws SQLException {
        List<Review> history = new ArrayList<>();
        String sql = "SELECT * FROM reviews WHERE reviewer_username = ? AND target_id = ? ORDER BY timestamp DESC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, reviewer);
            stmt.setInt(2, answerId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
            	Review r = new Review(
        		    rs.getInt("id"),
        		    rs.getString("reviewer_username"),
        		    rs.getString("target_type"),
        		    rs.getInt("target_id"),
        		    rs.getInt("rating"),
        		    rs.getString("comment"),
        		    rs.getTimestamp("timestamp"),
        		    rs.getObject("previous_review_id", Integer.class),
        		    rs.getBoolean("is_latest")
            	);
                history.add(r);
            }
        }
        return history;
    }
 // Clears the reviewer request for a user (removes the request entirely)
    public void clearReviewerRequest(String username) throws SQLException {
        ensureConnected();
        String sql = "DELETE FROM reviewer_requests WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.executeUpdate();
        }
    }

    public String getReviewerRequestStatus(String username) throws SQLException {
        ensureConnected();
        String sql = "SELECT status FROM reviewer_requests WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("status");  // returns "pending", "approved", or "denied"
                }
            }
        }
        return null;  // no request exists
    }

    // method for retrieving trusted reviewers
    public Set<String> getUpdatedTrustedReviewers(String studentUsername) throws SQLException {
        ensureConnected();
        Set<String> updated = new HashSet<>();

        String sql = "SELECT DISTINCT ru.reviewer_username " +
                     "FROM trusted_reviewers ru " +
                     "JOIN review_updates up ON ru.reviewer_username = up.reviewer_username " +
                     "WHERE ru.student_username = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, studentUsername);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                updated.add(rs.getString("reviewer_username"));
            }
        }

        return updated;
    }
    
    // method for getting trusted reviewer with weights
    public Map<String, Integer> getTrustedReviewersWithWeights(String studentUsername) throws SQLException {
        ensureConnected();
        Map<String, Integer> reviewers = new HashMap<>();
        String sql = "SELECT reviewer_username, weight FROM trusted_reviewers WHERE student_username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, studentUsername);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                reviewers.put(rs.getString("reviewer_username"), rs.getInt("weight"));
            }
        }
        return reviewers;
    }
    
    // method for removing trusted reviewer
    public void removeTrustedReviewer(String studentUsername, String reviewerUsername) throws SQLException {
        ensureConnected();
        String sql = "DELETE FROM trusted_reviewers WHERE student_username = ? AND reviewer_username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, studentUsername);
            stmt.setString(2, reviewerUsername);
            stmt.executeUpdate();
        }
    }
    
    // method for requesting reviewer role
    public void requestReviewerRoleRequest(String username) throws SQLException {
        ensureConnected();
        String sql = "MERGE INTO reviewer_requests (username, status) KEY(username) VALUES (?, 'pending')";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.executeUpdate();
        }
    }
    
    // method for getting pending reviewer requests
    public List<String> getPendingReviewerRequests() throws SQLException {
        ensureConnected();
        List<String> pending = new ArrayList<>();
        String sql = "SELECT username FROM reviewer_requests WHERE status = 'pending'";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                pending.add(rs.getString("username"));
            }
        }
        return pending;
    }
    
    public List<String> getAllReviewerRequestUsernames() throws SQLException {
        String sql = "SELECT username FROM reviewer_requests";
        List<String> usernames = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                usernames.add(rs.getString("username"));
            }
        }
        return usernames;
    }

    
    // method for checking if user has pending request already
    public boolean hasPendingReviewerRequest(String username) throws SQLException {
        ensureConnected();
        String sql = "SELECT 1 FROM reviewer_requests WHERE username = ? AND status = 'pending'";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next();  // true if a pending request exists
        }
    }
    
    
    
    // method for marking if request is approved
    public void approveReviewerRequest(String username) throws SQLException {
        ensureConnected();
        String updateStatus = "UPDATE reviewer_requests SET status = 'approved' WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(updateStatus)) {
            stmt.setString(1, username);
            stmt.executeUpdate();
        }
        addRoleToUser(username, "reviewer"); // Assign the role after approving
    }
    
    // method for marking if request is denied
    public void denyReviewerRequest(String username) throws SQLException {
        ensureConnected();
        String updateStatus = "UPDATE reviewer_requests SET status = 'denied' WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(updateStatus)) {
            stmt.setString(1, username);
            stmt.executeUpdate();
        }
    }
    
    // Method for retrieving feedback count for each review
    public int getFeedbackCountForAnswer(String reviewerUsername, int answerId) throws SQLException {
        ensureConnected();
        String sql = "SELECT COUNT(*) FROM messages WHERE recipient = ? AND answer_id = ? AND message_type = 'review-feedback'";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, reviewerUsername);
            stmt.setInt(2, answerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
    
    // Helper method to get feedback messages for reviews
    public List<Message> getFeedbackMessagesForAnswer(String reviewer, int answerId) throws SQLException {
        ensureConnected();
        String sql = "SELECT * FROM messages WHERE recipient = ? AND answer_id = ? AND message_type = 'review-feedback'";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, reviewer);
            stmt.setInt(2, answerId);
            ResultSet rs = stmt.executeQuery();

            List<Message> messages = new ArrayList<>();
            while (rs.next()) {
                messages.add(new Message(
                    rs.getInt("id"),
                    rs.getString("sender"),
                    rs.getString("recipient"),
                    rs.getInt("question_id"),
                    rs.getInt("answer_id"),
                    rs.getString("content"),
                    rs.getTimestamp("timestamp"),
                    rs.getBoolean("is_read"),
                    rs.getString("message_type")
                ));
            }
            return messages;
        }
    }
    
    // Helper method to mark review as outdated after being updated
    public void markReviewAsOutdated(int reviewId) throws SQLException {
        ensureConnected();
        String sql = "UPDATE reviews SET is_latest = FALSE WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, reviewId);
            stmt.executeUpdate();
        }
    }
    
    // Method to get feedback messages for reviewer
    public List<Message> getFeedbackForReviewer(String reviewer) throws SQLException {
        ensureConnected();
        String sql = "SELECT * FROM messages WHERE recipient = ? AND message_type = 'review-feedback' ORDER BY timestamp DESC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, reviewer);
            ResultSet rs = stmt.executeQuery();

            List<Message> messages = new ArrayList<>();
            while (rs.next()) {
                messages.add(new Message(
                    rs.getInt("id"),
                    rs.getString("sender"),
                    rs.getString("recipient"),
                    rs.getInt("question_id"),
                    rs.getInt("answer_id"),
                    rs.getString("content"),
                    rs.getTimestamp("timestamp"),
                    rs.getBoolean("is_read"),
                    rs.getString("message_type")
                ));
            }
            return messages;
        }
    }
    
    // method for clearing reviews
    public void clearAllReviews() throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM reviews")) {
            stmt.executeUpdate();
        }
        try (PreparedStatement reset = connection.prepareStatement("ALTER TABLE reviews ALTER COLUMN id RESTART WITH 1")) {
            reset.executeUpdate();
        }
    }
    
    // Method for saving reviews
    public void saveReview(String reviewerUsername, int answerId, int rating, String comment) throws SQLException {
        String sql = "INSERT INTO reviews (reviewer_username, target_type, target_id, rating, comment, is_latest) "
                   + "VALUES (?, 'answer', ?, ?, ?, TRUE)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, reviewerUsername);
            stmt.setInt(2, answerId);
            stmt.setInt(3, rating);
            stmt.setString(4, comment);
            stmt.executeUpdate();
        }

        // Log update to notify students who trust this reviewer
        String logUpdateSQL = "INSERT INTO review_updates (reviewer_username) VALUES (?)";
        try (PreparedStatement stmt = connection.prepareStatement(logUpdateSQL)) {
            stmt.setString(1, reviewerUsername);
            stmt.executeUpdate();
        }
    }
    
    // Method fetches messages for user; needed for inbox system
    public List<Message> getAllMessagesForUser(String username) throws SQLException {
        ensureConnected();
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM messages WHERE sender = ? OR recipient = ? ORDER BY timestamp";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, username);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
            	messages.add(new Message(
            		    rs.getInt("id"),
            		    rs.getString("sender"),
            		    rs.getString("recipient"),
            		    rs.getInt("question_id"),
            		    rs.getInt("answer_id"),
            		    rs.getString("content"),
            		    rs.getTimestamp("timestamp"),
            		    rs.getBoolean("is_read"),
            		    rs.getString("message_type")
            	));
            }
        }
        return messages;
    }    
    
    // Method for sending messages
    public void sendMessage(String sender, String recipient, int questionId, int answerId,
            				String content, String messageType) throws SQLException {
    	ensureConnected();
    	String sql = "INSERT INTO messages (sender, recipient, question_id, answer_id, content, message_type) VALUES (?, ?, ?, ?, ?, ?)";
    	try (PreparedStatement stmt = connection.prepareStatement(sql)) {
    		stmt.setString(1, sender);
    		stmt.setString(2, recipient);
    		stmt.setInt(3, questionId);
    		stmt.setInt(4, answerId);
    		stmt.setString(5, content);
    		stmt.setString(6, messageType);
    		stmt.executeUpdate();
    	}
    }
    
    // Method for fetching Messages Between Two Users
    public List<Message> getMessagesBetween(String user1, String user2, int questionId, int answerId) throws SQLException {
        ensureConnected();
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM messages WHERE " +
                "((sender = ? AND recipient = ?) OR (sender = ? AND recipient = ?)) " +
                "AND question_id = ? AND answer_id = ? ORDER BY timestamp";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user1);
            stmt.setString(2, user2);
            stmt.setString(3, user2);
            stmt.setString(4, user1);
            stmt.setInt(5, questionId);
            stmt.setInt(6, answerId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
            	messages.add(new Message(
            		    rs.getInt("id"),
            		    rs.getString("sender"),
            		    rs.getString("recipient"),
            		    rs.getInt("question_id"),
            		    rs.getInt("answer_id"),
            		    rs.getString("content"),
            		    rs.getTimestamp("timestamp"),
            		    rs.getBoolean("is_read"),
            		    rs.getString("message_type")
            	));
            }
        }
        return messages;
    }
    
    // Method for getting unread message count
    public int getUnreadMessageCount(String recipient, int questionId, int answerId) throws SQLException {
        ensureConnected();
        String sql = "SELECT COUNT(*) FROM messages WHERE recipient = ? AND question_id = ? AND answer_id = ? AND is_read = FALSE";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, recipient);
            stmt.setInt(2, questionId);
            stmt.setInt(3, answerId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
    
    // Method for marking messages as read
    public void markMessagesAsRead(String recipient, int questionId, int answerId) throws SQLException {
        ensureConnected();
        String sql = "UPDATE messages SET is_read = TRUE WHERE recipient = ? AND question_id = ? AND answer_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, recipient);
            stmt.setInt(2, questionId);
            stmt.setInt(3, answerId);
            stmt.executeUpdate();
        }
    }
    
    // helper function to delete messages in database in case
    public void clearAllMessages() throws SQLException {
        ensureConnected();
        String sql = "DELETE FROM messages";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.executeUpdate();
            System.out.println("Messages table reset successfully.");
        }
    }
    
    // Method for marking answer as accepted
    public boolean markAnswerAsAccepted(int questionId, int answerId) throws SQLException {
        ensureConnected();
        String query = "UPDATE questions SET acceptedAnswerId = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, answerId);
            pstmt.setInt(2, questionId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;  // Return true if update succeeded
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // Method for saving questions
    public void saveQuestion(Question question) throws SQLException {
        ensureConnected();
        String query = "INSERT INTO questions (title, content, author, tags) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, question.getTitle());
            pstmt.setString(2, question.getContent());
            pstmt.setString(3, question.getAuthor());
            pstmt.setString(4, String.join(",", question.getTags())); 
            pstmt.executeUpdate();

            // Set generated ID
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    question.setId(rs.getInt(1)); // Update question ID
                }
            }
        }
    }
    
    // Method for getting question by ID
    public Question getQuestionById(int questionId) throws SQLException {
        ensureConnected();
        String query = "SELECT * FROM questions WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, questionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String title = rs.getString("title");
                    String content = rs.getString("content");
                    String author = rs.getString("author");
                    List<String> tags = List.of(rs.getString("tags").split(",")); // Convert CSV to list
                    Integer acceptedAnswerId = rs.getObject("acceptedAnswerId", Integer.class);

                 // pass acceptedAnswerId to constructor
                 return new Question(questionId, title, content, author, tags, acceptedAnswerId);
                }
            }
        }
        return null; // Return null if question is not found
    }
    
    // Method for getting question by tag
    public List<Question> getQuestionsByTag(String tag) throws SQLException {
        ensureConnected(); // Ensure DB connection is open
        List<Question> filteredQuestions = new ArrayList<>();
        
        String sql = "SELECT * FROM questions WHERE tags LIKE ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, "%" + tag + "%"); // Use LIKE to match partial tags
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            int id = rs.getInt("id");
            String title = rs.getString("title");
            String content = rs.getString("content");
            String author = rs.getString("author");
            List<String> tags = List.of(rs.getString("tags").split(",\\s*")); // Convert back to List

            filteredQuestions.add(new Question(id, title, content, author, tags));
        }

        return filteredQuestions;
    }
    
    // Method for loading question list
    public List<Question> loadQuestions() throws SQLException {
        ensureConnected();
        List<Question> questions = new ArrayList<>();
        String query = "SELECT id, title, content, author, tags, acceptedAnswerId FROM questions";

        try (PreparedStatement pstmt = connection.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                String content = rs.getString("content");
                String author = rs.getString("author");
                String tagsString = rs.getString("tags");
                Integer acceptedAnswerId = rs.getObject("acceptedAnswerId", Integer.class);

                List<String> tags = Arrays.asList(tagsString.split(","));
                Question question = new Question(id, title, content, author, tags, acceptedAnswerId);
                questions.add(question);
            }
        }
        return questions;
    }
    
    // Method for updating questions
    public boolean updateQuestion(int questionId, String newTitle, String newContent) {
        String sql = "UPDATE questions SET title = ?, content = ? WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newTitle);
            stmt.setString(2, newContent);
            stmt.setInt(3, questionId);

            int rowsUpdated = stmt.executeUpdate(); 
            return rowsUpdated > 0; 
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean updateQuestion(int questionId, String newTitle, String newContent, Integer acceptedAnswerId) { 
        String sql = "UPDATE questions SET title = ?, content = ?, acceptedAnswerId = ? WHERE id = ?"; // Corrected SQL
        System.out.println("DatabaseHelper: Executing updateQuestion SQL:\n" + sql + "\nParameters: title=" + newTitle + ", content=" + newContent + ", acceptedAnswerId=" + acceptedAnswerId + ", id=" + questionId); // LOG SQL and parameters

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, newTitle);
            pstmt.setString(2, newContent);
            pstmt.setObject(3, acceptedAnswerId, Types.INTEGER); // Use setObject for Integer, handle NULL correctly
            pstmt.setInt(4, questionId);

            int affectedRows = pstmt.executeUpdate();
            System.out.println("DatabaseHelper: updateQuestion - Affected rows: " + affectedRows); // Log affected rows
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("DatabaseHelper: SQLException in updateQuestion(): " + e.getMessage()); // ERROR LOG
            e.printStackTrace();
            return false;
        }
    }
    
    // Method for deleting questions
    public boolean deleteQuestion(int questionId) {
        String sql = "DELETE FROM questions WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, questionId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0; 
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // Method for saving answers
    public void saveAnswer(Answer answer) throws SQLException {
        ensureConnected();
        String query = "INSERT INTO answers (question_id, content, author, parent_answer_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, answer.getQuestionId());
            pstmt.setString(2, answer.getContent());
            pstmt.setString(3, answer.getAuthor());
            if (answer.getParentAnswerId() != null) {
                pstmt.setInt(4, answer.getParentAnswerId());
            } else {
                pstmt.setNull(4, java.sql.Types.INTEGER);
            }
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    answer.setId(rs.getInt(1));
                }
            }
        }
    }

    public List<Answer> loadAnswersForQuestion(int questionId) throws SQLException {
        ensureConnected();
        Map<Integer, Answer> answerMap = new HashMap<>();
        String query = "SELECT * FROM answers WHERE question_id = ? ORDER BY parent_answer_id, id";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, questionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String content = rs.getString("content");
                    String author = rs.getString("author");
                    int parentId = rs.getInt("parent_answer_id");
                    Answer answer = new Answer(id, content, author, questionId, parentId == 0 ? null : parentId);
                    answerMap.put(id, answer);
                    
                    if (parentId != 0 && answerMap.containsKey(parentId)) {
                        answerMap.get(parentId).addReply(answer);
                    }
                }
            }
        }
        return new ArrayList<>(answerMap.values().stream().filter(a -> a.getParentAnswerId() == null).collect(Collectors.toList()));
    }
    
    
    public void saveThreadedAnswer(Answer answer) throws SQLException {
        String query = "INSERT INTO answers (question_id, content, author, parent_answer_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, answer.getQuestionId());
            pstmt.setString(2, answer.getContent());
            pstmt.setString(3, answer.getAuthor());
            if (answer.getParentAnswerId() != null) {
                pstmt.setInt(4, answer.getParentAnswerId());
            } else {
                pstmt.setNull(4, java.sql.Types.INTEGER);
            }
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    answer.setId(rs.getInt(1));
                }
            }
        }
    }

    
    public List<Answer> loadThreadedAnswersForQuestion(int questionId) throws SQLException {
        Map<Integer, Answer> answerMap = new HashMap<>();
        String query = "SELECT * FROM answers WHERE question_id = ? ORDER BY id";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, questionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String content = rs.getString("content");
                    String author = rs.getString("author");
                    Integer parentId = rs.getObject("parent_answer_id", Integer.class);
                    Answer answer = new Answer(id, content, author, questionId, parentId);
                    answerMap.put(id, answer);
                    
                    if (parentId != null && answerMap.containsKey(parentId)) {
                        answerMap.get(parentId).addReply(answer);
                    }
                }
            }
        }
        return new ArrayList<>(answerMap.values().stream()
            .filter(a -> a.getParentAnswerId() == null)
            .collect(Collectors.toList()));
    }
    
    // Method for updating answers 
    public boolean updateAnswerInDatabase(int answerId, String newContent) throws SQLException {
        ensureConnected();
        String query = "UPDATE answers SET content = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, newContent);
            pstmt.setInt(2, answerId);
            int rowsUpdated = pstmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean updateAnswer(int answerId, String newContent) throws SQLException {
        ensureConnected(); // Ensure database connection is open

        String query = "UPDATE answers SET content = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, newContent);
            stmt.setInt(2, answerId);

            int rowsUpdated = stmt.executeUpdate();
            return rowsUpdated > 0; // Return true if update was successful
        }
    }


    // Method for resetting invitation table for debugging
    public void resetInvitationsTable() throws SQLException {
        ensureConnected(); 

        String dropTable = "DROP TABLE IF EXISTS Invitations";
        String createTable = "CREATE TABLE Invitations ("
                + "invite_code VARCHAR(10) PRIMARY KEY, "
                + "roles VARCHAR(255), "
                + "expiration_timestamp TIMESTAMP)";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(dropTable);
            stmt.execute(createTable);
            System.out.println("Invitations table reset successfully.");
        }
    }
    
    // Check if the database is empty
    public boolean isDatabaseEmpty() throws SQLException {
        String query = "SELECT COUNT(*) AS count FROM cse360users";
        ResultSet resultSet = statement.executeQuery(query);
        if (resultSet.next()) {
            return resultSet.getInt("count") == 0;
        }
        return true;
    }

    // Registers a new user in the database.
    public void register(User user) throws SQLException {
        String insertUser = "INSERT INTO cse360users (userName, password) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertUser)) {
            pstmt.setString(1, user.getUserName());
            pstmt.setString(2, user.getPassword());
            pstmt.executeUpdate();
        }

        // Insert user roles into user_roles table
        for (String role : user.getRoles()) {
            addRoleToUser(user.getUserName(), role);
        }
    }

    // Get all roles assigned to a user
    public List<String> getUserRoles(String userName) throws SQLException {
        ensureConnected(); 
        String query = "SELECT role FROM user_roles WHERE userName = ?";
        List<String> roles = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, userName);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                roles.add(rs.getString("role"));
            }
        }
        return roles;
    }
    
    // Validates a user's login credentials.
    public boolean login(User user) throws SQLException {
        String query = "SELECT * FROM cse360users WHERE userName = ? AND password = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, user.getUserName());
            pstmt.setString(2, user.getPassword());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    // Checks if a user already exists in the database based on their userName.
    public boolean doesUserExist(String userName) {
        String query = "SELECT COUNT(*) FROM cse360users WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, userName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                // If the count is greater than 0, the user exists
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false; 
    }

    // Generates a new invitation code and inserts it into the database.
    public String generateInvitationCode() throws SQLException {
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 4); 
        } while (isInvitationCodeUsed(code)); 

        return code;
    }

    // Helper function to check if the code already exists
    private boolean isInvitationCodeUsed(String code) throws SQLException {
        String query = "SELECT COUNT(*) FROM Invitations WHERE invite_code = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, code);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0; 
            }
        }
        return false; 
    }

    // Validates an invitation code to check if it is unused.
    public boolean validateInvitationCode(String code) {
        String query = "SELECT expiration_timestamp FROM Invitations WHERE invite_code = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, code);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Timestamp expirationTimestamp = rs.getTimestamp("expiration_timestamp");
                if (expirationTimestamp != null) {
                    LocalDateTime expirationDateTime = expirationTimestamp.toLocalDateTime();

                    if (expirationDateTime.isBefore(LocalDateTime.now())) {
                        System.out.println("Invitation code has expired.");
                        return false;
                    }
                    return true; 
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("Invalid invitation code.");
        return false;
    }
    
    // Retrieves the invitation code roles for account setup validation
    public String getInvitationRoles(String code) throws SQLException {
        String query = "SELECT roles FROM Invitations WHERE invite_code = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, code);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("roles"); 
            }
        }
        return null; 
    }

    // Stores the invitation
    public void storeInvitation(String code, String roles, LocalDate date, String time) throws SQLException {
        ensureConnected(); 

        LocalDateTime expirationDateTime = LocalDateTime.parse(date + "T" + time + ":00");

        String query = "INSERT INTO Invitations (invite_code, roles, expiration_timestamp) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, code);
            stmt.setString(2, roles); 
            stmt.setTimestamp(3, Timestamp.valueOf(expirationDateTime));
            stmt.executeUpdate();
        }
    }
    
    // Deletes used invitation codes
    public void deleteUsedInvitation(String email, String code) throws SQLException {
        String query = "DELETE FROM Invitations WHERE email = ? AND invite_code = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, email);
            stmt.setString(2, code);
            stmt.executeUpdate();
        }
    }
    
    // Closes the database connection and statement.
    public void closeConnection() {
        try { 
            if (statement != null) statement.close(); 
        } catch (SQLException se2) { 
            se2.printStackTrace();
        } 
        try { 
            if (connection != null) connection.close(); 
        } catch (SQLException se) { 
            se.printStackTrace(); 
        } 
    }

    public List<String> getInvalidRoleUsers() throws SQLException {
        List<String> users = new ArrayList<>();
        String query = "SELECT DISTINCT userName FROM user_roles WHERE role NOT IN ('admin', 'student', 'instructor', 'staff', 'reviewer')";
        try (PreparedStatement pstmt = connection.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                users.add(rs.getString("userName"));
            }
        }
        return users;
    }
    
    // Helper function to check if a user is an Admin
    public boolean isUserAdmin(String userName) throws SQLException {
        String query = "SELECT COUNT(*) FROM user_roles WHERE userName = ? AND role = 'admin'";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, userName);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    // Sets a one-time password for a user
    public void setOneTimePassword(String username, String oneTimePassword) throws SQLException {
        String query = "UPDATE cse360users SET oneTimePassword = ? WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, oneTimePassword);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        }
    }

    // Validates the one-time password
    public boolean validateOneTimePassword(String username, String oneTimePassword) throws SQLException {
        String query = "SELECT oneTimePassword FROM cse360users WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedOneTimePassword = rs.getString("oneTimePassword");
                return storedOneTimePassword != null && storedOneTimePassword.equals(oneTimePassword);
            }
        }
        return false;
    }

    // Clears the one-time password after use
    public void clearOneTimePassword(String username) throws SQLException {
        String query = "UPDATE cse360users SET oneTimePassword = NULL WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        }
    }

    // Updates the user's password
    public void updatePassword(String username, String newPassword) throws SQLException {
        String query = "UPDATE cse360users SET password = ? WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, newPassword);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        }
    }

    // ADDED CODE START: Updated search to also match answers.content
    public List<Question> searchQuestions(String keyword) throws SQLException {
        ensureConnected();
        List<Question> searchResults = new ArrayList<>();

        // Use LEFT JOIN so we also get questions that have no answers
        // DISTINCT to avoid duplicate questions if multiple answers match the keyword
        String sql = 
            "SELECT DISTINCT q.id, q.title, q.content, q.author, q.tags " +
            "FROM questions q " +
            "LEFT JOIN answers a ON q.id = a.question_id " +
            "WHERE q.title LIKE ? OR q.content LIKE ? OR a.content LIKE ?";

        try (PreparedStatement pstmt 	= connection.prepareStatement(sql)) {
            pstmt.setString(1, "%" + keyword + "%");
            pstmt.setString(2, "%" + keyword + "%");
            pstmt.setString(3, "%" + keyword + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String title = rs.getString("title");
                    String content = rs.getString("content");
                    String author = rs.getString("author");
                    List<String> tags = List.of(rs.getString("tags").split(","));

                    searchResults.add(new Question(id, title, content, author, tags));
                }
            }
        }
        return searchResults;
    }
   
    public void updateQuestion(Question question) {
        String sql = "UPDATE questions SET title = ?, content = ?, acceptedAnswerId = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, question.getTitle());
            stmt.setString(2, question.getContent());
            stmt.setObject(3, question.getAcceptedAnswerId()); 
            stmt.setInt(4, question.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
 
    public void saveReview(Review review) throws SQLException {
        ensureConnected();
        connection.setAutoCommit(false); // Start transaction

        try {
            // 1. If this is an update (has previousReviewId), mark the old review as not latest
            if (review.getPreviousReviewId() != null) {
                String updateOldReviewSql = "UPDATE reviews SET is_latest = FALSE WHERE id = ?";
                try (PreparedStatement pstmtUpdate = connection.prepareStatement(updateOldReviewSql)) {
                    pstmtUpdate.setInt(1, review.getPreviousReviewId());
                    pstmtUpdate.executeUpdate();
                }
            }

            // 2. Insert the new review (which is the latest by default)
            String insertSql = "INSERT INTO reviews (reviewer_username, target_type, target_id, rating, comment, previous_review_id, is_latest, timestamp) "
                             + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmtInsert = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmtInsert.setString(1, review.getReviewerUsername());
                pstmtInsert.setString(2, review.getTargetType());
                pstmtInsert.setInt(3, review.getTargetId());
                pstmtInsert.setInt(4, review.getRating());
                pstmtInsert.setString(5, review.getComment());
                if (review.getPreviousReviewId() != null) {
                    pstmtInsert.setInt(6, review.getPreviousReviewId());
                } else {
                    pstmtInsert.setNull(6, java.sql.Types.INTEGER);
                }
                pstmtInsert.setBoolean(7, true); // New review is always latest initially
                pstmtInsert.setTimestamp(8, review.getTimestamp()); // Use timestamp from object

                pstmtInsert.executeUpdate();

                // Get the generated ID and set it on the review object
                try (ResultSet rs = pstmtInsert.getGeneratedKeys()) {
                    if (rs.next()) {
                        review.setId(rs.getInt(1));
                    }
                }
            }

            connection.commit(); // Commit transaction

        } catch (SQLException e) {
            connection.rollback(); // Rollback on error
            throw e; // Re-throw the exception
        } finally {
            connection.setAutoCommit(true); // Reset auto-commit
        }
    }


    /**
     * Deletes a specific review and its history (previous versions).
     * THIS IS A HARD DELETE. Consider soft delete (marking inactive) if needed.
     *
     * @param reviewId The ID of the latest review version to delete.
     * @return true if deletion was successful, false otherwise.
     * @throws SQLException If a database error occurs.
     */
     public boolean deleteReview(int reviewId) throws SQLException {
        ensureConnected();
        connection.setAutoCommit(false);
        int totalDeleted = 0;
        Integer currentId = reviewId; // Start with the latest ID provided

        try {
            while (currentId != null) {
                // Find the previous ID *before* deleting the current one
                Integer previousId = null;
                String findPreviousSql = "SELECT previous_review_id FROM reviews WHERE id = ?";
                try (PreparedStatement pstmtFind = connection.prepareStatement(findPreviousSql)) {
                    pstmtFind.setInt(1, currentId);
                    ResultSet rs = pstmtFind.executeQuery();
                    if (rs.next()) {
                        previousId = rs.getObject("previous_review_id", Integer.class);
                    }
                }

                // Delete the current review ID
                String deleteSql = "DELETE FROM reviews WHERE id = ?";
                try (PreparedStatement pstmtDelete = connection.prepareStatement(deleteSql)) {
                    pstmtDelete.setInt(1, currentId);
                    int deleted = pstmtDelete.executeUpdate();
                    if (deleted > 0) {
                        totalDeleted++;
                    } else {
                        // If we try to delete an ID that doesn't exist (maybe already deleted in a concurrent op?)
                        // break the loop to avoid infinite loop if previousId somehow points back.
                         break;
                    }
                }
                 currentId = previousId; // Move to the previous version
            }
            connection.commit();
            return totalDeleted > 0; // Return true if at least one record was deleted

        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    /**
     * Retrieves the latest review for a specific target (question or answer)
     * made by a specific reviewer.
     *
     * @param reviewerUsername The username of the reviewer.
     * @param targetType       "question" or "answer".
     * @param targetId         The ID of the question or answer.
     * @return The latest Review object, or null if not found.
     * @throws SQLException If a database error occurs.
     */
    public Review getLatestReviewByUserForTarget(String reviewerUsername, String targetType, int targetId) throws SQLException {
        ensureConnected();
        String sql = "SELECT * FROM reviews WHERE reviewer_username = ? AND target_type = ? AND target_id = ? AND is_latest = TRUE";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, reviewerUsername);
            pstmt.setString(2, targetType);
            pstmt.setInt(3, targetId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToReview(rs);
            }
        }
        return null;
    }


    public List<Review> getLatestReviewsForQuestion(int questionId) throws SQLException {
        return getLatestReviewsForTarget("question", questionId);
    }


    public List<Review> getLatestReviewsForAnswer(int answerId) throws SQLException {
        return getLatestReviewsForTarget("answer", answerId);
    }

  
    private List<Review> getLatestReviewsForTarget(String targetType, int targetId) throws SQLException {
        ensureConnected();
        List<Review> reviews = new ArrayList<>();
        String sql = "SELECT * FROM reviews WHERE target_type = ? AND target_id = ? AND is_latest = TRUE ORDER BY timestamp DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, targetType);
            pstmt.setInt(2, targetId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                reviews.add(mapResultSetToReview(rs));
            }
        }
        return reviews;
    }

    /**
     * Retrieves all reviews written by a specific reviewer, ordered by timestamp.
     * Includes all versions, allowing the dashboard to show history or just latest.
     *
     * @param reviewerUsername The username of the reviewer.
     * @return A list of all Review objects by the reviewer.
     * @throws SQLException If a database error occurs.
     */
    public List<Review> getAllReviewsByReviewer(String reviewerUsername) throws SQLException {
        ensureConnected();
        List<Review> reviews = new ArrayList<>();
        // Fetches all reviews, can be filtered later if needed (e.g., only latest)
        String sql = "SELECT * FROM reviews WHERE reviewer_username = ? ORDER BY timestamp DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, reviewerUsername);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                reviews.add(mapResultSetToReview(rs));
            }
        }
        return reviews;
    }


    public Review getReviewById(int reviewId) throws SQLException {
        ensureConnected();
        String sql = "SELECT * FROM reviews WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, reviewId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToReview(rs);
            }
        }
        return null;
    }


    private Review mapResultSetToReview(ResultSet rs) throws SQLException {
        return new Review(
                rs.getInt("id"),
                rs.getString("reviewer_username"),
                rs.getString("target_type"),
                rs.getInt("target_id"),
                rs.getInt("rating"),
                rs.getString("comment"),
                rs.getTimestamp("timestamp"),
                rs.getObject("previous_review_id", Integer.class), // Handle potential NULL
                rs.getBoolean("is_latest")
        );
    }
    
    public void deleteReviewsForTarget(String targetType, int targetId) throws SQLException {
        ensureConnected();
        String sql = "DELETE FROM reviews WHERE target_type = ? AND target_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, targetType);
            pstmt.setInt(2, targetId);
            pstmt.executeUpdate();
        }
    }


	public Answer getAnswerById(int answerId) throws SQLException {
		ensureConnected(); // Ensure the connection is ready
        String query = "SELECT * FROM answers WHERE id = ?";
        Answer answer = null;

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, answerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String content = rs.getString("content");
                    String author = rs.getString("author");
                    int questionId = rs.getInt("question_id");
                    Integer parentId = rs.getObject("parent_answer_id", Integer.class);

                    answer = new Answer(id, content, author, questionId, parentId);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching answer by ID " + answerId + ": " + e.getMessage());
            throw e; 
        }
        return answer;
	
	}
    
    
	  public boolean deleteReviewAndHistory(int startingReviewId) throws SQLException {
	        ensureConnected();
	        Set<Integer> idsToDelete = new HashSet<>();
	        if (startingReviewId <= 0) {
	             return false; // Invalid starting ID
	        }

	        Connection conn = this.connection; // Use the class member connection
	        try {
	            conn.setAutoCommit(false); // Start transaction
	            
	            List<Integer> queue = new ArrayList<>();
	            queue.add(startingReviewId);
	            idsToDelete.add(startingReviewId);

	            int head = 0;
	            while(head < queue.size()) {
	                 int currentId = queue.get(head++);


	                 String findPreviousSql = "SELECT previous_review_id FROM reviews WHERE id = ?";
	                 try (PreparedStatement pstmtPrev = conn.prepareStatement(findPreviousSql)) {
	                     pstmtPrev.setInt(1, currentId);
	                     ResultSet rsPrev = pstmtPrev.executeQuery();
	                     if (rsPrev.next()) {
	                         Integer prevId = rsPrev.getObject("previous_review_id", Integer.class);
	                         if (prevId != null && idsToDelete.add(prevId)) { // If not null and not already added
	                             queue.add(prevId);
	                         }
	                     }
	                 }

	                 // Find next review ID (the one that has currentId as its previous)
	                 String findNextSql = "SELECT id FROM reviews WHERE previous_review_id = ?";
	                 try (PreparedStatement pstmtNext = conn.prepareStatement(findNextSql)) {
	                     pstmtNext.setInt(1, currentId);
	                      ResultSet rsNext = pstmtNext.executeQuery();
	                     if (rsNext.next()) { 
	                         int nextId = rsNext.getInt("id");
	                         if (idsToDelete.add(nextId)) {
	                             queue.add(nextId);
	                         }
	                     }
	                 }
	            }

	            int totalDeleted = 0;
	            if (!idsToDelete.isEmpty()) {
	                String deleteSql = "DELETE FROM reviews WHERE id = ?";
	                try (PreparedStatement pstmtDelete = conn.prepareStatement(deleteSql)) {
	                    for (int idToDelete : idsToDelete) {
	                        pstmtDelete.setInt(1, idToDelete);
	                        totalDeleted += pstmtDelete.executeUpdate();
	                    }
	                }
	                System.out.println("Attempted to delete review history starting from ID " + startingReviewId + ". IDs targeted: " + idsToDelete + ". Records deleted: " + totalDeleted);
	            } else {
	                 System.out.println("No review IDs found in history chain for starting ID: " + startingReviewId);
	            }


	            conn.commit();
	            return totalDeleted > 0;

	        } catch (SQLException e) {
	            System.err.println("Error deleting review history for ID " + startingReviewId + ": " + e.getMessage());
	            if (conn != null) {
	                try {
	                    conn.rollback(); 
	                } catch (SQLException ex) {
	                    System.err.println("Error rolling back transaction: " + ex.getMessage());
	                }
	            }
	            throw e; // Re-throw the exception
	        } finally {
	            if (conn != null) {
	                try {
	                    conn.setAutoCommit(true); // Reset auto-commit state
	                } catch (SQLException ex) {
	                     System.err.println("Error resetting auto-commit: " + ex.getMessage());
	                }
	            }
	        }
	    }
	  public List<Answer> getAllAnswers() throws SQLException {
	         ensureConnected();
	         List<Answer> answers = new ArrayList<>();
	         String query = "SELECT id, question_id, COALESCE(content, '') as content, author, parent_answer_id FROM answers ORDER BY question_id DESC, id DESC";
	         try (Statement stmt = connection.createStatement();
	              ResultSet rs = stmt.executeQuery(query)) {
	             while (rs.next()) {
	                 int id = rs.getInt("id");
	                 int questionId = rs.getInt("question_id");
	                 String content = rs.getString("content"); 
	                 String author = rs.getString("author");
	                 Integer parentId = rs.getObject("parent_answer_id", Integer.class);

	                 Answer answer = new Answer(id, content, author, questionId, parentId);
	                 answers.add(answer);
	             }
	         } catch (SQLException e) {
	            System.err.println("Error fetching all answers: " + e.getMessage());
	            throw e;
	         }
	         return answers;
	    }
	  
    
}
