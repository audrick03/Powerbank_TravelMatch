package View;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class RecommendationView extends JFrame {
    private JPanel resultsPanel;
    private JButton btnBack, btnLogout;

    public RecommendationView() {
        setTitle("TravelMatch - Recommendations");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // White background with green accents
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);

        JLabel lblTitle = new JLabel("🌍 Recommended Destinations", SwingConstants.CENTER);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 26));
        lblTitle.setForeground(new Color(0, 102, 51));
        lblTitle.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, new Color(0, 128, 64)));

        resultsPanel = new JPanel();
        resultsPanel.setLayout(new GridLayout(0, 3, 15, 15)); // 3 columns of boxed cards
        resultsPanel.setBackground(Color.WHITE);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBackground(Color.WHITE);
        btnBack = createStyledButton("⬅ Back");
        btnLogout = createStyledButton("Logout");
        buttonPanel.add(btnBack);
        buttonPanel.add(btnLogout);

        mainPanel.add(lblTitle, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(resultsPanel), BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

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

    // Show recommendations with boxed cards
    public void showRecommendations(List<String> destinations) {
        resultsPanel.removeAll();
        for (String destination : destinations) {
            JPanel card = new JPanel(new BorderLayout());
            card.setBorder(BorderFactory.createLineBorder(new Color(0, 128, 64), 3));
            card.setBackground(Color.WHITE);

            JLabel textLabel = new JLabel(destination, SwingConstants.CENTER);
            textLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            textLabel.setForeground(new Color(0, 102, 51));

            card.add(textLabel, BorderLayout.CENTER);
            resultsPanel.add(card);
        }
        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    public void addBackListener(ActionListener listener) { btnBack.addActionListener(listener); }
    public void addLogoutListener(ActionListener listener) { btnLogout.addActionListener(listener); }

    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    public static void main(String[] args) {
        RecommendationView view = new RecommendationView();
        new Controller.RecommendationController(view);
        view.setVisible(true);
    }
}
