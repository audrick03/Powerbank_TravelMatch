package Repository;

import Model.User;
import Model.User.Role;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;



public class UserRepository {

    private static final int MINIMUM_PASSWORD_LENGTH = 8;

    private final Map<String, Account> accounts = new HashMap<>();

    public UserRepository() {
        // Demo-only credentials; replace this in-memory store for production use.
        addAccount("admin", "admin123", Role.ADMIN);
        addAccount("user", "user123", Role.NORMAL_USER);
    }

    public Optional<User> authenticate(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null) {
            return Optional.empty();
        }

        Account account = accounts.get(normalizeUsername(username));
        if (account == null || !account.password.equals(password)) {
            return Optional.empty();
        }

        return Optional.of(account.user);
    }

    public Optional<User> register(String username, String password) {
        if (password == null || password.length() < MINIMUM_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must be at least " + MINIMUM_PASSWORD_LENGTH + " characters");
        }

        User user = new User(username, Role.NORMAL_USER);
        String normalizedUsername = normalizeUsername(user.getUsername());
        if (accounts.containsKey(normalizedUsername)) {
            return Optional.empty();
        }

        accounts.put(normalizedUsername, new Account(password, user));
        return Optional.of(user);
    }

    private void addAccount(String username, String password, Role role) {
        User user = new User(username, role);
        accounts.put(normalizeUsername(username), new Account(password, user));
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private static final class Account {
        private final String password;
        private final User user;

        private Account(String password, User user) {
            this.password = password;
            this.user = user;
        }
    }
}
