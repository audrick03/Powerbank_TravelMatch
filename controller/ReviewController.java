package controller;

import model.ReviewModel;
import model.UserModel;
import repository.ReviewRepository;
import view.ReviewView;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

public class ReviewController {

    private final ReviewView view;
    private final ReviewRepository repository;
    private String accountName;

    public ReviewController(ReviewView view) {
        this.view = view;
        this.repository = new ReviewRepository();

        loadSavedReviews();
        view.setReviewSubmitListener(this::saveReview);
    }

    public void setCurrentUser(UserModel user) {
        accountName = user == null ? null : user.getUsername();
        view.setAccountName(accountName);
    }

    private boolean saveReview(String destination, int rating,
                            String title, String comment) {
        if (accountName == null || accountName.trim().isEmpty()) {
            view.showMessage("Please log in to write a review.");
            return false;
        }

        try {
            ReviewModel review = new ReviewModel(
                    UUID.randomUUID().toString(),
                    accountName,
                    destination,
                    rating,
                    title,
                    comment,
                    LocalDate.now(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)));

            repository.save(review);
            return true;
        } catch (IOException ex) {
            view.showMessage("Unable to save review:\n" + ex.getMessage());
            return false;
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
                    review.getAccountName(),
                    review.getDestinationName(),
                    review.getRating(),
                    review.getTitle(),
                    review.getComment(),
                    review.getCreatedDate());
        }
    }
}