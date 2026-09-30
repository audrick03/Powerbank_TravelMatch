package View;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class AdminView extends JFrame {
    private JTextField txtName, txtLocation, txtSeason, txtFee, txtCategory;
    private JButton btnAdd, btnViewStats, btnLogout;

    public AdminView() {
        setTitle("TravelMatch - Admin Panel");
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(8, 2, 10, 10));
        panel.setBackground(Color.WHITE);

        JLabel lblTitle = new JLabel("TravelMatch Admin Dashboard", SwingConstants.CENTER);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        lblTitle.setForeground(new Color(0, 153, 102));

        txtName = new JTextField();
        txtLocation = new JTextField();
        txtSeason = new JTextField();
        txtFee = new JTextField();
        txtCategory = new JTextField();

        btnAdd = new JButton("Add Destination");
        btnViewStats = new JButton("View Statistics");
        btnLogout = new JButton("Logout");

        panel.add(new JLabel("Destination Name:"));
        panel.add(txtName);
        panel.add(new JLabel("Location:"));
        panel.add(txtLocation);
        panel.add(new JLabel("Best Season:"));
        panel.add(txtSeason);
        panel.add(new JLabel("Fee:"));
        panel.add(txtFee);
        panel.add(new JLabel("Category:"));
        panel.add(txtCategory);
        panel.add(btnAdd);
        panel.add(btnViewStats);
        panel.add(btnLogout);

        add(lblTitle, BorderLayout.NORTH);
        add(panel, BorderLayout.CENTER);
    }

    public String getDestinationName() { return txtName.getText(); }
    public String getDestinationLocation() { return txtLocation.getText(); }
    public String getBestSeason() { return txtSeason.getText(); }
    public String getFee() { return txtFee.getText(); }
    public String getCategory() { return txtCategory.getText(); }

    public void addDestinationListener(ActionListener listener) { btnAdd.addActionListener(listener); }
    public void viewStatsListener(ActionListener listener) { btnViewStats.addActionListener(listener); }
    public void logoutListener(ActionListener listener) { btnLogout.addActionListener(listener); }

    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    public static void main(String[] args) {
        AdminView view = new AdminView();
        new Controller.AdminController(view);
        view.setVisible(true);
    }
}
