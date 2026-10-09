package model;

public final class TravelerUser extends UserModel {

    public TravelerUser(String username) {
        super(username);
    }

    @Override
    public Role getRole() {
        return Role.NORMAL_USER;
    }

    @Override
    public void dispatchLogin(UserLoginHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Login handler is required");
        }
        handler.onTravelerLogin(this);
    }
}
