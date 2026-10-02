
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
    private static final String DEFAULT_COUNT_TEXT = "0 destinations";
    
    // New Color Palette
    private static final Color PRIMARY = new Color(0x1D9E75);
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

    // Destination display components
    private JLabel lblDestinationTitle;
    private DefaultListModel<String> destinationModel;
    private JList<String> destinationList;

    public HomeView() {
        setTitle("TravelMatch");
        setSize(1400, 900);
        setMinimumSize(new Dimension(900, 650));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main container
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(240, 245, 250));

        // NAVBAR
        JPanel navBar = createNavBar();

        // HERO SECTION
        JPanel heroPanel = createHeroPanel();

        // CATEGORY SECTION
        JPanel categoryPanel = createCategorySection();

        // Destination results section
        JPanel destinationPanel = createDestinationPanel();

        // Content area
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(new Color(240, 245, 250));

        contentPanel.add(heroPanel, BorderLayout.NORTH);

        // Scrollable content containing categories and results
        JPanel centerContent = new JPanel();
        centerContent.setLayout(new BoxLayout(
                centerContent, BoxLayout.Y_AXIS
        ));
        centerContent.setBackground(new Color(240, 245, 250));

        categoryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        destinationPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        centerContent.add(categoryPanel);
        centerContent.add(destinationPanel);

        JScrollPane mainScroll = new JScrollPane(centerContent);
        mainScroll.setBorder(null);
        mainScroll.getVerticalScrollBar().setUnitIncrement(16);
        mainScroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        contentPanel.add(mainScroll, BorderLayout.CENTER);

        mainPanel.add(navBar, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        setContentPane(mainPanel);
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
        logo.setForeground(new Color(0x1D9E75));

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
        heroPanel.setBackground(new Color(0x1D9E75));
        heroPanel.setPreferredSize(new Dimension(1400, 250));
        heroPanel.setBorder(new EmptyBorder(35, 20, 25, 20));

        JLabel lblTitle = new JLabel(
                "Discover Your Perfect Destination"
        );
        lblTitle.setFont(new Font(
                FONT_NAME, Font.BOLD, 36
        ));
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSubtitle = new JLabel(
                "Let us recommend the ideal travel spot "
                + "based on your preferences"
        );
        lblSubtitle.setFont(new Font(
                FONT_NAME, Font.PLAIN, 14
        ));
        lblSubtitle.setForeground(Color.WHITE);
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel searchPanel = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 8, 0)
        );
        searchPanel.setOpaque(false);

        txtSearch = new JTextField(30);
        txtSearch.setPreferredSize(new Dimension(330, 35));
        txtSearch.setFont(new Font(
                FONT_NAME, Font.PLAIN, 14
        ));

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
        section.setBackground(new Color(240, 245, 250));
        section.setBorder(new EmptyBorder(15, 10, 5, 10));

        JLabel lblCategories = new JLabel("Popular Categories");
        lblCategories.setFont(new Font(
                FONT_NAME, Font.BOLD, 24
        ));
        lblCategories.setForeground(new Color(35, 35, 35));
        lblCategories.setBorder(
                new EmptyBorder(5, 5, 15, 5)
        );

        // Two rows of category cards
        JPanel cardsPanel = new JPanel(
                new GridLayout(2, 3, 20, 20)
        );
        cardsPanel.setBackground(BACKGROUND);
        cardsPanel.setBorder(new EmptyBorder(5, 5, 15, 5));

        cardsPanel.add(createCard(
                "🏖", "Beach", DEFAULT_COUNT_TEXT
        ));

        cardsPanel.add(createCard(
                "⛰", "Mountain", DEFAULT_COUNT_TEXT
        ));

        cardsPanel.add(createCard(
                "🏙", "City", DEFAULT_COUNT_TEXT
        ));

        cardsPanel.add(createCard(
                "🎯", "Adventure", DEFAULT_COUNT_TEXT
        ));

        cardsPanel.add(createCard(
                "🏛", "Cultural", DEFAULT_COUNT_TEXT
        ));

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
        card.setBorder(new LineBorder(
                Color.LIGHT_GRAY, 1
        ));
        card.setCursor(new Cursor(
                Cursor.HAND_CURSOR
        ));

        // Icon area
        JPanel iconPanel = new JPanel(
                new GridBagLayout()
        );
        iconPanel.setBackground(PRIMARY);
        iconPanel.setPreferredSize(
                new Dimension(200, 140)
        );

        JLabel lblIcon = new JLabel(icon);
        lblIcon.setFont(new Font(
                "Segoe UI Emoji", Font.PLAIN, 40
        ));
        iconPanel.add(lblIcon);

        // Category information
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(
                infoPanel, BoxLayout.Y_AXIS
        ));
        infoPanel.setBackground(new Color(250, 250, 250));
        infoPanel.setBorder(new EmptyBorder(
                10, 10, 10, 10
        ));

        JLabel lblName = new JLabel(name);
        lblName.setFont(new Font(
                FONT_NAME, Font.BOLD, 16
        ));
        lblName.setForeground(new Color(35, 35, 35));

        JLabel lblDest = new JLabel(destinations);
        lblDest.setFont(new Font(
                FONT_NAME, Font.PLAIN, 13
        ));
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
                card.setBorder(new LineBorder(
                        new Color(41, 98, 255), 2
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBorder(new LineBorder(
                        Color.LIGHT_GRAY, 1
                ));
            }
        };

        addClickListenerRecursively(card, clickListener);

        return card;
    }

    // Attach mouse listener to card and all its children
    private void addClickListenerRecursively(
            Component component, MouseAdapter listener) {

        component.addMouseListener(listener);
        component.setCursor(new Cursor(
                Cursor.HAND_CURSOR
        ));

        if (component instanceof Container) {
            for (Component child :
                    ((Container) component).getComponents()) {
                addClickListenerRecursively(child, listener);
            }
        }
    }

    // =========================
    // DESTINATION DISPLAY
    // =========================

    private JPanel createDestinationPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new CompoundBorder(
                new EmptyBorder(10, 15, 15, 15),
                new LineBorder(
                        new Color(220, 220, 220), 1
                )
        ));

        panel.setPreferredSize(
                new Dimension(1200, 280)
        );

        lblDestinationTitle = new JLabel(
                "Select a category to explore destinations"
        );
        lblDestinationTitle.setFont(new Font(
                FONT_NAME, Font.BOLD, 20
        ));
        lblDestinationTitle.setForeground(
                new Color(35, 35, 35)
        );

        destinationModel = new DefaultListModel<>();

        destinationList = new JList<>(
                destinationModel
        );
        destinationList.setFont(new Font(
                FONT_NAME, Font.PLAIN, 16
        ));
        destinationList.setForeground(
                new Color(40, 40, 40)
        );
        destinationList.setBackground(Color.WHITE);
        destinationList.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );
        destinationList.setFixedCellHeight(32);
        destinationList.setBorder(
                new EmptyBorder(5, 10, 5, 10)
        );

        JScrollPane scrollPane = new JScrollPane(
                destinationList
        );
        scrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        );
        scrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );

        panel.add(
                lblDestinationTitle,
                BorderLayout.NORTH
        );
        panel.add(
                scrollPane,
                BorderLayout.CENTER
        );

        return panel;
    }

    // Display selected category and its destinations
    public void showCategoryDestinations(
            String category, List<String> destinations) {

        lblDestinationTitle.setText(
                category + " Destinations ("
                + destinations.size() + ")"
        );

        destinationModel.clear();

        for (String destination : destinations) {
            destinationModel.addElement(destination);
        }

        destinationList.clearSelection();

        // Bring destination list into view
        lblDestinationTitle.scrollRectToVisible(
                lblDestinationTitle.getBounds()
        );
    }

    // Display search results in the same UI list
    public void showSearchResults(
            String query, List<String> results) {

        lblDestinationTitle.setText(
                "Search Results for \"" + query
                + "\" (" + results.size() + ")"
        );

        destinationModel.clear();

        for (String result : results) {
            destinationModel.addElement(result);
        }

        destinationList.clearSelection();
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

    public void addCategoryListener(
            Consumer<String> listener) {
        this.categoryListener = listener;
    }

    // =========================
    // CATEGORY COUNT
    // =========================

    public void setCategoryCount(
            String category, int count) {

        JLabel label = countLabels.get(category);

        if (label != null) {
            label.setText(
                    count + " destinations"
            );
        }
    }

    // =========================
    // SEARCH INPUT
    // =========================

    public String getSearchText() {
        return txtSearch.getText().trim();
    }
}