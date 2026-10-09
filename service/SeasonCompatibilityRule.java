package service;

import model.DestinationModel;
import model.PreferenceModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SeasonCompatibilityRule implements CompatibilityRule {

    private static final String[] MONTH_NAMES = {
            "january", "february", "march", "april", "may", "june",
            "july", "august", "september", "october", "november", "december"
    };
    private static final Pattern MONTH_PATTERN = Pattern.compile(
            "\\b(january|jan|february|feb|march|mar|april|apr|may|june|jun|"
                    + "july|jul|august|aug|september|sep|sept|october|oct|"
                    + "november|nov|december|dec)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern RANGE_SEPARATOR =
            Pattern.compile("\\s*(?:-|\\u2013|\\u2014|to)\\s*", Pattern.CASE_INSENSITIVE);

    @Override
    public String getName() {
        return "Travel season";
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
        String bestTime = destination.getBestTime();
        if (bestTime == null) {
            return 0.0;
        }
        String normalizedBestTime = bestTime.toLowerCase(Locale.ROOT);
        if (normalizedBestTime.contains("year-round")) {
            return 1.0;
        }

        String preferredMonth = preferences.getMonth();
        if (preferredMonth == null) {
            return 0.0;
        }
        int month = parseMonth(preferredMonth);
        List<Integer> recommendedMonths = parseRecommendedMonths(normalizedBestTime);
        if (recommendedMonths.isEmpty()) {
            throw new IllegalStateException(
                    "Destination has an unrecognized best travel time: " + bestTime);
        }
        if (recommendedMonths.contains(month)) {
            return 1.0;
        }
        int nearestMonthDistance = recommendedMonths.stream()
                .mapToInt(recommendedMonth -> {
                    int difference = Math.abs(month - recommendedMonth);
                    return Math.min(difference, 12 - difference);
                })
                .min()
                .orElse(12);
        return nearestMonthDistance == 1 ? 0.5 : 0.0;
    }

    private int parseMonth(String monthText) {
        String normalized = monthText.trim().toLowerCase(Locale.ROOT);
        for (int index = 0; index < MONTH_NAMES.length; index++) {
            String name = MONTH_NAMES[index];
            if (normalized.equals(name) || normalized.equals(name.substring(0, 3))) {
                return index;
            }
        }
        throw new IllegalArgumentException("Unsupported month preference: " + monthText);
    }

    private List<Integer> parseRecommendedMonths(String bestTime) {
        List<Integer> monthIndexes = new ArrayList<>();
        Matcher matcher = MONTH_PATTERN.matcher(bestTime);
        int previousEnd = 0;
        int previousMonth = -1;
        while (matcher.find()) {
            int currentMonth = parseMonth(matcher.group());
            String separator = bestTime.substring(previousEnd, matcher.start());
            boolean range = previousMonth >= 0 && RANGE_SEPARATOR.matcher(separator).matches();
            if (range) {
                int month = previousMonth;
                while (month != currentMonth) {
                    month = (month + 1) % 12;
                    if (!monthIndexes.contains(month)) {
                        monthIndexes.add(month);
                    }
                }
            } else if (!monthIndexes.contains(currentMonth)) {
                monthIndexes.add(currentMonth);
            }
            previousMonth = currentMonth;
            previousEnd = matcher.end();
        }
        return monthIndexes;
    }
}
