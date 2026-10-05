package view;

import controller.*;
import model.*;
import repository.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Recommendations screen (one scrolling page):
 *   top bar  ->  regions  ->  categories  ->  destinations  ->  recommended activities
 * The view only displays data; RecommendationController decides what to show.
 */
public class RecommendationView extends JFrame {

    private static final String FONT = "Segoe UI";
    private static final String EMOJI_FONT = "Segoe UI Emoji";

    private static final Color PRIMARY = new Color(0x0F7B52);
    private static final Color PRIMARY_LIGHT = new Color(0x15A06B);
    private static final Color PANEL_TINT = new Color(0xEAF7F1);
    private static final Color BACKGROUND = new Color(0xF4F8F7);
    private static final Color TEXT_DARK = new Color(0x1B2B34);
    private static final Color TEXT_MUTED = new Color(0x5F6F7A);
    private static final Color BORDER_LIGHT = new Color(0xDDE5E8);

    private static final int ACTIVITY_SLOTS = 5;

    private final Map<String, BufferedImage> imageCache = new HashMap<>();
    private final Map<String, SelectableCard> regionCards = new LinkedHashMap<>();
    private final Map<String, Pill> regionPills = new LinkedHashMap<>();
    private final Map<String, SelectableCard> categoryCards = new LinkedHashMap<>();

    // top bar
    private final JButton btnHome = new NavButton("🏠  Home");
    private final JButton btnPreferences = new NavButton("👤  Preferences");
    private final JButton btnLogout = new NavButton("🚪  Logout");

    // destination section
    private final JLabel lblDestIcon = new JLabel();
    private final JLabel lblDestTitle = new JLabel();
    private final JLabel lblDestBadge = new JLabel();
    private final JLabel lblDestSub = new JLabel();
    private final JPanel destGrid = new JPanel(new GridLayout(0, 5, 14, 14));

    // activities section
    private final JPanel activityPanel = new JPanel(new BorderLayout(18, 0));
    private final JButton btnDetails = new ActionButton("View Details", true);
    private final JButton btnBackToCategories = new ActionButton("←  Back to Categories", false);
    private DestinationModel shownDestination;

    private ScrollPanel content;
    private JScrollPane scroll;
    private JPanel categorySection;
    private int row = 0;

    private Consumer<String> regionListener = r -> {};
    private Consumer<String> categoryListener = c -> {};
    private Consumer<DestinationModel> destinationListener = d -> {};
    private Consumer<DestinationModel> detailsListener = d -> {};

    public RecommendationView() {
        setTitle("TravelMatch - Recommendations");
        setSize(1280, 860);
        setMinimumSize(new Dimension(1000, 700));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(createTopBar(), BorderLayout.NORTH);

        content = new ScrollPanel(new GridBagLayout());
        content.setBackground(BACKGROUND);
        content.setBorder(new EmptyBorder(14, 28, 28, 28));

        addRow(createPageHeader("Explore the Philippines",
                "Choose a region to discover amazing destinations."), 0);
        addRow(createRegionRow(), 10);

        categorySection = createCategorySection();
        addRow(categorySection, 22);

        addRow(createDestinationSection(), 22);

        activityPanel.setBackground(PANEL_TINT);
        activityPanel.setBorder(new CompoundBorder(new LineBorder(PRIMARY, 1), new EmptyBorder(12, 12, 12, 16)));
        activityPanel.setVisible(false);
        addRow(activityPanel, 18);

        // filler so everything stays at the top
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridx = 0;
        filler.gridy = row++;
        filler.weighty = 1;
        content.add(Box.createGlue(), filler);

        scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        root.add(scroll, BorderLayout.CENTER);

        btnDetails.addActionListener(e -> {
            if (shownDestination != null) {
                detailsListener.accept(shownDestination);
            }
        });

        add(root);
    }

    // =========================================================
    // PUBLIC API (used by the controller)
    // =========================================================

    public void addHomeListener(ActionListener l)        { btnHome.addActionListener(l); }
    public void addPreferenceListener(ActionListener l)  { btnPreferences.addActionListener(l); }
    public void addLogoutListener(ActionListener l)      { btnLogout.addActionListener(l); }
    public void addBackToCategoriesListener(ActionListener l) { btnBackToCategories.addActionListener(l); }

    public void addRegionListener(Consumer<String> l)          { regionListener = l; }
    public void addCategoryListener(Consumer<String> l)        { categoryListener = l; }
    public void addDestinationListener(Consumer<DestinationModel> l){ destinationListener = l; }
    public void addDetailsListener(Consumer<DestinationModel> l)    { detailsListener = l; }

    public void setSelectedRegion(String region) {
        for (Map.Entry<String, SelectableCard> e : regionCards.entrySet()) {
            boolean on = e.getKey().equals(region);
            e.getValue().setSelected(on);
            regionPills.get(e.getKey()).setFilled(on);
        }
    }

    public void setSelectedCategory(String category) {
        for (Map.Entry<String, SelectableCard> e : categoryCards.entrySet()) {
            e.getValue().setSelected(e.getKey().equals(category));
        }
    }

    /** Destination cards for one region + category. 'selected' may be null. */
    public void showDestinations(String region, String category,
                                 List<DestinationModel> list, DestinationModel selected) {
        String catLabel = DestinationRepository.label(category);
        lblDestIcon.setText(iconFor(category));
        lblDestTitle.setText(region + " — " + catLabel + " Destinations");
        lblDestBadge.setText(region.toUpperCase());
        lblDestSub.setText("Explore the best " + catLabel.toLowerCase() + " destinations in " + region + ".");

        destGrid.removeAll();
        if (list.isEmpty()) {
            destGrid.setLayout(new GridLayout(0, 1));
            destGrid.add(label("No destinations found for this region and category yet.", 15, false, TEXT_MUTED));
        } else {
            destGrid.setLayout(new GridLayout(0, 5, 14, 14));
            for (DestinationModel d : list) {
                destGrid.add(createDestinationCard(d, category, d == selected));
            }
            // keep card width constant when a row is not full
            int missing = (5 - list.size() % 5) % 5;
            for (int i = 0; i < missing; i++) {
                JPanel blank = new JPanel();
                blank.setOpaque(false);
                destGrid.add(blank);
            }
        }
        refresh(destGrid);
    }

    /** The green "Recommended Activities" panel under the destination cards. */
    public void showActivities(DestinationModel d, String category) {
        shownDestination = d;
        String cat = category != null ? category : d.getCategories().get(0);

        activityPanel.removeAll();

        // left: destination photo
        CoverPanel photo = new CoverPanel(loadDestinationImage(d, cat), iconFor(cat));
        photo.setPreferredSize(new Dimension(200, 10));
        activityPanel.add(photo, BorderLayout.WEST);

        // center: header + activity cards
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setOpaque(false);
        titles.add(left(label(iconFor(cat) + "  " + d.getName() + " — Recommended Activities", 18, true, TEXT_DARK)));
        titles.add(Box.createVerticalStrut(2));
        titles.add(left(label("Experience the best of " + d.getName() + " with these top activities, "
                + "perfectly matched to your " + DestinationRepository.label(cat).toLowerCase() + " preference.",
                12, false, TEXT_MUTED)));
        header.add(titles, BorderLayout.WEST);
        JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        badgeWrap.setOpaque(false);
        badgeWrap.add(createBadge("✓ Great Match for You", true));
        header.add(badgeWrap, BorderLayout.EAST);
        center.add(header, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(1, ACTIVITY_SLOTS, 10, 0));
        grid.setOpaque(false);
        List<String> acts = d.getActivities(cat);
        int shown = Math.min(ACTIVITY_SLOTS, acts.size());
        for (int i = 0; i < shown; i++) {
            grid.add(createActivityCard(d, cat, acts.get(i)));
        }
        for (int i = shown; i < ACTIVITY_SLOTS; i++) {
            JPanel blank = new JPanel();
            blank.setOpaque(false);
            grid.add(blank);
        }
        center.add(grid, BorderLayout.CENTER);
        activityPanel.add(center, BorderLayout.CENTER);

        // right: buttons
        JPanel buttons = new JPanel(new GridBagLayout());
        buttons.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 10, 0);
        c.gridy = 0;
        buttons.add(btnDetails, c);
        c.gridy = 1;
        c.insets = new Insets(0, 0, 0, 0);
        buttons.add(btnBackToCategories, c);
        buttons.setPreferredSize(new Dimension(190, 10));
        activityPanel.add(buttons, BorderLayout.EAST);

        activityPanel.setVisible(true);
        refresh(content);

        SwingUtilities.invokeLater(() ->
                activityPanel.scrollRectToVisible(new Rectangle(0, 0,
                        activityPanel.getWidth(), activityPanel.getHeight())));
    }

    public void hideActivities() {
        shownDestination = null;
        activityPanel.setVisible(false);
        refresh(content);
    }

    public void scrollToCategories() {
        SwingUtilities.invokeLater(() ->
                scroll.getViewport().setViewPosition(new Point(0, Math.max(0, categorySection.getY() - 10))));
    }

    /** Full details of one destination (View Details button). */
    public void showDetails(DestinationModel d, String category) {
        String cat = category != null ? category : d.getCategories().get(0);
        StringBuilder sb = new StringBuilder("<html><body style='font-family:" + FONT
                + ";width:440px'>");
        sb.append("<h2 style='color:#0F7B52;margin:0'>").append(esc(d.getName())).append("</h2>");
        sb.append("<p style='color:#5F6F7A;margin:2px 0 8px 0'>📍 ").append(esc(d.getProvince()))
                .append(" · ").append(esc(d.getRegion())).append("</p>");
        sb.append("<p><b>TravelMatch Score:</b> ").append(d.getScore()).append("/5</p>");
        sb.append("<p>").append(esc(d.getDescription())).append("</p>");
        sb.append("<p><b>Best time:</b> ").append(esc(nz(d.getBestTime())))
                .append("<br><b>Duration:</b> ").append(esc(nz(d.getDuration())))
                .append("<br><b>Budget:</b> ").append(esc(nz(d.getBudget())))
                .append("<br><b>Difficulty:</b> ").append(esc(nz(d.getDifficulty()))).append("</p>");
        sb.append("<p><b>Activities</b></p><ul>");
        for (String a : d.getActivities(cat)) {
            sb.append("<li>").append(esc(a)).append("</li>");
        }
        sb.append("</ul><p><b>Places to visit</b></p><ul>");
        for (String p : d.getPlaces()) {
            sb.append("<li>").append(esc(p.replace("|", "—"))).append("</li>");
        }
        sb.append("</ul><p><b>Travel tips</b></p><ul>");
        for (String t : DestinationRepository.travelTips(cat)) {
            sb.append("<li>").append(esc(t)).append("</li>");
        }
        sb.append("</ul><p><b>What to bring</b></p><ul>");
        for (String b : DestinationRepository.whatToBring(cat)) {
            sb.append("<li>").append(esc(b)).append("</li>");
        }
        sb.append("</ul></body></html>");

        JEditorPane pane = new JEditorPane("text/html", sb.toString());
        pane.setEditable(false);
        pane.setCaretPosition(0);
        JScrollPane sp = new JScrollPane(pane);
        sp.setPreferredSize(new Dimension(500, 460));
        JOptionPane.showMessageDialog(this, sp, d.getName() + " — Details", JOptionPane.PLAIN_MESSAGE);
    }

    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    // =========================================================
    // PAGE SECTIONS
    // =========================================================

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER_LIGHT),
                new EmptyBorder(8, 18, 8, 18)));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brand.setOpaque(false);
        JLabel pin = new JLabel("📍");
        pin.setFont(new Font(EMOJI_FONT, Font.PLAIN, 28));
        pin.setForeground(PRIMARY);
        JLabel title = label("TravelMatch  —  Recommendations", 24, true, PRIMARY);
        brand.add(pin);
        brand.add(title);

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        nav.setOpaque(false);
        nav.add(btnHome);
        nav.add(divider());
        nav.add(btnPreferences);
        nav.add(divider());
        nav.add(btnLogout);

        bar.add(brand, BorderLayout.WEST);
        bar.add(nav, BorderLayout.EAST);
        return bar;
    }

    private JPanel createPageHeader(String title, String sub) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.add(left(label(title, 26, true, TEXT_DARK)));
        p.add(Box.createVerticalStrut(2));
        p.add(left(label(sub, 14, false, TEXT_MUTED)));
        return p;
    }

    private JPanel createRegionRow() {
        JPanel grid = new JPanel(new GridLayout(1, 3, 16, 0));
        grid.setOpaque(false);
        for (String region : DestinationRepository.REGIONS) {
            grid.add(createRegionCard(region));
        }
        return grid;
    }

    private JPanel createCategorySection() {
        JPanel section = new JPanel(new BorderLayout(0, 8));
        section.setOpaque(false);
        section.add(createPageHeader("Choose a Category",
                "Select a category to see destinations in your chosen region."), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(1, 5, 14, 0));
        grid.setOpaque(false);
        for (String category : DestinationRepository.CATEGORIES) {
            grid.add(createCategoryCard(category));
        }
        section.add(grid, BorderLayout.CENTER);
        return section;
    }

    private JPanel createDestinationSection() {
        JPanel section = new JPanel(new BorderLayout(0, 10));
        section.setOpaque(false);

        lblDestIcon.setFont(new Font(EMOJI_FONT, Font.PLAIN, 24));
        lblDestIcon.setForeground(PRIMARY);
        lblDestTitle.setFont(new Font(FONT, Font.BOLD, 22));
        lblDestTitle.setForeground(PRIMARY);
        lblDestBadge.setFont(new Font(FONT, Font.BOLD, 11));
        lblDestBadge.setForeground(PRIMARY);
        lblDestBadge.setBorder(new EmptyBorder(3, 10, 3, 10));
        lblDestBadge.setOpaque(true);
        lblDestBadge.setBackground(PANEL_TINT);
        lblDestSub.setFont(new Font(FONT, Font.PLAIN, 14));
        lblDestSub.setForeground(TEXT_MUTED);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleRow.setOpaque(false);
        titleRow.add(lblDestIcon);
        titleRow.add(lblDestTitle);
        titleRow.add(lblDestBadge);

        JPanel head = new JPanel();
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setOpaque(false);
        head.add(left(titleRow));
        head.add(Box.createVerticalStrut(2));
        head.add(left(lblDestSub));

        destGrid.setOpaque(false);
        section.add(head, BorderLayout.NORTH);
        section.add(destGrid, BorderLayout.CENTER);
        return section;
    }

    private void addRow(JComponent comp, int topGap) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row++;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        c.insets = new Insets(topGap, 0, 0, 0);
        content.add(comp, c);
    }

    // =========================================================
    // CARDS
    // =========================================================

    private SelectableCard createRegionCard(String region) {
        SelectableCard card = new SelectableCard(new BorderLayout(14, 0), 10);

        CoverPanel photo = new CoverPanel(loadImage(region), "🌏");
        photo.setPreferredSize(new Dimension(170, 125));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        info.add(Box.createVerticalGlue());
        info.add(left(label(region.toUpperCase(), 18, true, PRIMARY)));
        info.add(Box.createVerticalStrut(6));
        info.add(left(text(DestinationRepository.regionDescription(region), 170, 13, TEXT_MUTED, false)));
        info.add(Box.createVerticalStrut(10));
        Pill pill = new Pill("Explore  →", false);
        pill.setPreferredSize(new Dimension(110, 32));
        pill.setMaximumSize(new Dimension(110, 32));
        info.add(left(pill));
        info.add(Box.createVerticalGlue());

        card.add(photo, BorderLayout.WEST);
        card.add(info, BorderLayout.CENTER);

        regionCards.put(region, card);
        regionPills.put(region, pill);
        makeClickable(card, () -> regionListener.accept(region));
        return card;
    }

    private SelectableCard createCategoryCard(String category) {
        SelectableCard card = new SelectableCard(new BorderLayout(0, 6), 6);

        CoverPanel photo = new CoverPanel(loadImage(category), iconFor(category));
        photo.setPreferredSize(new Dimension(150, 85));

        JPanel foot = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        foot.setOpaque(false);
        foot.add(label(iconFor(category) + "  " + DestinationRepository.label(category), 14, true, TEXT_DARK));

        card.add(photo, BorderLayout.CENTER);
        card.add(foot, BorderLayout.SOUTH);

        categoryCards.put(category, card);
        makeClickable(card, () -> categoryListener.accept(category));
        return card;
    }

    private SelectableCard createDestinationCard(DestinationModel d, String category, boolean selected) {
        SelectableCard card = new SelectableCard(new BorderLayout(0, 8), 6);

        String firstCat = d.getCategories().get(0);
        CoverPanel photo = new CoverPanel(loadDestinationImage(d, firstCat), iconFor(firstCat));
        photo.setPreferredSize(new Dimension(150, 95));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(0, 4, 0, 4));

        JPanel nameRow = new JPanel(new BorderLayout(6, 0));
        nameRow.setOpaque(false);
        nameRow.add(text(d.getName(), 105, 14, TEXT_DARK, true), BorderLayout.CENTER);
        nameRow.add(createBadge(DestinationRepository.label(category != null ? category : firstCat), false),
                BorderLayout.EAST);
        top.add(left(nameRow));
        top.add(Box.createVerticalStrut(3));
        top.add(left(label("📍 " + d.getProvince(), 12, false, TEXT_MUTED)));
        top.add(Box.createVerticalStrut(6));
        top.add(left(text(d.getDescription(), 190, 12, TEXT_DARK, false)));

        Pill view = new Pill("View Recommendations  →", true);
        view.setPreferredSize(new Dimension(150, 30));

        JPanel info = new JPanel(new BorderLayout(0, 8));
        info.setOpaque(false);
        info.add(top, BorderLayout.NORTH);
        info.add(view, BorderLayout.SOUTH);

        card.add(photo, BorderLayout.NORTH);
        card.add(info, BorderLayout.CENTER);

        card.setSelected(selected);
        makeClickable(card, () -> destinationListener.accept(d));
        return card;
    }

    private JPanel createActivityCard(DestinationModel d, String category, String activity) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(new LineBorder(BORDER_LIGHT, 1), new EmptyBorder(4, 4, 8, 4)));

        BufferedImage own = loadImage(clean(activity));
        BufferedImage img = own != null ? own : loadDestinationImage(d, category);
        CoverPanel photo = new CoverPanel(img, activityIcon(activity));
        photo.setPreferredSize(new Dimension(120, 62));

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(0, 4, 0, 4));
        body.add(left(text(activityIcon(activity) + " " + activity, 125, 12, TEXT_DARK, true)));
        body.add(Box.createVerticalStrut(3));
        body.add(left(text("Experience " + activity.toLowerCase() + " in " + d.getName() + ".",
                125, 11, TEXT_MUTED, false)));

        String difficulty = difficultyOf(activity);
        JPanel foot = new JPanel(new BorderLayout(4, 0));
        foot.setOpaque(false);
        foot.setBorder(new EmptyBorder(0, 4, 0, 4));
        foot.add(label("🕒 " + durationOf(activity), 11, false, TEXT_MUTED), BorderLayout.WEST);
        foot.add(createDifficultyBadge(difficulty), BorderLayout.EAST);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(body, BorderLayout.NORTH);

        card.add(photo, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);
        card.add(foot, BorderLayout.SOUTH);
        return card;
    }

    // =========================================================
    // SMALL UI HELPERS
    // =========================================================

    private JLabel divider() {
        JLabel l = new JLabel("|");
        l.setForeground(BORDER_LIGHT);
        l.setFont(new Font(FONT, Font.PLAIN, 22));
        return l;
    }

    private void makeClickable(SelectableCard card, Runnable onClick) {
        MouseAdapter adapter = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { onClick.run(); }
            @Override public void mouseEntered(MouseEvent e) { card.setHover(true); }
            @Override public void mouseExited(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), card);
                if (!card.contains(p)) {
                    card.setHover(false);
                }
            }
        };
        attach(card, adapter);
    }

    private void attach(Component c, MouseAdapter adapter) {
        c.addMouseListener(adapter);
        c.setCursor(new Cursor(Cursor.HAND_CURSOR));
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                attach(child, adapter);
            }
        }
    }

    private void refresh(JComponent c) {
        c.revalidate();
        c.repaint();
    }

    private <T extends JComponent> T left(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    private JLabel label(String text, int size, boolean bold, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(FONT, bold ? Font.BOLD : Font.PLAIN, size));
        l.setForeground(color);
        return l;
    }

    // wrapping text label (HTML so long text breaks into lines)
    private JLabel text(String text, int width, int size, Color color, boolean bold) {
        JLabel l = new JLabel("<html><body style='width:" + width + "px'>" + esc(text) + "</body></html>");
        l.setFont(new Font(FONT, bold ? Font.BOLD : Font.PLAIN, size));
        l.setForeground(color);
        return l;
    }

    private JLabel createBadge(String text, boolean large) {
        JLabel badge = new JLabel(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PANEL_TINT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setOpaque(false);
        badge.setFont(new Font(FONT, Font.BOLD, large ? 12 : 10));
        badge.setForeground(PRIMARY);
        badge.setBorder(new EmptyBorder(large ? 5 : 3, large ? 14 : 8, large ? 5 : 3, large ? 14 : 8));
        return badge;
    }

    private JLabel createDifficultyBadge(String difficulty) {
        final Color bg;
        final Color fg;
        switch (difficulty) {
            case "Hard":     bg = new Color(0xFDE3E1); fg = new Color(0xB42318); break;
            case "Moderate": bg = new Color(0xDFF3E8); fg = PRIMARY;             break;
            default:         bg = new Color(0xDFF3E8); fg = PRIMARY;             break;
        }
        JLabel badge = new JLabel(difficulty) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setOpaque(false);
        badge.setFont(new Font(FONT, Font.BOLD, 10));
        badge.setForeground(fg);
        badge.setBorder(new EmptyBorder(2, 9, 2, 9));
        return badge;
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String nz(String s) {
        return s == null ? "—" : s;
    }

    private static String clean(String name) {
        return name.replaceAll("[^A-Za-z0-9 _-]", "").trim();
    }

    private String iconFor(String category) {
        switch (category) {
            case "Beach":     return "🏝";
            case "Mountain":  return "⛰";
            case "City":      return "🏙";
            case "Adventure": return "🥾";
            case "Cultural":  return "🏛";
            default:          return "📍";
        }
    }

    // Picks an emoji from the activity name
    private String activityIcon(String a) {
        String s = a.toLowerCase();
        if (s.contains("photo"))                                   return "📸";
        if (s.contains("cloud"))                                   return "☁️";
        if (s.contains("sunrise") || s.contains("sunset"))         return "🌅";
        if (s.contains("surf"))                                    return "🏄";
        if (s.contains("dive") || s.contains("diving"))            return "🤿";
        if (s.contains("snorkel"))                                 return "🤿";
        if (s.contains("kayak"))                                   return "🛶";
        if (s.contains("island"))                                  return "🏝";
        if (s.contains("hot spring"))                              return "♨️";
        if (s.contains("waterfall"))                               return "💧";
        if (s.contains("cliff") || s.contains("canyoneering"))     return "🧗";
        if (s.contains("swim") || s.contains("lagoon"))            return "🏊";
        if (s.contains("camp"))                                    return "🏕";
        if (s.contains("hik") || s.contains("trek") || s.contains("trail") || s.contains("walking")) return "🥾";
        if (s.contains("cycl") || s.contains("biking"))            return "🚴";
        if (s.contains("atv") || s.contains("4x4") || s.contains("kalesa")) return "🚙";
        if (s.contains("zip") || s.contains("coaster") || s.contains("rides")) return "🎢";
        if (s.contains("bird"))                                    return "🐦";
        if (s.contains("turtle"))                                  return "🐢";
        if (s.contains("wildlife") || s.contains("tarsier"))       return "🐾";
        if (s.contains("food") || s.contains("tasting") || s.contains("restaurant")
                || s.contains("café") || s.contains("seafood") || s.contains("dining")) return "🍜";
        if (s.contains("shop") || s.contains("souvenir") || s.contains("crafts")) return "🛍";
        if (s.contains("night"))                                   return "🌃";
        if (s.contains("museum") || s.contains("art") || s.contains("heritage") || s.contains("church")
                || s.contains("historical") || s.contains("cultur") || s.contains("basilica")
                || s.contains("magellan") || s.contains("fort") || s.contains("coffin")) return "🏛";
        if (s.contains("cave"))                                    return "🕳";
        if (s.contains("view") || s.contains("sight"))             return "👀";
        return "📍";
    }

    // Estimated duration shown on an activity card (guessed from the activity name)
    private String durationOf(String a) {
        String s = a.toLowerCase();
        if (s.contains("camp"))                                     return "1-2 days";
        if (s.contains("island hopping"))                           return "4-8 hours";
        if (s.contains("hik") || s.contains("trek") || s.contains("trail")) return "3-6 hours";
        if (s.contains("dive") || s.contains("diving"))             return "2-4 hours";
        if (s.contains("surf") || s.contains("kayak") || s.contains("lagoon")) return "2-3 hours";
        if (s.contains("snorkel"))                                  return "1-3 hours";
        if (s.contains("swim"))                                     return "1-4 hours";
        if (s.contains("museum") || s.contains("church") || s.contains("heritage")
                || s.contains("historical") || s.contains("basilica") || s.contains("fort")) return "1-2 hours";
        if (s.contains("food") || s.contains("shop") || s.contains("night") || s.contains("dining")) return "1-3 hours";
        return "2-4 hours";
    }

    // Estimated difficulty shown on an activity card (guessed from the activity name)
    private String difficultyOf(String a) {
        String s = a.toLowerCase();
        if (s.contains("canyoneering") || s.contains("cliff") || s.contains("trek")) return "Hard";
        if (s.contains("hik") || s.contains("dive") || s.contains("diving") || s.contains("kayak")
                || s.contains("island hopping") || s.contains("camp") || s.contains("surf")
                || s.contains("cave") || s.contains("climb")) return "Moderate";
        return "Easy";
    }

    // =========================================================
    // IMAGES  (images/<name>.jpg|jpeg|png on classpath or project root)
    // =========================================================

    private BufferedImage loadDestinationImage(DestinationModel d, String category) {
        BufferedImage own = loadImage(clean(d.getName()));
        return own != null ? own : loadImage(category);
    }

    private BufferedImage loadImage(String name) {
        if (imageCache.containsKey(name)) {
            return imageCache.get(name);
        }
        BufferedImage result = null;
        for (String ext : new String[]{".jpg", ".jpeg", ".png"}) {
            String file = name + ext;
            try (InputStream in = RecommendationView.class.getResourceAsStream("/images/" + file)) {
                if (in != null) {
                    result = ImageIO.read(in);
                }
            } catch (IOException | IllegalArgumentException ignored) {
            }
            if (result == null) {
                File f = new File("images/" + file);
                if (f.exists()) {
                    try {
                        result = ImageIO.read(f);
                    } catch (IOException ignored) {
                    }
                }
            }
            if (result != null) {
                break;
            }
        }
        imageCache.put(name, result);
        return result;
    }

    // =========================================================
    // CUSTOM COMPONENTS
    // =========================================================

    // Scroll content that always matches the window width (no sideways scrolling)
    private static class ScrollPanel extends JPanel implements Scrollable {
        ScrollPanel(LayoutManager lm) { super(lm); }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height - 40; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    // White card with a green border + check mark when selected / hovered
    private static class SelectableCard extends JPanel {
        private final int pad;
        private boolean selected;
        private boolean hover;

        SelectableCard(LayoutManager layout, int pad) {
            super(layout);
            this.pad = pad;
            setBackground(Color.WHITE);
            updateBorder();
        }

        void setSelected(boolean selected) {
            this.selected = selected;
            updateBorder();
            repaint();
        }

        void setHover(boolean hover) {
            this.hover = hover;
            updateBorder();
        }

        private void updateBorder() {
            boolean strong = selected || hover;
            int t = strong ? 2 : 1;
            setBorder(new CompoundBorder(
                    new CompoundBorder(new EmptyBorder(2 - t, 2 - t, 2 - t, 2 - t),
                            new LineBorder(strong ? PRIMARY : BORDER_LIGHT, t)),
                    new EmptyBorder(pad, pad, pad, pad)));
        }

        @Override
        protected void paintChildren(Graphics g) {
            super.paintChildren(g);
            if (selected) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = 20;
                int x = getWidth() - size - 12;
                int y = 12;
                g2.setColor(Color.WHITE);
                g2.fillOval(x - 2, y - 2, size + 4, size + 4);
                g2.setColor(PRIMARY);
                g2.fillOval(x, y, size, size);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawPolyline(new int[]{x + 5, x + 9, x + 15}, new int[]{y + 10, y + 14, y + 6}, 3);
                g2.dispose();
            }
        }
    }

    // Paints a photo scaled to "cover" the panel, or a green gradient + emoji if no photo.
    private static class CoverPanel extends JPanel {
        private final BufferedImage source;
        private final String emoji;
        private BufferedImage cache;
        private int cw, ch;

        CoverPanel(BufferedImage source, String emoji) {
            this.source = source;
            this.emoji = emoji;
            setBackground(PRIMARY);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            if (source != null) {
                if (cache == null || cw != w || ch != h) {
                    cache = scaleCover(source, w, h);
                    cw = w;
                    ch = h;
                }
                g2.drawImage(cache, 0, 0, null);
            } else {
                g2.setPaint(new GradientPaint(0, 0, PRIMARY, w, h, PRIMARY_LIGHT));
                g2.fillRect(0, 0, w, h);
                g2.setFont(new Font(EMOJI_FONT, Font.PLAIN, Math.min(44, Math.max(20, h / 2))));
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(Color.WHITE);
                g2.drawString(emoji, (w - fm.stringWidth(emoji)) / 2, h / 2 + fm.getAscent() / 3);
            }
            g2.dispose();
        }

        private static BufferedImage scaleCover(BufferedImage img, int w, int h) {
            double scale = Math.max((double) w / img.getWidth(), (double) h / img.getHeight());
            int dw = (int) Math.ceil(img.getWidth() * scale);
            int dh = (int) Math.ceil(img.getHeight() * scale);
            BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = out.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.drawImage(img, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
            g2.dispose();
            return out;
        }
    }

    // Rounded "button" drawn as a label (the whole card is the click target)
    private static class Pill extends JLabel {
        private boolean filled;

        Pill(String text, boolean filled) {
            super(text, SwingConstants.CENTER);
            setFont(new Font(FONT, Font.BOLD, 12));
            setBorder(new EmptyBorder(6, 12, 6, 12));
            setOpaque(false);
            setFilled(filled);
        }

        void setFilled(boolean filled) {
            this.filled = filled;
            setForeground(filled ? Color.WHITE : PRIMARY);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (filled) {
                g2.setColor(PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
            } else {
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(PRIMARY);
                g2.setStroke(new BasicStroke(1.6f));
                g2.draw(new RoundRectangle2D.Float(0.8f, 0.8f, getWidth() - 1.6f, getHeight() - 1.6f, 6, 6));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Real clickable rounded button (filled or outlined)
    private static class ActionButton extends JButton {
        private final boolean filled;

        ActionButton(String text, boolean filled) {
            super(text);
            this.filled = filled;
            setForeground(filled ? Color.WHITE : PRIMARY);
            setFont(new Font(FONT, Font.BOLD, 13));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(10, 18, 10, 18));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean over = getModel().isRollover();
            if (filled) {
                g2.setColor(over ? PRIMARY_LIGHT : PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
            } else {
                g2.setColor(over ? PANEL_TINT : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(PRIMARY);
                g2.setStroke(new BasicStroke(1.6f));
                g2.draw(new RoundRectangle2D.Float(0.8f, 0.8f, getWidth() - 1.6f, getHeight() - 1.6f, 6, 6));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Flat text button for the top bar
    private static class NavButton extends JButton {
        NavButton(String text) {
            super(text);
            setForeground(TEXT_DARK);
            setFont(new Font(FONT, Font.PLAIN, 14));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(8, 14, 8, 14));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { setForeground(PRIMARY); }
                @Override public void mouseExited(MouseEvent e)  { setForeground(TEXT_DARK); }
            });
        }
    }

    // Quick test: run this class directly
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RecommendationView view = new RecommendationView();
            new RecommendationController(view);
            view.setVisible(true);
        });
    }
}