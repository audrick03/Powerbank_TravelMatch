package model;

public interface UserLoginHandler {

    void onAdministratorLogin(AdministratorUser user);

    void onTravelerLogin(TravelerUser user);
}
