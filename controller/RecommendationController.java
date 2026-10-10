package controller;

import model.*;
import repository.*;
import service.*;
import view.*;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Drives the Recommendations page, preserving the ranked results while a
 * destination is selected.
 */
public class RecommendationController {

    private final RecommendationView view;
    private final List<DestinationModel> destinations;
    private final List<DestinationRecommendation> recommendations;
    private final Runnable onHome;
    private final UserModel currentUser;

    private DestinationModel selectedDestination;
    private final ReviewView reviewView = new ReviewView();
    private final ReviewController reviewController;
    private JFrame reviewFrame;

    public RecommendationController(RecommendationView view) {
        this(view, topDestinations(), null, null, null);
    }

    public RecommendationController(RecommendationView view,
                                    List<DestinationRecommendation> recommendations,
                                    Runnable onHome,
                                    UserModel currentUser) {
        this(view, null, limitRecommendations(recommendations), onHome, currentUser);
    }

    private RecommendationController(RecommendationView view,
                                     List<DestinationModel> destinations,
                                     List<DestinationRecommendation> recommendations,
                                     Runnable onHome,
                                     UserModel currentUser) {
        if (view == null || (destinations == null && recommendations == null)) {
            throw new IllegalArgumentException("A recommendation view and results are required");
        }
        this.view = view;
        this.destinations = destinations;
        this.recommendations = recommendations;
        this.onHome = onHome;
        this.currentUser = currentUser;

        view.addHomeListener(e -> {
            view.dispose();
            if (this.onHome != null) {
                this.onHome.run();
            }
        });
        view.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                if (RecommendationController.this.onHome != null) {
                    RecommendationController.this.onHome.run();
                }
            }
        });
        view.addDestinationListener(this::handleDestinationClick);
        view.addDetailsListener(d -> view.showDetails(d, null));
        view.addBackToDestinationsListener(e -> backToDestinations());
        view.addReviewListener(this::openReviews);
        reviewController = new ReviewController(reviewView);

        refresh();
    }

    // DESTINATION: show its own recommended activities
    private void handleDestinationClick(DestinationModel destination) {
        selectedDestination = destination;
        refresh();
    }

    /** Reuse the application's existing destination-scoped review feature. */
    private void openReviews(DestinationModel destination) {
        reviewController.reload();
        reviewView.setDestination(destination.getName());
        reviewController.setCurrentUser(currentUser);
        if (reviewFrame == null || !reviewFrame.isDisplayable()) {
            reviewFrame = new JFrame("TravelMatch - Reviews");
            reviewFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            reviewFrame.setLayout(new BorderLayout());
            reviewFrame.add(new JScrollPane(reviewView), BorderLayout.CENTER);
            reviewFrame.setSize(900, 700);
            reviewFrame.setMinimumSize(new java.awt.Dimension(700, 550));
            reviewFrame.setLocationRelativeTo(view);
        }
        reviewFrame.setVisible(true);
        reviewFrame.toFront();
    }

    private void backToDestinations() {
        selectedDestination = null;
        refresh();
        view.scrollToTop();
    }

    // Push the current selection and the same ranked results to the view.
    private void refresh() {
        if (recommendations == null) {
            view.showDestinations(destinations, selectedDestination);
        } else {
            view.showRecommendations(recommendations, selectedDestination);
        }

        if (selectedDestination != null) {
            view.showActivities(selectedDestination, null);
        } else {
            view.hideActivities();
        }
    }

    private static List<DestinationRecommendation> limitRecommendations(
            List<DestinationRecommendation> recommendations) {
        if (recommendations == null) {
            throw new IllegalArgumentException("Recommendations are required");
        }
        int visibleCount = Math.min(
                recommendations.size(), PreferenceService.MAX_RECOMMENDATIONS);
        return Collections.unmodifiableList(
                new ArrayList<>(recommendations.subList(0, visibleCount)));
    }

    private static List<DestinationModel> topDestinations() {
        List<DestinationModel> destinations =
                new ArrayList<>(DestinationRepository.getInstance().getAll());
        destinations.sort(Comparator.comparingDouble(DestinationModel::getScore).reversed());
        int visibleCount = Math.min(destinations.size(), PreferenceService.MAX_RECOMMENDATIONS);
        return Collections.unmodifiableList(new ArrayList<>(destinations.subList(0, visibleCount)));
    }
}