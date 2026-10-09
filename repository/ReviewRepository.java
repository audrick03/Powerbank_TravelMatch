package repository;

import model.ReviewModel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReviewRepository {

    private static final Path SAVE_FILE = Paths.get("reviews.txt");

    public List<ReviewModel> findAll() {
        List<ReviewModel> reviews = new ArrayList<>();

        if (!Files.exists(SAVE_FILE)) {
            return reviews;
        }

        try {
            for (String line : Files.readAllLines(SAVE_FILE, StandardCharsets.UTF_8)) {
                ReviewModel review = parseReview(line);
                if (review != null) {
                    reviews.add(review);
                }
            }
        } catch (IOException ignored) {
        }
        return reviews;
    }

    public List<ReviewModel> findByDestination(String destinationName) {
        List<ReviewModel> result = new ArrayList<>();
        for (ReviewModel review : findAll()) {
            if (review.getDestinationName().equals(destinationName)) {
                result.add(review);
            }
        }
        return result;
    }

    public void save(ReviewModel review) throws IOException {
        String line = String.join("\t",
                escape(review.getReviewId()),
                escape(review.getUserId()),
                escape(review.getDestinationName()),
                String.valueOf(review.getRating()),
                escape(review.getTitle()),
                escape(review.getComment()),
                escape(review.getCreatedDate()));
        Files.write(SAVE_FILE, Collections.singletonList(line), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public int getReviewCount(String destinationName) {
        return findByDestination(destinationName).size();
    }

    public double getAverageRating(String destinationName) {
        List<ReviewModel> reviews = findByDestination(destinationName);
        if (reviews.isEmpty()) {
            return 0;
        }
        double total = 0;
        for (ReviewModel review : reviews) {
            total += review.getRating();
        }
        return total / reviews.size();
    }

    public boolean hasReview(String userId, String destinationName) {
        for (ReviewModel review : findByDestination(destinationName)) {
            if (review.getUserId().equals(userId)) {
                return true;
            }
        }
        return false;
    }

    private ReviewModel parseReview(String line) {
        String[] p = line.split("\t", -1);
        if (p.length < 7) {
            return null;
        }
        try {
            return new ReviewModel(unescape(p[0]), unescape(p[1]), unescape(p[2]),
                    Integer.parseInt(p[3]), unescape(p[4]), unescape(p[5]), unescape(p[6]));
        } catch (Exception ex) {
            return null;
        }
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n");
    }

    private String unescape(String s) {
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;

        for (char c : s.toCharArray()) {
            if (escaped) {
                switch (c) {
                    case 'n':
                        sb.append('\n');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    default:
                        sb.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}