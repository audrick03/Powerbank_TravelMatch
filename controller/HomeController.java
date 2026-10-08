package controller;

import model.*;
import repository.*;
import service.*;
import view.*;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.util.List;

/**
 * Home flow:
 *   REGION -> CATEGORY -> DESTINATION -> RECOMMENDED ACTIVITIES / PLACES / DETAILS
 * The controller remembers what the user selected, so each step only shows
 * data for that region + category + destination.
 */
public class HomeController {

    private enum Level { HOME, CATEGORIES, LIST, DETAIL }

    private final DestinationRepository repository = DestinationRepository.getInstance();

    private final HomeView homeView;
    private final ExplorePanel explore;
    private LoginView loginView;
    private LoginController loginController;
    private UserModel currentUser;
    private boolean openPreferencesAfterLogin;

    // Current selections
    private Level level = Level.HOME;
    private String selectedRegion;
    private String selectedCategory;   // null when the list came from a search
    private boolean fromSearch;
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
        fromSearch = false;

        updateRegionCounts();
        homeView.showHomePage();
        homeView.setVisible(true);
        homeView.toFront();
    }

    // Back button inside the explore pages: one step up the flow
    private void goBack() {
        switch (level) {
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

        if (openPreferencesAfterLogin) {
            openPreferencesAfterLogin = false;
            confirmPreferenceChange();
        }
    }

    private void logout() {
        currentUser = null;
        openPreferencesAfterLogin = false;
        homeView.setLoginButtonText("Login");
        showHome();
    }

    private void showPreferences() {
        if (currentUser == null) {
            openPreferencesAfterLogin = true;
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
                        repository));
        homeView.setVisible(false);
        preferenceController.showView();
    }

    // Recommendations button: flat list window (reads from the repository)
    private void showRecommendations() {
        RecommendationView view = new RecommendationView();
        view.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        new RecommendationController(
                view,
                currentUser,
                this::showPreferences,
                this::logout);
        view.setVisible(true);
    }

    // Region cards show how many destinations they contain
    private void updateRegionCounts() {
        for (String region : DestinationRepository.REGIONS) {
            homeView.setRegionCount(region, repository.getByRegion(region).size());
        }
    }
}