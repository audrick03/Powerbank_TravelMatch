package controller;

import view.*;

public class ReviewController {

    private static final String DESTINATION_PLACEHOLDER = "--- Select a destination ---";

    private final ReviewView view;

    public ReviewController(ReviewView view) {
        if (view == null) {
            throw new IllegalArgumentException("Review view is required");
        }

        this.view = view;
        view.addSubmitListener(event -> handleSubmit());
    }

    private void handleSubmit() {
        view.clearMessage();

        String username = view.getUsername();
        String destination = view.getDestination();
        String comment = view.getComment();

        if (username.isEmpty()) {
            view.showError("Please enter your username.");
        } else if (username.length() < 3) {
            view.showError("Username must be at least 3 characters long.");
        } else if (username.length() > 20) {
            view.showError("Username must not exceed 20 characters.");
        } else if (!username.matches("[a-zA-Z0-9_]+")) {
            view.showError("Username can only contain letters, numbers, and underscores.");
        } else if (destination == null || destination.trim().isEmpty()
                || DESTINATION_PLACEHOLDER.equals(destination)) {
            view.showError("Please select a destination.");
        } else if (comment.isEmpty()) {
            view.showError("Please enter a comment.");
        } else {
            view.addReview(username, destination, view.getRating(), comment);
            view.clearComment();
        }
    }
}
