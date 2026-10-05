package View;

import Model.Destination;
import Repository.DestinationRepository;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The "explore" part of TravelMatch, shown inside HomeView:
 *   CATEGORIES (for a region) -> DESTINATION LIST -> RECOMMENDATION DETAIL
 * It only displays what the controller gives it.
 */
public class ExplorePanel extends JPanel {

    private static final String FONT = "Segoe UI";
    private static final String EMOJI_FONT = "Segoe UI Emoji";

    private static final Color PRIMARY = new Color(0x1D9E75);
    private static final Color PRIMARY_LIGHT = new Color(0x2EC4A0);
    private static final Color BADGE_BG = new Color(0xE3F5EE);
    private static final Color BACKGROUND = new Color(0xF1F5F9);
    private static final Color TEXT_DARK = new Color(0x0F2A43);
    private static final Color TEXT_MUTED = new Color(0x7A8CA8);
    private static final Color BORDER_LIGHT = new Color(0xE2E8F0);

    private static final String PAGE_CATEGORIES = "CATEGORIES";
    private static final String PAGE_LIST = "LIST";
    private static final String PAGE_DETAIL = "DETAIL";

    private final CardLayout layout = new CardLayout();
    private final JPanel pages = new JPanel(layout);
    private final Map<String, BufferedImage> imageCache = new HashMap<>();

    // top bar
    private final JButton btnBack = new PillButton("← Back", Color.WHITE, new Color(0xF1F5F9), TEXT_DARK, BORDER_LIGHT);
    private final JButton btnTrip = new PillButton("My Trip (0)", PRIMARY, PRIMARY_LIGHT, Color.WHITE, null);
    private final JLabel lblCrumb = new JLabel(" ");

    // categories page
    private final JLabel lblCatTitle = new JLabel();
    private final JLabel lblCatSub = new JLabel();
    private final JPanel catGrid = new JPanel(new GridLayout(0, 3, 22, 22));
    private JScrollPane catScroll;

    // destination list page
    private final JLabel lblListTitle = new JLabel();
    private final JLabel lblListSub = new JLabel();
    private final JPanel listGrid = new JPanel(new GridLayout(0, 3, 22, 22));
    private JScrollPane listScroll;

    // detail page
    private final JPanel detailContent = new JPanel();
    private JScrollPane detailScroll;

    // trip planner (in memory)
    private final Set<String> trip = new LinkedHashSet<>();

    private Consumer<String> categoryListener = c -> {};
    private Consumer<Destination> destinationListener = d -> {};

    public ExplorePanel() {
        setLayout(new BorderLayout());
        setBackground(BACKGROUND);

        add(createTopBar(), BorderLayout.NORTH);

        pages.setBackground(BACKGROUND);
        catScroll = buildGridPage(lblCatTitle, lblCatSub, catGrid);
        listScroll = buildGridPage(lblListTitle, lblListSub, listGrid);

        detailContent.setLayout(new BoxLayout(detailContent, BoxLayout.Y_AXIS));
        detailContent.setBackground(BACKGROUND);
        JPanel detailWrap = new JPanel(new BorderLayout());
        detailWrap.setBackground(BACKGROUND);
        detailWrap.add(detailContent, BorderLayout.NORTH);
        detailScroll = wrap(detailWrap);

        pages.add(catScroll, PAGE_CATEGORIES);
        pages.add(listScroll, PAGE_LIST);
        pages.add(detailScroll, PAGE_DETAIL);
        add(pages, BorderLayout.CENTER);

        btnTrip.addActionListener(e -> showTrip());
    }

    // =========================================================
    // PUBLIC API (used by the controller)
    // =========================================================

    public void addBackListener(ActionListener l)               { btnBack.addActionListener(l); }
    public void addCategoryListener(Consumer<String> l)         { categoryListener = l; }
    public void addDestinationListener(Consumer<Destination> l) { destinationListener = l; }
    public Set<String> getTripItems()                           { return trip; }

    /** REGION -> CATEGORY: five category cards for the chosen region. */
    public void showCategories(String region, Map<String, Integer> counts) {
        lblCatTitle.setText("Explore " + region);
        lblCatSub.setText("Choose a category to see destinations in " + region + ".");
        lblCrumb.setText(region);

        catGrid.removeAll();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            catGrid.add(createCategoryCard(e.getKey(), e.getValue()));
        }
        refresh(catGrid);
        layout.show(pages, PAGE_CATEGORIES);
        toTop(catScroll);
    }

    /**
     * CATEGORY -> DESTINATION: cards for the given list.
     * category may be null (search results); then activities of all categories are shown.
     */
    public void showDestinations(String title, String subtitle, String crumb,
                                 List<Destination> list, String category) {
        lblListTitle.setText(title);
        lblListSub.setText(subtitle);
        lblCrumb.setText(crumb);

        listGrid.removeAll();
        if (list.isEmpty()) {
            JLabel empty = new JLabel("No destinations found.");
            empty.setFont(new Font(FONT, Font.PLAIN, 16));
            empty.setForeground(TEXT_MUTED);
            listGrid.add(empty);
        }
        for (Destination d : list) {
            listGrid.add(createDestinationCard(d, category));
        }
        refresh(listGrid);
        layout.show(pages, PAGE_LIST);
        toTop(listScroll);
    }

    /** DESTINATION -> activities, places, travel info and "why recommended". */
    public void showDetail(Destination d, String category) {
        String cat = category != null ? category : d.getCategories().get(0);

        lblCrumb.setText(d.getRegion() + "  ›  " + DestinationRepository.label(cat) + "  ›  " + d.getName());

        detailContent.removeAll();
        detailContent.setBorder(new EmptyBorder(0, 0, 30, 0));

        detailContent.add(left(createHero(d, cat)));

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(BACKGROUND);
        body.setBorder(new EmptyBorder(20, 30, 0, 30));

        // score + match badge + description
        JPanel scoreRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        scoreRow.setOpaque(false);
        scoreRow.add(createBadge("⭐ TravelMatch Recommendation Score: " + d.getScore() + "/5"));
        scoreRow.add(createBadge("✓ Great Match for You"));
        body.add(left(scoreRow));
        body.add(Box.createVerticalStrut(10));
        body.add(left(text(d.getDescription(), 900, 16, TEXT_DARK, false)));

        // Recommended activities (only this destination + category)
        body.add(Box.createVerticalStrut(24));
        body.add(left(sectionTitle("Recommended Activities")));
        body.add(Box.createVerticalStrut(10));
        JPanel actGrid = new JPanel(new GridLayout(0, 3, 18, 18));
        actGrid.setOpaque(false);
        for (String activity : d.getActivities(cat)) {
            actGrid.add(createActivityCard(d, activity));
        }
        body.add(left(actGrid));

        // Recommended places
        body.add(Box.createVerticalStrut(24));
        body.add(left(sectionTitle("Recommended Places")));
        body.add(Box.createVerticalStrut(10));
        JPanel placeGrid = new JPanel(new GridLayout(0, 3, 18, 18));
        placeGrid.setOpaque(false);
        for (String place : d.getPlaces()) {
            placeGrid.add(createPlaceCard(place));
        }
        body.add(left(placeGrid));

        // Travel information
        body.add(Box.createVerticalStrut(24));
        body.add(left(sectionTitle("Travel Information")));
        body.add(Box.createVerticalStrut(10));
        JPanel info = new JPanel(new GridLayout(1, 4, 18, 0));
        info.setOpaque(false);
        info.add(infoBox("Best Time to Visit", d.getBestTime()));
        info.add(infoBox("Recommended Duration", d.getDuration()));
        info.add(infoBox("Estimated Budget", d.getBudget()));
        info.add(infoBox("Difficulty", d.getDifficulty()));
        body.add(left(info));

        body.add(Box.createVerticalStrut(18));
        JPanel lists = new JPanel(new GridLayout(1, 2, 18, 0));
        lists.setOpaque(false);
        lists.add(listBox("Travel Tips", DestinationRepository.travelTips(cat)));
        lists.add(listBox("What to Bring", DestinationRepository.whatToBring(cat)));
        body.add(left(lists));

        // Why TravelMatch recommends this
        body.add(Box.createVerticalStrut(24));
        body.add(left(createWhyBox(d, cat)));

        detailContent.add(left(body));
        refresh(detailContent);
        layout.show(pages, PAGE_DETAIL);
        toTop(detailScroll);
    }

    // =========================================================
    // TOP BAR
    // =========================================================

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout(14, 0));
        bar.setBackground(Color.WHITE);
        bar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER_LIGHT),
                new EmptyBorder(10, 25, 10, 25)));

        lblCrumb.setFont(new Font(FONT, Font.BOLD, 15));
        lblCrumb.setForeground(TEXT_MUTED);

        bar.add(btnBack, BorderLayout.WEST);
        bar.add(lblCrumb, BorderLayout.CENTER);
        bar.add(btnTrip, BorderLayout.EAST);
        return bar;
    }

    private void showTrip() {
        String message = trip.isEmpty()
                ? "Your trip is empty. Open a destination and press \"Add to My Trip\"."
                : "My Trip:\n\n• " + String.join("\n• ", trip);
        JOptionPane.showMessageDialog(this, message, "My Trip", JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateTripButton() {
        btnTrip.setText("My Trip (" + trip.size() + ")");
    }

    // =========================================================
    // PAGE BUILDING
    // =========================================================

    private JScrollPane buildGridPage(JLabel title, JLabel sub, JPanel grid) {
        title.setFont(new Font(FONT, Font.BOLD, 30));
        title.setForeground(TEXT_DARK);
        sub.setFont(new Font(FONT, Font.PLAIN, 15));
        sub.setForeground(TEXT_MUTED);

        grid.setBackground(BACKGROUND);

        JPanel head = new JPanel();
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setOpaque(false);
        head.setBorder(new EmptyBorder(0, 0, 16, 0));
        head.add(left(title));
        head.add(Box.createVerticalStrut(4));
        head.add(left(sub));

        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(BACKGROUND);
        page.setBorder(new EmptyBorder(22, 30, 25, 30));
        page.add(head, BorderLayout.NORTH);

        JPanel gridWrap = new JPanel(new BorderLayout());
        gridWrap.setOpaque(false);
        gridWrap.add(grid, BorderLayout.NORTH);
        page.add(gridWrap, BorderLayout.CENTER);

        return wrap(page);
    }

    private JScrollPane wrap(JComponent view) {
        JScrollPane scroll = new JScrollPane(view);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    private void refresh(JComponent c) {
        c.revalidate();
        c.repaint();
    }

    private void toTop(JScrollPane scroll) {
        SwingUtilities.invokeLater(() -> scroll.getViewport().setViewPosition(new Point(0, 0)));
    }

    private <T extends JComponent> T left(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    // =========================================================
    // CARDS
    // =========================================================

    private JPanel createCategoryCard(String category, int count) {
        JPanel card = baseCard();
        card.setPreferredSize(new Dimension(200, 300));

        CoverPanel photo = new CoverPanel(loadImage(category), iconFor(category), null);
        photo.setPreferredSize(new Dimension(200, 190));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(Color.WHITE);
        info.setBorder(new EmptyBorder(12, 16, 14, 16));

        info.add(left(label(iconFor(category) + "  " + DestinationRepository.label(category), 20, true, TEXT_DARK)));
        info.add(Box.createVerticalStrut(4));
        info.add(left(text(DestinationRepository.categoryDescription(category), 240, 13, TEXT_MUTED, false)));
        info.add(Box.createVerticalStrut(6));
        info.add(left(label(count + (count == 1 ? " destination" : " destinations"), 13, true, PRIMARY)));

        card.add(photo, BorderLayout.CENTER);
        card.add(info, BorderLayout.SOUTH);

        makeClickable(card, () -> categoryListener.accept(category));
        return card;
    }

    private JPanel createDestinationCard(Destination d, String category) {
        JPanel card = baseCard();
        card.setPreferredSize(new Dimension(200, 430));

        String firstCat = d.getCategories().get(0);
        CoverPanel photo = new CoverPanel(loadDestinationImage(d, firstCat), iconFor(firstCat), null);
        photo.setPreferredSize(new Dimension(200, 150));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(Color.WHITE);
        info.setBorder(new EmptyBorder(12, 16, 14, 16));

        info.add(left(text(d.getName(), 240, 17, TEXT_DARK, true)));
        info.add(Box.createVerticalStrut(2));
        info.add(left(label("📍 " + d.getProvince(), 13, false, TEXT_MUTED)));
        info.add(Box.createVerticalStrut(8));
        info.add(left(text(d.getDescription(), 240, 13, TEXT_DARK, false)));
        info.add(Box.createVerticalStrut(8));

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        badges.setOpaque(false);
        for (String c : d.getCategories()) {
            badges.add(createBadge(DestinationRepository.label(c)));
        }
        info.add(left(badges));

        info.add(Box.createVerticalStrut(10));
        info.add(left(label("Recommended Activities:", 13, true, TEXT_DARK)));
        List<String> acts = d.getActivities(category);
        for (int i = 0; i < Math.min(3, acts.size()); i++) {
            info.add(left(label(activityIcon(acts.get(i)) + " " + acts.get(i), 13, false, TEXT_MUTED)));
        }
        info.add(Box.createVerticalGlue());
        info.add(Box.createVerticalStrut(8));
        info.add(left(label("View Recommendations →", 14, true, PRIMARY)));

        card.add(photo, BorderLayout.NORTH);
        card.add(info, BorderLayout.CENTER);

        makeClickable(card, () -> destinationListener.accept(d));
        return card;
    }

    private JPanel createActivityCard(Destination d, String activity) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(new LineBorder(BORDER_LIGHT, 1), new EmptyBorder(14, 16, 14, 16)));

        JLabel icon = new JLabel(activityIcon(activity));
        icon.setFont(new Font(EMOJI_FONT, Font.PLAIN, 30));

        String key = d.getName() + " — " + activity;
        JButton add = new PillButton(trip.contains(key) ? "✓ Added" : "Add to My Trip",
                PRIMARY, PRIMARY_LIGHT, Color.WHITE, null);
        add.addActionListener(e -> {
            if (trip.remove(key)) {
                add.setText("Add to My Trip");
            } else {
                trip.add(key);
                add.setText("✓ Added");
            }
            updateTripButton();
        });

        card.add(left(icon));
        card.add(Box.createVerticalStrut(6));
        card.add(left(text(activity, 220, 16, TEXT_DARK, true)));
        card.add(Box.createVerticalStrut(10));
        card.add(left(add));
        return card;
    }

    private JPanel createPlaceCard(String place) {
        String[] parts = place.split("\\|", 2);
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(new LineBorder(BORDER_LIGHT, 1), new EmptyBorder(14, 16, 14, 16)));

        card.add(left(label("📍", 22, false, PRIMARY)));
        card.add(Box.createVerticalStrut(4));
        card.add(left(text(parts[0], 220, 16, TEXT_DARK, true)));
        if (parts.length > 1) {
            card.add(Box.createVerticalStrut(4));
            card.add(left(text(parts[1], 220, 13, TEXT_MUTED, false)));
        }
        return card;
    }

    private JPanel createHero(Destination d, String category) {
        CoverPanel hero = new CoverPanel(loadDestinationImage(d, category), iconFor(category), new Color(0, 0, 0, 90));
        hero.setLayout(new BorderLayout());
        hero.setPreferredSize(new Dimension(100, 230));
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 230));

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        text.setBorder(new EmptyBorder(0, 30, 22, 30));

        JLabel title = label(d.getName(), 38, true, Color.WHITE);
        JLabel place = label("📍 " + d.getProvince(), 17, false, Color.WHITE);
        text.add(left(title));
        text.add(Box.createVerticalStrut(4));
        text.add(left(place));

        hero.add(text, BorderLayout.SOUTH);
        return hero;
    }

    private JPanel createWhyBox(Destination d, String category) {
        List<String> acts = d.getActivities(category);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(4, acts.size()); i++) {
            if (i > 0) {
                sb.append(i == Math.min(4, acts.size()) - 1 ? ", and " : ", ");
            }
            sb.append(acts.get(i).toLowerCase());
        }
        String why = d.getName() + " is an excellent match for travelers interested in "
                + DestinationRepository.label(category).toLowerCase()
                + " experiences, including " + sb + ".";

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(BADGE_BG);
        box.setBorder(new CompoundBorder(new LineBorder(PRIMARY, 1), new EmptyBorder(16, 20, 16, 20)));

        box.add(left(label("Why TravelMatch Recommends " + d.getName(), 20, true, TEXT_DARK)));
        box.add(Box.createVerticalStrut(8));
        box.add(left(text("“" + why + "”", 900, 15, TEXT_DARK, false)));
        box.add(Box.createVerticalStrut(10));
        box.add(left(createBadge("✓ Great Match for You")));
        return box;
    }

    private JPanel infoBox(String title, String value) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(Color.WHITE);
        box.setBorder(new CompoundBorder(new LineBorder(BORDER_LIGHT, 1), new EmptyBorder(12, 16, 12, 16)));
        box.add(left(label(title, 12, true, TEXT_MUTED)));
        box.add(Box.createVerticalStrut(4));
        box.add(left(text(value == null ? "—" : value, 200, 15, TEXT_DARK, true)));
        return box;
    }

    private JPanel listBox(String title, List<String> items) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(Color.WHITE);
        box.setBorder(new CompoundBorder(new LineBorder(BORDER_LIGHT, 1), new EmptyBorder(14, 18, 14, 18)));
        box.add(left(label(title, 16, true, TEXT_DARK)));
        box.add(Box.createVerticalStrut(6));
        for (String item : items) {
            box.add(left(text("• " + item, 420, 14, TEXT_DARK, false)));
        }
        return box;
    }

    // =========================================================
    // SMALL UI HELPERS
    // =========================================================

    private JPanel baseCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(cardBorder(false));
        return card;
    }

    private Border cardBorder(boolean hover) {
        if (hover) {
            return new LineBorder(PRIMARY, 2);
        }
        return new CompoundBorder(new EmptyBorder(1, 1, 1, 1), new LineBorder(BORDER_LIGHT, 1));
    }

    private void makeClickable(JPanel card, Runnable onClick) {
        MouseAdapter adapter = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { onClick.run(); }
            @Override public void mouseEntered(MouseEvent e) { card.setBorder(cardBorder(true)); }
            @Override public void mouseExited(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), card);
                if (!card.contains(p)) {
                    card.setBorder(cardBorder(false));
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

    private JLabel sectionTitle(String text) {
        return label(text, 24, true, TEXT_DARK);
    }

    private JLabel createBadge(String text) {
        JLabel badge = new JLabel(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BADGE_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setOpaque(false);
        badge.setFont(new Font(FONT, Font.BOLD, 12));
        badge.setForeground(PRIMARY);
        badge.setBorder(new EmptyBorder(4, 12, 4, 12));
        return badge;
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String iconFor(String category) {
        switch (category) {
            case "Beach":     return "🏖";
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
        if (s.contains("island hopping") || s.contains("island"))  return "🏝";
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

    // =========================================================
    // IMAGES  (images/<name>.jpg|jpeg|png on classpath or project root)
    // =========================================================

    // Destination photo = images/<Destination name>.jpg (symbols removed),
    // otherwise falls back to the category photo, then to a gradient.
    private BufferedImage loadDestinationImage(Destination d, String category) {
        BufferedImage own = loadImage(d.getName().replaceAll("[^A-Za-z0-9 _-]", "").trim());
        return own != null ? own : loadImage(category);
    }

    private BufferedImage loadImage(String name) {
        if (imageCache.containsKey(name)) {
            return imageCache.get(name);
        }
        BufferedImage result = null;
        for (String ext : new String[]{".jpg", ".jpeg", ".png"}) {
            String file = name + ext;
            try (InputStream in = ExplorePanel.class.getResourceAsStream("/images/" + file)) {
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

    // Paints a photo scaled to "cover" the panel, or a green gradient + emoji if no photo.
    private static class CoverPanel extends JPanel {
        private final BufferedImage source;
        private final String emoji;
        private final Color overlay;
        private BufferedImage cache;
        private int cw, ch;

        CoverPanel(BufferedImage source, String emoji, Color overlay) {
            this.source = source;
            this.emoji = emoji;
            this.overlay = overlay;
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
                g2.setFont(new Font(EMOJI_FONT, Font.PLAIN, 44));
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(Color.WHITE);
                g2.drawString(emoji, (w - fm.stringWidth(emoji)) / 2, h / 2 + fm.getAscent() / 3);
            }
            if (overlay != null) {
                g2.setColor(overlay);
                g2.fillRect(0, 0, w, h);
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

    // Rounded button with hover color
    private static class PillButton extends JButton {
        private final Color bg, bgHover, outline;

        PillButton(String text, Color bg, Color bgHover, Color fg, Color outline) {
            super(text);
            this.bg = bg;
            this.bgHover = bgHover;
            this.outline = outline;
            setForeground(fg);
            setFont(new Font(FONT, Font.BOLD, 13));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(9, 18, 9, 18));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getModel().isRollover() ? bgHover : bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            if (outline != null) {
                g2.setColor(outline);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}