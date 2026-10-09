package controller;

import model.*;
import repository.*;
import service.*;
import view.*;

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

    private DestinationModel selectedDestination;

    public RecommendationController(RecommendationView view) {
        this(view, topDestinations(), null, null);
    }

    public RecommendationController(RecommendationView view,
                                    List<DestinationRecommendation> recommendations,
                                    Runnable onHome) {
        this(view, null, limitRecommendations(recommendations), onHome);
    }

    private RecommendationController(RecommendationView view,
                                     List<DestinationModel> destinations,
                                     List<DestinationRecommendation> recommendations,
                                     Runnable onHome) {
        if (view == null || (destinations == null && recommendations == null)) {
            throw new IllegalArgumentException("A recommendation view and results are required");
        }
        this.view = view;
        this.destinations = destinations;
        this.recommendations = recommendations;
        this.onHome = onHome;

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

        refresh();
    }

    // DESTINATION: show its own recommended activities
    private void handleDestinationClick(DestinationModel destination) {
        selectedDestination = destination;
        refresh();
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