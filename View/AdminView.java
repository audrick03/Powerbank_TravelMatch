package View;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class AdminView extends JFrame {
    private JTextField txtName, txtLocation, txtFee;
    private JComboBox<String> cmbSeason, cmbCategory;
    private JButton btnAdd, btnViewStats, btnLogout, btnClear;

    public AdminView() {
        setTitle("TravelMatch - Admin Panel");
        setSize(600, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        //Plain white background
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        //Title
        JLabel lblTitle = new JLabel("TravelMatch Admin Dashboard", SwingConstants.CENTER);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 24));
        lblTitle.setForeground(new Color(0, 102, 51));
        lblTitle.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, new Color(0, 128, 64)));

        //Input fields
        txtName = new JTextField();
        txtLocation = new JTextField();
        txtFee = new JTextField();

        //Combo boxes
        cmbSeason = new JComboBox<>(new String[]{"Summer", "Rainy", "Winter", "Spring"});
        cmbCategory = new JComboBox<>(new String[]{"Beach", "Mountain", "City", "Cultural", "Adventure"});

        //Buttons
        btnAdd = createStyledButton("Add Destination");
        btnViewStats = createStyledButton("View Statistics");
        btnLogout = createStyledButton("Logout");
        btnClear = createStyledButton("Clear Fields");

        //Validation for fee
        addNumberValidation(txtFee);

        //Layout
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        mainPanel.add(lblTitle, gbc);

        gbc.gridwidth = 1;
        addField(mainPanel, gbc, "🌐 Destination Name:", txtName, 1);
        addField(mainPanel, gbc, "🔑 Location:", txtLocation, 2);
        addField(mainPanel, gbc, "☀️ Best Season:", cmbSeason, 3);
        addField(mainPanel, gbc, "💰 Fee:", txtFee, 4);
        addField(mainPanel, gbc, "🏖️ Category:", cmbCategory, 5);

        gbc.gridx = 0; gbc.gridy = 6;
        mainPanel.add(btnAdd, gbc);
        gbc.gridx = 1;
        mainPanel.add(btnViewStats, gbc);

        gbc.gridx = 0; gbc.gridy = 7;
        mainPanel.add(btnClear, gbc);
        gbc.gridx = 1;
        mainPanel.add(btnLogout, gbc);

        add(mainPanel);
    }

    //Helper to style buttons
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(0, 153, 102));
        button.setForeground(Color.WHITE);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(new Color(0, 128, 64), 2, true));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { button.setBackground(new Color(0, 180, 90)); }
            public void mouseExited(MouseEvent e) { button.setBackground(new Color(0, 153, 102)); }
        });
        return button;
    }

    //Helper to add label + field
    private void addField(JPanel panel, GridBagConstraints gbc, String label, JComponent field, int row) {
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lbl.setForeground(new Color(0, 102, 51));
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        panel.add(field, gbc);
    }

    //Validation: numbers only
    private void addNumberValidation(JTextField field) {
        field.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isDigit(c) && c != KeyEvent.VK_BACK_SPACE) {
                    e.consume();
                    showMessage("Fee must be numeric!");
                }
            }
        });
    }

    // Clear fields
    public void clearFields() {
        txtName.setText("");
        txtLocation.setText("");
        txtFee.setText("");
        cmbSeason.setSelectedIndex(0);
        cmbCategory.setSelectedIndex(0);
    }

    // 🧩 Getters
    public String getDestinationName() { return txtName.getText(); }
    public String getDestinationLocation() { return txtLocation.getText(); }
    public String getBestSeason() { return (String) cmbSeason.getSelectedItem(); }
    public String getFee() { return txtFee.getText(); }
    public String getCategory() { return (String) cmbCategory.getSelectedItem(); }

    // 🧩 Listeners
    public void addDestinationListener(ActionListener listener) { btnAdd.addActionListener(listener); }
    public void viewStatsListener(ActionListener listener) { btnViewStats.addActionListener(listener); }
    public void logoutListener(ActionListener listener) { btnLogout.addActionListener(listener); }
    public void clearListener(ActionListener listener) { btnClear.addActionListener(listener); }

    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    public static void main(String[] args) {
        AdminView view = new AdminView();
        new Controller.AdminController(view);
        view.setVisible(true);
    }
}
