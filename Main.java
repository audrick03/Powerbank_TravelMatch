import javax.swing.SwingUtilities;

import controller.*;
import view.*;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HomeView homeView = new HomeView();
            LoginView loginView = new LoginView();

            HomeController homeController = new HomeController(homeView, loginView);
            homeController.start();
        });
    }
}