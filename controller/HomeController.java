package controller;

import model.*;
import repository.*;
import service.*;
import view.*;

import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import java.util.List;
import java.util.Optional;


/**
 * Home flow:
 *   REGION -> CATEGORY -> DESTINATION -> PLACES / DETAILS
 * The controller remembers what the user selected, so each step only shows
 * data for that region + category + destination.
 */
public class HomeController {

    private enum Level { HOME, CATEGORIES, LIST, DETAIL, REVIEW }

    private final DestinationRepository repository = DestinationRepository.getInstance();

    private final HomeView homeView;
    private final ExplorePanel explore;
    private LoginView loginView;
    private final ReviewView reviewView = new ReviewView();
    private ReviewController reviewController;
    private LoginController loginController;
    private UserModel currentUser;
    private boolean openPreferencesAfterLogin;
    private boolean openRecommendationsAfterLogin;

    // Current selections
    private Level level = Level.HOME;
    private String selectedRegion;
    private String selectedCategory;   // null when the list came from a search
    private boolean fromSearch;
    private DestinationModel selectedDestination;   // the destination being viewed (for reviews)
    private Runnable restoreList = () -> {};

    public HomeController(HomeView homeView, LoginView loginView) {
        this(homeView, loginView, null);
    }

    public HomeController(HomeView homeView, LoginView loginView, UserModel currentUser) {
        this.homeView = homeView;
        this.loginView = loginView;
        this.currentUser = currentUser;
        homeView.setLoginButtonText(currentUser == null ? "Login" : "Logout");
        this.explore = homeView.getExplorePanel();
        if (loginView != null) {
            createLoginController();
        }

        reviewController = new ReviewController(reviewView);

        attachListeners();
        updateRegionCounts();
    }

    // Connect buttons and cards to controller methods
    private void attachListeners() {
        homeView.addLoginListener(e -> showLogin());
        homeView.addHomeListener(e -> showHome());
        homeView.addSearchListener(e -> handleSearch());
        homeView.addPreferenceListener(e -> showPreferences());
        homeView.addRecommendationListener(e -> showRecommendations());

        homeView.addRegionListener(this::handleRegionClick);
        explore.addCategoryListener(this::handleCategoryClick);
        explore.addDestinationListener(this::handleDestinationClick);
        explore.addBackListener(e -> goBack());
        explore.addReviewListener(e -> showReviews());
    }

    // Show the home page
    public void start() {
        showHome();
    }

    // =========================
    // NAVIGATION
    // =========================

    // HOME button: back to the home page (hero + regions)
    private void showHome() {
        if (loginView != null) {
            loginView.setVisible(false);
        }

        level = Level.HOME;
        selectedRegion = null;
        selectedCategory = null;
        selectedDestination = null;
        fromSearch = false;

        updateRegionCounts();
        homeView.showHomePage();
        homeView.setVisible(true);
        homeView.toFront();
    }

    // Back button inside the explore pages: one step up the flow
    private void goBack() {
        switch (level) {
            case REVIEW:
                handleDestinationClick(selectedDestination);
                break;
            case DETAIL:
                restoreList.run();
                break;
            case LIST:
                if (fromSearch || selectedRegion == null) {
                    showHome();
                } else {
                    handleRegionClick(selectedRegion);
                }
                break;
            default:
                showHome();
                break;
        }
    }

    // REGION -> CATEGORY
    private void handleRegionClick(String region) {
        selectedRegion = region;
        selectedCategory = null;
        fromSearch = false;
        level = Level.CATEGORIES;

        explore.showCategories(region, repository.countByCategory(region));
        homeView.showExplorePage();
    }

    // CATEGORY -> DESTINATION LIST (only this region + category)
    private void handleCategoryClick(String category) {
        selectedCategory = category;
        showCategoryList();
    }

    private void showCategoryList() {
        List<DestinationModel> list =
                repository.getByRegionAndCategory(selectedRegion, selectedCategory);

        explore.showDestinations(
                DestinationRepository.label(selectedCategory) + " in " + selectedRegion,
                list.size() + (list.size() == 1 ? " destination" : " destinations"),
                selectedRegion + "  ›  " + DestinationRepository.label(selectedCategory),
                list,
                selectedCategory
        );

        level = Level.LIST;
        fromSearch = false;
        restoreList = this::showCategoryList;
        homeView.showExplorePage();
    }

    // DESTINATION -> activities, places, details
    private void handleDestinationClick(DestinationModel destination) {
        selectedDestination = destination;
        level = Level.DETAIL;
        explore.showDetail(destination, selectedCategory);
        homeView.showExplorePage();
    }

    // =========================
    // SEARCH
    // =========================

    private void handleSearch() {
        String query = homeView.getSearchText();

        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(
                    homeView,
                    "Please enter a destination or preference.",
                    "Search",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        showSearchResults(query);

        if (repository.search(query).isEmpty()) {
            JOptionPane.showMessageDialog(
                    homeView,
                    "No destinations found for: " + query,
                    "No Results",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    private void showSearchResults(String query) {
        List<DestinationModel> results = repository.search(query);

        selectedCategory = null;
        fromSearch = true;
        level = Level.LIST;
        restoreList = () -> showSearchResults(query);

        explore.showDestinations(
                "Search Results for \"" + query + "\"",
                results.size() + " found",
                "Search",
                results,
                null
        );
        homeView.showExplorePage();
    }

    // =========================
    // OTHER SCREENS
    // =========================

    // Show the login page
    private void showLogin() {
        if (currentUser != null) {
            logout();
            return;
        }
        if (loginView == null) {
            loginView = new LoginView();
            createLoginController();
        }
        loginController.showView();
        homeView.setVisible(false);
    }

    private void createLoginController() {
        loginController = new LoginController(loginView, this::handleLoginSuccess);
    }

    private void handleLoginSuccess(UserModel user) {
        currentUser = user;
        homeView.setLoginButtonText("Logout");
        showHome();

        if (openRecommendationsAfterLogin) {
            openRecommendationsAfterLogin = false;
            showRecommendations();
            return;
        }

        if (openPreferencesAfterLogin) {
            openPreferencesAfterLogin = false;
            confirmPreferenceChange();
        }
    }

    private void logout() {
        currentUser = null;
        openPreferencesAfterLogin = false;
        openRecommendationsAfterLogin = false;
        homeView.setLoginButtonText("Login");
        showHome();
    }

    private void showPreferences() {
        if (currentUser == null) {
            openPreferencesAfterLogin = true;
            openRecommendationsAfterLogin = false;
            showLogin();
            return;
        }
        confirmPreferenceChange();
    }

    private void confirmPreferenceChange() {
        int choice = JOptionPane.showConfirmDialog(
                homeView,
                "Do you want to change your saved travel preferences?",
                "Change Preferences",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            openPreferenceView();
        }
    }

    private void openPreferenceView() {
        PreferenceView preferenceView = new PreferenceView();
        PreferenceModel preferenceModel = new PreferenceModel();
        PreferenceController preferenceController = new PreferenceController(
                preferenceView,
                preferenceModel,
                currentUser,
                new PreferenceService(
                        new PreferenceRepository(),
                        repository),
                this::showHome);
        homeView.setVisible(false);
        preferenceController.showView();
    }

    // Recommendations are personalized and require saved preferences.
    private void showRecommendations() {
        if (currentUser == null) {
            openRecommendationsAfterLogin = true;
            openPreferencesAfterLogin = false;
            showLogin();
            return;
        }

        PreferenceService preferenceService = new PreferenceService(
                new PreferenceRepository(), repository);
        Optional<PreferenceModel> savedPreferences;
        try {
            savedPreferences = preferenceService.loadPreferences(currentUser);
        } catch (IllegalStateException exception) {
            JOptionPane.showMessageDialog(
                    homeView,
                    "Could not load your saved preferences: " + exception.getMessage(),
                    "Recommendations",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!savedPreferences.isPresent()) {
            JOptionPane.showMessageDialog(
                    homeView,
                    "Please complete your travel preferences to see personalized recommendations.",
                    "Travel Preferences Required",
                    JOptionPane.INFORMATION_MESSAGE);
            openPreferenceView();
            return;
        }

        List<DestinationRecommendation> recommendations;
        try {
            recommendations = preferenceService.getRankedRecommendations(
                    savedPreferences.get());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            JOptionPane.showMessageDialog(
                    homeView,
                    "Could not find destinations: " + exception.getMessage(),
                    "Recommendations",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        RecommendationView view = new RecommendationView();
        view.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        new RecommendationController(view, recommendations, this::showHome, currentUser);
        homeView.setVisible(false);
        view.setVisible(true);
    }

    // Review button (destination header): reviews of the destination being viewed,
    // shown directly below its header
    private void showReviews() {
        if (selectedDestination == null) {
            return;
        }
        level = Level.REVIEW;
        reviewController.reload();
        reviewView.setDestination(selectedDestination.getName());
        reviewView.setAuthenticated(currentUser != null);
        explore.showReviews(selectedDestination, selectedCategory, reviewView);
        homeView.showExplorePage();
    }

    // Region cards show how many destinations they contain
    private void updateRegionCounts() {
        for (String region : DestinationRepository.REGIONS) {
            homeView.setRegionCount(region, repository.getByRegion(region).size());
        }
    }
}