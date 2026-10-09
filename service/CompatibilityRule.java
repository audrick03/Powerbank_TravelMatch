package service;

import model.DestinationModel;
import model.PreferenceModel;

/**
 * A pluggable recommendation policy that can filter and score a destination.
 */
public interface CompatibilityRule {

    default String getName() {
        String simpleName = getClass().getSimpleName();
        return simpleName.isEmpty() ? getClass().getName() : simpleName;
    }

    default double getWeight() {
        return 1.0;
    }

    boolean isEligible(PreferenceModel preferences, DestinationModel destination);

    double score(PreferenceModel preferences, DestinationModel destination);
}
