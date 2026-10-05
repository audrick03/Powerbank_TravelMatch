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
    private String[] interests;

    public PreferenceModel() {
    }

    public PreferenceModel(String budget, String month, String groupType, String[] interests) {

        this.budget = budget;
        this.month = month;
        this.groupType = groupType;
        this.interests = interests;
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

    public String[] getInterests() {
        return interests;
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

    public void setInterests(String[] interests) {
        this.interests = interests;
    }

    @Override
    public String toString() {
        return "PreferenceModel{" +
                "budget='" + budget + '\'' +
                ", month='" + month + '\'' +
                ", groupType='" + groupType + '\'' +
                ", interests=" + Arrays.toString(interests) +
                '}';
    }
}