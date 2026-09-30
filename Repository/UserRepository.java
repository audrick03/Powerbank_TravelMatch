package Repository;

import Model.User;
import Model.User.Role;
import View.AdminView;
import View.LoginView;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import Controller.AdminController;
import Controller.LoginController;

public class UserRepository {

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
