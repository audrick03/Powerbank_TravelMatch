package Controller;

import View.AdminView;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AdminController {
    private AdminView adminView;

    public AdminController(AdminView adminView) {
        this.adminView = adminView;
        this.adminView.addDestinationListener(new AddDestinationListener());
        this.adminView.viewStatsListener(new ViewStatsListener());
        this.adminView.logoutListener(new LogoutListener());
    }

    class AddDestinationListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String name = adminView.getDestinationName();
            String location = adminView.getDestinationLocation();
            String season = adminView.getBestSeason();
            String fee = adminView.getFee();
            String category = adminView.getCategory();

            // Simulate saving to database
            System.out.println("Destination added:");
            System.out.println("Name: " + name);
            System.out.println("Location: " + location);
            System.out.println("Best Season: " + season);
            System.out.println("Fee: " + fee);
            System.out.println("Category: " + category);

            adminView.showMessage("Destination added successfully!");
        }
    }

    class ViewStatsListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Simulate viewing statistics
            adminView.showMessage("Most searched destinations:\n1. Palawan\n2. Baguio\n3. Siargao");
        }
    }

    class LogoutListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            adminView.showMessage("Logged out successfully!");
            adminView.dispose();
        }
    }
}
