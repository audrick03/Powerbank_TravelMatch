package controller;

import model.PreferenceModel;
import model.UserModel;
import service.PreferenceService;
import view.HomeView;
import view.PreferenceView;

import java.util.Arrays;
import java.util.Optional;

public class PreferenceController {

    private final PreferenceView view;
    private final PreferenceModel model;
    private final UserModel user;
    private final PreferenceService preferenceService;

    public PreferenceController(PreferenceView view, PreferenceModel model, UserModel user,
                                PreferenceService preferenceService) {
        if (view == null || model == null || user == null || preferenceService == null) {
            throw new IllegalArgumentException(
                    "Preference view, model, signed-in user, and service are required");
        }

        this.view = view;
        this.model = model;
        this.user = user;
        this.preferenceService = preferenceService;

        loadSavedPreferences();
        initializeListeners();
    }

    private void initializeListeners() {
        view.addRecommendListener(e -> handleRecommend());
        view.addResetListener(e -> handleReset());
        view.addForwardListener(e -> handleForward());
    }

    private void loadSavedPreferences() {
        try {
            Optional<PreferenceModel> savedPreferences =
                    preferenceService.loadPreferences(user);
            if (savedPreferences.isPresent()) {
                copyPreferences(savedPreferences.get());
                view.setPreferences(model);
            }
        } catch (IllegalStateException exception) {
            view.showError("Could not load your saved preferences: " + exception.getMessage());
        }
    }

    private void handleRecommend() {
        view.clearMessage();

        String budget = view.getBudget();
        String month = view.getMonth();
        String groupType = view.getGroupType();
        String activityLevel = view.getActivityLevel();
        String[] interests = view.getSelectedInterests();

        if (interests.length == 0) {
            view.showError("Please select at least one interest.");
            return;
        }
        if ("--- Select ---".equals(budget)
                || "--- Select ---".equals(month)
                || "--- Select ---".equals(groupType)
                || "--- Select ---".equals(activityLevel)) {
            view.showError("Please fill in all fields.");
            return;
        }

        model.setBudget(budget);
        model.setMonth(month);
        model.setGroupType(groupType);
        model.setActivityLevel(activityLevel);
        model.setInterests(interests);

        try {
            preferenceService.savePreferences(user, model);
        } catch (IllegalStateException exception) {
            view.showError("Could not save your preferences: " + exception.getMessage());
            return;
        }

        String summary =
                "Travel Preferences Saved!\n\n" +
                "Budget: " + model.getBudget() + "\n" +
                "Month: " + model.getMonth() + "\n" +
                "Group Type: " + model.getGroupType() + "\n" +
                "Activity Level: " + model.getActivityLevel() + "\n" +
                "Interests: " + Arrays.toString(model.getInterests());
        view.showMessage(summary);
    }

    private void handleReset() {
        view.clearForm();

        model.setBudget("");
        model.setMonth("");
        model.setGroupType("");
        model.setActivityLevel("");
        model.setInterests(new String[0]);

        view.showMessage("Preferences cleared.");
    }

    private void backToHome() {
        HomeView homeView = new HomeView();
        HomeController homeController = new HomeController(homeView, null, user);
        homeController.start();
        view.dispose();
    }

    private void handleForward() {
        if (!hasCompletePreferences()) {
            view.showError("Please fill in and save your preferences before proceeding.");
            return;
        }

        String summary =
                "Proceeding with the following preferences:\n\n" +
                "Budget: " + model.getBudget() + "\n" +
                "Month: " + model.getMonth() + "\n" +
                "Group Type: " + model.getGroupType() + "\n" +
                "Activity Level: " + model.getActivityLevel() + "\n" +
                "Interests: " + Arrays.toString(model.getInterests());
        view.showMessage(summary);
        backToHome();
    }

    private boolean hasCompletePreferences() {
        return model.getBudget() != null && !model.getBudget().trim().isEmpty()
                && model.getMonth() != null && !model.getMonth().trim().isEmpty()
                && model.getGroupType() != null && !model.getGroupType().trim().isEmpty()
                && model.getActivityLevel() != null
                && !model.getActivityLevel().trim().isEmpty()
                && model.getInterests() != null && model.getInterests().length > 0;
    }

    private void copyPreferences(PreferenceModel source) {
        model.setBudget(source.getBudget());
        model.setMonth(source.getMonth());
        model.setGroupType(source.getGroupType());
        model.setActivityLevel(source.getActivityLevel());
        model.setInterests(source.getInterests());
    }

    public void showView() {
        view.setVisible(true);
    }
}
