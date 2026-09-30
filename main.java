// import View.AdminView;
// import Controller.AdminController;
import View.LoginView;
import Controller.LoginController;

public class Main {
    public static void main(String[] args) {

            LoginView loginView = new LoginView();
            new LoginController(loginView);
            loginView.setVisible(true);

            // AdminView adminView = new AdminView();
            // new AdminController(adminView);
            // adminView.setVisible(true);


    }
}
