package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public class ReviewView extends JFrame {

    private static final Color PRIMARY = new Color(0x1D9E75);
    private static final Color ERROR = new Color(0xC0392B);

    private final JTextField usernameField = new JTextField(20);
    private final JComboBox<String> destinationField = new JComboBox<>(new String[]{
            "--- Select a destination ---", "New York", "Paris", "Tokyo", "Sydney", "Rio de Janeiro", ""
    });
    private final JComboBox<Integer> ratingBox =
            new JComboBox<>(new Integer[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
    private final JTextArea commentArea = new JTextArea(4, 20);
    private final JLabel messageLabel = new JLabel(" ");
    private final JPanel destinationList = new JPanel();
    private final Map<String, JPanel> destinationPanels = new HashMap<>();

    public ReviewView() {
        super("TravelMatch - User Reviews");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(520, 600));
        setSize(620, 700);
        setLocationRelativeTo(null);

        commentArea.setLineWrap(true);
        commentArea.setWrapStyleWord(true);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        destinationList.setLayout(new BoxLayout(destinationList, BoxLayout.Y_AXIS));
        destinationList.setBackground(Color.WHITE);

        setContentPane(createContent());
        getRootPane().setDefaultButton(createButton);
    }

    private final JButton createButton = new JButton("Submit review");

    private JPanel createContent() {
        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("Share a destination review", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setForeground(PRIMARY);
        content.add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.add(createReviewForm(), BorderLayout.NORTH);

        JPanel reviews = new JPanel(new BorderLayout());
        reviews.setBorder(BorderFactory.createTitledBorder("Destination reviews"));
        JScrollPane scrollPane = new JScrollPane(destinationList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        reviews.add(scrollPane, BorderLayout.CENTER);
        center.add(reviews, BorderLayout.CENTER);
        content.add(center, BorderLayout.CENTER);
        return content;
    }

    private JPanel createReviewForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Write a review"));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 8, 4, 8);

        addFormRow(form, c, 0, "Username", usernameField);
        addFormRow(form, c, 1, "Destination", destinationField);
        addFormRow(form, c, 2, "Rating ⭐ (1-10)", ratingBox);
        addFormRow(form, c, 3, "Comment", new JScrollPane(commentArea));

        c.gridy = 8;
        form.add(messageLabel, c);
        c.gridy = 9;
        form.add(createButton, c);
        return form;
    }

    private void addFormRow(JPanel form, GridBagConstraints c, int row,
                            String label, Component field) {
        c.gridy = row * 2;
        form.add(new JLabel(label), c);
        c.gridy = row * 2 + 1;
        form.add(field, c);
    }

    public String getUsername() {
        return usernameField.getText().trim();
    }

    public String getDestination() {
        return (String) destinationField.getSelectedItem();
    }

    public int getRating() {
        return (Integer) ratingBox.getSelectedItem();
    }

    public String getComment() {
        return commentArea.getText().trim();
    }

    public void addSubmitListener(ActionListener listener) {
        createButton.addActionListener(listener);
    }

    public void showError(String message) {
        messageLabel.setForeground(ERROR);
        messageLabel.setText(message);
    }

    public void clearMessage() {
        messageLabel.setText(" ");
    }

    public void clearComment() {
        commentArea.setText("");
    }

    public void addReview(String username, String destination, int rating, String comment) {
        String key = destination.toLowerCase(java.util.Locale.ROOT);
        JPanel panel = destinationPanels.get(key);
        if (panel == null) {
            panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.setBorder(BorderFactory.createTitledBorder(destination));
            destinationPanels.put(key, panel);
            destinationList.add(panel);
        }

        JPanel review = new JPanel(new BorderLayout(4, 4));
        review.setAlignmentX(Component.LEFT_ALIGNMENT);
        review.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(4, 8, 8, 8),
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xD9E2DF))));

        JLabel authorAndRating = new JLabel("@" +username.toLowerCase() + "  |  " + "⭐" + rating + "/10");
        authorAndRating.setFont(authorAndRating.getFont().deriveFont(Font.BOLD));
        JTextArea reviewComment = new JTextArea(comment);
        reviewComment.setEditable(false);
        reviewComment.setLineWrap(true);
        reviewComment.setWrapStyleWord(true);
        reviewComment.setOpaque(false);

        review.add(authorAndRating, BorderLayout.NORTH);
        review.add(reviewComment, BorderLayout.CENTER);
        panel.add(review);
        panel.revalidate();
        panel.repaint();
        destinationList.revalidate();
        destinationList.repaint();
    }
}
