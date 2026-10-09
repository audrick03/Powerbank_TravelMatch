package view;

import controller.*;
import model.*;
import repository.*;
import service.DestinationRecommendation;
import service.PreferenceService;

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
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Recommendations screen (one scrolling page):
 *   top bar  ->  destinations  ->  destination panel
 * The view only displays data; RecommendationController decides what to show.
 */
public class RecommendationView extends JFrame {

    private static final String FONT = "Segoe UI";
    private static final String LI_CLOSE = "</li>";
    private static final String[] IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png"};

    private static final Color PRIMARY = new Color(0x0F7B52);
    private static final Color PRIMARY_LIGHT = new Color(0x15A06B);
    private static final Color PANEL_TINT = new Color(0xEAF7F1);
    private static final Color BACKGROUND = new Color(0xF4F8F7);
    private static final Color TEXT_DARK = new Color(0x1B2B34);
    private static final Color TEXT_MUTED = new Color(0x5F6F7A);
    private static final Color BORDER_LIGHT = new Color(0xDDE5E8);

    private final Map<String, BufferedImage> imageCache = new HashMap<>();

    // top bar
    private final JButton btnHome = new NavButton("🏠  Home");

    // destination section
    private final JLabel lblDestTitle = new JLabel("Destinations");
    private final JLabel lblDestSub = new JLabel("Explore the best destinations across the Philippines.");
    private final JPanel destGrid = new JPanel(new GridLayout(0, 5, 14, 14));

    // activities section
    private final JPanel activityPanel = new JPanel(new BorderLayout(18, 0));
    private final JButton btnDetails = new ActionButton("View Details", true);
    private final JButton btnBackToDestinations = new ActionButton("←  Back to Destinations", false);
    private DestinationModel shownDestination;

    private ScrollPanel content;
    private JScrollPane scroll;
    private int row = 0;

    private transient Consumer<DestinationModel> destinationListener = d -> {};
    private transient Consumer<DestinationModel> detailsListener = d -> {};

    public RecommendationView() {
        setTitle("TravelMatch - Recommendations");
        setSize(1280, 860);
        setMinimumSize(new Dimension(1000, 700));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(createTopBar(), BorderLayout.NORTH);

        content = new ScrollPanel(new GridBagLayout());
        content.setBackground(BACKGROUND);
        content.setBorder(new EmptyBorder(14, 28, 28, 28));

        addRow(createDestinationSection(), 0);

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
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
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
    public void addBackToDestinationsListener(ActionListener l) { btnBackToDestinations.addActionListener(l); }

    public void addDestinationListener(Consumer<DestinationModel> l){ destinationListener = l; }
    public void addDetailsListener(Consumer<DestinationModel> l)    { detailsListener = l; }

    /** Destination cards (every region and category). 'selected' may be null. */
    public void showDestinations(List<DestinationModel> list, DestinationModel selected) {
        lblDestTitle.setText("Destinations");
        lblDestSub.setText("Explore the best destinations across the Philippines.");
        showDestinationCards(list, selected, null);
    }

    /** Personalized destination cards, ordered by preference compatibility. */
    public void showRecommendations(List<DestinationRecommendation> recommendations,
                                    DestinationModel selected) {
        if (recommendations == null) {
            throw new IllegalArgumentException("Recommendations are required");
        }
        lblDestTitle.setText("Your Top 10 Recommendations");
        lblDestSub.setText("Ranked for your saved travel preferences.");
        List<DestinationRecommendation> topRecommendations = recommendations.subList(
                0, Math.min(recommendations.size(), PreferenceService.MAX_RECOMMENDATIONS));
        List<DestinationModel> destinations = new java.util.ArrayList<>();
        for (DestinationRecommendation recommendation : topRecommendations) {
            destinations.add(recommendation.getDestination());
        }
        showDestinationCards(destinations, selected, topRecommendations);
    }

    private void showDestinationCards(List<DestinationModel> list, DestinationModel selected,
                                      List<DestinationRecommendation> recommendations) {
        destGrid.removeAll();
        if (list.isEmpty()) {
            destGrid.setLayout(new GridLayout(0, 1));
            String emptyMessage = recommendations == null
                    ? "No destinations found yet."
                    : "No destinations matched your preferences.";
            destGrid.add(label(emptyMessage, 15, false, TEXT_MUTED));
        } else {
            destGrid.setLayout(new GridLayout(0, 5, 14, 14));
            int visibleCount = Math.min(list.size(), PreferenceService.MAX_RECOMMENDATIONS);
            for (int index = 0; index < visibleCount; index++) {
                DestinationModel destination = list.get(index);
                String match = recommendations == null ? null
                        : recommendations.get(index).getMatchPercentage() + "% match";
                destGrid.add(createDestinationCard(
                        destination, null, destination == selected, match));
            }
            // keep card width constant when a row is not full
            int missing = (5 - visibleCount % 5) % 5;
            for (int i = 0; i < missing; i++) {
                JPanel blank = new JPanel();
                blank.setOpaque(false);
                destGrid.add(blank);
            }
        }
        refresh(destGrid);
    }

    /** The green destination panel (photo, name, buttons) under the destination cards. */
    public void showActivities(DestinationModel d, String category) {
        shownDestination = d;
        String cat = category != null ? category : d.getCategories().get(0);

        activityPanel.removeAll();

        // left: destination photo
        CoverPanel photo = new CoverPanel(loadDestinationImage(d, cat));
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
        titles.add(left(label(d.getName(), 18, true, TEXT_DARK)));
        titles.add(Box.createVerticalStrut(2));
        titles.add(left(label(d.getProvince() + " · " + d.getRegion(), 12, false, TEXT_MUTED)));
        header.add(titles, BorderLayout.WEST);
        center.add(header, BorderLayout.NORTH);
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
        buttons.add(btnBackToDestinations, c);
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

    public void scrollToTop() {
        SwingUtilities.invokeLater(() ->
                scroll.getViewport().setViewPosition(new Point(0, 0)));
    }

    /** Full details of one destination (View Details button). */
    public void showDetails(DestinationModel d, String category) {
        String cat = category != null ? category : d.getCategories().get(0);
        StringBuilder sb = new StringBuilder("<html><body style='font-family:" + FONT
                + ";width:440px'>");
        sb.append("<h2 style='color:#0F7B52;margin:0'>").append(esc(d.getName())).append("</h2>");
        sb.append("<p style='color:#5F6F7A;margin:2px 0 8px 0'>").append(esc(d.getProvince()))
                .append(" · ").append(esc(d.getRegion())).append("</p>");
        sb.append("<p><span style='color:#FFB300'>★</span> <b>User Reviews:</b> ").append(d.getScore()).append("/5</p>");
        sb.append("<p>").append(esc(d.getDescription())).append("</p>");
        sb.append("<p><b>Best time:</b> ").append(esc(nz(d.getBestTime())))
                .append("<br><b>Duration:</b> ").append(esc(nz(d.getDuration())))
                .append("<br><b>Budget:</b> ").append(esc(nz(d.getBudget())))
                .append("<br><b>Difficulty:</b> ").append(esc(nz(d.getDifficulty()))).append("</p>");
        sb.append("<p><b>Activities</b></p><ul>");
        for (String a : d.getActivities(cat)) {
            sb.append("<li>").append(esc(a)).append(LI_CLOSE);
        }
        sb.append("</ul><p><b>Places to visit</b></p><ul>");
        for (String p : d.getPlaces()) {
            sb.append("<li>").append(esc(p.replace("|", "—"))).append(LI_CLOSE);
        }
        sb.append("</ul><p><b>Travel tips</b></p><ul>");
        for (String t : DestinationRepository.travelTips(cat)) {
            sb.append("<li>").append(esc(t)).append(LI_CLOSE);
        }
        sb.append("</ul><p><b>What to bring</b></p><ul>");
        for (String b : DestinationRepository.whatToBring(cat)) {
            sb.append("<li>").append(esc(b)).append(LI_CLOSE);
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
        // Logo: images/Logo.png (or .jpg); nothing is shown if it is missing
        BufferedImage logoImg = loadImage("Logo");
        JLabel pin = new JLabel();
        if (logoImg != null) {
            pin.setIcon(new ImageIcon(logoImg.getScaledInstance(-1, 36, Image.SCALE_SMOOTH)));
        }
        JLabel title = label("TravelMatch  —  Recommendations", 24, true, PRIMARY);
        brand.add(pin);
        brand.add(title);

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        nav.setOpaque(false);
        nav.add(btnHome);

        bar.add(brand, BorderLayout.WEST);
        bar.add(nav, BorderLayout.EAST);
        return bar;
    }

    private JPanel createDestinationSection() {
        JPanel section = new JPanel(new BorderLayout(0, 10));
        section.setOpaque(false);

        lblDestTitle.setFont(new Font(FONT, Font.BOLD, 22));
        lblDestTitle.setForeground(PRIMARY);
        lblDestSub.setFont(new Font(FONT, Font.PLAIN, 14));
        lblDestSub.setForeground(TEXT_MUTED);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleRow.setOpaque(false);
        titleRow.add(lblDestTitle);

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

    private SelectableCard createDestinationCard(DestinationModel d, String category,
                                                 boolean selected, String match) {
        SelectableCard card = new SelectableCard(new BorderLayout(0, 8), 6);

        String firstCat = d.getCategories().get(0);
        CoverPanel photo = new CoverPanel(loadDestinationImage(d, firstCat));
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
        top.add(left(label("" + d.getProvince(), 12, false, TEXT_MUTED)));
        if (match != null) {
            top.add(Box.createVerticalStrut(4));
            top.add(left(createBadge(match, false)));
        }
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

    // =========================================================
    // SMALL UI HELPERS
    // =========================================================

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

        private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String nz(String s) {
        return s == null ? "—" : s;
    }

    private static String clean(String name) {
        return name.replaceAll("[^A-Za-z0-9 _-]", "").trim();
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
        try (InputStream in = RecommendationView.class.getResourceAsStream("/images/" + file)) {
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

    // Paints a photo scaled to "cover" the panel, or a green gradient if no photo.
    private static class CoverPanel extends JPanel {
        private final transient BufferedImage source;
        private transient BufferedImage cache;
        private int cw;
        private int ch;

        CoverPanel(BufferedImage source) {
            this.source = source;
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
                g2.draw(new RoundRectangle2D.Float(0.8f, 0.8f, getWidth() - 1.6f, getHeight() - 1.6f, 6f, 6f));
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
                g2.draw(new RoundRectangle2D.Float(0.8f, 0.8f, getWidth() - 1.6f, getHeight() - 1.6f, 6f, 6f));
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