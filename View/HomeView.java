package View;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
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

public class HomeView extends JFrame {

    private static final String FONT_NAME = "Segoe UI";
    private static final String EMOJI_FONT = "Segoe UI Emoji";
    private static final String DEFAULT_COUNT_TEXT = "0 destinations";

    // Page names for the CardLayout
    private static final String PAGE_HOME = "HOME";
    private static final String PAGE_DISCOVER = "DISCOVER";

    // Color palette
    private static final Color PRIMARY = new Color(0x1D9E75);
    private static final Color PRIMARY_LIGHT = new Color(0x2EC4A0);
    private static final Color BADGE_BG = new Color(0xE3F5EE);
    private static final Color BACKGROUND = new Color(0xF1F5F9);
    private static final Color TEXT_DARK = new Color(0x0F2A43);
    private static final Color TEXT_MUTED = new Color(0x7A8CA8);
    private static final Color BORDER_LIGHT = new Color(0xE2E8F0);

    // Image cache (name -> image, null if the file was not found)
    private final Map<String, BufferedImage> imageCache = new HashMap<>();

    // Navigation buttons
    private JButton btnHome;
    private JButton btnLogin;
    private JButton btnGian;
    private JButton btnAudrick;

    // Search components
    private JButton btnSearch;
    private JTextField txtSearch;

    // Category counts
    private final Map<String, JLabel> countLabels = new HashMap<>();

    // Category click listener
    private Consumer<String> categoryListener = category -> {};

    // Page switching (Home page <-> Destination cards page)
    private CardLayout pageLayout;
    private JPanel pageContainer;
    private JScrollPane homeScroll;
    private JScrollPane discoverScroll;

    // Destination display components
    private JLabel lblDestinationTitle;
    private JPanel destinationCardsPanel;

    public HomeView() {
        setTitle("TravelMatch");
        setSize(1400, 900);
        setMinimumSize(new Dimension(900, 650));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BACKGROUND);

        JPanel navBar = createNavBar();

        pageLayout = new CardLayout();
        pageContainer = new JPanel(pageLayout);
        pageContainer.setBackground(BACKGROUND);
        pageContainer.add(createHomePage(), PAGE_HOME);
        pageContainer.add(createDiscoverPage(), PAGE_DISCOVER);

        mainPanel.add(navBar, BorderLayout.NORTH);
        mainPanel.add(pageContainer, BorderLayout.CENTER);

        setContentPane(mainPanel);
        pageLayout.show(pageContainer, PAGE_HOME);
    }

    // =========================
    // IMAGE LOADING
    // =========================

    // Looks for images/<name>.jpg|jpeg|png on the classpath first,
    // then in an "images" folder in the project root.
    private BufferedImage loadImage(String name) {
        if (imageCache.containsKey(name)) {
            return imageCache.get(name);
        }

        BufferedImage result = null;
        for (String ext : new String[]{".jpg", ".jpeg", ".png"}) {
            String file = name + ext;

            try (InputStream in = HomeView.class
                    .getResourceAsStream("/images/" + file)) {
                if (in != null) {
                    result = ImageIO.read(in);
                }
            } catch (IOException ignored) {
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

    // File names match the category names:
    // Beach.jpg, Mountain.jpg, City.jpg, Adventure.jpg, Cultural.jpg
    private String imageNameFor(String category) {
        return category;
    }

    // =========================
    // PAGES
    // =========================

    private JScrollPane createHomePage() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BACKGROUND);

        JPanel heroPanel = createHeroPanel();
        JPanel categoryPanel = createCategorySection();

        heroPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        categoryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(heroPanel);
        content.add(categoryPanel);

        homeScroll = wrapInScroll(content);
        return homeScroll;
    }

    private JScrollPane createDiscoverPage() {
        JPanel page = new JPanel(new BorderLayout(0, 15));
        page.setBackground(BACKGROUND);
        page.setBorder(new EmptyBorder(20, 30, 20, 30));

        lblDestinationTitle = new JLabel("Explore Destinations");
        lblDestinationTitle.setFont(new Font(FONT_NAME, Font.BOLD, 28));
        lblDestinationTitle.setForeground(TEXT_DARK);

        destinationCardsPanel = new JPanel(new GridLayout(0, 3, 22, 22));
        destinationCardsPanel.setBackground(BACKGROUND);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(destinationCardsPanel, BorderLayout.NORTH);

        page.add(lblDestinationTitle, BorderLayout.NORTH);
        page.add(wrapper, BorderLayout.CENTER);

        discoverScroll = wrapInScroll(page);
        return discoverScroll;
    }

    private JScrollPane wrapInScroll(JComponent view) {
        JScrollPane scroll = new JScrollPane(view);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        return scroll;
    }

    // HOME button: go back to the home page and reset the search box
    public void showHomePage() {
        txtSearch.setText("");
        pageLayout.show(pageContainer, PAGE_HOME);
        scrollToTop(homeScroll);
    }

    private void showDiscoverPage() {
        pageLayout.show(pageContainer, PAGE_DISCOVER);
        scrollToTop(discoverScroll);
    }

    private void scrollToTop(JScrollPane scroll) {
        SwingUtilities.invokeLater(() ->
                scroll.getViewport().setViewPosition(new Point(0, 0))
        );
    }

    // =========================
    // NAVIGATION BAR
    // =========================

    private JPanel createNavBar() {
        JPanel navBar = new JPanel(new BorderLayout());
        navBar.setBackground(Color.WHITE);
        navBar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER_LIGHT),
                new EmptyBorder(10, 25, 10, 25)
        ));

        JLabel logo = new JLabel(
                "TravelMatch", new PinIcon(22, 28), SwingConstants.LEFT
        );
        logo.setIconTextGap(12);
        logo.setFont(new Font(FONT_NAME, Font.BOLD, 28));
        logo.setForeground(PRIMARY);

        JPanel navButtons = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 12, 0)
        );
        navButtons.setOpaque(false);

        btnHome = createNavButton("Home");
        btnGian = createNavButton("Gian");
        btnAudrick = createNavButton("Audrick");
        btnLogin = createNavButton("Login");

        navButtons.add(btnHome);
        navButtons.add(btnGian);
        navButtons.add(btnAudrick);
        navButtons.add(btnLogin);

        navBar.add(logo, BorderLayout.WEST);
        navBar.add(navButtons, BorderLayout.EAST);

        return navBar;
    }

    private JButton createNavButton(String text) {
        return new RoundedButton(
                text,
                Color.WHITE, new Color(0xF1F5F9),
                TEXT_DARK, BORDER_LIGHT, 1
        );
    }

    // =========================
    // HERO SECTION
    // =========================

    private JPanel createHeroPanel() {
        // Photo (images/Hero.jpg) with a green tint on top.
        // Falls back to solid green if the image is missing.
        CoverImagePanel heroPanel = new CoverImagePanel(
                loadImage("Hero"), PRIMARY,
                new Color(0x1D, 0x9E, 0x75, 205)
        );
        heroPanel.setLayout(new BoxLayout(heroPanel, BoxLayout.Y_AXIS));
        heroPanel.setPreferredSize(new Dimension(100, 250));
        heroPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));
        heroPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel lblTitle = new JLabel("Discover Your Perfect Destination");
        lblTitle.setFont(new Font(FONT_NAME, Font.BOLD, 46));
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSubtitle = new JLabel(
                "Let us recommend the ideal travel spot "
                + "based on your preferences"
        );
        lblSubtitle.setFont(new Font(FONT_NAME, Font.PLAIN, 17));
        lblSubtitle.setForeground(Color.WHITE);
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel searchPanel = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 10, 0)
        );
        searchPanel.setOpaque(false);
        searchPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        searchPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        txtSearch = new SearchField("Where do you want to go?");
        txtSearch.setPreferredSize(new Dimension(380, 42));

        btnSearch = new RoundedButton(
                "Search",
                PRIMARY, PRIMARY_LIGHT,
                Color.WHITE, Color.WHITE, 2
        );
        btnSearch.setPreferredSize(new Dimension(100, 42));

        // Pressing Enter also triggers search
        txtSearch.addActionListener(e -> btnSearch.doClick());

        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);

        heroPanel.add(Box.createVerticalGlue());
        heroPanel.add(lblTitle);
        heroPanel.add(Box.createVerticalStrut(10));
        heroPanel.add(lblSubtitle);
        heroPanel.add(Box.createVerticalStrut(22));
        heroPanel.add(searchPanel);
        heroPanel.add(Box.createVerticalGlue());

        return heroPanel;
    }

    // =========================
    // CATEGORY SECTION
    // =========================

    private JPanel createCategorySection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(BACKGROUND);
        section.setBorder(new EmptyBorder(20, 35, 25, 35));

        JLabel lblCategories = new JLabel("Popular Categories");
        lblCategories.setFont(new Font(FONT_NAME, Font.BOLD, 30));
        lblCategories.setForeground(TEXT_DARK);
        lblCategories.setBorder(new EmptyBorder(0, 0, 15, 0));

        JPanel cardsPanel = new JPanel(new GridLayout(2, 3, 22, 22));
        cardsPanel.setBackground(BACKGROUND);

        cardsPanel.add(createCard("Beach", DEFAULT_COUNT_TEXT));
        cardsPanel.add(createCard("Mountain", DEFAULT_COUNT_TEXT));
        cardsPanel.add(createCard("City", DEFAULT_COUNT_TEXT));
        cardsPanel.add(createCard("Adventure", DEFAULT_COUNT_TEXT));
        cardsPanel.add(createCard("Cultural", DEFAULT_COUNT_TEXT));

        // Empty sixth cell for the 2x3 grid
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        cardsPanel.add(filler);

        section.add(lblCategories, BorderLayout.NORTH);
        section.add(cardsPanel, BorderLayout.CENTER);

        return section;
    }

    // =========================
    // CATEGORY CARD
    // =========================

    private Border cardBorder(boolean hover) {
        if (hover) {
            return new LineBorder(PRIMARY, 2);
        }
        return new CompoundBorder(
                new EmptyBorder(1, 1, 1, 1),
                new LineBorder(BORDER_LIGHT, 1)
        );
    }

    private JPanel createCard(String name, String destinations) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(cardBorder(false));
        card.setPreferredSize(new Dimension(200, 290));

        // Photo area
        BufferedImage photo = loadImage(imageNameFor(name));
        CoverImagePanel imagePanel =
                new CoverImagePanel(photo, PRIMARY, null);
        imagePanel.setLayout(new GridBagLayout());
        imagePanel.setPreferredSize(new Dimension(200, 215));

        // If the photo is missing, show the emoji icon instead
        if (photo == null) {
            JLabel lblIcon = new JLabel(iconFor(name));
            lblIcon.setFont(new Font(EMOJI_FONT, Font.PLAIN, 44));
            imagePanel.add(lblIcon);
        }

        // Category information
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Color.WHITE);
        infoPanel.setBorder(new EmptyBorder(12, 16, 14, 16));

        JLabel lblName = new JLabel(name);
        lblName.setFont(new Font(FONT_NAME, Font.BOLD, 20));
        lblName.setForeground(TEXT_DARK);

        JLabel lblDest = new JLabel(destinations);
        lblDest.setFont(new Font(FONT_NAME, Font.PLAIN, 14));
        lblDest.setForeground(TEXT_MUTED);

        countLabels.put(name, lblDest);

        infoPanel.add(lblName);
        infoPanel.add(Box.createVerticalStrut(4));
        infoPanel.add(lblDest);

        card.add(imagePanel, BorderLayout.CENTER);
        card.add(infoPanel, BorderLayout.SOUTH);

        MouseAdapter clickListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                categoryListener.accept(name);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBorder(cardBorder(true));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Ignore exits into a child of the card
                Point p = SwingUtilities.convertPoint(
                        e.getComponent(), e.getPoint(), card);
                if (!card.contains(p)) {
                    card.setBorder(cardBorder(false));
                }
            }
        };

        addClickListenerRecursively(card, clickListener);

        return card;
    }

    // Attach mouse listener to card and all its children
    private void addClickListenerRecursively(
            Component component, MouseAdapter listener) {

        component.addMouseListener(listener);
        component.setCursor(new Cursor(Cursor.HAND_CURSOR));

        if (component instanceof Container) {
            for (Component child :
                    ((Container) component).getComponents()) {
                addClickListenerRecursively(child, listener);
            }
        }
    }

    // =========================
    // DESTINATION CARDS
    // =========================

    private JPanel createDestinationCard(
            String name, List<String> categories) {

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(cardBorder(false));
        card.setPreferredSize(new Dimension(200, 285));

        // Header uses the first category's photo
        String firstCategory = categories.get(0);
        BufferedImage photo = loadImage(imageNameFor(firstCategory));

        JPanel header;
        if (photo != null) {
            header = new CoverImagePanel(photo, PRIMARY, null);
        } else {
            header = new JPanel(new GridBagLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setPaint(new GradientPaint(
                            0, 0, PRIMARY,
                            getWidth(), getHeight(), PRIMARY_LIGHT
                    ));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.dispose();
                }
            };
            JLabel lblIcon = new JLabel(iconFor(firstCategory));
            lblIcon.setFont(new Font(EMOJI_FONT, Font.PLAIN, 44));
            header.add(lblIcon);
        }
        header.setPreferredSize(new Dimension(200, 150));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(Color.WHITE);
        info.setBorder(new EmptyBorder(14, 16, 14, 16));

        // HTML lets long names wrap onto extra lines
        JLabel lblName = new JLabel(
                "<html><body style='width:200px'>"
                + name.replace("&", "&amp;")
                + "</body></html>"
        );
        lblName.setFont(new Font(FONT_NAME, Font.BOLD, 17));
        lblName.setForeground(TEXT_DARK);
        lblName.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel badgeRow = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 6, 0)
        );
        badgeRow.setOpaque(false);
        badgeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (String category : categories) {
            badgeRow.add(createBadge(category));
        }

        info.add(lblName);
        info.add(Box.createVerticalStrut(10));
        info.add(badgeRow);

        card.add(header, BorderLayout.NORTH);
        card.add(info, BorderLayout.CENTER);

        MouseAdapter hover = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBorder(cardBorder(true));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(
                        e.getComponent(), e.getPoint(), card);
                if (!card.contains(p)) {
                    card.setBorder(cardBorder(false));
                }
            }
        };
        addHoverRecursively(card, hover);

        return card;
    }

    private void addHoverRecursively(Component c, MouseAdapter l) {
        c.addMouseListener(l);
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                addHoverRecursively(child, l);
            }
        }
    }

    // Rounded "pill" label for a category
    private JLabel createBadge(String text) {
        JLabel badge = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );
                g2.setColor(BADGE_BG);
                g2.fillRoundRect(
                        0, 0, getWidth(), getHeight(),
                        getHeight(), getHeight()
                );
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setOpaque(false);
        badge.setFont(new Font(FONT_NAME, Font.BOLD, 12));
        badge.setForeground(PRIMARY);
        badge.setBorder(new EmptyBorder(4, 12, 4, 12));
        return badge;
    }

    private String iconFor(String category) {
        switch (category) {
            case "Beach":     return "🏖";
            case "Mountain":  return "⛰";
            case "City":      return "🏙";
            case "Adventure": return "🎯";
            case "Cultural":  return "🏛";
            default:          return "📍";
        }
    }

    // Rebuild the card grid and switch to the destination page
    private void populateDestinationCards(
            Map<String, List<String>> destinations) {

        destinationCardsPanel.removeAll();

        if (destinations.isEmpty()) {
            JLabel lblEmpty = new JLabel("No destinations found");
            lblEmpty.setFont(new Font(FONT_NAME, Font.PLAIN, 16));
            destinationCardsPanel.add(lblEmpty);
        }

        for (Map.Entry<String, List<String>> entry
                : destinations.entrySet()) {
            destinationCardsPanel.add(
                    createDestinationCard(
                            entry.getKey(), entry.getValue()
                    )
            );
        }

        destinationCardsPanel.revalidate();
        destinationCardsPanel.repaint();

        showDiscoverPage();
    }

    // Display selected category and its destinations
    public void showCategoryDestinations(
            String category, Map<String, List<String>> destinations) {

        lblDestinationTitle.setText(
                category + " Destinations ("
                + destinations.size() + ")"
        );
        populateDestinationCards(destinations);
    }

    // Display search results as cards too
    public void showSearchResults(
            String query, Map<String, List<String>> results) {

        lblDestinationTitle.setText(
                "Search Results for \"" + query
                + "\" (" + results.size() + ")"
        );
        populateDestinationCards(results);
    }

    // =========================
    // CONTROLLER LISTENERS
    // =========================

    public void addHomeListener(ActionListener listener) {
        btnHome.addActionListener(listener);
    }

    public void addLoginListener(ActionListener listener) {
        btnLogin.addActionListener(listener);
    }

    public void addSearchListener(ActionListener listener) {
        btnSearch.addActionListener(listener);
    }

    public void addCategoryListener(Consumer<String> listener) {
        this.categoryListener = listener;
    }

    // =========================
    // CATEGORY COUNT
    // =========================

    public void setCategoryCount(String category, int count) {
        JLabel label = countLabels.get(category);

        if (label != null) {
            label.setText(count + " destinations");
        }
    }

    // =========================
    // SEARCH INPUT
    // =========================

    public String getSearchText() {
        return txtSearch.getText().trim();
    }

    // =====================================================
    // CUSTOM COMPONENTS
    // =====================================================

    // Panel that paints an image scaled to "cover" its area,
    // with an optional color overlay on top.
    private static class CoverImagePanel extends JPanel {
        private final BufferedImage source;
        private final Color overlay;
        private BufferedImage cache;
        private int cacheW, cacheH;

        CoverImagePanel(BufferedImage source, Color base, Color overlay) {
            this.source = source;
            this.overlay = overlay;
            setBackground(base);
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); // fills the base color
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }

            if (source != null) {
                if (cache == null || cacheW != w || cacheH != h) {
                    cache = scaleCover(source, w, h);
                    cacheW = w;
                    cacheH = h;
                }
                g.drawImage(cache, 0, 0, null);
            }

            if (overlay != null) {
                g.setColor(overlay);
                g.fillRect(0, 0, w, h);
            }
        }

        private static BufferedImage scaleCover(
                BufferedImage img, int w, int h) {
            double scale = Math.max(
                    (double) w / img.getWidth(),
                    (double) h / img.getHeight()
            );
            int dw = (int) Math.ceil(img.getWidth() * scale);
            int dh = (int) Math.ceil(img.getHeight() * scale);

            BufferedImage out =
                    new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = out.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(img, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
            g2.dispose();
            return out;
        }
    }

    // Rounded button with hover color and optional outline
    private static class RoundedButton extends JButton {
        private final Color bg;
        private final Color bgHover;
        private final Color outline;
        private final int outlineWidth;

        RoundedButton(String text, Color bg, Color bgHover,
                      Color fg, Color outline, int outlineWidth) {
            super(text);
            this.bg = bg;
            this.bgHover = bgHover;
            this.outline = outline;
            this.outlineWidth = outlineWidth;

            setForeground(fg);
            setFont(new Font(FONT_NAME, Font.BOLD, 13));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(9, 20, 9, 20));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(getModel().isRollover() ? bgHover : bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

            if (outline != null) {
                float half = outlineWidth / 2f;
                g2.setColor(outline);
                g2.setStroke(new BasicStroke(outlineWidth));
                g2.draw(new RoundRectangle2D.Float(
                        half, half,
                        getWidth() - outlineWidth,
                        getHeight() - outlineWidth,
                        10, 10));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // White rounded text field with placeholder text
    private static class SearchField extends JTextField {
        private final String placeholder;

        SearchField(String placeholder) {
            this.placeholder = placeholder;
            setOpaque(false);
            setBorder(new EmptyBorder(0, 16, 0, 16));
            setFont(new Font(FONT_NAME, Font.PLAIN, 15));
            setForeground(TEXT_DARK);
            setCaretColor(TEXT_DARK);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();

            super.paintComponent(g);

            if (getText().isEmpty()) {
                Graphics2D g3 = (Graphics2D) g.create();
                g3.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g3.setColor(new Color(0x94A3B8));
                g3.setFont(getFont());
                FontMetrics fm = g3.getFontMetrics();
                int x = getInsets().left;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g3.drawString(placeholder, x, y);
                g3.dispose();
            }
        }
    }

    // Location-pin logo icon drawn with Java2D (no emoji font needed)
    private static class PinIcon implements Icon {
        private final int w;
        private final int h;

        PinIcon(int w, int h) {
            this.w = w;
            this.h = h;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);

            Area pin = new Area(new Ellipse2D.Double(0, 0, w, w));

            Path2D tip = new Path2D.Double();
            tip.moveTo(w * 0.10, w * 0.70);
            tip.lineTo(w * 0.90, w * 0.70);
            tip.lineTo(w / 2.0, h);
            tip.closePath();
            pin.add(new Area(tip));

            double d = w * 0.40;
            pin.subtract(new Area(new Ellipse2D.Double(
                    (w - d) / 2, (w - d) / 2, d, d)));

            g2.setColor(PRIMARY);
            g2.fill(pin);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return w;
        }

        @Override
        public int getIconHeight() {
            return h;
        }
    }
}