package View;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;



public class AdminView extends JFrame {
    private final JButton manageUsersButton = new JButton("Manage Users");
    private final JButton manageDestinationsButton = new JButton("Manage Destinations");
    private final JButton viewReportsButton = new JButton("View Reports");
    private final JButton logoutButton = new JButton("Logout");

    public AdminView() {
        setTitle("Admin Dashboard");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 1, 10, 10));
        panel.add(manageUsersButton);
        panel.add(manageDestinationsButton);
        panel.add(viewReportsButton);
        panel.add(logoutButton);

        add(panel);
    }

    public void addManageUsersListener(ActionListener listener) {
        manageUsersButton.addActionListener(listener);
    }

    public void addManageDestinationsListener(ActionListener listener) {
        manageDestinationsButton.addActionListener(listener);
    }

    public void addViewReportsListener(ActionListener listener) {
        viewReportsButton.addActionListener(listener);
    }

    public void addLogoutListener(ActionListener listener) {
        logoutButton.addActionListener(listener);
    }


    
}
