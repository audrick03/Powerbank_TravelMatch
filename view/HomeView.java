package view;

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
    private static final String[] IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png"};
    private static final String DEFAULT_COUNT_TEXT = "0 destinations";

    // Page names for the CardLayout
    private static final String PAGE_HOME = "HOME";
    private static final String PAGE_DISCOVER = "DISCOVER";
    private static final String PAGE_EXPLORE = "EXPLORE";

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
    private JButton btnRecommendation;
    private JButton btnPreference;

    // Search components
    private JButton btnSearch;
    private JTextField txtSearch;

    // Category counts
    private final Map<String, JLabel> countLabels = new HashMap<>();

    // Region click listener + region count labels (Luzon / Visayas / Mindanao)
    private transient Consumer<String> regionListener = region -> {};
    private final Map<String, JLabel> regionCountLabels = new HashMap<>();

    // Region -> Category -> Destination -> Recommendation pages
    private ExplorePanel explorePanel;

    // Page switching (Home page <-> Destination cards page)
    private CardLayout pageLayout;
    private JPanel pageContainer;
    private JScrollPane homeScroll;
    private JScrollPane discoverScroll;

    // Destination display components
    private JLabel lblDestinationTitle;
    private JPanel destinationCardsPanel;
    private JPanel regionCardsPanel;

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
        explorePanel = new ExplorePanel();
        pageContainer.add(explorePanel, PAGE_EXPLORE);

        mainPanel.add(navBar, BorderLayout.NORTH);
        mainPanel.add(pageContainer, BorderLayout.CENTER);

        setContentPane(mainPanel);
        pageLayout.show(pageContainer, PAGE_HOME);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateResponsiveLayout();
            }
        });
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
        try (InputStream in = HomeView.class.getResourceAsStream("/images/" + file)) {
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

    // File names match the category names:
    // Beach.jpg, Mountain.jpg, City.jpg, Adventure.jpg, Cultural.jpg
    private String imageNameFor(String category) {
        return category;
    }

    // =========================
    // PAGES
    // =========================

    private JScrollPane createHomePage() {
        ViewportWidthPanel content = new ViewportWidthPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BACKGROUND);

        JPanel heroPanel = createHeroPanel();
        JPanel categoryPanel = createRegionSection();

        heroPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        categoryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(heroPanel);
        content.add(categoryPanel);

        homeScroll = wrapInScroll(content);
        return homeScroll;
    }

    private JScrollPane createDiscoverPage() {
        ViewportWidthPanel page = new ViewportWidthPanel(new BorderLayout(0, 15));
        page.setBackground(BACKGROUND);
        page.setBorder(new EmptyBorder(20, 30, 20, 30));

        lblDestinationTitle = new JLabel("Explore Destinations");
        lblDestinationTitle.setFont(new Font(FONT_NAME, Font.BOLD, 28));
        lblDestinationTitle.setForeground(TEXT_DARK);

        destinationCardsPanel = new JPanel();
        destinationCardsPanel.setOpaque(false);

        destinationCardsPanel.setLayout(new GridLayout(0, getColumnCount(), 22, 22));
        

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
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
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
        btnRecommendation = createNavButton("Recommendations");
        btnPreference = createNavButton("Preferences");
        btnLogin = createNavButton("Login");

        navButtons.add(btnHome);
        navButtons.add(btnRecommendation);
        navButtons.add(btnPreference);
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
    // CARDS
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
                            0f, 0f, PRIMARY,
                            (float) getWidth(), (float) getHeight(), PRIMARY_LIGHT
                    ));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.dispose();
                }
            };
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
                        + "</body></html>");
        lblName.setFont(new Font(FONT_NAME, Font.BOLD, 17));
        lblName.setForeground(TEXT_DARK);
        lblName.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                int textWidth = Math.max(100, info.getWidth() - 32);
                lblName.setText("<html><body style='width:" + textWidth + "px'>"
                        + name.replace("&", "&amp;") + "</body></html>");
            }
        });

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
    // REGION SECTION (home page)
    // =========================

    private JPanel createRegionSection() {
        regionCardsPanel = new JPanel();
        regionCardsPanel.setOpaque(false);
        JPanel section = new JPanel(new BorderLayout());
        section.setOpaque(false);

        regionCardsPanel.setLayout(new GridLayout(0, getColumnCount(), 22, 22));


        JLabel lblRegions = new JLabel("Explore the Philippines");
        lblRegions.setFont(new Font(FONT_NAME, Font.BOLD, 30));
        lblRegions.setForeground(TEXT_DARK);
        lblRegions.setBorder(new EmptyBorder(0, 0, 15, 0));

        for (String region : DestinationRepository.REGIONS) {
            regionCardsPanel.add(createRegionCard(region));
        }

        section.add(lblRegions, BorderLayout.NORTH);
        section.add(regionCardsPanel, BorderLayout.CENTER);
        return section;
    }

    // Photo: images/Luzon.jpg, images/Visayas.jpg, images/Mindanao.jpg
    private JPanel createRegionCard(String region) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(cardBorder(false));
        card.setPreferredSize(new Dimension(200, 360));

        BufferedImage photo = loadImage(region);
        CoverImagePanel imagePanel = new CoverImagePanel(photo, PRIMARY, null);
        imagePanel.setLayout(new GridBagLayout());
        imagePanel.setPreferredSize(new Dimension(200, 230));

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Color.WHITE);
        infoPanel.setBorder(new EmptyBorder(12, 16, 14, 16));

        JLabel lblName = new JLabel(region.toUpperCase());
        lblName.setFont(new Font(FONT_NAME, Font.BOLD, 22));
        lblName.setForeground(TEXT_DARK);

        String description = DestinationRepository.regionDescription(region);
        JLabel lblDesc = new JLabel(
                "<html><body style='width:260px'>" + description + "</body></html>");
        lblDesc.setFont(new Font(FONT_NAME, Font.PLAIN, 14));
        lblDesc.setForeground(TEXT_MUTED);
        card.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                int textWidth = Math.max(140, infoPanel.getWidth() - 32);
                lblDesc.setText("<html><body style='width:" + textWidth + "px'>"
                        + description + "</body></html>");
            }
        });

        JLabel lblCount = new JLabel(DEFAULT_COUNT_TEXT);
        lblCount.setFont(new Font(FONT_NAME, Font.BOLD, 13));
        lblCount.setForeground(PRIMARY);
        regionCountLabels.put(region, lblCount);

        JLabel lblExplore = new JLabel("Explore →");
        lblExplore.setFont(new Font(FONT_NAME, Font.BOLD, 15));
        lblExplore.setForeground(PRIMARY);

        infoPanel.add(lblName);
        infoPanel.add(Box.createVerticalStrut(4));
        infoPanel.add(lblDesc);
        infoPanel.add(Box.createVerticalStrut(6));
        infoPanel.add(lblCount);
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(lblExplore);

        card.add(imagePanel, BorderLayout.CENTER);
        card.add(infoPanel, BorderLayout.SOUTH);

        MouseAdapter clickListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                regionListener.accept(region);
            }

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
        addClickListenerRecursively(card, clickListener);
        return card;
    }

    public void addRegionListener(Consumer<String> listener) {
        this.regionListener = listener;
    }

    public void setRegionCount(String region, int count) {
        JLabel label = regionCountLabels.get(region);
        if (label != null) {
            label.setText(count + (count == 1 ? " destination" : " destinations"));
        }
    }

    // The explore pages (categories / destinations / recommendation)
    public ExplorePanel getExplorePanel() {
        return explorePanel;
    }

    public void showExplorePage() {
        pageLayout.show(pageContainer, PAGE_EXPLORE);
    }

    // =========================
    // CONTROLLER LISTENERS
    // =========================

    public void addPreferenceListener(ActionListener listener) {
        btnPreference.addActionListener(listener);
    }

    public void addRecommendationListener(ActionListener listener) {
        btnRecommendation.addActionListener(listener);
    }

    public void addHomeListener(ActionListener listener) {
        btnHome.addActionListener(listener);
    }

    public void addLoginListener(ActionListener listener) {
        btnLogin.addActionListener(listener);
    }

    public void setLoginButtonText(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Login button text is required");
        }
        btnLogin.setText(text);
    }

    public void addSearchListener(ActionListener listener) {
        btnSearch.addActionListener(listener);
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
    private int getColumnCount() {
        int width = discoverScroll == null
                ? getWidth() : discoverScroll.getViewport().getWidth();
        return Math.max(1, Math.min(4, (width + 22) / 262));
    }

    private void updateResponsiveLayout() {
        int discoverColumns = getColumnCount();
        if (regionCardsPanel != null) {
            int width = homeScroll == null
                    ? getWidth() : homeScroll.getViewport().getWidth();
            int regionColumns = Math.max(1, Math.min(3, (width + 22) / 322));
            regionCardsPanel.setLayout(new GridLayout(0, regionColumns, 22, 22));
        }
        if (destinationCardsPanel != null) {
            destinationCardsPanel.setLayout(new GridLayout(0, discoverColumns, 22, 22));
        }
        revalidate();
        repaint();
    }

    private static class ViewportWidthPanel extends JPanel implements Scrollable {
        ViewportWidthPanel() { super(); }
        ViewportWidthPanel(LayoutManager layout) { super(layout); }

        @Override public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(16, visibleRect.height - 40);
        }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
    // Panel that paints an image scaled to "cover" its area,
    // with an optional color overlay on top.
    private static class CoverImagePanel extends JPanel {
        private final transient BufferedImage source;
        private final Color overlay;
        private transient BufferedImage cache;
        private int cacheW;
        private int cacheH;

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
                g2.setStroke(new BasicStroke((float) outlineWidth));
                g2.draw(new RoundRectangle2D.Float(
                        half, half,
                        getWidth() - (float) outlineWidth,
                        getHeight() - (float) outlineWidth,
                        10f, 10f));
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