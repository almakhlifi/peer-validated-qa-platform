package databasePart1;
import java.sql.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import application.User;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
/**
 * The DatabaseHelper class is responsible for managing the connection to the database,
 * performing operations such as user registration, login validation, and handling invitation codes.
 */
public class DatabaseHelper {

    // JDBC driver name and database URL 
    static final String JDBC_DRIVER = "org.h2.Driver";   
    static final String DB_URL = "jdbc:h2:~/FoundationDatabase";  

    //  Database credentials 
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

    public void addRoleToUser(String username, String role) throws SQLException {
        String query = "INSERT INTO user_roles (userName, role) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, role);
            pstmt.executeUpdate();
        }
    }

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

        // Proceed with role removal if checks are passed
        String query = "DELETE FROM user_roles WHERE userName = ? AND role = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, role);
            pstmt.executeUpdate();
        }
    }

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
                + "expiration_timestamp TIMESTAMP)"; // Stores Date & Time
        statement.execute(invitationTable);
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
        // insert into cse360users table
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
        ensureConnected(); // Ensure the database connection is open before querying

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
        return false; // If an error occurs, assume user doesn't exist
    }

    // Generates a new invitation code and inserts it into the database.
    public String generateInvitationCode() {
        String code = UUID.randomUUID().toString().substring(0, 4); // Generate a random 4-character code
        String query = "INSERT INTO InvitationCodes (code) VALUES (?)";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, code);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return code;
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
                    LocalDateTime expirationDateTime = expirationTimestamp.toLocalDateTime(); // Corrected

                    if (expirationDateTime.isBefore(LocalDateTime.now())) {
                        System.out.println("Invitation code has expired.");
                        return false;
                    }
                    return true; // Code is valid
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("Invalid invitation code.");
        return false;
    }





    // Marks the invitation code as used in the database.
    private void markInvitationCodeAsUsed(String code) {
        String query = "DELETE FROM Invitations WHERE invite_code = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, code);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void storeInvitation(String code, String role, LocalDate date, String time) throws SQLException {
        ensureConnected(); // Ensure DB connection is open

        //Convert Date & Time to LocalDateTime
        LocalDateTime expirationDateTime = LocalDateTime.parse(date + "T" + time + ":00");

        String query = "INSERT INTO Invitations (invite_code, role, expiration_timestamp) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, code);
            stmt.setString(2, role);
            stmt.setTimestamp(3, Timestamp.valueOf(expirationDateTime)); //Corrected
            stmt.executeUpdate();
        }
    }

    public boolean validateInvitation(String email, String code) throws SQLException {
        String query = "SELECT role, expiration_date FROM Invitations WHERE email = ? AND invite_code = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, email);
            stmt.setString(2, code);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                LocalDate expirationDate = rs.getDate("expiration_date").toLocalDate();
                if (expirationDate.isBefore(LocalDate.now())) {
                    return false; // Invitation expired
                }
                return true;
            }
            return false;
        }
    }

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

    // Helper method to validate role before inserting into the database
    private boolean isValidRole(String role) {
        String[] validRoles = {"admin", "student", "instructor", "staff", "reviewer"};
        for (String validRole : validRoles) {
            if (validRole.equals(role)) {
                return true;
            }
        }
        return false;
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

    public boolean isUserAdmin(String userName) throws SQLException {
        String query = "SELECT COUNT(*) FROM user_roles WHERE userName = ? AND role = 'admin'";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, userName);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    // New method to set a one-time password for a user
    public void setOneTimePassword(String username, String oneTimePassword) throws SQLException {
        String query = "UPDATE cse360users SET oneTimePassword = ? WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, oneTimePassword);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        }
    }

    // New method to validate the one-time password
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

    // New method to clear the one-time password after use
    public void clearOneTimePassword(String username) throws SQLException {
        String query = "UPDATE cse360users SET oneTimePassword = NULL WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        }
    }

    // New method to update the user's password
    public void updatePassword(String username, String newPassword) throws SQLException {
        String query = "UPDATE cse360users SET password = ? WHERE userName = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, newPassword);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        }
    }
}