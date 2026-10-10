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
    private static final int MAX_TEXT_LENGTH = 255;

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
            List<String> travelTips;
            List<String> whatToBring;
            List<String> recommendedPlaces;

            if (!adminView.hasSelectedChoices()) {
                adminView.showMessage("⚠️ Please fill in all fields before adding a destination.");
                return;
            }
            if (!adminView.hasValidMonthRange()) {
                adminView.showMessage("The end month must be later than the start month.");
                return;
            }

            try {
                travelTips = parseItems(adminView.getTravelTips(), "Travel tips");
                whatToBring = parseItems(adminView.getWhatToBring(), "What to bring");
                recommendedPlaces = parseItems(adminView.getRecommendedPlaces(),
                        "Recommended places");
                validateAdminInput(name, location, description, region, category, season,
                        duration, difficulty, fee, travelTips, whatToBring, recommendedPlaces);
            } catch (IllegalArgumentException exception) {
                adminView.showMessage(exception.getMessage());
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

    private static void validateAdminInput(String name, String location, String description,
                                          String region, String category, String season,
                                          String duration, String difficulty, String fee,
                                          List<String> travelTips, List<String> whatToBring,
                                          List<String> recommendedPlaces) {
        if (!isValidNameOrLocation(name)) {
            throw new IllegalArgumentException(
                    "Destination name must contain only letters, spaces, and punctuation "
                            + "and be no longer than "
                            + MAX_TEXT_LENGTH + " characters.");
        }
        if (!isValidNameOrLocation(location)) {
            throw new IllegalArgumentException(
                    "Location must contain only letters, spaces, and punctuation "
                            + "and be no longer than "
                            + MAX_TEXT_LENGTH + " characters.");
        }
        requireText("Description", description);
        requireText("Region", region);
        requireText("Category", category);
        requireText("Best time", season);
        requireText("Duration", duration);
        requireText("Difficulty", difficulty);
        requireText("Fee", fee);
        requireTextList("Travel tips", travelTips);
        requireTextList("What to bring", whatToBring);
        requireTextList("Recommended places", recommendedPlaces);
    }

    private static boolean isValidNameOrLocation(String value) {
        if (!isValidText(value) || value.codePoints().noneMatch(Character::isLetter)) {
            return false;
        }
        return value.codePoints().allMatch(codePoint -> Character.isLetter(codePoint)
                || codePoint == ' '
                || Character.getType(codePoint) == Character.NON_SPACING_MARK
                || Character.getType(codePoint) == Character.COMBINING_SPACING_MARK
                || Character.getType(codePoint) == Character.ENCLOSING_MARK
                || Character.getType(codePoint) == Character.CONNECTOR_PUNCTUATION
                || Character.getType(codePoint) == Character.DASH_PUNCTUATION
                || Character.getType(codePoint) == Character.START_PUNCTUATION
                || Character.getType(codePoint) == Character.END_PUNCTUATION
                || Character.getType(codePoint) == Character.INITIAL_QUOTE_PUNCTUATION
                || Character.getType(codePoint) == Character.FINAL_QUOTE_PUNCTUATION
                || Character.getType(codePoint) == Character.OTHER_PUNCTUATION);
    }

    private static boolean isValidText(String value) {
        return value != null && !value.isBlank()
                && value.codePointCount(0, value.length()) <= MAX_TEXT_LENGTH;
    }

    private static void requireText(String field, String value) {
        if (!isValidText(value)) {
            throw new IllegalArgumentException(field + " must be nonblank and no longer than "
                    + MAX_TEXT_LENGTH + " characters.");
        }
    }

    private static void requireTextList(String field, List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be blank.");
        }
        for (String value : values) {
            requireText(field, value);
        }
    }

    private static List<String> parseItems(String text, String field) {
        if (text == null) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        List<String> items = new ArrayList<>();
        for (String line : text.split("\\R")) {
            String item = line.strip();
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
