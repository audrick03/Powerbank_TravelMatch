// import View.AdminView;
// import Controller.AdminController;

import View.HomeView;
import Controller.HomeController;
import View.LoginView;
import Controller.LoginController;
import View.ReviewView;
import Controller.ReviewController;
import View.AdminView;
import Controller.AdminController;


//test

public class Main {
    public static void main(String[] args) {


            HomeView homeView = new HomeView();
            new HomeController(homeView, new LoginView());
            homeView.setVisible(true);

            // LoginView loginView = new LoginView();
            // new LoginController(loginView);
            // loginView.setVisible(true);

            // AdminView adminView = new AdminView();
            // new AdminController(adminView);
            // adminView.setVisible(true);


    }
}
