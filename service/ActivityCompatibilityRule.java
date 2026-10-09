package service;

import model.DestinationModel;
import model.PreferenceModel;

import java.util.Locale;

public class ActivityCompatibilityRule implements CompatibilityRule {

    @Override
    public String getName() {
        return "Activity level";
    }

    @Override
    public double getWeight() {
        return 0.25;
    }

    @Override
    public boolean isEligible(PreferenceModel preferences, DestinationModel destination) {
        return true;
    }

    @Override
    public double score(PreferenceModel preferences, DestinationModel destination) {
        double preferredDifficulty = difficultyValue(preferences.getActivityLevel(), true);
        double destinationDifficulty = difficultyValue(destination.getDifficulty(), false);
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
        if (normalized.contains("moderate") && (normalized.contains("hard") || normalized.contains("difficult"))) {
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
}
