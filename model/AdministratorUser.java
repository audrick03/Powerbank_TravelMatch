package model;

public final class AdministratorUser extends UserModel {

    public AdministratorUser(String username) {
        super(username);
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }

    @Override
    public void dispatchLogin(UserLoginHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Login handler is required");
        }
        handler.onAdministratorLogin(this);
    }
}
