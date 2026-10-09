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
import java.util.Optional;

public class PreferenceService {

    public static final int MAX_RECOMMENDATIONS = 10;

    private static final List<CompatibilityRule> DEFAULT_RULES = List.of(
            new BudgetCompatibilityRule(),
            new SeasonCompatibilityRule(),
            new ActivityCompatibilityRule(),
            new InterestCompatibilityRule());

    private final PreferenceRepository preferenceRepository;
    private final DestinationRepository destinationRepository;
    private final List<CompatibilityRule> compatibilityRules;

    public PreferenceService(PreferenceRepository preferenceRepository) {
        this(preferenceRepository, DestinationRepository.getInstance(), DEFAULT_RULES);
    }

    public PreferenceService(PreferenceRepository preferenceRepository,
                             DestinationRepository destinationRepository) {
        this(preferenceRepository, destinationRepository, DEFAULT_RULES);
    }

    public PreferenceService(PreferenceRepository preferenceRepository,
                             DestinationRepository destinationRepository,
                             List<CompatibilityRule> compatibilityRules) {
        if (preferenceRepository == null || destinationRepository == null
                || compatibilityRules == null || compatibilityRules.isEmpty()
                || compatibilityRules.stream().anyMatch(rule -> rule == null)) {
            throw new IllegalArgumentException(
                    "Repositories and at least one compatibility rule are required");
        }

        this.preferenceRepository = preferenceRepository;
        this.destinationRepository = destinationRepository;
        this.compatibilityRules = Collections.unmodifiableList(
                new ArrayList<>(compatibilityRules));
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
        List<DestinationModel> destinations = new ArrayList<>();
        for (DestinationRecommendation recommendation : getRankedRecommendations(preferences)) {
            destinations.add(recommendation.getDestination());
        }
        return Collections.unmodifiableList(destinations);
    }

    public List<DestinationRecommendation> getRankedRecommendations(PreferenceModel preferences) {
        validatePreferences(preferences);

        List<DestinationRecommendation> scoredDestinations = new ArrayList<>();
        double totalWeight = compatibilityRules.stream()
                .mapToDouble(CompatibilityRule::getWeight)
                .sum();
        if (!Double.isFinite(totalWeight) || totalWeight <= 0.0
                || compatibilityRules.stream().anyMatch(rule ->
                        !Double.isFinite(rule.getWeight()) || rule.getWeight() <= 0.0)) {
            throw new IllegalStateException("Compatibility rule weights must be positive and finite");
        }

        for (DestinationModel destination : destinationRepository.getAll()) {
            boolean eligible = true;
            double weightedScore = 0.0;
            java.util.Map<String, Double> componentScores = new java.util.LinkedHashMap<>();
            for (CompatibilityRule rule : compatibilityRules) {
                if (!rule.isEligible(preferences, destination)) {
                    eligible = false;
                    break;
                }
                double score = rule.score(preferences, destination);
                if (!Double.isFinite(score) || score < 0.0 || score > 1.0) {
                    throw new IllegalStateException(
                            "Compatibility rule scores must be between 0.0 and 1.0");
                }
                String ruleName = rule.getName();
                if (ruleName == null || ruleName.trim().isEmpty()
                        || componentScores.containsKey(ruleName)) {
                    throw new IllegalStateException(
                            "Compatibility rule names must be non-empty and unique");
                }
                componentScores.put(ruleName, score);
                weightedScore += score * rule.getWeight();
            }
            if (eligible) {
                scoredDestinations.add(new DestinationRecommendation(
                        destination, weightedScore / totalWeight, componentScores));
            }
        }

        scoredDestinations.sort(
                Comparator.comparingDouble(DestinationRecommendation::getCompatibilityScore).reversed()
                        .thenComparing(
                                Comparator.comparingDouble(
                                        (DestinationRecommendation scored) ->
                                                scored.getDestination().getScore())
                                        .reversed()));

        if (scoredDestinations.size() > MAX_RECOMMENDATIONS) {
            scoredDestinations = new ArrayList<>(
                    scoredDestinations.subList(0, MAX_RECOMMENDATIONS));
        }
        return Collections.unmodifiableList(scoredDestinations);
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
}
