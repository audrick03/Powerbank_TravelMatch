package model;

import java.util.Arrays;

/**
 * PreferenceModel
 *
 * Stores the user's travel preferences.
 */
public class PreferenceModel {

    private String budget;
    private String month;
    private String groupType;
    private String activityLevel;
    private String[] interests;

    public PreferenceModel() {
    }

    public PreferenceModel(String budget, String month, String groupType, String activityLevel, String[] interests) {
        if (budget == null || budget.trim().isEmpty()) {
            throw new IllegalArgumentException("Budget cannot be blank");
        }
        if (month == null || month.trim().isEmpty()) {
            throw new IllegalArgumentException("Month cannot be blank");
        }
        if (groupType == null || groupType.trim().isEmpty()) {
            throw new IllegalArgumentException("Group type cannot be blank");
        }
        if (activityLevel == null || activityLevel.trim().isEmpty()) {
            throw new IllegalArgumentException("Activity level cannot be blank");
        }
        if (interests == null || interests.length == 0) {
            throw new IllegalArgumentException("Interests cannot be blank");
        }

        this.budget = budget;
        this.month = month;
        this.groupType = groupType;
        this.activityLevel = activityLevel;
        this.interests = interests.clone();
    }

    // Getters

    public String getBudget() {
        return budget;
    }

    public String getMonth() {
        return month;
    }

    public String getGroupType() {
        return groupType;
    }

    public String getActivityLevel() {
        return activityLevel;
    }

    public String[] getInterests() {
        return interests == null ? null : interests.clone();
    }

    // Setters

    public void setBudget(String budget) {
        this.budget = budget;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public void setGroupType(String groupType) {
        this.groupType = groupType;
    }

    public void setActivityLevel(String activityLevel) {
        this.activityLevel = activityLevel;
    }

    public void setInterests(String[] interests) {
        this.interests = interests == null ? null : interests.clone();
    }

    @Override
    public String toString() {
        return "PreferenceModel{" +
                "budget='" + budget + '\'' +
                ", month='" + month + '\'' +
                ", groupType='" + groupType + '\'' +
                ", activityLevel='" + activityLevel + '\'' +
                ", interests=" + Arrays.toString(interests) +
                '}';
    }
}