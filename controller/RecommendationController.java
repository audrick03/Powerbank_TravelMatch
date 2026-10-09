package controller;

import model.*;
import repository.*;
import view.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Drives the Recommendations page:
 *   DESTINATIONS (all regions) -> selected destination panel
 * It remembers what the user picked and tells the view what to show.
 */
public class RecommendationController {

    private final RecommendationView view;
    private final DestinationRepository repository = DestinationRepository.getInstance();

    private DestinationModel selectedDestination;

    public RecommendationController(RecommendationView view) {
        this.view = view;

        view.addHomeListener(e -> view.dispose());
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

    // Push the current selection to the view: all destinations, in region order
    private void refresh() {
        List<DestinationModel> list = new ArrayList<>();
        for (String region : DestinationRepository.REGIONS) {
            list.addAll(repository.getByRegion(region));
        }

        view.showDestinations(list, selectedDestination);

        if (selectedDestination != null) {
            view.showActivities(selectedDestination, null);
        } else {
            view.hideActivities();
        }
    }
}