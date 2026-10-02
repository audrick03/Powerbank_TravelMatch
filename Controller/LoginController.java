package Controller;

import Model.PreferenceModel;
import Model.User;
import Repository.UserRepository;
import View.AdminView;
import View.LoginView;
import View.PreferenceView;
import View.RegisterView;

import java.util.Optional;

//test

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
        loginView.addRegisterListener(event -> openRegisterView());
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
            openPreferenceView();
        }
    }

    private void openRegisterView() {
        RegisterView registerView = new RegisterView();
        new RegisterController(
                registerView,
                userRepository,
                user -> {
                    loginView.setUsername(user.getUsername());
                    loginView.showView();
                    loginView.showMessage("Account created. You can now log in.");
                },
                loginView::showView
        );
        loginView.setVisible(false);
        registerView.showView();
    }

    private void openPreferenceView() {
        PreferenceView preferenceView = new PreferenceView();
        PreferenceModel preferenceModel = new PreferenceModel();

        new PreferenceController(preferenceView, preferenceModel);

        loginView.closeView();
        preferenceView.showView();
    }

    private void openAdminView() {
        AdminView adminView = new AdminView();
        new AdminController(adminView);
        loginView.closeView();
        adminView.setVisible(true);
    }

    public void showView() {
        // TODO Auto-generated method stub
        loginView.showView();
    }
}
