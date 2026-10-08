package view;

import model.PreferenceModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * PreferenceView - collects traveler preferences for destination recommendations.
 *
 * MVC role:
 * Displays the preference form only.
 * No recommendation logic is performed here.
 * Controllers read the user input and handle processing.
 */
public class PreferenceView extends JFrame {

    private static final Color PRIMARY = new Color(0x1D9E75);
    private static final Color ERROR   = new Color(0xC0392B);

    private final JComboBox<String> budgetBox;
    private final JComboBox<String> monthBox;
    private final JComboBox<String> groupTypeBox;
    private final JComboBox<String> activityLevelBox;

    private final JCheckBox beachInterest;
    private final JCheckBox adventureInterest;
    private final JCheckBox natureInterest;
    private final JCheckBox cultureInterest;
    private final JCheckBox foodInterest;

    private final JButton recommendButton;
    private final JButton resetButton;
    private final JButton forwardButton;

    private final JLabel messageLabel;

    public PreferenceView() {
        super("TravelMatch - Travel Preferences");

        budgetBox = new JComboBox<>(new String[]{
                "--- Select ---",
                "Low Budget (Under ₱3500 per person)",
                "Medium Budget (₱3,500 to ₱8,000 per person)",
                "High Budget (₱8,000+ per person)"
        });

        monthBox = new JComboBox<>(new String[]{
                "--- Select ---", "January", "February", "March", "April",
                "May", "June", "July", "August",
                "September", "October", "November", "December"
        });

        groupTypeBox = new JComboBox<>(new String[]{
                "--- Select ---",
                "Solo Traveler",
                "Couple",
                "Family",
                "Friends"
        });

        activityLevelBox = new JComboBox<>(new String[]{
                "--- Select ---",
                "Low Activity Level",
                "Medium Activity Level",
                "Hard Activity Level"
        });

        beachInterest = new JCheckBox("Beach");
        adventureInterest = new JCheckBox("Adventure");
        natureInterest = new JCheckBox("Nature");
        cultureInterest = new JCheckBox("Culture");
        foodInterest = new JCheckBox("Food");

        recommendButton = new JButton("Find Destinations");
        resetButton = new JButton("Reset");
        forwardButton = new JButton("Back to Home");
        messageLabel = new JLabel(" ");

        initComponents();
        layoutComponents();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(600, 650);
        setResizable(false);
        setLocationRelativeTo(null);
    }

    // ---------- Setup ----------

    private void initComponents() {

        recommendButton.setBackground(PRIMARY);
        recommendButton.setForeground(Color.BLACK);
        recommendButton.setFocusPainted(false);

        resetButton.setFocusPainted(false);
        forwardButton.setFocusPainted(false);
        messageLabel.setForeground(ERROR);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
    }

    private void layoutComponents() {

        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(25, 40, 25, 40));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.insets = new Insets(4, 0, 4, 0);

        JLabel title = new JLabel("Travel Preferences", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setForeground(PRIMARY);

        JLabel subtitle = new JLabel(
                "Tell us what kind of trip you're looking for",
                SwingConstants.CENTER
        );
        subtitle.setForeground(Color.GRAY);

        int row = 0;

        gc.gridy = row++;
        content.add(title, gc);

        gc.gridy = row++;
        gc.insets = new Insets(0, 0, 20, 0);
        content.add(subtitle, gc);

        gc.insets = new Insets(4, 0, 2, 0);

        gc.gridy = row++;
        content.add(new JLabel("Budget"), gc);

        gc.gridy = row++;
        content.add(budgetBox, gc);

        gc.gridy = row++;
        content.add(new JLabel("Travel Month"), gc);

        gc.gridy = row++;
        content.add(monthBox, gc);

        gc.gridy = row++;
        content.add(new JLabel("Group Type"), gc);

        gc.gridy = row++;
        content.add(groupTypeBox, gc);

        gc.gridy = row++;
        content.add(new JLabel("Activity Level"), gc);
        
        gc.gridy = row++;
        content.add(activityLevelBox, gc);

        gc.gridy = row++;
        content.add(new JLabel("Interests"), gc);

        JPanel interestsPanel = new JPanel(new GridLayout(0, 2));
        interestsPanel.add(beachInterest);
        interestsPanel.add(adventureInterest);
        interestsPanel.add(natureInterest);
        interestsPanel.add(cultureInterest);
        interestsPanel.add(foodInterest);

        gc.gridy = row++;
        content.add(interestsPanel, gc);

        gc.gridy = row++;
        gc.insets = new Insets(10, 0, 10, 0);
        content.add(messageLabel, gc);

        gc.gridy = row++;
        gc.ipady = 8;
        content.add(recommendButton, gc);

        gc.gridy = row++;
        gc.ipady = 0;
        content.add(resetButton, gc);

        gc.gridy = row++;
        gc.ipady = 0;
        content.add(forwardButton, gc);

        setContentPane(content);
    }

    // ---------- Get User Input ----------

    public String getBudget() {
        return (String) budgetBox.getSelectedItem();
    }

    public String getMonth() {
        return (String) monthBox.getSelectedItem();
    }

    public String getGroupType() {
        return (String) groupTypeBox.getSelectedItem();
    }

    public String getActivityLevel() {
        return (String) activityLevelBox.getSelectedItem();
    }

    public String[] getSelectedInterests() {

        java.util.List<String> interests = new java.util.ArrayList<>();

        if (beachInterest.isSelected()) interests.add("Beach");
        if (adventureInterest.isSelected()) interests.add("Adventure");
        if (natureInterest.isSelected()) interests.add("Nature");
        if (cultureInterest.isSelected()) interests.add("Culture");
        if (foodInterest.isSelected()) interests.add("Food");

        return interests.toArray(new String[0]);
    }

    public void setPreferences(PreferenceModel preferences) {
        if (preferences == null) {
            throw new IllegalArgumentException("Preferences are required");
        }

        budgetBox.setSelectedItem(preferences.getBudget());
        monthBox.setSelectedItem(preferences.getMonth());
        groupTypeBox.setSelectedItem(preferences.getGroupType());
        activityLevelBox.setSelectedItem(preferences.getActivityLevel());

        java.util.List<String> interests =
                java.util.Arrays.asList(preferences.getInterests());
        beachInterest.setSelected(interests.contains("Beach"));
        adventureInterest.setSelected(interests.contains("Adventure"));
        natureInterest.setSelected(interests.contains("Nature"));
        cultureInterest.setSelected(interests.contains("Culture"));
        foodInterest.setSelected(interests.contains("Food"));
    }

    // ---------- Register Listeners ----------

    public void addRecommendListener(ActionListener listener) {
        recommendButton.addActionListener(listener);
    }

    public void addResetListener(ActionListener listener) {
        resetButton.addActionListener(listener);
    }

    public void addForwardListener(ActionListener listener) {
        forwardButton.addActionListener(listener);
    }

    // ---------- Update Screen ----------

    public void showError(String message) {
        messageLabel.setText(message);
    }

    public void clearMessage() {
        messageLabel.setText(" ");
    }

    public void clearForm() {

        budgetBox.setSelectedIndex(0);
        monthBox.setSelectedIndex(0);
        groupTypeBox.setSelectedIndex(0);
        activityLevelBox.setSelectedIndex(0);
        beachInterest.setSelected(false);
        adventureInterest.setSelected(false);
        natureInterest.setSelected(false);
        cultureInterest.setSelected(false);
        foodInterest.setSelected(false);

        clearMessage();
    }

    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    public void showView() {
        setVisible(true);
    }

    public void closeView() {
        dispose();
    }
}