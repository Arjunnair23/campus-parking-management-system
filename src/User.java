/**
 * User.java
 * -------------------------------------------------------
 * One registered account, as read from the MySQL `users` table.
 * The password itself is never stored - only a random salt and
 * the PBKDF2 hash of the password combined with that salt.
 * -------------------------------------------------------
 */
public class User {
    private final String username;
    private final String fullName;
    private final String salt;          // Base64
    private final String passwordHash;  // Base64

    public User(String username, String fullName, String salt, String passwordHash) {
        this.username = username;
        this.fullName = fullName;
        this.salt = salt;
        this.passwordHash = passwordHash;
    }

    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getSalt() { return salt; }
    public String getPasswordHash() { return passwordHash; }
}