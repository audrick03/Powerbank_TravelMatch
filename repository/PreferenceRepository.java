package repository;

import model.PreferenceModel;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class PreferenceRepository {

    private static final String DEFAULT_JDBC_URL = "jdbc:sqlite:travelmatch.db";

    private final String jdbcUrl;

    public PreferenceRepository() {
        this(DEFAULT_JDBC_URL);
    }

    public PreferenceRepository(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("JDBC URL is required");
        }
        this.jdbcUrl = jdbcUrl;
        initializeDatabase();
    }

    public void savePreferences(String username, PreferenceModel preferences) {
        if (username == null || username.trim().isEmpty() || preferences == null) {
            throw new IllegalArgumentException("Username and preferences are required");
        }
        String[] requestedInterests = preferences.getInterests();
        if (isBlank(preferences.getBudget())
                || isBlank(preferences.getMonth())
                || isBlank(preferences.getGroupType())
                || isBlank(preferences.getActivityLevel())
                || requestedInterests == null || requestedInterests.length == 0) {
            throw new IllegalArgumentException("Complete preferences are required");
        }
        for (String interest : requestedInterests) {
            if (isBlank(interest)) {
                throw new IllegalArgumentException("Preference interests cannot be blank");
            }
        }

        String usernameKey = normalizeUsername(username);
        String saveSql = "INSERT INTO user_preferences "
                + "(username_key, budget, month, group_type, activity_level) "
                + "VALUES (?, ?, ?, ?, ?) "
                + "ON CONFLICT(username_key) DO UPDATE SET "
                + "budget = excluded.budget, month = excluded.month, "
                + "group_type = excluded.group_type, activity_level = excluded.activity_level";
        String deleteInterestsSql =
                "DELETE FROM preference_interests WHERE username_key = ?";
        String insertInterestSql = "INSERT INTO preference_interests "
                + "(username_key, interest, sort_order) VALUES (?, ?, ?)";

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement save = connection.prepareStatement(saveSql);
                 PreparedStatement deleteInterests = connection.prepareStatement(deleteInterestsSql);
                 PreparedStatement insertInterest = connection.prepareStatement(insertInterestSql)) {
                save.setString(1, usernameKey);
                save.setString(2, preferences.getBudget());
                save.setString(3, preferences.getMonth());
                save.setString(4, preferences.getGroupType());
                save.setString(5, preferences.getActivityLevel());
                save.executeUpdate();

                deleteInterests.setString(1, usernameKey);
                deleteInterests.executeUpdate();

                Set<String> interests = new LinkedHashSet<>();
                for (String interest : requestedInterests) {
                    interests.add(interest.trim());
                }

                int sortOrder = 0;
                for (String interest : interests) {
                    insertInterest.setString(1, usernameKey);
                    insertInterest.setString(2, interest);
                    insertInterest.setInt(3, sortOrder++);
                    insertInterest.addBatch();
                }
                insertInterest.executeBatch();
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                throw exception;
            }
        } catch (SQLException exception) {
            throw databaseFailure("save user preferences", exception);
        }
    }

    public Optional<PreferenceModel> loadPreferences(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }

        String usernameKey = normalizeUsername(username);
        String loadSql = "SELECT budget, month, group_type, activity_level "
                + "FROM user_preferences WHERE username_key = ?";
        String loadInterestsSql = "SELECT interest FROM preference_interests "
                + "WHERE username_key = ? ORDER BY sort_order";

        try (Connection connection = openConnection();
             PreparedStatement load = connection.prepareStatement(loadSql)) {
            load.setString(1, usernameKey);
            try (ResultSet result = load.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }

                String budget = result.getString("budget");
                String month = result.getString("month");
                String groupType = result.getString("group_type");
                String activityLevel = result.getString("activity_level");
                List<String> interests = new ArrayList<>();

                try (PreparedStatement loadInterests =
                             connection.prepareStatement(loadInterestsSql)) {
                    loadInterests.setString(1, usernameKey);
                    try (ResultSet interestResults = loadInterests.executeQuery()) {
                        while (interestResults.next()) {
                            interests.add(interestResults.getString("interest"));
                        }
                    }
                }

                return Optional.of(new PreferenceModel(
                        budget,
                        month,
                        groupType,
                        activityLevel,
                        interests.toArray(new String[0])));
            }
        } catch (SQLException exception) {
            throw databaseFailure("load user preferences", exception);
        }
    }

    private void initializeDatabase() {
        String createPreferencesTable = "CREATE TABLE IF NOT EXISTS user_preferences ("
                + "username_key TEXT NOT NULL PRIMARY KEY, "
                + "budget TEXT NOT NULL, "
                + "month TEXT NOT NULL, "
                + "group_type TEXT NOT NULL, "
                + "activity_level TEXT NOT NULL, "
                + "FOREIGN KEY (username_key) REFERENCES users(username_key) ON DELETE CASCADE"
                + ")";
        String createInterestsTable = "CREATE TABLE IF NOT EXISTS preference_interests ("
                + "username_key TEXT NOT NULL, "
                + "interest TEXT NOT NULL, "
                + "sort_order INTEGER NOT NULL, "
                + "PRIMARY KEY (username_key, interest), "
                + "UNIQUE (username_key, sort_order), "
                + "FOREIGN KEY (username_key) REFERENCES user_preferences(username_key) "
                + "ON DELETE CASCADE"
                + ")";

        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(createPreferencesTable);
            statement.execute(createInterestsTable);
        } catch (SQLException exception) {
            throw databaseFailure("initialize preference database", exception);
        }
    }

    private Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException exception) {
            connection.close();
            throw exception;
        }
        return connection;
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private IllegalStateException databaseFailure(String operation, SQLException cause) {
        return new IllegalStateException("Could not " + operation, cause);
    }
}
