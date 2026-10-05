package controller;

import model.*;
import view.*;

import java.util.Arrays;

public class PreferenceController {

    private final PreferenceView view;
    private final PreferenceModel model;

    public PreferenceController(PreferenceView view, PreferenceModel model) {
        if (view == null || model == null) {
            throw new IllegalArgumentException("Preference view and model are required");
        }

        this.view = view;
        this.model = model;

        initializeListeners();
    }

    private void initializeListeners() {
        view.addRecommendListener(e -> handleRecommend());
        view.addResetListener(e -> handleReset());
        view.addForwardListener(e -> handleForward());
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
        } else if (budget.equals("--- Select ---") || month.equals("--- Select ---") || groupType.equals("--- Select ---")) {
            view.showError("Please fill in all fields.");
            return;
        }

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

    private void backToHome() {
        HomeView homeView = new HomeView();
        HomeController homeController = new HomeController(homeView, null);
        homeController.start();
        view.dispose();
    }

    private void handleForward() {
        if (model.getBudget() == null
                || model.getMonth() == null
                || model.getGroupType() == null
                || model.getInterests() == null
                || model.getInterests().length == 0) {
            view.showError("Please fill up and save the form before proceeding.");
            return;
        } else {
            String summary =
                    "Proceeding with the following preferences:\n\n" +
                    "Budget: " + model.getBudget() + "\n" +
                    "Month: " + model.getMonth() + "\n" +
                    "Group Type: " + model.getGroupType() + "\n" +
                    "Interests: " + Arrays.toString(model.getInterests());

            view.showMessage(summary);

            backToHome();

        }
    }

    public void showView() {
        view.setVisible(true);
    }
}