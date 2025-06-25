package databasePart1;

import application.Answer;
import application.Question;
import application.QuestionSystem;

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
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    
    // Table creation for storing accounts
    private void createTables() throws SQLException {
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
        	

            // Add PARENT_ANSWER_ID column to answers table
            String alterAnswerTable = "ALTER TABLE answers ADD COLUMN IF NOT EXISTS PARENT_ANSWER_ID INT";
            statement.execute(alterAnswerTable);

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

                    return new Question(questionId, title, content, author, tags);
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
        String query = "SELECT * FROM questions";
        try (PreparedStatement pstmt = connection.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                String content = rs.getString("content");
                String author = rs.getString("author");
                List<String> tags = List.of(rs.getString("tags").split(",")); // Convert CSV tags to list

                Question question = new Question(id, title, content, author, tags);
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
    
 
  
    
}
