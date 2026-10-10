package view;

import controller.AdminController;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Predicate;

public class AdminView extends JFrame {
    private static final int MAX_TEXT_LENGTH = 255;

    private JTextField txtName;
    private JTextField txtLocation;
    private JTextField txtFee;
    private JTextArea txtDescription;
    private JTextArea txtTravelTips;
    private JTextArea txtWhatToBring;
    private JTextArea txtRecommendedPlaces;
    private JComboBox<String> cmbStartMonth;
    private JComboBox<String> cmbEndMonth;
    private JComboBox<String> cmbDuration;
    private JComboBox<String> cmbDifficulty;
    private JComboBox<String> cmbRegion;
    private JComboBox<String> cmbCategory;
    private JButton btnAdd;
    private JButton btnLogout;
    private JButton btnClear;

    public AdminView() {
        super("TravelMatch - Admin Panel");
        initializeView();
    }

    private void initializeView() {
        setSize(760, 760);
        setMinimumSize(new Dimension(650, 600));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel title = new JLabel("TravelMatch Admin Dashboard", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(new Color(0, 102, 51));
        title.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, new Color(0, 128, 64)));
        content.add(title, BorderLayout.NORTH);

        txtName = new JTextField(24);
        txtLocation = new JTextField(24);
        txtFee = new JTextField(24);
        txtDescription = createTextArea(4);
        txtTravelTips = createTextArea(4);
        txtWhatToBring = createTextArea(4);
        txtRecommendedPlaces = createTextArea(4);

        String[] months = {
                "--- Select Month ---", "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        };
        cmbStartMonth = new JComboBox<>(months);
        cmbEndMonth = new JComboBox<>(months);
        cmbDuration = new JComboBox<>(new String[]{
                "--- Select Day ---","1 day", "2 days", "3 days", "4 days", "5 days",
                "6 days", "7 days", "8 days", "9 days", "10 days"
        });
        cmbDifficulty = new JComboBox<>(new String[]{"--- Select Difficulty ---", "Easy", "Moderate", "Hard"});
        cmbRegion = new JComboBox<>(new String[]{"--- Select Region ---", "Luzon", "Visayas", "Mindanao"});
        cmbCategory = new JComboBox<>(new String[]{
                "--- Select Category ---", "Beach", "Mountain", "City", "Cultural", "Adventure"
        });

        addInputValidation(txtName, text -> text.isEmpty()
                || isValidNameOrLocation(text));
        addInputValidation(txtLocation, text -> text.isEmpty()
                || isValidNameOrLocation(text));
        addInputValidation(txtFee, text -> text.length() <= MAX_TEXT_LENGTH
                && (text.isEmpty() || text.matches("\\d*")));
        addInputValidation(txtDescription, text -> text.codePointCount(0, text.length())
                <= MAX_TEXT_LENGTH);
        addInputValidation(txtTravelTips, AdminView::isWithinLineLengthLimit);
        addInputValidation(txtWhatToBring, AdminView::isWithinLineLengthLimit);
        addInputValidation(txtRecommendedPlaces, AdminView::isWithinLineLengthLimit);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(7, 8, 7, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        addField(form, gbc, "Destination Name (max 255):", txtName, 0);
        addField(form, gbc, "Location / Province (max 255):", txtLocation, 1);
        addField(form, gbc, "Description (max 255):", new JScrollPane(txtDescription), 2);
        addField(form, gbc, "Region:", cmbRegion, 3);
        addField(form, gbc, "Category:", cmbCategory, 4);
        addField(form, gbc, "Fee per person:", txtFee, 5);
        
        JPanel monthRange = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        monthRange.setOpaque(false);
        monthRange.add(cmbStartMonth);
        monthRange.add(new JLabel("-"));
        monthRange.add(cmbEndMonth);

        addField(form, gbc, "Best Time to Visit (Month range):", monthRange, 6);
        addField(form, gbc, "Recommended Duration:", cmbDuration, 7);
        addField(form, gbc, "Difficulty:", cmbDifficulty, 8);
        addField(form, gbc, "Travel Tips (one per line, max 255 each):",
                new JScrollPane(txtTravelTips), 9);
        addField(form, gbc, "What to Bring (one per line, max 255 each):",
                new JScrollPane(txtWhatToBring), 10);
        addField(form, gbc,
                "<html>Recommended Places:<br><small>One per line; max 255 each; "
                        + "use | before a description</small></html>",
                new JScrollPane(txtRecommendedPlaces), 11);
        gbc.gridx = 0;
        gbc.gridy = 12;
        gbc.gridwidth = 2;
        gbc.weighty = 1;
        form.add(Box.createVerticalGlue(), gbc);

        content.add(new JScrollPane(form), BorderLayout.CENTER);

        btnAdd = createStyledButton("Add Destination");
        btnClear = createStyledButton("Clear Fields");
        btnLogout = createStyledButton("Logout");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        buttons.setBackground(Color.WHITE);
        buttons.add(btnLogout);
        buttons.add(btnClear);
        buttons.add(btnAdd);
        content.add(buttons, BorderLayout.SOUTH);

        setContentPane(content);
    }

    private JTextArea createTextArea(int rows) {
        JTextArea textArea = new JTextArea(rows, 24);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        return textArea;
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(0, 92, 60));
        button.setForeground(Color.WHITE);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 65, 42), 1, true),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(0, 112, 72));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(0, 92, 60));
            }
        });
        return button;
    }

    private void addField(JPanel panel, GridBagConstraints gbc, String label,
                          JComponent field, int row) {
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx = 0;
        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        fieldLabel.setForeground(new Color(0, 102, 51));
        panel.add(fieldLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
    }

    private void addInputValidation(JTextComponent component, Predicate<String> isValid) {
        ((AbstractDocument) component.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass bypass, int offset, String text,
                                     AttributeSet attributes) throws BadLocationException {
                replaceIfValid(bypass, offset, 0, text, attributes, isValid);
            }

            @Override
            public void replace(FilterBypass bypass, int offset, int length, String text,
                                AttributeSet attributes) throws BadLocationException {
                replaceIfValid(bypass, offset, length, text, attributes, isValid);
            }

            @Override
            public void remove(FilterBypass bypass, int offset, int length)
                    throws BadLocationException {
                replaceIfValid(bypass, offset, length, "", null, isValid);
            }
        });
    }

    private static boolean isValidNameOrLocation(String value) {
        if (value == null || value.isBlank()
                || value.codePointCount(0, value.length()) > MAX_TEXT_LENGTH
                || value.codePoints().noneMatch(Character::isLetter)) {
            return false;
        }
        return value.codePoints().allMatch(codePoint -> Character.isLetter(codePoint)
                || codePoint == ' '
                || Character.getType(codePoint) == Character.NON_SPACING_MARK
                || Character.getType(codePoint) == Character.COMBINING_SPACING_MARK
                || Character.getType(codePoint) == Character.ENCLOSING_MARK
                || Character.getType(codePoint) == Character.CONNECTOR_PUNCTUATION
                || Character.getType(codePoint) == Character.DASH_PUNCTUATION
                || Character.getType(codePoint) == Character.START_PUNCTUATION
                || Character.getType(codePoint) == Character.END_PUNCTUATION
                || Character.getType(codePoint) == Character.INITIAL_QUOTE_PUNCTUATION
                || Character.getType(codePoint) == Character.FINAL_QUOTE_PUNCTUATION
                || Character.getType(codePoint) == Character.OTHER_PUNCTUATION);
    }

    private static boolean isWithinLineLengthLimit(String value) {
        if (value == null) {
            return false;
        }
        for (String line : value.split("\\R", -1)) {
            if (line.codePointCount(0, line.length()) > MAX_TEXT_LENGTH) {
                return false;
            }
        }
        return true;
    }

    private static void replaceIfValid(DocumentFilter.FilterBypass bypass,
                                       int offset, int length, String insertedText,
                                       AttributeSet attributes, Predicate<String> isValid)
            throws BadLocationException {
        Document document = bypass.getDocument();
        String current = document.getText(0, document.getLength());
        String insertion = insertedText == null ? "" : insertedText;
        String candidate = current.substring(0, offset) + insertion
                + current.substring(offset + length);
        if (isValid.test(candidate)) {
            bypass.replace(offset, length, insertion, attributes);
        }
    }

    public void clearFields() {
        txtName.setText("");
        txtLocation.setText("");
        txtDescription.setText("");
        txtTravelTips.setText("");
        txtWhatToBring.setText("");
        txtRecommendedPlaces.setText("");
        txtFee.setText("");
        cmbStartMonth.setSelectedIndex(0);
        cmbEndMonth.setSelectedIndex(0);
        cmbDuration.setSelectedIndex(0);
        cmbDifficulty.setSelectedIndex(0);
        cmbRegion.setSelectedIndex(0);
        cmbCategory.setSelectedIndex(0);
    }

    public String getDestinationName() { return txtName.getText(); }
    public String getDestinationLocation() { return txtLocation.getText(); }
    public String getDescription() { return txtDescription.getText(); }
    public String getBestSeason() {
        return cmbStartMonth.getSelectedItem() + " - " + cmbEndMonth.getSelectedItem();
    }
    public String getDuration() { return (String) cmbDuration.getSelectedItem(); }
    public String getDifficulty() { return (String) cmbDifficulty.getSelectedItem(); }
    public String getTravelTips() { return txtTravelTips.getText(); }
    public String getWhatToBring() { return txtWhatToBring.getText(); }
    public String getRecommendedPlaces() { return txtRecommendedPlaces.getText(); }
    public String getRegion() { return (String) cmbRegion.getSelectedItem(); }
    public String getFee() { return txtFee.getText(); }
    public String getCategory() { return (String) cmbCategory.getSelectedItem(); }
    public boolean hasSelectedChoices() {
        return cmbStartMonth.getSelectedIndex() > 0
                && cmbEndMonth.getSelectedIndex() > 0
                && cmbDuration.getSelectedIndex() > 0
                && cmbDifficulty.getSelectedIndex() > 0
                && cmbRegion.getSelectedIndex() > 0
                && cmbCategory.getSelectedIndex() > 0;
    }
    public boolean hasValidMonthRange() {
        return cmbStartMonth.getSelectedIndex() < cmbEndMonth.getSelectedIndex();
    }

    public void addDestinationListener(ActionListener listener) {
        btnAdd.addActionListener(listener);
    }

    public void logoutListener(ActionListener listener) {
        btnLogout.addActionListener(listener);
    }

    public void clearListener(ActionListener listener) {
        btnClear.addActionListener(listener);
    }

    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    public static void main(String[] args) {
        AdminView view = new AdminView();
        new AdminController(view);
        view.setVisible(true);
    }
}
