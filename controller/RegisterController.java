package controller;

import model.*;
import repository.*;
import service.*;
import view.*;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Optional;
import java.util.function.Consumer;

public class RegisterController {

    private static final int MINIMUM_PASSWORD_LENGTH = 8;

    private final RegisterView view;
    private final AuthenticationService authenticationService;
    private final Consumer<UserModel> onRegistered;
    private final Runnable onBack;

    public RegisterController(RegisterView view, UserRepository userRepository,
                              Consumer<UserModel> onRegistered, Runnable onBack) {
        this(view, new AuthenticationService(userRepository), onRegistered, onBack);
    }

    public RegisterController(RegisterView view, AuthenticationService authenticationService,
                              Consumer<UserModel> onRegistered, Runnable onBack) {
        if (view == null || authenticationService == null || onRegistered == null || onBack == null) {
            throw new IllegalArgumentException("Registration dependencies are required");
        }

        this.view = view;
        this.authenticationService = authenticationService;
        this.onRegistered = onRegistered;
        this.onBack = onBack;

        view.addRegisterListener(event -> register());
        view.addBackListener(event -> returnToLogin());
        view.setDefaultCloseOperation(RegisterView.DO_NOTHING_ON_CLOSE);
        view.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                returnToLogin();
            }
        });
    }

    private void register() {
        String username = view.getUsername();
        String password = view.getPassword();
        String confirmedPassword = view.getConfirmedPassword();

        if (username.isEmpty()) {
            view.setMessage("Please enter a username.");
            return;
        }
        if (password.isEmpty()) {
            view.setMessage("Please enter a password.");
            return;
        }
        if (password.length() < MINIMUM_PASSWORD_LENGTH) {
            view.setMessage("Password must be at least 8 characters.");
            return;
        }
        if (!password.equals(confirmedPassword)) {
            view.setMessage("Passwords do not match.");
            return;
        }

        Optional<UserModel> registeredUser = authenticationService.register(username, password);
        if (!registeredUser.isPresent()) {
            view.setMessage("That username is already in use.");
            return;
        }

        view.clearPasswordFields();
        view.closeView();
        onRegistered.accept(registeredUser.get());
    }

    private void returnToLogin() {
        view.closeView();
        onBack.run();
    }
}
