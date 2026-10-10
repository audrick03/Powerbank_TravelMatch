package view;

import model.*;
import repository.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The "explore" part of TravelMatch, shown inside HomeView:
 *   CATEGORIES (for a region) -> DESTINATION LIST -> RECOMMENDATION DETAIL
 * It only displays what the controller gives it.
 */
public class ExplorePanel extends JPanel {

    private final ReviewRepository reviewRepository = new ReviewRepository();

    private static final String FONT = "Segoe UI";
    private static final String SYMBOLS = "[^A-Za-z0-9 _-]";
    private static final String[] IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png"};

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
    private static final String PAGE_REVIEW = "REVIEW";
    private static final Color STAR_GOLD = new Color(0xFFB300);

    private final CardLayout layout = new CardLayout();
    private final JPanel pages = new JPanel(layout);
    private final Map<String, BufferedImage> imageCache = new HashMap<>();

    // top bar
    private final JButton btnBack = new PillButton("← Back", Color.WHITE, new Color(0xF1F5F9), TEXT_DARK, BORDER_LIGHT);
    private final JLabel lblCrumb = new JLabel(" ");

    // categories page
    private final JLabel lblCatTitle = new JLabel();
    private final JLabel lblCatSub = new JLabel();
    private final JPanel catGrid =new JPanel();
    private JScrollPane catScroll;

    // destination list page
    private final JLabel lblListTitle = new JLabel();
    private final JLabel lblListSub = new JLabel();
    private final JPanel listGrid =new JPanel();
    private JScrollPane listScroll;

    // detail page (the Review button lives in its header, so it only shows here)
    private final JButton btnReview = new PillButton("Review", PRIMARY, PRIMARY_LIGHT, Color.WHITE, null);
    private final ViewportWidthPanel detailContent = new ViewportWidthPanel(new BorderLayout());
    private JScrollPane detailScroll;

    // review page: destination header + the review panel directly below it
    private final JPanel reviewContent = new JPanel();
    private JScrollPane reviewScroll;

    private transient Consumer<String> categoryListener = c -> {};
    private transient Consumer<DestinationModel> destinationListener = d -> {};

    public ExplorePanel() {
        setLayout(new BorderLayout());
        setBackground(BACKGROUND);

        add(createTopBar(), BorderLayout.NORTH);

        pages.setBackground(BACKGROUND);
        catScroll = buildGridPage(lblCatTitle, lblCatSub, catGrid);
        listScroll = buildGridPage(lblListTitle, lblListSub, listGrid);

        detailContent.setLayout(new BoxLayout(detailContent, BoxLayout.Y_AXIS));
        detailContent.setBackground(BACKGROUND);
        detailContent.setLayout(new BoxLayout(detailContent, BoxLayout.Y_AXIS));
        detailContent.setBackground(BACKGROUND);
        detailScroll = wrap(detailContent);

        reviewContent.setLayout(new BoxLayout(reviewContent, BoxLayout.Y_AXIS));
        reviewContent.setBackground(BACKGROUND);
        JPanel reviewWrap = new ViewportWidthPanel(new BorderLayout());
        reviewWrap.setBackground(BACKGROUND);
        reviewWrap.add(reviewContent, BorderLayout.NORTH);
        reviewScroll = wrap(reviewWrap);

        pages.add(catScroll, PAGE_CATEGORIES);
        pages.add(listScroll, PAGE_LIST);
        pages.add(detailScroll, PAGE_DETAIL);
        pages.add(reviewScroll, PAGE_REVIEW);
        add(pages, BorderLayout.CENTER);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateResponsiveLayout();
            }
        });
        updateResponsiveLayout();
    }

    // =========================================================
    // PUBLIC API (used by the controller)
    // =========================================================

    public void addBackListener(ActionListener l)               { btnBack.addActionListener(l); }
    public void addCategoryListener(Consumer<String> l)         { categoryListener = l; }
    public void addDestinationListener(Consumer<DestinationModel> l) { destinationListener = l; }
    public void addReviewListener(ActionListener l)             { btnReview.addActionListener(l); }

    /** REGION -> CATEGORY: five category cards for the chosen region. */
    public void showCategories(String region, Map<String, Integer> counts) {
        lblCatTitle.setText("Explore " + region);
        lblCatSub.setText("Choose a category to see destinations in " + region + ".");
        lblCrumb.setText(region);

        catGrid.removeAll();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            catGrid.add(createCategoryCard(e.getKey(), e.getValue()));
        }
        updateResponsiveLayout();
        refresh(catGrid);
        layout.show(pages, PAGE_CATEGORIES);
        toTop(catScroll);
    }

    /**
     * CATEGORY -> DESTINATION: cards for the given list.
     * category may be null (search results); then activities of all categories are shown.
     */
    public void showDestinations(String title, String subtitle, String crumb,
                                 List<DestinationModel> list, String category) {
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
        for (DestinationModel d : list) {
            listGrid.add(createDestinationCard(d, category));
        }
        updateResponsiveLayout();
        refresh(listGrid);
        layout.show(pages, PAGE_LIST);
        toTop(listScroll);
    }

    /** DESTINATION -> places and travel info. */
    public void showDetail(DestinationModel d, String category) {
        String cat = category != null ? category : d.getCategories().get(0);

        lblCrumb.setText(d.getRegion() + "  ›  " + DestinationRepository.label(cat) + "  ›  " + d.getName());

        detailContent.removeAll();
        detailContent.setBorder(new EmptyBorder(0, 0, 30, 0));

        detailContent.add(left(createHero(d, cat, true)));

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(BACKGROUND);
        body.setBorder(new EmptyBorder(20, 30, 0, 30));
        body.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // score + description
        JPanel scoreRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        scoreRow.setOpaque(false);
        double rating = reviewRepository.getAverageRating(d.getName());
        int reviewCount = reviewRepository.getReviewCount(d.getName());
        String ratingText;
        if (reviewCount == 0) {
            ratingText = "User Reviews: 0/5";
        } else {
            ratingText = String.format(java.util.Locale.US, "User Reviews: %.1f/5 (%d)", rating, reviewCount);
        }
        scoreRow.add(createRatingBadge(ratingText));
        if (reviewCount == 0) {
            JLabel emptyReview=label("No reviews yet", 14, false, TEXT_MUTED);
            body.add(left(emptyReview));

        }
        body.add(left(scoreRow));
        body.add(Box.createVerticalStrut(10));
        JLabel description = text(d.getDescription(), 900, 16, TEXT_DARK, false);
        body.add(left(description));

        // Recommended places
        body.add(Box.createVerticalStrut(24));
        body.add(left(sectionTitle("Recommended Places")));
        body.add(Box.createVerticalStrut(10));
        JPanel placeGrid = new JPanel(new GridLayout(0, 3, 18, 18));
        placeGrid.setOpaque(false);
        for (String place : d.getPlaces()) {
            placeGrid.add(createPlaceCard(place));
        }
        placeGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        body.add(left(placeGrid));

        // Travel information
        body.add(Box.createVerticalStrut(24));
        body.add(left(sectionTitle("Travel Information")));
        body.add(Box.createVerticalStrut(10));
        JPanel info = new JPanel(new GridLayout(0, 4, 18, 12));
        info.setOpaque(false);
        info.add(infoBox("Best Time to Visit", d.getBestTime()));
        info.add(infoBox("Recommended Duration", d.getDuration()));
        info.add(infoBox("Estimated Budget", d.getBudget()));
        info.add(infoBox("Difficulty", d.getDifficulty()));
        info.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        body.add(left(info));

        body.add(Box.createVerticalStrut(18));
        JPanel lists = new JPanel(new GridLayout(0, 2, 18, 12));
        lists.setOpaque(false);
        List<String> travelTips = d.getTravelTips().isEmpty()
                ? DestinationRepository.travelTips(cat) : d.getTravelTips();
        List<String> whatToBring = d.getWhatToBring().isEmpty()
                ? DestinationRepository.whatToBring(cat) : d.getWhatToBring();
        lists.add(listBox("Travel Tips", travelTips));
        lists.add(listBox("What to Bring", whatToBring));
        lists.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        body.add(left(lists));

        detailContent.add(left(body));
        detailScroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                updateDetailLayout(detailScroll.getViewport().getWidth(), description,
                        d.getDescription(), placeGrid, info, lists);
            }
        });
        refresh(detailContent);
        layout.show(pages, PAGE_DETAIL);
        toTop(detailScroll);
        SwingUtilities.invokeLater(() -> updateDetailLayout(
                detailScroll.getViewport().getWidth(), description, d.getDescription(),
                placeGrid, info, lists));
    }

    /** DESTINATION -> its reviews, shown directly below the destination header. */
    public void showReviews(DestinationModel d, String category, JComponent reviewPanel) {
        String cat = category != null ? category : d.getCategories().get(0);

        lblCrumb.setText(d.getRegion() + "  ›  " + DestinationRepository.label(cat)
                + "  ›  " + d.getName() + "  ›  Reviews");

        reviewContent.removeAll();
        reviewContent.setBorder(new EmptyBorder(0, 0, 30, 0));
        reviewContent.add(left(createHero(d, cat, false)));

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(BACKGROUND);
        body.setBorder(new EmptyBorder(20, 30, 0, 30));
        body.add(reviewPanel, BorderLayout.CENTER);
        reviewContent.add(left(body));

        refresh(reviewContent);
        layout.show(pages, PAGE_REVIEW);
        toTop(reviewScroll);
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
        return bar;
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
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
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
        card.setPreferredSize(new Dimension(220, 300));

        CoverPanel photo = new CoverPanel(loadImage(category), null);
        photo.setPreferredSize(new Dimension(200, 190));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(Color.WHITE);
        info.setBorder(new EmptyBorder(12, 16, 14, 16));

        info.add(left(label(DestinationRepository.label(category), 20, true, TEXT_DARK)));
        info.add(Box.createVerticalStrut(4));
        JLabel description = text(DestinationRepository.categoryDescription(category), 200, 13, TEXT_MUTED, false);
        info.add(left(description));
        info.add(Box.createVerticalStrut(6));
        info.add(left(label(count + (count == 1 ? " destination" : " destinations"), 13, true, PRIMARY)));

        card.add(photo, BorderLayout.CENTER);
        card.add(info, BorderLayout.SOUTH);
        card.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent event) {
                setWrappedLabelWidth(description, DestinationRepository.categoryDescription(category),
                        Math.max(100, info.getWidth() - 32));
            }
        });

        makeClickable(card, () -> categoryListener.accept(category));
        return card;
    }

    private JPanel createDestinationCard(DestinationModel d, String category) {
        JPanel card = baseCard();
        card.setPreferredSize(new Dimension(220, 350));

        String firstCat = d.getCategories().get(0);
        CoverPanel photo = new CoverPanel(loadDestinationImage(d, firstCat), null);
        photo.setPreferredSize(new Dimension(200, 150));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(Color.WHITE);
        info.setBorder(new EmptyBorder(12, 16, 14, 16));

        JLabel name = text(d.getName(), 200, 17, TEXT_DARK, true);
        info.add(left(name));
        info.add(Box.createVerticalStrut(2));
        info.add(left(label("" + d.getProvince(), 13, false, TEXT_MUTED)));
        info.add(Box.createVerticalStrut(8));
        JLabel description = text(d.getDescription(), 200, 13, TEXT_DARK, false);
        info.add(left(description));
        info.add(Box.createVerticalStrut(8));

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        badges.setOpaque(false);
        for (String c : d.getCategories()) {
            badges.add(createBadge(DestinationRepository.label(c)));
        }
        info.add(left(badges));

        info.add(Box.createVerticalGlue());
        info.add(Box.createVerticalStrut(8));
        info.add(left(label("View Recommendations →", 14, true, PRIMARY)));

        card.add(photo, BorderLayout.NORTH);
        card.add(info, BorderLayout.CENTER);
        card.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent event) {
                int textWidth = Math.max(100, info.getWidth() - 32);
                setWrappedLabelWidth(name, d.getName(), textWidth);
                setWrappedLabelWidth(description, d.getDescription(), textWidth);
            }
        });

        makeClickable(card, () -> destinationListener.accept(d));
        return card;
    }

    private JPanel createPlaceCard(String place) {
        String[] parts = place.split("\\|", 2);
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(new LineBorder(BORDER_LIGHT, 1), new EmptyBorder(14, 16, 14, 16)));

        // Place photo: images/<Place name>.jpg (hidden if there is none)
        BufferedImage placeImg = loadImage(parts[0].replaceAll(SYMBOLS, "").trim());
        CoverPanel placePhoto = new CoverPanel(placeImg, null);
        placePhoto.setPreferredSize(new Dimension(200, 90));
        placePhoto.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        placePhoto.setVisible(placeImg != null);
        card.add(left(placePhoto));
        card.add(Box.createVerticalStrut(4));
        JLabel name = text(parts[0], 190, 16, TEXT_DARK, true);
        card.add(left(name));
        if (parts.length > 1) {
            card.add(Box.createVerticalStrut(4));
            JLabel description = text(parts[1], 190, 13, TEXT_MUTED, false);
            card.add(left(description));
            card.addComponentListener(new ComponentAdapter() {
                @Override public void componentResized(ComponentEvent event) {
                    int textWidth = Math.max(100, card.getWidth() - 32);
                    setWrappedLabelWidth(name, parts[0], textWidth);
                    setWrappedLabelWidth(description, parts[1], textWidth);
                }
            });
        } else {
            card.addComponentListener(new ComponentAdapter() {
                @Override public void componentResized(ComponentEvent event) {
                    setWrappedLabelWidth(name, parts[0], Math.max(100, card.getWidth() - 32));
                }
            });
        }
        return card;
    }

    private JPanel createHero(DestinationModel d, String category, boolean withReviewButton) {
        CoverPanel hero = new CoverPanel(loadDestinationImage(d, category), new Color(0, 0, 0, 90));
        hero.setLayout(new BorderLayout());
        hero.setPreferredSize(new Dimension(100, 230));
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 230));

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        text.setBorder(new EmptyBorder(0, 30, 22, 30));

        JLabel title = label(d.getName(), 38, true, Color.WHITE);
        JLabel place = label("" + d.getProvince(), 17, false, Color.WHITE);
        text.add(left(title));
        text.add(Box.createVerticalStrut(4));
        text.add(left(place));

        hero.add(text, BorderLayout.SOUTH);

        if (withReviewButton) {
            JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            topRight.setOpaque(false);
            topRight.setBorder(new EmptyBorder(16, 30, 0, 30));
            topRight.add(btnReview);
            hero.add(topRight, BorderLayout.NORTH);
        }
        return hero;
    }

    private JPanel infoBox(String title, String value) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(Color.WHITE);
        box.setBorder(new CompoundBorder(new LineBorder(BORDER_LIGHT, 1), new EmptyBorder(12, 16, 12, 16)));
        box.add(left(label(title, 12, true, TEXT_MUTED)));
        box.add(Box.createVerticalStrut(4));
        String displayValue = value == null ? "—" : value;
        JLabel valueLabel = text(displayValue, 140, 15, TEXT_DARK, true);
        box.add(left(valueLabel));
        box.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent event) {
                setWrappedLabelWidth(valueLabel, displayValue, Math.max(100, box.getWidth() - 32));
            }
        });
        return box;
    }

    private JPanel listBox(String title, List<String> items) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(Color.WHITE);
        box.setBorder(new CompoundBorder(new LineBorder(BORDER_LIGHT, 1), new EmptyBorder(14, 18, 14, 18)));
        box.add(left(label(title, 16, true, TEXT_DARK)));
        box.add(Box.createVerticalStrut(6));
        java.util.ArrayList<JLabel> itemLabels = new java.util.ArrayList<>();
        for (String item : items) {
            JLabel itemLabel = text("• " + item, 300, 14, TEXT_DARK, false);
            itemLabels.add(itemLabel);
            box.add(left(itemLabel));
        }
        box.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent event) {
                int width = Math.max(100, box.getWidth() - 36);
                for (int i = 0; i < itemLabels.size(); i++) {
                    setWrappedLabelWidth(itemLabels.get(i), "• " + items.get(i), width);
                }
            }
        });
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

    // Green badge with a gold star icon in front of the text
    private JLabel createRatingBadge(String text) {
        JLabel badge = createBadge(text);
        badge.setIcon(new StarIcon(14));
        badge.setIconTextGap(6);
        return badge;
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // =========================================================
    // IMAGES  (images/<name>.jpg|jpeg|png on classpath or project root)
    // =========================================================

    // Destination photo = images/<Destination name>.jpg (symbols removed),
    // otherwise falls back to the category photo, then to a gradient.
    private BufferedImage loadDestinationImage(DestinationModel d, String category) {
        BufferedImage own = loadImage(d.getName().replaceAll(SYMBOLS, "").trim());
        return own != null ? own : loadImage(category);
    }

        private BufferedImage loadImage(String name) {
        if (imageCache.containsKey(name)) {
            return imageCache.get(name);
        }
        BufferedImage result = null;
        for (String ext : IMAGE_EXTENSIONS) {
            result = readImage(name + ext);
            if (result != null) {
                break;
            }
        }
        imageCache.put(name, result);
        return result;
    }

    // Classpath first, then the "images" folder in the project root
    private static BufferedImage readImage(String file) {
        BufferedImage fromClasspath = readFromClasspath(file);
        return fromClasspath != null ? fromClasspath : readFromDisk(file);
    }

    private static BufferedImage readFromClasspath(String file) {
        try (InputStream in = ExplorePanel.class.getResourceAsStream("/images/" + file)) {
            return in != null ? ImageIO.read(in) : null;
        } catch (IOException | IllegalArgumentException ex) {
            return null;   // unreadable image: treated as missing
        }
    }

    private static BufferedImage readFromDisk(String file) {
        File f = new File("images/" + file);
        if (!f.exists()) {
            return null;
        }
        try {
            return ImageIO.read(f);
        } catch (IOException ex) {
            return null;   // unreadable image: treated as missing
        }
    }

    private int getResponsiveColumns(JScrollPane scroll) {
        int width = Math.max(0, scroll.getViewport().getWidth() - 60);
        return Math.max(1, Math.min(5, (width + 22) / 242));
    }

    private void updateResponsiveLayout() {
        catGrid.setLayout(new GridLayout(0, getResponsiveColumns(catScroll), 22, 22));
        listGrid.setLayout(new GridLayout(0, getResponsiveColumns(listScroll), 22, 22));
        revalidate();
        repaint();
    }

    private void updateDetailLayout(int viewportWidth, JLabel description, String descriptionText,
                                    JPanel placeGrid, JPanel info, JPanel lists) {
        int availableWidth = Math.max(150, viewportWidth - 60);
        setWrappedLabelWidth(description, descriptionText, availableWidth);
        placeGrid.setLayout(new GridLayout(0, responsiveColumns(availableWidth, 230, 18, 3), 18, 18));
        info.setLayout(new GridLayout(0, responsiveColumns(availableWidth, 180, 18, 4), 18, 12));
        lists.setLayout(new GridLayout(0, responsiveColumns(availableWidth, 340, 18, 2), 18, 12));
        placeGrid.setMaximumSize(new Dimension(availableWidth, Integer.MAX_VALUE));
        info.setMaximumSize(new Dimension(availableWidth, Integer.MAX_VALUE));
        lists.setMaximumSize(new Dimension(availableWidth, Integer.MAX_VALUE));
        placeGrid.revalidate();
        info.revalidate();
        lists.revalidate();
    }

    private static int responsiveColumns(int availableWidth, int minCellWidth,
                                         int gap, int maxColumns) {
        return Math.max(1, Math.min(maxColumns, (availableWidth + gap) / (minCellWidth + gap)));
    }

    private static void setWrappedLabelWidth(JLabel label, String value, int width) {
        label.setText("<html><body style='width:" + width + "px'>"
                + esc(value) + "</body></html>");
    }

    // =========================================================
    // CUSTOM COMPONENTS
    // =========================================================

    // Paints a photo scaled to "cover" the panel, or a green gradient if no photo.
    /**
     * A panel that always matches the scroll pane's width. Without this, a wide
     * review list makes the page wider than the window, which stretched the
     * header photo (it looked zoomed in) and pushed content off screen.
     */
    private static class ViewportWidthPanel extends JPanel implements Scrollable {
        ViewportWidthPanel(LayoutManager layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private static class CoverPanel extends JPanel {
        private final transient BufferedImage source;
        private final Color overlay;
        private transient BufferedImage cache;
        private int cw;
        private int ch;

        CoverPanel(BufferedImage source, Color overlay) {
            this.source = source;
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
                g2.setPaint(new GradientPaint(0f, 0f, PRIMARY, (float) w, (float) h, PRIMARY_LIGHT));
                g2.fillRect(0, 0, w, h);
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

    // Small gold star drawn with Java2D (no emoji font needed)
    private static class StarIcon implements Icon {
        private final int size;

        StarIcon(int size) { this.size = size; }

        @Override public int getIconWidth()  { return size; }
        @Override public int getIconHeight() { return size; }

        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double cx = x + size / 2.0;
            double cy = y + size / 2.0;
            double outer = size / 2.0;
            double inner = outer * 0.45;
            Path2D star = new Path2D.Double();
            for (int i = 0; i < 10; i++) {
                double r = i % 2 == 0 ? outer : inner;
                double a = -Math.PI / 2 + i * Math.PI / 5;
                double px = cx + r * Math.cos(a);
                double py = cy + r * Math.sin(a);
                if (i == 0) {
                    star.moveTo(px, py);
                } else {
                    star.lineTo(px, py);
                }
            }
            star.closePath();
            g2.setColor(STAR_GOLD);
            g2.fill(star);
            g2.dispose();
        }
    }

    // Rounded button with hover color
    private static class PillButton extends JButton {
        private final Color bg;
        private final Color bgHover;
        private final Color outline;

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
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, 10f, 10f));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
    
}