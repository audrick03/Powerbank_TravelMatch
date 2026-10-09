package view;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * Reviews of ONE destination. It is a panel, shown by ExplorePanel directly
 * below the destination header; the controller tells it which destination
 * with setDestination(). Only that destination's reviews are listed.
 */
public class ReviewView extends JPanel {
    private boolean authenticated;
    private JButton btnWriteReview;
    private JButton btnLoginPrompt;

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        updateWriteButtons();
    }

    private void updateWriteButtons() {
        if (btnWriteReview != null && btnLoginPrompt != null) {
            btnWriteReview.setVisible(authenticated);
            btnLoginPrompt.setVisible(!authenticated);
        }
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    private static final String SANS = "SansSerif";

    private static final Color DARK_GREEN  = new Color(0, 102, 51);
    private static final Color MID_GREEN   = new Color(0, 128, 64);
    private static final Color BTN_GREEN   = new Color(0, 153, 102);
    private static final Color LIGHT_GREEN = new Color(232, 245, 233);
    private static final Color TEXT_DARK   = new Color(20, 45, 35);
    private static final Color TEXT_MUTED  = new Color(110, 125, 118);
    private static final Color DIVIDER     = new Color(215, 228, 220);

    private static final int COLLAPSED_CHARS = 180;
    private static final int MAX_COMMENT = 500;
    private static final String[] RATING_WORDS =
            {"Tap a star", "Terrible", "Poor", "Average", "Very good", "Excellent"};

    // ---- Data -------------------------------------------------------
    private static class Review {
        final String author;
        final String destination;
        final String title;
        final String comment;
        final String date;
        final int rating;

        Review(String author, String destination, int rating, String title, String comment) {
            this(author, destination, rating, title, comment,
                    LocalDate.now(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)));
        }

        Review(String author, String destination, int rating, String title, String comment, String date) {
            this.author = author;
            this.destination = destination;
            this.rating = rating;
            this.title = title;
            this.comment = comment;
            this.date = date;
        }
    }

    /** Lets the controller save a newly written review. */
    public interface ReviewSubmitListener {
        void onReviewSubmitted(String author, String destination, int rating, String title, String comment);
    }

    private final List<Review> reviews = new ArrayList<>();
    private String currentDestination;          // null = show every review
    private transient ReviewSubmitListener submitListener;

    private final JPanel listPanel = new JPanel();
    private final StarRating summaryStars = new StarRating(0, 26, false);
    private final JLabel lblSummary = new JLabel();
    private final JLabel lblHeading = new JLabel("Traveler Reviews");
    private final Timer resizeTimer;

    public ReviewView() {
        setLayout(new BorderLayout());
        setOpaque(false);

        // ---- White card: header + summary + review list ----
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(Color.WHITE);
        container.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DIVIDER, 1),
                BorderFactory.createEmptyBorder(16, 24, 12, 24)));

        container.add(createHeader());
        container.add(Box.createVerticalStrut(10));
        container.add(createSummaryRow());

        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);
        listPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(listPanel);

        add(container, BorderLayout.NORTH);

        // Re-wrap comment text when the width changes
        resizeTimer = new Timer(150, e -> refreshList());
        resizeTimer.setRepeats(false);
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { resizeTimer.restart(); }
        });

        refreshList();
    }

    // ---------------------------------------------------------------
    //  Header + summary
    // ---------------------------------------------------------------
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblHeading.setFont(new Font(SANS, Font.BOLD, 22));
        lblHeading.setForeground(TEXT_DARK);

        // Both buttons are created once; setAuthenticated() shows the right one,
        // so logging in after this view was built still works.
        btnWriteReview = createStyledButton("✎  Write a review");
        btnWriteReview.addActionListener(e -> openWriteDialog());

        btnLoginPrompt = createOutlineButton("Log in to write a review");
        btnLoginPrompt.addActionListener(e -> showMessage("Please log in to write a review."));

        updateWriteButtons();

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(btnWriteReview);
        right.add(btnLoginPrompt);

        header.add(lblHeading, BorderLayout.CENTER);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel createSummaryRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2, 0, 0, 0, DARK_GREEN),
                BorderFactory.createEmptyBorder(10, 0, 4, 0)));

        lblSummary.setFont(new Font(SANS, Font.BOLD, 20));
        lblSummary.setForeground(TEXT_DARK);

        row.add(summaryStars);
        row.add(lblSummary);
        return row;
    }

    // ---------------------------------------------------------------
    //  List rendering (only the current destination's reviews)
    // ---------------------------------------------------------------
    private List<Review> visibleReviews() {
        List<Review> shown = new ArrayList<>();
        for (Review r : reviews) {
            if (currentDestination == null || r.destination.equalsIgnoreCase(currentDestination)) {
                shown.add(r);
            }
        }
        return shown;
    }

    private void refreshList() {
        listPanel.removeAll();
        List<Review> shown = visibleReviews();

        if (shown.isEmpty()) {
            JLabel empty = new JLabel("No reviews yet. Click “Write a review” to be the first!");
            empty.setFont(new Font(SANS, Font.PLAIN, 15));
            empty.setForeground(TEXT_MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(16, 0, 12, 0));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            listPanel.add(empty);
            summaryStars.setRating(0);
            lblSummary.setText("No reviews yet");
        } else {
            double sum = 0;
            for (Review r : shown) {
                listPanel.add(createReviewRow(r));
                sum += r.rating;
            }
            double avg = sum / shown.size();
            summaryStars.setRating((int) Math.round(avg));
            lblSummary.setText(String.format("%.1f  ·  %d review%s",
                    avg, shown.size(), shown.size() == 1 ? "" : "s"));
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    /** Width available for comment text, based on the panel width. */
    private int textWidth() {
        int available = getWidth() > 0 ? getWidth() : 900;
        return Math.max(280, available - 320);
    }

    private JPanel createReviewRow(Review r) {
        JPanel row = new JPanel(new BorderLayout(18, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, DIVIDER),
                BorderFactory.createEmptyBorder(12, 0, 12, 0)));

        // ---- Left: avatar + name ----
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(170, 48));
        left.add(new Avatar(r.author));
        JLabel name = new JLabel(html(r.author, 85));
        name.setFont(new Font(SANS, Font.PLAIN, 14));
        name.setForeground(TEXT_DARK);
        left.add(name);

        // ---- Right: rating, title, comment, date ----
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        top.add(new StarRating(r.rating, 18, false));
        top.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(top);

        if (!r.title.isEmpty()) {
            JLabel title = new JLabel(html(r.title, textWidth()));
            title.setFont(new Font(SANS, Font.BOLD, 17));
            title.setForeground(TEXT_DARK);
            title.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            right.add(title);
        }

        boolean longComment = r.comment.length() > COLLAPSED_CHARS;
        String collapsed = longComment
                ? r.comment.substring(0, COLLAPSED_CHARS).trim() + "…" : r.comment;

        JLabel comment = new JLabel(html(collapsed, textWidth()));
        comment.setFont(new Font(SANS, Font.PLAIN, 15));
        comment.setForeground(TEXT_DARK);
        comment.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        comment.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(comment);

        if (longComment) {
            JLabel more = new JLabel("More");
            more.setFont(new Font(SANS, Font.PLAIN, 14));
            more.setForeground(MID_GREEN);
            more.setCursor(new Cursor(Cursor.HAND_CURSOR));
            more.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
            more.setAlignmentX(Component.LEFT_ALIGNMENT);
            final boolean[] expanded = {false};
            more.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    expanded[0] = !expanded[0];
                    comment.setText(html(expanded[0] ? r.comment : collapsed, textWidth()));
                    more.setText(expanded[0] ? "Less" : "More");
                    listPanel.revalidate();
                    listPanel.repaint();
                }
            });
            right.add(more);
        }

        JLabel date = new JLabel("Reviewed on " + r.date);
        date.setFont(new Font(SANS, Font.PLAIN, 13));
        date.setForeground(TEXT_MUTED);
        date.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        date.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(date);

        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.CENTER);
        return row;
    }

    private JLabel createChip(String text) {
        JLabel chip = new JLabel(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(LIGHT_GREEN);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        chip.setOpaque(false);
        chip.setFont(new Font(SANS, Font.PLAIN, 12));
        chip.setForeground(DARK_GREEN);
        chip.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        return chip;
    }

    /** Escapes text so it is safe inside an HTML label. */
    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\n", "<br>");
    }

    private static String html(String s, int width) {
        return "<html><div style='width:" + width + "px'>" + esc(s) + "</div></html>";
    }

    // ---------------------------------------------------------------
    //  "Write a review" dialog (the destination is the one being viewed)
    // ---------------------------------------------------------------
    private void openWriteDialog() {

        if (!authenticated) {
            showMessage("Please log in to write a review.");
            return;
        }
        if (currentDestination == null || currentDestination.isEmpty()) {
            showMessage("Open a destination first to write a review.");
            return;
        }

        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Write a review", Dialog.ModalityType.APPLICATION_MODAL);

        // ---- Green banner ----
        JPanel banner = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0f, 0f, DARK_GREEN, (float) getWidth(), 0f, new Color(0, 160, 90)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        banner.setLayout(new BoxLayout(banner, BoxLayout.Y_AXIS));
        banner.setBorder(BorderFactory.createEmptyBorder(16, 28, 16, 28));
        JLabel bTitle = new JLabel("✎  Write a review");
        bTitle.setFont(new Font(SANS, Font.BOLD, 22));
        bTitle.setForeground(Color.WHITE);
        JLabel bSub = new JLabel("Share your experience with other travelers");
        bSub.setFont(new Font(SANS, Font.PLAIN, 13));
        bSub.setForeground(new Color(200, 235, 210));
        banner.add(bTitle);
        banner.add(Box.createVerticalStrut(2));
        banner.add(bSub);

        // ---- Form ----
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(20, 28, 8, 28));

        HintTextField nameField = new HintTextField("e.g. Juan Dela Cruz");
        styleInput(nameField);

        JPanel destRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        destRow.setOpaque(false);
        destRow.add(createChip("📍 " + currentDestination));

        // ---- Rating ----
        StarRating picker = new StarRating(0, 36, true);
        JLabel ratingWord = new JLabel(RATING_WORDS[0]);
        ratingWord.setFont(new Font(SANS, Font.BOLD, 14));
        ratingWord.setForeground(TEXT_MUTED);
        picker.setOnChange(() -> {
            ratingWord.setText(RATING_WORDS[picker.getRating()]);
            ratingWord.setForeground(DARK_GREEN);
        });
        JPanel pickerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pickerRow.setOpaque(false);
        pickerRow.add(picker);
        pickerRow.add(Box.createHorizontalStrut(14));
        pickerRow.add(ratingWord);

        HintTextField titleField = new HintTextField("Sum up your visit in a few words");
        styleInput(titleField);

        HintTextArea commentArea = new HintTextArea("What did you love? What should others know?", 4, 30);
        commentArea.setLineWrap(true);
        commentArea.setWrapStyleWord(true);
        styleInput(commentArea);
        ((AbstractDocument) commentArea.getDocument()).setDocumentFilter(new MaxLengthFilter(MAX_COMMENT));

        JScrollPane commentScroll = new JScrollPane(commentArea);
        commentScroll.setBorder(null);

        JLabel counter = new JLabel("0 / " + MAX_COMMENT);
        counter.setFont(new Font(SANS, Font.PLAIN, 12));
        counter.setForeground(TEXT_MUTED);
        commentArea.getDocument().addDocumentListener(onTextChange(
                () -> counter.setText(commentArea.getText().length() + " / " + MAX_COMMENT)));

        JPanel commentBlock = new JPanel(new BorderLayout(0, 4));
        commentBlock.setOpaque(false);
        commentBlock.add(commentScroll, BorderLayout.CENTER);
        JPanel counterRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        counterRow.setOpaque(false);
        counterRow.add(counter);
        commentBlock.add(counterRow, BorderLayout.SOUTH);

        JLabel error = new JLabel(" ");
        error.setForeground(new Color(200, 40, 40));
        error.setFont(new Font(SANS, Font.BOLD, 12));
        error.setAlignmentX(Component.LEFT_ALIGNMENT);

        addField(form, "Destination", destRow);
        addField(form, "Your name *", nameField);
        addField(form, "Your rating *", pickerRow);
        addField(form, "Review title", titleField);
        addField(form, "Your comment *", commentBlock);
        form.add(error);

        // ---- Buttons ----
        JButton cancel = createOutlineButton("Cancel");
        JButton submit = createStyledButton("Submit review");
        submit.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MID_GREEN, 2, true),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)));
        cancel.addActionListener(e -> dlg.dispose());
        submit.addActionListener(e -> {
            if (submitReview(nameField, picker, titleField, commentArea, error)) {
                dlg.dispose();
            }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        buttons.setBackground(Color.WHITE);
        buttons.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DIVIDER));
        buttons.add(cancel);
        buttons.add(submit);

        dlg.getRootPane().registerKeyboardAction(e -> dlg.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        dlg.add(banner, BorderLayout.NORTH);
        dlg.add(form, BorderLayout.CENTER);
        dlg.add(buttons, BorderLayout.SOUTH);
        dlg.pack();
        dlg.setSize(Math.max(dlg.getWidth(), 540), dlg.getHeight());
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    // Validates the form and stores the review. Returns true when the dialog can close.
    private boolean submitReview(JTextField nameField, StarRating picker,
                                 JTextField titleField, JTextArea commentArea, JLabel error) {
        String author = nameField.getText().trim();
        String comment = commentArea.getText().trim();
        int rating = picker.getRating();
        if (author.isEmpty() || rating == 0 || comment.isEmpty()) {
            error.setText("⚠  Please fill in your name, star rating, and comment.");
            return false;
        }
        String title = titleField.getText().trim();

        reviews.add(0, new Review(author, currentDestination, rating, title, comment));
        refreshList();
        if (submitListener != null) {
            submitListener.onReviewSubmitted(author, currentDestination, rating, title, comment);
        }
        return true;
    }

    private void addField(JPanel form, String labelText, JComponent field) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font(SANS, Font.BOLD, 13));
        label.setForeground(DARK_GREEN);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (field instanceof JTextField || field instanceof JComboBox) {
            field.setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height + 4));
        }
        form.add(label);
        form.add(Box.createVerticalStrut(4));
        form.add(field);
        form.add(Box.createVerticalStrut(12));
    }

    // ---------------------------------------------------------------
    //  Input styling helpers
    // ---------------------------------------------------------------
    private void styleInput(JTextComponent c) {
        c.setFont(new Font(SANS, Font.PLAIN, 14));
        c.setForeground(TEXT_DARK);
        setInputBorder(c, false);
        c.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { setInputBorder(c, true); }
            @Override public void focusLost(FocusEvent e)   { setInputBorder(c, false); }
        });
    }

    private void setInputBorder(JComponent c, boolean focused) {
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(focused ? MID_GREEN : new Color(205, 222, 212), 2, true),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
    }

    private JButton createOutlineButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font(SANS, Font.BOLD, 14));
        b.setForeground(MID_GREEN);
        b.setBackground(Color.WHITE);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MID_GREEN, 2, true),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(LIGHT_GREEN); }
            @Override public void mouseExited(MouseEvent e)  { b.setBackground(Color.WHITE); }
        });
        return b;
    }

    private static DocumentListener onTextChange(Runnable r) {
        return new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { r.run(); }
            @Override public void removeUpdate(DocumentEvent e)  { r.run(); }
            @Override public void changedUpdate(DocumentEvent e) { r.run(); }
        };
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(BTN_GREEN);
        button.setForeground(Color.WHITE);
        button.setFont(new Font(SANS, Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createLineBorder(MID_GREEN, 2, true));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { button.setBackground(new Color(0, 180, 90)); }
            @Override public void mouseExited(MouseEvent e)  { button.setBackground(BTN_GREEN); }
        });
        return button;
    }

    // ---------------------------------------------------------------
    //  Custom components
    // ---------------------------------------------------------------
    /** Stops the comment from growing past a maximum length. */
    private static class MaxLengthFilter extends DocumentFilter {
        private final int max;
        MaxLengthFilter(int max) { this.max = max; }

        @Override public void replace(FilterBypass fb, int off, int len, String text, AttributeSet a)
                throws BadLocationException {
            String t = text == null ? "" : text;
            if (fb.getDocument().getLength() - len + t.length() <= max) {
                super.replace(fb, off, len, t, a);
            }
        }

        @Override public void insertString(FilterBypass fb, int off, String text, AttributeSet a)
                throws BadLocationException {
            if (fb.getDocument().getLength() + text.length() <= max) {
                super.insertString(fb, off, text, a);
            }
        }
    }

    /** Star rating. Gold = rated. Hover preview + click when interactive. */
    private static class StarRating extends JComponent {
        private static final Color STAR_ON  = new Color(255, 179, 0);
        private static final Color STAR_OFF = new Color(214, 224, 218);
        private static final int GAP = 4;

        private int rating;
        private int hover = 0;
        private final int d;
        private transient Runnable onChange;

        StarRating(int rating, int diameter, boolean interactive) {
            this.rating = rating;
            this.d = diameter;
            Dimension size = new Dimension(5 * d + 4 * GAP + 2, d + 2);
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);

            if (interactive) {
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                MouseAdapter mouse = new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) { setRating(indexAt(e.getX())); }
                    @Override public void mouseMoved(MouseEvent e)   { hover = indexAt(e.getX()); repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { hover = 0; repaint(); }
                };
                addMouseListener(mouse);
                addMouseMotionListener(mouse);
            }
        }

        private int indexAt(int x) {
            return Math.max(1, Math.min(5, x / (d + GAP) + 1));
        }

        void setOnChange(Runnable r) { this.onChange = r; }
        int getRating() { return rating; }

        void setRating(int r) {
            this.rating = r;
            repaint();
            if (onChange != null) onChange.run();
        }

        private static Path2D star(double cx, double cy, double outer) {
            double inner = outer * 0.45;
            Path2D p = new Path2D.Double();
            for (int i = 0; i < 10; i++) {
                double r = (i % 2 == 0) ? outer : inner;
                double a = -Math.PI / 2 + i * Math.PI / 5;
                double x = cx + r * Math.cos(a);
                double y = cy + r * Math.sin(a);
                if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
            }
            p.closePath();
            return p;
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int shown = hover > 0 ? hover : rating;
            g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < 5; i++) {
                Path2D s = star(i * (d + GAP) + d / 2.0 + 1, d / 2.0 + 1, d / 2.0);
                g2.setColor(i < shown ? STAR_ON : STAR_OFF);
                g2.fill(s);
                g2.draw(s);   // the outline rounds the star's points slightly
            }
            g2.dispose();
        }
    }

    /** Round avatar showing the reviewer's first initial. */
    private static class Avatar extends JComponent {
        private final String letter;
        Avatar(String name) {
            letter = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
            Dimension size = new Dimension(48, 48);
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0f, 0f, new Color(129, 199, 132), 0f, (float) getHeight(), MID_GREEN));
            g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font(SANS, Font.BOLD, 22));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(letter, (getWidth() - fm.stringWidth(letter)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }

    /** Text field that shows grey hint text while empty. */
    private static class HintTextField extends JTextField {
        private final String hint;
        HintTextField(String hint) { this.hint = hint; }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(new Color(160, 175, 168));
                g2.setFont(getFont().deriveFont(Font.ITALIC));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(hint, getInsets().left, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        }
    }

    /** Text area that shows grey hint text while empty. */
    private static class HintTextArea extends JTextArea {
        private final String hint;
        HintTextArea(String hint, int rows, int cols) { super(rows, cols); this.hint = hint; }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(new Color(160, 175, 168));
                g2.setFont(getFont().deriveFont(Font.ITALIC));
                g2.drawString(hint, getInsets().left, getInsets().top + g2.getFontMetrics().getAscent());
                g2.dispose();
            }
        }
    }

    // ---------------------------------------------------------------
    //  Public API
    // ---------------------------------------------------------------
    /** Choose which destination's reviews are shown (and written). */
    public void setDestination(String destinationName) {
        this.currentDestination = destinationName;
        lblHeading.setText(destinationName == null || destinationName.isEmpty()
                ? "Traveler Reviews" : "Traveler Reviews: " + destinationName);
        refreshList();
    }

    /** Adds an already-saved review (used by the controller when loading from file). */
    public void addLoadedReview(String author, String destination, int rating,
                                String title, String comment, String date) {
        reviews.add(0, new Review(author, destination, rating, title, comment, date));
        refreshList();
    }

    /** Removes all reviews held by the view (the controller reloads them from file). */
    public void clearReviews() {
        reviews.clear();
        refreshList();
    }

    public void setReviewSubmitListener(ReviewSubmitListener listener) { this.submitListener = listener; }

    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ReviewView view = new ReviewView();
            new controller.ReviewController(view);
            JFrame frame = new JFrame("TravelMatch - Reviews");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.add(new JScrollPane(view));
            frame.setSize(900, 700);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}