package controller;

import view.ReviewView;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Locale;

public class ReviewController {
    // Reviews are saved here (created in the folder you run the app from)
    private static final Path SAVE_FILE = Paths.get("reviews.txt");

    private final ReviewView view;

    public ReviewController(ReviewView view) {
        this.view = view;

        loadSavedReviews();

        view.setReviewSubmitListener(this::saveReview);
    }

    // ---------------------------------------------------------------
    //  Saving / loading (one review per line, tab-separated)
    //  author | destination | rating | title | comment | date
    // ---------------------------------------------------------------
    private void saveReview(String author, String destination, int rating,
                            String title, String comment) {
        String date = LocalDate.now(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH));
        String line = String.join("\t",
                escape(author), escape(destination), String.valueOf(rating),
                escape(title), escape(comment), escape(date));
        try {
            Files.write(SAVE_FILE, Collections.singletonList(line), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            view.showMessage("Your review is shown, but it could not be saved:\n" + ex.getMessage());
        }
    }

    private void loadSavedReviews() {
        if (!Files.exists(SAVE_FILE)) return;
        try {
            for (String line : Files.readAllLines(SAVE_FILE, StandardCharsets.UTF_8)) {
                loadReviewLine(line);
            }
        } catch (IOException ex) {
            view.showMessage("Could not load saved reviews:\n" + ex.getMessage());
        }
    }

    // Adds one saved line to the view; damaged lines are skipped
    private void loadReviewLine(String line) {
        String[] p = line.split("\t", -1);
        if (p.length < 6) return;
        int rating = parseRating(p[2]);
        if (rating == 0) return;
        view.addLoadedReview(unescape(p[0]), unescape(p[1]), rating,
                unescape(p[3]), unescape(p[4]), unescape(p[5]));
    }

    // 1 to 5, or 0 when the text is not a number
    private static int parseRating(String text) {
        try {
            return Math.max(1, Math.min(5, Integer.parseInt(text)));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\t", "\\t")
                .replace("\r", "").replace("\n", "\\n");
    }

    private static String unescape(String s) {
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (char c : s.toCharArray()) {
            if (escaped) {
                sb.append(unescapeChar(c));
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                sb.append(c);
            }
        }
        if (escaped) {
            sb.append('\\');   // a lone backslash at the end stays as it is
        }
        return sb.toString();
    }

    private static char unescapeChar(char c) {
        if (c == 'n') return '\n';
        if (c == 't') return '\t';
        return c;
    }
}