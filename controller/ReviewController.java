package controller;

import model.ReviewModel;
import repository.ReviewRepository;
import view.ReviewView;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

public class ReviewController {

    private final ReviewView view;
    private final ReviewRepository repository;

    public ReviewController(ReviewView view) {
        this.view = view;
        this.repository = new ReviewRepository();

        loadSavedReviews();
        view.setReviewSubmitListener(this::saveReview);
    }

    private void saveReview(String author, String destination, int rating,
                            String title, String comment) {
        if (!view.isAuthenticated()) {
            view.showMessage("Please log in to write a review.");
            return;
        }

        try {
            ReviewModel review = new ReviewModel(
                    UUID.randomUUID().toString(),
                    author,
                    destination,
                    rating,
                    title,
                    comment,
                    LocalDate.now(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)));

            repository.save(review);
        } catch (Exception ex) {
            view.showMessage("Unable to save review:\n" + ex.getMessage());
        }
    }

    /** Re-reads reviews.txt so reviews saved from another screen show up. */
    public void reload() {
        view.clearReviews();
        loadSavedReviews();
    }

    private void loadSavedReviews() {
        for (ReviewModel review : repository.findAll()) {
            view.addLoadedReview(
                    review.getUserId(),
                    review.getDestinationName(),
                    review.getRating(),
                    review.getTitle(),
                    review.getComment(),
                    review.getCreatedDate());
        }
    }
}