package Controller;

import Model.User;
import Repository.UserRepository;
import View.AdminView;
import View.LoginView;
import java.util.Optional;

public class LoginController {

    private final LoginView loginView;
    private final UserRepository userRepository;

    public LoginController(LoginView loginView) {
        this(loginView, new UserRepository());
    }

    public LoginController(LoginView loginView, UserRepository userRepository) {
        if (loginView == null || userRepository == null) {
            throw new IllegalArgumentException("Login view and user repository are required");
        }

        this.loginView = loginView;
        this.userRepository = userRepository;
        loginView.addLoginListener(event -> login());
        loginView.addRegisterListener(event ->
            loginView.showMessage("Account registration is not available yet."));
    }

    private void login() {
        Optional<User> authenticatedUser =
            userRepository.authenticate(loginView.getUsername(), loginView.getPassword());

        if (!authenticatedUser.isPresent()) {
            loginView.showError("Invalid username or password.");
            return;
        }

        User user = authenticatedUser.get();
        loginView.clearError();
        if (user.isAdmin()) {
            openAdminView();
        } else {
            loginView.showMessage(
                "Login successful. The normal-user area is not available yet.");
        }
    }

    private void openAdminView() {
        AdminView adminView = new AdminView();
        new AdminController(adminView);
        loginView.closeView();
        adminView.setVisible(true);
    }
}
