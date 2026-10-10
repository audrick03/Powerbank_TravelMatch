package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One destination in TravelMatch.
 * Region + category + destination decide which activities are shown,
 * so every destination carries its OWN activity list (never shared).
 */
public class DestinationModel {
    private static final int MAX_TEXT_LENGTH = 255;

    private String name;
    private String province;
    private String region;                    // Luzon / Visayas / Mindanao
    private final List<String> categories = new ArrayList<>();
    private String description;
    private double score;                     // TravelMatch score (0-5)
    private String bestTime;
    private String duration;
    private String difficulty;
    private String budget;
    private final List<String> travelTips = new ArrayList<>();
    private final List<String> whatToBring = new ArrayList<>();
    private final List<String> activities = new ArrayList<>();
    private final List<String> places = new ArrayList<>();   // "Name | short description"
    // Optional per-category activity lists (e.g. Davao City is both City and Cultural)
    private final Map<String, List<String>> categoryActivities = new LinkedHashMap<>();

    public DestinationModel(String name, String province, String region,
                       List<String> categories, String description) {
        this.name = name;
        this.province = province;
        this.region = region;
        this.categories.addAll(categories);
        this.description = description;
    }

    // ---------- getters ----------
    public String getName()        { return name; }
    public String getProvince()    { return province; }
    public String getRegion()      { return region; }
    public String getDescription() { return description; }
    public double getScore()       { return score; }
    public String getBestTime()    { return bestTime; }
    public String getDuration()    { return duration; }
    public String getDifficulty()  { return difficulty; }
    public String getBudget()      { return budget; }
    public List<String> getTravelTips() { return Collections.unmodifiableList(travelTips); }
    public List<String> getWhatToBring() { return Collections.unmodifiableList(whatToBring); }

    public List<String> getCategories() { return Collections.unmodifiableList(categories); }
    public List<String> getActivities() { return Collections.unmodifiableList(activities); }
    /** Activities for the category the user picked (falls back to the default list). */
    public List<String> getActivities(String category) {
        List<String> specific = category == null ? null : categoryActivities.get(category);
        return Collections.unmodifiableList(specific != null ? specific : activities);
    }

    public List<String> getPlaces()     { return Collections.unmodifiableList(places); }

    public boolean hasCategory(String category) {
        return categories.contains(category);
    }

    // ---------- setters (used by admin edit / repository) ----------
    public void setName(String name)               { this.name = name; }
    public void setProvince(String province)       { this.province = province; }
    public void setRegion(String region)           { this.region = region; }
    public void setDescription(String description) { this.description = description; }
    public void setScore(double score)             { this.score = score; }

    public void setTravelInfo(String bestTime, String duration,
                              String difficulty, String budget) {
        String validatedBestTime = requireText("Best time", bestTime);
        String validatedDuration = requireText("Duration", duration);
        String validatedDifficulty = requireText("Difficulty", difficulty);
        String validatedBudget = requireText("Budget", budget);
        this.bestTime = validatedBestTime;
        this.duration = validatedDuration;
        this.difficulty = validatedDifficulty;
        this.budget = validatedBudget;
    }

    public void setTravelTips(List<String> tips) {
        List<String> validatedTips = requireTextList("Travel tips", tips);
        travelTips.clear();
        travelTips.addAll(validatedTips);
    }

    public void setWhatToBring(List<String> items) {
        List<String> validatedItems = requireTextList("What to bring", items);
        whatToBring.clear();
        whatToBring.addAll(validatedItems);
    }

    public void setCategories(List<String> list) {
        List<String> validatedCategories = requireTextList("Categories", list);
        categories.clear();
        categories.addAll(validatedCategories);
    }

    public void setActivities(List<String> list) {
        List<String> validatedActivities = requireTextList("Activities", list);
        activities.clear();
        activities.addAll(validatedActivities);
    }

    public void setActivities(String category, List<String> list) {
        String validatedCategory = requireText("Activity category", category);
        List<String> validatedActivities = requireTextList("Activities", list);
        categoryActivities.put(validatedCategory, validatedActivities);
    }

    public void setPlaces(List<String> list) {
        List<String> validatedPlaces = requireTextList("Places", list);
        places.clear();
        places.addAll(validatedPlaces);
    }

    private static String requireText(String field, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        if (value.codePointCount(0, value.length()) > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException(field + " cannot exceed "
                    + MAX_TEXT_LENGTH + " characters");
        }
        return value;
    }

    private static List<String> requireTextList(String field, List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be empty");
        }

        List<String> validated = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            validated.add(requireText(field + " item " + (index + 1), values.get(index)));
        }
        return validated;
    }

    @Override
    public String toString() {
        return name + " — " + province;
    }
}