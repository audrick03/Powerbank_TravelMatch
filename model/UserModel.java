package model;

public abstract class UserModel {

    public enum Role {
        NORMAL_USER,
        ADMIN
    }

    private final String username;

    protected UserModel(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }
        this.username = username.trim();
    }

    public String getUsername() {
        return username;
    }

    public abstract Role getRole();

    public abstract void dispatchLogin(UserLoginHandler handler);
}
