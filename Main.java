import Controller.HomeController;
import View.HomeView;
import View.LoginView;

public class Main {

    public static void main(String[] args) {
        HomeView homeView = new HomeView();
        LoginView loginView = new LoginView();

        HomeController homeController = new HomeController(homeView, loginView);
        homeController.start();
    }
}