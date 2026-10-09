package service;

import model.DestinationModel;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DestinationRecommendation {

    private final DestinationModel destination;
    private final double compatibilityScore;
    private final Map<String, Double> componentScores;

    public DestinationRecommendation(DestinationModel destination,
                                     double compatibilityScore,
                                     Map<String, Double> componentScores) {
        if (destination == null || !Double.isFinite(compatibilityScore)
                || compatibilityScore < 0.0 || compatibilityScore > 1.0
                || componentScores == null || componentScores.isEmpty()) {
            throw new IllegalArgumentException(
                    "Destination, normalized compatibility, and component scores are required");
        }
        this.destination = destination;
        this.compatibilityScore = compatibilityScore;
        Map<String, Double> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Double> component : componentScores.entrySet()) {
            String name = component.getKey();
            Double score = component.getValue();
            if (name == null || name.trim().isEmpty() || score == null
                    || !Double.isFinite(score) || score < 0.0 || score > 1.0) {
                throw new IllegalArgumentException("Each compatibility component must be normalized");
            }
            copy.put(name, score);
        }
        this.componentScores = Collections.unmodifiableMap(copy);
    }

    public DestinationModel getDestination() {
        return destination;
    }

    public double getCompatibilityScore() {
        return compatibilityScore;
    }

    public int getMatchPercentage() {
        return (int) Math.round(compatibilityScore * 100.0);
    }

    public Map<String, Double> getComponentScores() {
        return componentScores;
    }
}
