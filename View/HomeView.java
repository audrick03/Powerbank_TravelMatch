package View;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class HomeView extends JFrame {

    private static final String FONT_NAME = "Arial";
    private static final String EMOJI_FONT = "Segoe UI Emoji";
    private static final String DEFAULT_COUNT_TEXT = "0 destinations";

    // Page names for the CardLayout
    private static final String PAGE_HOME = "HOME";
    private static final String PAGE_DISCOVER = "DISCOVER";

    // Color palette
    private static final Color PRIMARY = new Color(0x1D9E75);
    private static final Color PRIMARY_LIGHT = new Color(0x2EC4A0);
    private static final Color BADGE_BG = new Color(0xE3F5EE);
    private static final Color ERROR = new Color(0xC0392B);
    private static final Color BACKGROUND = new Color(240, 245, 250);

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

        // Main container
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BACKGROUND);

        // NAVBAR (always visible)
        JPanel navBar = createNavBar();

        // PAGES (only one visible at a time)
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
    // PAGES
    // =========================

    // Home page: hero + category cards
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

    // Destination page: grid of destination cards
    private JScrollPane createDiscoverPage() {
        JPanel page = new JPanel(new BorderLayout(0, 15));
        page.setBackground(BACKGROUND);
        page.setBorder(new EmptyBorder(20, 15, 20, 15));

        lblDestinationTitle = new JLabel("Explore Destinations");
        lblDestinationTitle.setFont(new Font(FONT_NAME, Font.BOLD, 28));
        lblDestinationTitle.setForeground(new Color(35, 35, 35));
        lblDestinationTitle.setBorder(new EmptyBorder(0, 5, 0, 5));

        // 3 cards per row, as many rows as needed
        destinationCardsPanel = new JPanel(new GridLayout(0, 3, 20, 20));
        destinationCardsPanel.setBackground(BACKGROUND);
        destinationCardsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Wrapper keeps cards at their natural height (no stretching)
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
        navBar.setBorder(new EmptyBorder(10, 20, 10, 20));

        JLabel logo = new JLabel("🌎 TravelMatch");
        logo.setFont(new Font(FONT_NAME, Font.BOLD, 24));
        logo.setForeground(PRIMARY);

        JPanel navButtons = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 10, 0)
        );
        navButtons.setOpaque(false);

        btnHome = new JButton("Home");
        btnLogin = new JButton("Login");
        btnGian = new JButton("Gian");
        btnAudrick = new JButton("Audrick");

        btnHome.setFocusPainted(false);
        btnLogin.setFocusPainted(false);
        btnGian.setFocusPainted(false);
        btnAudrick.setFocusPainted(false);

        navButtons.add(btnHome);
        navButtons.add(btnGian);
        navButtons.add(btnAudrick);
        navButtons.add(btnLogin);

        navBar.add(logo, BorderLayout.WEST);
        navBar.add(navButtons, BorderLayout.EAST);

        return navBar;
    }

    // =========================
    // HERO SECTION
    // =========================

    private JPanel createHeroPanel() {
        JPanel heroPanel = new JPanel();
        heroPanel.setLayout(new BoxLayout(
                heroPanel, BoxLayout.Y_AXIS
        ));
        heroPanel.setBackground(PRIMARY);
        heroPanel.setPreferredSize(new Dimension(100, 250));
        heroPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));
        heroPanel.setBorder(new EmptyBorder(35, 20, 25, 20));

        JLabel lblTitle = new JLabel(
                "Discover Your Perfect Destination"
        );
        lblTitle.setFont(new Font(FONT_NAME, Font.BOLD, 36));
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSubtitle = new JLabel(
                "Let us recommend the ideal travel spot "
                + "based on your preferences"
        );
        lblSubtitle.setFont(new Font(FONT_NAME, Font.PLAIN, 14));
        lblSubtitle.setForeground(Color.WHITE);
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel searchPanel = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 8, 0)
        );
        searchPanel.setOpaque(false);

        txtSearch = new JTextField(30);
        txtSearch.setPreferredSize(new Dimension(330, 35));
        txtSearch.setFont(new Font(FONT_NAME, Font.PLAIN, 14));

        btnSearch = new JButton("Search");
        btnSearch.setPreferredSize(new Dimension(80, 35));
        btnSearch.setFocusPainted(false);

        // Pressing Enter also triggers search
        txtSearch.addActionListener(e -> btnSearch.doClick());

        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);

        heroPanel.add(lblTitle);
        heroPanel.add(Box.createVerticalStrut(10));
        heroPanel.add(lblSubtitle);
        heroPanel.add(Box.createVerticalStrut(25));
        heroPanel.add(searchPanel);

        return heroPanel;
    }

    // =========================
    // CATEGORY SECTION
    // =========================

    private JPanel createCategorySection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(BACKGROUND);
        section.setBorder(new EmptyBorder(15, 10, 5, 10));

        JLabel lblCategories = new JLabel("Popular Categories");
        lblCategories.setFont(new Font(FONT_NAME, Font.BOLD, 24));
        lblCategories.setForeground(new Color(35, 35, 35));
        lblCategories.setBorder(new EmptyBorder(5, 5, 15, 5));

        // Two rows of category cards
        JPanel cardsPanel = new JPanel(new GridLayout(2, 3, 20, 20));
        cardsPanel.setBackground(BACKGROUND);
        cardsPanel.setBorder(new EmptyBorder(5, 5, 15, 5));

        cardsPanel.add(createCard("🏖", "Beach", DEFAULT_COUNT_TEXT));
        cardsPanel.add(createCard("⛰", "Mountain", DEFAULT_COUNT_TEXT));
        cardsPanel.add(createCard("🏙", "City", DEFAULT_COUNT_TEXT));
        cardsPanel.add(createCard("🎯", "Adventure", DEFAULT_COUNT_TEXT));
        cardsPanel.add(createCard("🏛", "Cultural", DEFAULT_COUNT_TEXT));

        // Empty sixth cell for the 2x3 grid
        cardsPanel.add(new JPanel() {{
            setOpaque(false);
        }});

        section.add(lblCategories, BorderLayout.NORTH);
        section.add(cardsPanel, BorderLayout.CENTER);

        return section;
    }

    // =========================
    // CATEGORY CARD
    // =========================

    private JPanel createCard(
            String icon, String name, String destinations) {

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new LineBorder(Color.LIGHT_GRAY, 1));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Icon area
        JPanel iconPanel = new JPanel(new GridBagLayout());
        iconPanel.setBackground(PRIMARY);
        iconPanel.setPreferredSize(new Dimension(200, 140));

        JLabel lblIcon = new JLabel(icon);
        lblIcon.setFont(new Font(EMOJI_FONT, Font.PLAIN, 40));
        iconPanel.add(lblIcon);

        // Category information
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(new Color(250, 250, 250));
        infoPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel lblName = new JLabel(name);
        lblName.setFont(new Font(FONT_NAME, Font.BOLD, 16));
        lblName.setForeground(new Color(35, 35, 35));

        JLabel lblDest = new JLabel(destinations);
        lblDest.setFont(new Font(FONT_NAME, Font.PLAIN, 13));
        lblDest.setForeground(Color.BLACK);

        countLabels.put(name, lblDest);

        infoPanel.add(lblName);
        infoPanel.add(Box.createVerticalStrut(5));
        infoPanel.add(lblDest);

        card.add(iconPanel, BorderLayout.CENTER);
        card.add(infoPanel, BorderLayout.SOUTH);

        // Make entire card and its children clickable
        MouseAdapter clickListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                categoryListener.accept(name);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBorder(new LineBorder(PRIMARY, 2));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBorder(new LineBorder(Color.LIGHT_GRAY, 1));
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

    // One destination card: gradient emoji header, name, category badges
    private JPanel createDestinationCard(
            String name, List<String> categories) {

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new LineBorder(new Color(226, 232, 240), 1));
        card.setPreferredSize(new Dimension(200, 260));

        // Header with a gradient and the first category's icon
        JPanel header = new JPanel(new GridBagLayout()) {
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
        header.setPreferredSize(new Dimension(200, 120));

        JLabel lblIcon = new JLabel(iconFor(categories.get(0)));
        lblIcon.setFont(new Font(EMOJI_FONT, Font.PLAIN, 44));
        header.add(lblIcon);

        // Info section
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(Color.WHITE);
        info.setBorder(new EmptyBorder(14, 14, 14, 14));

        // HTML lets long names wrap onto extra lines
        JLabel lblName = new JLabel(
                "<html><body style='width:200px'>"
                + name.replace("&", "&amp;")
                + "</body></html>"
        );
        lblName.setFont(new Font(FONT_NAME, Font.BOLD, 16));
        lblName.setForeground(new Color(15, 23, 42));
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

        // Hover effect
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBorder(new LineBorder(PRIMARY, 2));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBorder(
                        new LineBorder(new Color(226, 232, 240), 1)
                );
            }
        });

        return card;
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
}