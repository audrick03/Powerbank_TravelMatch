package controller;

import java.util.Optional;

import model.*;
import repository.*;
import service.*;
import view.*;

public class LoginController {

    private final LoginView loginView;
    private final AuthenticationService authenticationService;

    public LoginController(LoginView loginView) {
        this(loginView, new UserRepository());
    }

    public LoginController(LoginView loginView, UserRepository userRepository) {
        this(loginView, new AuthenticationService(userRepository));
    }

    public LoginController(LoginView loginView, AuthenticationService authenticationService) {
        if (loginView == null || authenticationService == null) {
            throw new IllegalArgumentException("Login view and authentication service are required");
        }

        this.loginView = loginView;
        this.authenticationService = authenticationService;
        loginView.addLoginListener(event -> login());
        loginView.addRegisterListener(event -> openRegisterView());
        loginView.addBackToHomeListener(event -> openHomeView());
    }

    private void login() {
        Optional<UserModel> authenticatedUser =
            authenticationService.authenticate(loginView.getUsername(), loginView.getPassword());

        if (!authenticatedUser.isPresent()) {
            loginView.showError("Invalid username or password.");
            return;
        }

        UserModel user = authenticatedUser.get();
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
                authenticationService,
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
        adminView.setVisible(true);
    }

    public void showView() {
        loginView.showView();
    }

    public void openHomeView() {
        HomeView homeView = new HomeView();
        new HomeController(homeView, loginView);
        loginView.closeView();
        homeView.setVisible(true);
    }
}
