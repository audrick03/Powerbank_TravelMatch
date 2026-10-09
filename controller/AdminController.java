package controller;

import model.DestinationModel;
import repository.DestinationRepository;
import view.AdminView;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class AdminController {
    private final AdminView adminView;
    private final DestinationRepository destinationRepository;
    private final Runnable onClose;

    public AdminController(AdminView adminView) {
        this(adminView, DestinationRepository.getInstance(), null);
    }

    public AdminController(AdminView adminView, DestinationRepository destinationRepository) {
        this(adminView, destinationRepository, null);
    }

    public AdminController(AdminView adminView, DestinationRepository destinationRepository,
                           Runnable onClose) {
        if (adminView == null || destinationRepository == null) {
            throw new IllegalArgumentException("Admin view and destination repository are required");
        }
        this.adminView = adminView;
        this.destinationRepository = destinationRepository;
        this.onClose = onClose;
        this.adminView.addDestinationListener(new AddDestinationListener());
        this.adminView.logoutListener(new LogoutListener());
        this.adminView.clearListener(new ClearListener());
        this.adminView.setDefaultCloseOperation(AdminView.DO_NOTHING_ON_CLOSE);
        this.adminView.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                closeAdminView();
            }
        });
    }

    class AddDestinationListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String name = adminView.getDestinationName();
            String location = adminView.getDestinationLocation();
            String region = adminView.getRegion();
            String season = adminView.getBestSeason();
            String duration = adminView.getDuration();
            String difficulty = adminView.getDifficulty();
            String fee = adminView.getFee();
            String category = adminView.getCategory();
            String description = adminView.getDescription();
            List<String> travelTips = parseItems(adminView.getTravelTips());
            List<String> whatToBring = parseItems(adminView.getWhatToBring());
            List<String> recommendedPlaces = parseItems(adminView.getRecommendedPlaces());

            if (name.trim().isEmpty() || location.trim().isEmpty()
                    || description.trim().isEmpty() || !adminView.hasSelectedChoices()
                    || region == null || season == null || duration == null || difficulty == null
                    || fee.trim().isEmpty() || category == null || travelTips.isEmpty()
                    || whatToBring.isEmpty() || recommendedPlaces.isEmpty()) {
                adminView.showMessage("⚠️ Please fill in all fields before adding a destination.");
                return;
            }

            long feeAmount;
            try {
                feeAmount = Long.parseLong(fee);
            } catch (NumberFormatException exception) {
                adminView.showMessage("Fee must be a valid whole-number amount.");
                return;
            }
            if (feeAmount <= 0) {
                adminView.showMessage("Fee must be greater than zero.");
                return;
            }

            DestinationModel destination = new DestinationModel(
                    name.trim(),
                    location.trim(),
                    region,
                    java.util.Collections.singletonList(category),
                    description.trim());
            destination.setTravelInfo(season, duration, difficulty,
                    "₱" + feeAmount + " per person");
            destination.setTravelTips(travelTips);
            destination.setWhatToBring(whatToBring);
            destination.setPlaces(recommendedPlaces);
            if (!destinationRepository.add(destination)) {
                adminView.showMessage("A destination with that name already exists.");
                return;
            }

            adminView.showMessage("Destination added for this session.");
            adminView.clearFields();
        }
    }

    private static List<String> parseItems(String text) {
        List<String> items = new ArrayList<>();
        for (String line : text.split("\\R")) {
            String item = line.trim();
            if (!item.isEmpty()) {
                items.add(item);
            }
        }
        return items;
    }

    class LogoutListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            adminView.showMessage("👋 Logged out successfully!");
            closeAdminView();
        }
    }

    private void closeAdminView() {
        adminView.dispose();
        if (onClose != null) {
            onClose.run();
        }
    }

    class ClearListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            adminView.clearFields();
            adminView.showMessage("🧹 All fields cleared!");
        }
    }
}
