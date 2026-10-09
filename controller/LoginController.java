package controller;

import model.*;
import repository.*;
import service.*;
import view.*;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Optional;
import java.util.function.Consumer;

public class LoginController implements UserLoginHandler {

    private final LoginView loginView;
    private final AuthenticationService authenticationService;
    private final Consumer<UserModel> onUserLoggedIn;
    private final Runnable onBackToHome;

    public LoginController(LoginView loginView) {
        this(loginView, new AuthenticationService(new UserRepository()), null, null);
    }

    public LoginController(LoginView loginView, UserRepository userRepository) {
        this(loginView, new AuthenticationService(userRepository), null, null);
    }

    public LoginController(LoginView loginView, AuthenticationService authenticationService) {
        this(loginView, authenticationService, null, null);
    }

    public LoginController(LoginView loginView, Consumer<UserModel> onUserLoggedIn) {
        this(loginView, new AuthenticationService(new UserRepository()), onUserLoggedIn, null);
    }

    public LoginController(LoginView loginView, AuthenticationService authenticationService,
                           Consumer<UserModel> onUserLoggedIn) {
        this(loginView, authenticationService, onUserLoggedIn, null);
    }

    public LoginController(LoginView loginView, AuthenticationService authenticationService,
                           Consumer<UserModel> onUserLoggedIn, Runnable onBackToHome) {
        if (loginView == null || authenticationService == null) {
            throw new IllegalArgumentException("Login view and authentication service are required");
        }

        this.loginView = loginView;
        this.authenticationService = authenticationService;
        this.onUserLoggedIn = onUserLoggedIn;
        this.onBackToHome = onBackToHome;
        loginView.addLoginListener(event -> login());
        loginView.addRegisterListener(event -> openRegisterView());
        loginView.addBackToHomeListener(event -> openHomeView());
        loginView.setDefaultCloseOperation(LoginView.DO_NOTHING_ON_CLOSE);
        loginView.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                openHomeView();
            }
        });
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
        user.dispatchLogin(this);
    }

    @Override
    public void onAdministratorLogin(AdministratorUser user) {
        openAdminView();
    }

    @Override
    public void onTravelerLogin(TravelerUser user) {
        if (onUserLoggedIn != null) {
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
                new PreferenceService(new PreferenceRepository(), DestinationRepository.getInstance()),
                this::openHomeView);

        loginView.closeView();
        preferenceController.showView();
    }

    private void openAdminView() {
        AdminView adminView = new AdminView();
        if (onBackToHome == null) {
            new AdminController(adminView);
        } else {
            loginView.closeView();
            new AdminController(adminView, DestinationRepository.getInstance(), onBackToHome);
        }
        adminView.setVisible(true);
    }

    public void showView() {
        loginView.showView();
    }

    public void openHomeView() {
        loginView.closeView();
        if (onBackToHome != null) {
            onBackToHome.run();
        } else {
            HomeView homeView = new HomeView();
            new HomeController(homeView, null);
            homeView.setVisible(true);
        }
    }
}
