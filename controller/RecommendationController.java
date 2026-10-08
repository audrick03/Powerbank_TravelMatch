package controller;

import model.*;
import repository.*;
import view.*;

import java.util.List;

/**
 * Drives the Recommendations page:
 *   REGION -> CATEGORY -> DESTINATION -> RECOMMENDED ACTIVITIES
 * It remembers what the user picked and tells the view what to show.
 */
public class RecommendationController {

    private final RecommendationView view;
    private final DestinationRepository repository = DestinationRepository.getInstance();
    private final Runnable preferenceAction;
    private final Runnable logoutAction;
    private final UserModel currentUser;

    private String selectedRegion = DestinationRepository.REGIONS.get(0);
    private String selectedCategory = DestinationRepository.CATEGORIES.get(0);
    private DestinationModel selectedDestination;

    public RecommendationController(RecommendationView view) {
        this(view, null, null, null);
    }

    public RecommendationController(RecommendationView view, UserModel currentUser,
                                    Runnable preferenceAction, Runnable logoutAction) {
        this.view = view;
        this.currentUser = currentUser;
        this.preferenceAction = preferenceAction;
        this.logoutAction = logoutAction;

        view.addHomeListener(e -> view.dispose());
        view.addPreferenceListener(e -> {
            if (this.preferenceAction != null) {
                view.dispose();
                this.preferenceAction.run();
            } else {
                openPreferences();
            }
        });
        view.setLogoutButtonText(currentUser == null ? "Login" : "Logout");
        view.addLogoutListener(e -> {
            if (this.logoutAction != null) {
                view.dispose();
                this.logoutAction.run();
            } else if (this.currentUser == null) {
                openPreferences();
            } else {
                logout();
            }
        });

        view.addRegionListener(this::handleRegionClick);
        view.addCategoryListener(this::handleCategoryClick);
        view.addDestinationListener(this::handleDestinationClick);
        view.addDetailsListener(d -> view.showDetails(d, selectedCategory));
        view.addBackToCategoriesListener(e -> backToCategories());

        refresh();
    }

    // REGION: keep the category, clear the destination
    private void handleRegionClick(String region) {
        selectedRegion = region;
        selectedDestination = null;
        refresh();
    }

    // CATEGORY: show only destinations of this region + category
    private void handleCategoryClick(String category) {
        selectedCategory = category;
        selectedDestination = null;
        refresh();
    }

    // DESTINATION: show its own recommended activities
    private void handleDestinationClick(DestinationModel destination) {
        selectedDestination = destination;
        refresh();
    }

    private void backToCategories() {
        selectedDestination = null;
        refresh();
        view.scrollToCategories();
    }

    // Push the current selections to the view
    private void refresh() {
        List<DestinationModel> list =
                repository.getByRegionAndCategory(selectedRegion, selectedCategory);

        view.setSelectedRegion(selectedRegion);
        view.setSelectedCategory(selectedCategory);
        view.showDestinations(selectedRegion, selectedCategory, list, selectedDestination);

        if (selectedDestination != null) {
            view.showActivities(selectedDestination, selectedCategory);
        } else {
            view.hideActivities();
        }
    }

    // Same way HomeController opens the preference screen
    private void openPreferences() {
        LoginView loginView = new LoginView();
        LoginController loginController = new LoginController(loginView);
        loginController.showView();
        view.dispose();
    }

    private void logout() {
        view.showMessage("👋 Logged out successfully!");
        view.dispose();
    }
}