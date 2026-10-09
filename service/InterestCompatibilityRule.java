package service;

import model.DestinationModel;
import model.PreferenceModel;
import repository.DestinationRepository;

import java.util.Locale;

public class InterestCompatibilityRule implements CompatibilityRule {

    @Override
    public String getName() {
        return "Interests";
    }

    @Override
    public double getWeight() {
        return 0.35;
    }

    @Override
    public boolean isEligible(PreferenceModel preferences, DestinationModel destination) {
        return true;
    }

    @Override
    public double score(PreferenceModel preferences, DestinationModel destination) {
        String[] interests = preferences.getInterests();
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
}
