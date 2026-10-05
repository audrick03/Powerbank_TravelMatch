package repository;

import model.*;
import model.User.Role;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public class UserRepository {

    private static final int MINIMUM_PASSWORD_LENGTH = 8;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int HASH_LENGTH_BITS = 256;
    private static final int PBKDF2_ITERATIONS = 210_000;
    private static final String DEFAULT_JDBC_URL = "jdbc:sqlite:travelmatch.db";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final String jdbcUrl;

    public UserRepository() {
        this(DEFAULT_JDBC_URL);
    }

    public UserRepository(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("JDBC URL is required");
        }

        this.jdbcUrl = jdbcUrl;
        initializeDatabase();
    }

    public Optional<User> authenticate(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null) {
            return Optional.empty();
        }

        String sql = "SELECT username, role, password_salt, password_hash "
                + "FROM users WHERE username_key = ?";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizeUsername(username));

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }

                byte[] salt = result.getBytes("password_salt");
                byte[] storedHash = result.getBytes("password_hash");
                byte[] candidateHash = hashPassword(password, salt);
                if (!MessageDigest.isEqual(storedHash, candidateHash)) {
                    return Optional.empty();
                }

                return Optional.of(new User(
                        result.getString("username"),
                        Role.valueOf(result.getString("role"))));
            }
        } catch (SQLException exception) {
            throw databaseFailure("authenticate user", exception);
        }
    }

    public Optional<User> register(String username, String password) {
        if (password == null || password.length() < MINIMUM_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must be at least " + MINIMUM_PASSWORD_LENGTH + " characters");
        }

        User user = new User(username, Role.NORMAL_USER);
        String usernameKey = normalizeUsername(user.getUsername());
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        byte[] passwordHash = hashPassword(password, salt);

        String sql = "INSERT INTO users "
                + "(username, username_key, role, password_salt, password_hash) "
                + "VALUES (?, ?, ?, ?, ?) "
                + "ON CONFLICT(username_key) DO NOTHING";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getUsername());
            statement.setString(2, usernameKey);
            statement.setString(3, user.getRole().name());
            statement.setBytes(4, salt);
            statement.setBytes(5, passwordHash);

            return statement.executeUpdate() == 1 ? Optional.of(user) : Optional.empty();
        } catch (SQLException exception) {
            throw databaseFailure("register user", exception);
        }
    }

    private void initializeDatabase() {
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users ("
                + "username TEXT NOT NULL, "
                + "username_key TEXT NOT NULL UNIQUE, "
                + "role TEXT NOT NULL CHECK (role IN ('NORMAL_USER', 'ADMIN')), "
                + "password_salt BLOB NOT NULL, "
                + "password_hash BLOB NOT NULL, "
                + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP"
                + ")";

        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(createUsersTable);
            seedDemoAccount(connection, "admin", "admin123", Role.ADMIN);
            seedDemoAccount(connection, "user", "user123", Role.NORMAL_USER);
        } catch (SQLException exception) {
            throw databaseFailure("initialize user database", exception);
        }
    }

    private void seedDemoAccount(Connection connection, String username, String password, Role role)
            throws SQLException {
        String usernameKey = normalizeUsername(username);
        String existsSql = "SELECT 1 FROM users WHERE username_key = ?";
        try (PreparedStatement exists = connection.prepareStatement(existsSql)) {
            exists.setString(1, usernameKey);
            try (ResultSet result = exists.executeQuery()) {
                if (result.next()) {
                    return;
                }
            }
        }

        byte[] salt = new byte[SALT_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(salt);

        String insertSql = "INSERT INTO users "
                + "(username, username_key, role, password_salt, password_hash) "
                + "VALUES (?, ?, ?, ?, ?) "
                + "ON CONFLICT(username_key) DO NOTHING";
        try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
            insert.setString(1, username);
            insert.setString(2, usernameKey);
            insert.setString(3, role.name());
            insert.setBytes(4, salt);
            insert.setBytes(5, hashPassword(password, salt));
            insert.executeUpdate();
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private byte[] hashPassword(String password, byte[] salt) {
        char[] passwordChars = password.toCharArray();
        PBEKeySpec keySpec =
                new PBEKeySpec(passwordChars, salt, PBKDF2_ITERATIONS, HASH_LENGTH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(keySpec)
                    .getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not hash account password", exception);
        } finally {
            keySpec.clearPassword();
            Arrays.fill(passwordChars, '\0');
        }
    }

    private IllegalStateException databaseFailure(String operation, SQLException cause) {
        return new IllegalStateException("Could not " + operation, cause);
    }
}
