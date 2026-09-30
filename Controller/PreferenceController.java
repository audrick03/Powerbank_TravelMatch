package Controller;

import Model.PreferenceModel;
import View.PreferenceView;

import java.util.Arrays;

public class PreferenceController {

    private final PreferenceView view;
    private final PreferenceModel model;

    public PreferenceController(PreferenceView view, PreferenceModel model) {
        this.view = view;
        this.model = model;

        initializeListeners();
    }

    private void initializeListeners() {
        view.addRecommendListener(e -> handleRecommend());
        view.addResetListener(e -> handleReset());
    }

    private void handleRecommend() {

        view.clearMessage();

        String budget = view.getBudget();
        String month = view.getMonth();
        String groupType = view.getGroupType();
        String[] interests = view.getSelectedInterests();

        // Validate interests
        if (interests.length == 0) {
            view.showError("Please select at least one interest.");
            return;
        }

        // Save preferences
        model.setBudget(budget);
        model.setMonth(month);
        model.setGroupType(groupType);
        model.setInterests(interests);

        String summary =
                "Travel Preferences Saved!\n\n" +
                "Budget: " + model.getBudget() + "\n" +
                "Month: " + model.getMonth() + "\n" +
                "Group Type: " + model.getGroupType() + "\n" +
                "Interests: " + Arrays.toString(model.getInterests());

        view.showMessage(summary);
    }

    private void handleReset() {

        view.clearForm();

        model.setBudget("");
        model.setMonth("");
        model.setGroupType("");
        model.setInterests(new String[0]);

        view.showMessage("Preferences cleared.");
    }
}