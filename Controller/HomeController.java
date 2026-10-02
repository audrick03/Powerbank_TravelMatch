package Controller;
import View.HomeView;
import View.LoginView;

import javax.swing.JOptionPane;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HomeController {

    private static final String BEACH = "Beach";
    private static final String MOUNTAIN = "Mountain";
    private static final String CITY = "City";
    private static final String ADVENTURE = "Adventure";
    private static final String CULTURAL = "Cultural";

    private static final String[] CATEGORIES = {
        BEACH, MOUNTAIN, CITY, ADVENTURE, CULTURAL
    };

    // Destination name -> categories
    private static final Map<String, List<String>> DESTINATIONS =
            new LinkedHashMap<>();

    static {
        add("Batad Rice Terraces", CULTURAL, MOUNTAIN);
        add("Batanes", MOUNTAIN, BEACH);
        add("Biri Island", ADVENTURE, BEACH);
        add("Apo Reef Natural Park", ADVENTURE, BEACH);
        add("Mount Pulag", MOUNTAIN, ADVENTURE);
        add("Sagada", MOUNTAIN, ADVENTURE);
        add("Gigantes Islands", BEACH, ADVENTURE);
        add("Coron", BEACH, ADVENTURE);
        add("Vigan City", CULTURAL, CITY);
        add("Mount Dulang-Dulang", MOUNTAIN, ADVENTURE);
        add("Mount Pinatubo", ADVENTURE, MOUNTAIN);
        add("Siquijor", BEACH, CULTURAL);
        add("Siargao Island", BEACH, ADVENTURE);
        add("El Nido", BEACH);
        add("Cebu City", CITY, CULTURAL);
        add("San Fernando City (La Union)", BEACH, ADVENTURE);
        add("Davao City", CITY, MOUNTAIN);
        add("Intramuros (City of Manila)", CULTURAL);
        add("Binondo (City of Manila)", CULTURAL);
        add("La Mesa Watershed Reserve (Quezon City)", MOUNTAIN);
        add("Poblacion (Makati City)", CITY);
        add("National Museum Complex (City of Manila)", CULTURAL);
        add("Bonifacio Global City (BGC)", CITY);
        add("Las Piñas–Parañaque Critical Habitat", BEACH);
        add("Ninoy Aquino Parks & Wildlife (Quezon City)", ADVENTURE);
        add("Ayala Triangle & Greenbelt (Makati City)", CITY);
    }

    private static void add(String name, String... categories) {
        DESTINATIONS.put(name, Arrays.asList(categories));
    }

    private final HomeView homeView;
    private final LoginView loginView;

    public HomeController(HomeView homeView, LoginView loginView) {
        this.homeView = homeView;
        this.loginView = loginView;

        attachListeners();
        updateCategoryCounts();
    }

    // Connect buttons and category cards to controller methods
    private void attachListeners() {
        homeView.addLoginListener(e -> showLogin());
        homeView.addHomeListener(e -> showHome());
        homeView.addSearchListener(e -> handleSearch());

        // Category card click listener
        homeView.addCategoryListener(this::handleCategoryClick);
    }

    // Show the home page
    public void start() {
        showHome();
    }

    private void showHome() {
        if (loginView != null) {
            loginView.setVisible(false);
        }

        homeView.setVisible(true);
        homeView.toFront();
    }

    // Show the login page
    private void showLogin() {
        homeView.setVisible(false);

        if (loginView != null) {
            loginView.setVisible(true);
            loginView.toFront();
        }
    }

    // Update category counts on the home page
    private void updateCategoryCounts() {
        for (String category : CATEGORIES) {
            homeView.setCategoryCount(
                    category,
                    getByCategory(category).size()
            );
        }
    }

    // Get all destinations belonging to a category
    private List<String> getByCategory(String category) {
        return DESTINATIONS.entrySet()
                .stream()
                .filter(entry ->
                        entry.getValue().contains(category))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // Search by destination name or category
    private List<String> searchDestinations(String query) {
        String q = query.trim().toLowerCase();

        return DESTINATIONS.entrySet()
                .stream()
                .filter(entry ->
                        entry.getKey().toLowerCase().contains(q)
                        || entry.getValue()
                                .stream()
                                .anyMatch(category ->
                                        category.equalsIgnoreCase(q)))
                .map(entry ->
                        entry.getKey() + " ("
                        + String.join(" / ", entry.getValue())
                        + ")")
                .collect(Collectors.toList());
    }

    // Handle category card clicks
    private void handleCategoryClick(String category) {
        List<String> destinations = getByCategory(category);

        homeView.showCategoryDestinations(
                category,
                destinations
        );
    }

    // Handle search button
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

        List<String> results = searchDestinations(query);

        if (results.isEmpty()) {
            homeView.showSearchResults(query, results);
            JOptionPane.showMessageDialog(
                    homeView,
                    "No destinations found for: " + query,
                    "No Results",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        // Display search results in the UI
        homeView.showSearchResults(query, results);
    }
}