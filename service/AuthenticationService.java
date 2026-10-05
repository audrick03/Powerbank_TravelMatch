package service;

import model.*;
import repository.*;

import java.util.Optional;

public class AuthenticationService {

    private final UserRepository userRepository;

    public AuthenticationService(UserRepository userRepository) {
        if (userRepository == null) {
            throw new IllegalArgumentException("User repository is required");
        }

        this.userRepository = userRepository;
    }

    public Optional<UserModel> authenticate(String username, String password) {
        return userRepository.authenticate(username, password);
    }

    public Optional<UserModel> register(String username, String password) {
        return userRepository.register(username, password);
    }
}
