package controller;

import java.util.Optional;

import model.*;
import repository.*;
import service.*;
import view.*;

import java.util.function.Consumer;

public class LoginController {

    private final LoginView loginView;
    private final AuthenticationService authenticationService;
    private final Consumer<UserModel> onUserLoggedIn;

    public LoginController(LoginView loginView) {
        this(loginView, new AuthenticationService(new UserRepository()), null);
    }

    public LoginController(LoginView loginView, UserRepository userRepository) {
        this(loginView, new AuthenticationService(userRepository), null);
    }

    public LoginController(LoginView loginView, AuthenticationService authenticationService) {
        this(loginView, authenticationService, null);
    }

    public LoginController(LoginView loginView, Consumer<UserModel> onUserLoggedIn) {
        this(loginView, new AuthenticationService(new UserRepository()), onUserLoggedIn);
    }

    public LoginController(LoginView loginView, AuthenticationService authenticationService,
                           Consumer<UserModel> onUserLoggedIn) {
        if (loginView == null || authenticationService == null) {
            throw new IllegalArgumentException("Login view and authentication service are required");
        }

        this.loginView = loginView;
        this.authenticationService = authenticationService;
        this.onUserLoggedIn = onUserLoggedIn;
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
        } else if (onUserLoggedIn != null) {
            loginView.closeView();
            onUserLoggedIn.accept(user);
        } else {
            openPreferenceView(user);
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

    private void openPreferenceView(UserModel user) {
        PreferenceView preferenceView = new PreferenceView();
        PreferenceModel preferenceModel = new PreferenceModel();
        PreferenceController preferenceController = new PreferenceController(
                preferenceView,
                preferenceModel,
                user,
                new PreferenceService(
                        new PreferenceRepository(),
                        DestinationRepository.getInstance()));

        loginView.closeView();
        preferenceController.showView();
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
