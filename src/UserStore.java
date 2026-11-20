import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.Properties;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * UserStore.java
 * -------------------------------------------------------
 * Stores registered users in a MySQL database over JDBC.
 *
 * Connection settings come from db.properties (project root).
 * On startup the database and the `users` table are created
 * if missing.
 *
 * Passwords are never stored: each user gets a random salt and
 * we keep only the PBKDF2-HMAC-SHA256 hash of the password.
 * All SQL uses PreparedStatements, so usernames/passwords
 * cannot be used for SQL injection.
 *
 * The MySQL driver (Connector/J jar) is only needed at RUN time,
 * not to compile.
 * -------------------------------------------------------
 */
public class UserStore {

    private static final int ITERATIONS = 65536;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String url;
    private final String dbUser;
    private final String dbPassword;

    /** Reads connection settings, connects once to verify them, and creates the table if needed. */
    public UserStore(String propertiesPath) {
        Properties p = new Properties();
        try (InputStream in = new FileInputStream(propertiesPath)) {
            p.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + propertiesPath
                    + ". Create it in the project root (see db.properties).", e);
        }

        String host = p.getProperty("db.host", "localhost").trim();
        String port = p.getProperty("db.port", "3306").trim();
        String name = p.getProperty("db.name", "campus_parking").trim();
        this.dbUser = p.getProperty("db.user", "root").trim();
        this.dbPassword = p.getProperty("db.password", "");

        // createDatabaseIfNotExist: first run works even before the database exists
        this.url = "jdbc:mysql://" + host + ":" + port + "/" + name
                + "?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        initSchema();
    }

    // ---------------- public API ----------------

    /** Returns null on success, or a human-readable error message. */
    public String register(String fullName, String username, String password) {
        fullName = fullName == null ? "" : fullName.trim();
        username = username == null ? "" : username.trim();
        password = password == null ? "" : password;

        if (fullName.length() < 2 || fullName.length() > 50) return "Full name must be 2-50 characters.";
        if (!username.matches("[A-Za-z0-9_]{3,20}"))
            return "Username must be 3-20 letters, digits or underscores.";
        if (password.length() < 6) return "Password must be at least 6 characters.";
        if (password.length() > 100) return "Password is too long.";

        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash(password, salt));

        String sql = "INSERT INTO users (username, full_name, password_salt, password_hash) VALUES (?, ?, ?, ?)";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, fullName);
            ps.setString(3, saltB64);
            ps.setString(4, hashB64);
            ps.executeUpdate();
            return null;
        } catch (SQLException e) {
            // 1062 = duplicate entry for the UNIQUE username column
            if (e.getErrorCode() == 1062) return "That username is already taken.";
            System.out.println("Database error during register: " + e.getMessage());
            return "Server error. Please try again.";
        }
    }

    /** Returns the User if username + password are correct, otherwise null. */
    public User authenticate(String username, String password) {
        if (username == null || password == null) return null;

        String sql = "SELECT username, full_name, password_salt, password_hash FROM users WHERE username = ?";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    // burn similar time so response timing doesn't reveal whether the username exists
                    hash(password, new byte[16]);
                    return null;
                }
                User u = new User(rs.getString("username"), rs.getString("full_name"),
                        rs.getString("password_salt"), rs.getString("password_hash"));
                byte[] salt = Base64.getDecoder().decode(u.getSalt());
                byte[] expected = Base64.getDecoder().decode(u.getPasswordHash());
                byte[] actual = hash(password, salt);
                return MessageDigest.isEqual(expected, actual) ? u : null;
            }
        } catch (SQLException e) {
            System.out.println("Database error during login: " + e.getMessage());
            return null;
        }
    }

    public int count() {
        try (Connection c = connect();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    // ---------------- database plumbing ----------------

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, dbUser, dbPassword);
    }

    /** Verifies the connection and creates the users table if it doesn't exist yet. */
    private void initSchema() {
        String create = "CREATE TABLE IF NOT EXISTS users ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "username VARCHAR(20) NOT NULL UNIQUE, "
                + "full_name VARCHAR(50) NOT NULL, "
                + "password_salt VARCHAR(64) NOT NULL, "
                + "password_hash VARCHAR(64) NOT NULL, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ") CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
        try (Connection c = connect(); Statement st = c.createStatement()) {
            st.executeUpdate(create);
        } catch (SQLException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (msg.contains("No suitable driver")) {
                throw new IllegalStateException("MySQL driver not found. Put mysql-connector-j-*.jar in the "
                        + "lib/ folder and start with:  java -cp \"bin;lib/*\" ApiServer", e);
            }
            throw new IllegalStateException("Could not connect to MySQL at " + url.split("\\?")[0]
                    + " as user '" + dbUser + "': " + msg
                    + "\nCheck that MySQL is running and that db.properties has the right user/password.", e);
        }
    }

    private static byte[] hash(String password, byte[] salt) {
        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }
}