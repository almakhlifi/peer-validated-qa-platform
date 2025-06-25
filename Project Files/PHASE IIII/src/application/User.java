package application;
import java.util.List;

/**
 * The User class represents a user entity in the system.
 * It contains the user's details such as userName, password, and role.
 */
public class User {
    private String userName;
    private String password;
    private List<String> roles;

    // Constructor to initialize a new User object with userName, password, and role.
    public User( String userName, String password, List<String> roles) {
        this.userName = userName;
        this.password = password;
        this.roles = roles;
    }
    
    // new constructor
    public User(String userName) {
        this.userName = userName;
        this.password = null;
        this.roles = null;
    }

    public String getUserName() { return userName; }
    public String getPassword() { return password; }
    public List<String> getRoles() { return roles; }
    
    // set multiple roles
    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
