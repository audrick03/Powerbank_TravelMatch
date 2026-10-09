package controller;

import model.PreferenceModel;
import model.UserModel;
import service.DestinationRecommendation;
import service.PreferenceService;
import view.RecommendationView;
import view.HomeView;
import view.PreferenceView;

import java.util.List;
import java.util.Optional;

public class PreferenceController {

    private final PreferenceView view;
    private final PreferenceModel model;
    private final UserModel user;
    private final PreferenceService preferenceService;
    private final Runnable onReturnHome;

    public PreferenceController(PreferenceView view, PreferenceModel model, UserModel user,
                                PreferenceService preferenceService) {
        this(view, model, user, preferenceService, null);
    }

    public PreferenceController(PreferenceView view, PreferenceModel model, UserModel user,
                                PreferenceService preferenceService, Runnable onReturnHome) {
        if (view == null || model == null || user == null || preferenceService == null) {
            throw new IllegalArgumentException(
                    "Preference view, model, signed-in user, and service are required");
        }

        this.view = view;
        this.model = model;
        this.user = user;
        this.preferenceService = preferenceService;
        this.onReturnHome = onReturnHome;

        loadSavedPreferences();
        initializeListeners();
    }

    private void initializeListeners() {
        view.addRecommendListener(e -> handleRecommend());
        view.addResetListener(e -> handleReset());
        view.addForwardListener(e -> backToHome());
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

        List<DestinationRecommendation> recommendations;
        try {
            recommendations = preferenceService.getRankedRecommendations(model);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            view.showError("Could not find destinations: " + exception.getMessage());
            return;
        }

        try {
            preferenceService.savePreferences(user, model);
        } catch (IllegalStateException exception) {
            view.showError("Could not save your preferences: " + exception.getMessage());
            return;
        }

        view.showRecommendations(recommendations);
        if (!recommendations.isEmpty()) {
            RecommendationView recommendationView = new RecommendationView();
            new RecommendationController(
                    recommendationView, recommendations, onReturnHome);
            view.dispose();
            recommendationView.setVisible(true);
        }
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
        view.dispose();
        if (onReturnHome != null) {
            onReturnHome.run();
        } else {
            HomeView homeView = new HomeView();
            HomeController homeController = new HomeController(homeView, null, user);
            homeController.start();
        }
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
