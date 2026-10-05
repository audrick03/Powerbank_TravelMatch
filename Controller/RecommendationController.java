package Controller;

import Model.Destination;
import Model.PreferenceModel;
import Repository.DestinationRepository;
import View.PreferenceView;
import View.RecommendationView;

import java.util.List;

/**
 * Drives the Recommendations page:
 *   REGION -> CATEGORY -> DESTINATION -> RECOMMENDED ACTIVITIES
 * It remembers what the user picked and tells the view what to show.
 */
public class RecommendationController {

    private final RecommendationView view;
    private final DestinationRepository repository = DestinationRepository.getInstance();

    private String selectedRegion = DestinationRepository.REGIONS.get(0);
    private String selectedCategory = DestinationRepository.CATEGORIES.get(0);
    private Destination selectedDestination;

    public RecommendationController(RecommendationView view) {
        this.view = view;

        view.addHomeListener(e -> view.dispose());
        view.addPreferenceListener(e -> openPreferences());
        view.addLogoutListener(e -> logout());

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
    private void handleDestinationClick(Destination destination) {
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
        List<Destination> list =
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
        PreferenceView preferenceView = new PreferenceView();
        PreferenceController preferenceController = new PreferenceController(preferenceView, null);

        preferenceController.showView();
        preferenceView.setVisible(true);

        PreferenceModel preferenceModel = new PreferenceModel();
        preferenceController = new PreferenceController(preferenceView, preferenceModel);

        view.dispose();
    }

    private void logout() {
        view.showMessage("👋 Logged out successfully!");
        view.dispose();
    }
}