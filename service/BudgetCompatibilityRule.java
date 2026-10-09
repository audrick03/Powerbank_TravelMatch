package service;

import model.DestinationModel;
import model.PreferenceModel;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BudgetCompatibilityRule implements CompatibilityRule {

    private static final Pattern PRICE_PATTERN =
            Pattern.compile("\\d[\\d,]*(?:\\.\\d+)?");

    @Override
    public String getName() {
        return "Budget fit";
    }

    @Override
    public double getWeight() {
        return 0.15;
    }

    @Override
    public boolean isEligible(PreferenceModel preferences, DestinationModel destination) {
        return maximumBudget(destination) <= budgetCap(preferences.getBudget());
    }

    @Override
    public double score(PreferenceModel preferences, DestinationModel destination) {
        double cap = budgetCap(preferences.getBudget());
        if (Double.isInfinite(cap)) {
            return 1.0;
        }
        return 1.0 - maximumBudget(destination) / cap;
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
}
