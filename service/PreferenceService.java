package service;

import model.DestinationModel;
import model.PreferenceModel;
import model.UserModel;
import repository.DestinationRepository;
import repository.PreferenceRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PreferenceService {

    private static final Pattern PRICE_PATTERN =
            Pattern.compile("\\d[\\d,]*(?:\\.\\d+)?");
    private static final List<String> MONTHS = Arrays.asList(
            "january", "february", "march", "april", "may", "june",
            "july", "august", "september", "october", "november", "december");

    private final PreferenceRepository preferenceRepository;
    private final DestinationRepository destinationRepository;

    public PreferenceService(PreferenceRepository preferenceRepository) {
        this(preferenceRepository, DestinationRepository.getInstance());
    }

    public PreferenceService(PreferenceRepository preferenceRepository,
                            DestinationRepository destinationRepository) {
        if (preferenceRepository == null || destinationRepository == null) {
            throw new IllegalArgumentException("Preference and destination repositories are required");
        }

        this.preferenceRepository = preferenceRepository;
        this.destinationRepository = destinationRepository;
    }

    public void savePreferences(UserModel user, PreferenceModel preferences) {
        requireUser(user);
        validatePreferences(preferences);
        preferenceRepository.savePreferences(user.getUsername(), preferences);
    }

    public Optional<PreferenceModel> loadPreferences(UserModel user) {
        requireUser(user);
        return preferenceRepository.loadPreferences(user.getUsername());
    }

    public List<DestinationModel> getRecommendedDestinations(PreferenceModel preferences) {
        validatePreferences(preferences);

        double budgetCap = budgetCap(preferences.getBudget());
        List<ScoredDestination> scoredDestinations = new ArrayList<>();
        for (DestinationModel destination : destinationRepository.getAll()) {
            double destinationMaximum = maximumBudget(destination);
            if (destinationMaximum > budgetCap) {
                continue;
            }

            double compatibility =
                    (budgetCompatibility(destinationMaximum, budgetCap)
                            + monthCompatibility(preferences.getMonth(), destination.getBestTime())
                            + activityCompatibility(
                                    preferences.getActivityLevel(), destination.getDifficulty())
                            + interestCompatibility(preferences.getInterests(), destination))
                            / 4.0;
            scoredDestinations.add(new ScoredDestination(destination, compatibility));
        }

        scoredDestinations.sort(
                Comparator.comparingDouble(ScoredDestination::getCompatibility).reversed()
                        .thenComparing(
                                Comparator.comparingDouble(
                                        (ScoredDestination scored) -> scored.destination.getScore())
                                        .reversed()));

        List<DestinationModel> results = new ArrayList<>(scoredDestinations.size());
        for (ScoredDestination scored : scoredDestinations) {
            results.add(scored.destination);
        }
        return Collections.unmodifiableList(results);
    }

    private void requireUser(UserModel user) {
        if (user == null) {
            throw new IllegalArgumentException("Signed-in user is required");
        }
    }

    private void validatePreferences(PreferenceModel preferences) {
        if (preferences == null) {
            throw new IllegalArgumentException("Preferences are required");
        }
        if (isBlank(preferences.getBudget())
                || isBlank(preferences.getMonth())
                || isBlank(preferences.getGroupType())
                || isBlank(preferences.getActivityLevel())) {
            throw new IllegalArgumentException("All preference fields are required");
        }
        String[] interests = preferences.getInterests();
        if (interests == null || interests.length == 0
                || Arrays.stream(interests).anyMatch(this::isBlank)) {
            throw new IllegalArgumentException("At least one valid interest is required");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private double budgetCap(String budget) {
        if (budget == null) {
            throw new IllegalArgumentException("Budget is required");
        }
        String normalized = budget.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("low budget")) {
            return 3500.0;
        }
        if (normalized.startsWith("medium budget")) {
            return 8000.0;
        }
        if (normalized.startsWith("high budget")) {
            return Double.POSITIVE_INFINITY;
        }
        throw new IllegalArgumentException("Unsupported budget preference: " + budget);
    }

    private double maximumBudget(DestinationModel destination) {
        String budget = destination.getBudget();
        Matcher matcher = PRICE_PATTERN.matcher(budget == null ? "" : budget);
        double maximum = -1.0;
        while (matcher.find()) {
            maximum = Double.parseDouble(matcher.group().replace(",", ""));
        }
        if (maximum < 0.0) {
            throw new IllegalStateException(
                    "Destination has an unrecognized budget: " + destination.getName());
        }
        return maximum;
    }

    private double budgetCompatibility(double destinationMaximum, double budgetCap) {
        if (Double.isInfinite(budgetCap)) {
            return 1.0;
        }
        return 1.0 - destinationMaximum / budgetCap;
    }

    private double monthCompatibility(String preferredMonth, String bestTime) {
        if (preferredMonth == null || bestTime == null) {
            return 0.0;
        }
        String normalizedBestTime = bestTime.toLowerCase(Locale.ROOT);
        if (normalizedBestTime.contains("year-round")) {
            return 1.0;
        }

        String normalizedMonth = preferredMonth.trim().toLowerCase(Locale.ROOT);
        int monthIndex = MONTHS.indexOf(normalizedMonth);
        if (monthIndex < 0) {
            throw new IllegalArgumentException("Unsupported month preference: " + preferredMonth);
        }
        String abbreviation = normalizedMonth.substring(0, 3);
        for (String token : normalizedBestTime.split("[^a-z]+")) {
            if (token.equals(normalizedMonth) || token.equals(abbreviation)) {
                return 1.0;
            }
        }
        return 0.0;
    }

    private double activityCompatibility(String activityLevel, String difficulty) {
        double preferredDifficulty = difficultyValue(activityLevel, true);
        double destinationDifficulty = difficultyValue(difficulty, false);
        return 1.0 - Math.abs(preferredDifficulty - destinationDifficulty) / 2.0;
    }

    private double difficultyValue(String value, boolean preference) {
        if (value == null) {
            throw new IllegalArgumentException(
                    preference ? "Activity level is required" : "Destination difficulty is required");
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (preference) {
            if (normalized.startsWith("low")) {
                return 0.0;
            }
            if (normalized.startsWith("medium")) {
                return 1.0;
            }
            if (normalized.startsWith("hard")) {
                return 2.0;
            }
            throw new IllegalArgumentException("Unsupported activity level: " + value);
        }
        if (normalized.contains("easy") && normalized.contains("moderate")) {
            return 0.5;
        }
        if (normalized.contains("moderate") && (normalized.contains("hard")
                || normalized.contains("difficult"))) {
            return 1.5;
        }
        if (normalized.contains("easy")) {
            return 0.0;
        }
        if (normalized.contains("moderate")) {
            return 1.0;
        }
        if (normalized.contains("hard") || normalized.contains("difficult")) {
            return 2.0;
        }
        throw new IllegalStateException("Unrecognized destination difficulty: " + value);
    }

    private double interestCompatibility(String[] interests, DestinationModel destination) {
        if (interests == null || interests.length == 0) {
            return 0.0;
        }

        int matched = 0;
        for (String interest : interests) {
            if (interestMatches(interest, destination)) {
                matched++;
            }
        }
        return (double) matched / interests.length;
    }

    private boolean interestMatches(String interest, DestinationModel destination) {
        if (interest == null) {
            return false;
        }
        String normalizedInterest = interest.trim().toLowerCase(Locale.ROOT);
        String activities = String.join(" ", destination.getActivities()).toLowerCase(Locale.ROOT);
        switch (normalizedInterest) {
            case "beach":
                return destination.hasCategory(DestinationRepository.BEACH)
                        || containsAny(activities, "beach", "island", "snorkel", "swimming");
            case "adventure":
                return destination.hasCategory(DestinationRepository.ADVENTURE)
                        || containsAny(activities, "adventure", "hiking", "trek", "surf", "off-road");
            case "nature":
                return destination.hasCategory(DestinationRepository.MOUNTAIN)
                        || containsAny(
                                activities, "nature", "waterfall", "wildlife", "bird", "landscape");
            case "culture":
                return destination.hasCategory(DestinationRepository.CULTURAL)
                        || containsAny(activities, "cultural", "heritage", "museum", "church");
            case "food":
                return containsAny(activities, "food", "restaurant", "culinary", "tasting", "cuisine");
            default:
                return false;
        }
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static final class ScoredDestination {
        private final DestinationModel destination;
        private final double compatibility;

        private ScoredDestination(DestinationModel destination, double compatibility) {
            this.destination = destination;
            this.compatibility = compatibility;
        }

        private double getCompatibility() {
            return compatibility;
        }
    }
}
