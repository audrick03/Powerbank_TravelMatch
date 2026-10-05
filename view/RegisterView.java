package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class RegisterView extends JFrame {

    private static final Color PRIMARY = new Color(0x1D9E75);
    private static final Color ERROR = new Color(0xC0392B);

    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JPasswordField confirmPasswordField = new JPasswordField(20);
    private final JCheckBox showPassword = new JCheckBox("Show passwords");
    private final JLabel messageLabel = new JLabel(" ");
    private final JButton registerButton = new JButton("Create account");
    private final JButton backButton = new JButton("Back to login");
    private char passwordEcho;
    private char confirmationPasswordEcho;

    public RegisterView() {
        super("TravelMatch - Create an account");
        initComponents();
        layoutComponents();
        wireInternalBehavior();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(420, 440);
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        messageLabel.setForeground(ERROR);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        registerButton.setBackground(PRIMARY);
        registerButton.setForeground(Color.WHITE);
        registerButton.setFocusPainted(false);
        registerButton.setOpaque(true);
        registerButton.setBorderPainted(false);

        backButton.setBorderPainted(false);
        backButton.setContentAreaFilled(false);
        backButton.setForeground(PRIMARY);
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        showPassword.setFocusPainted(false);
    }

    private void layoutComponents() {
        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(24, 40, 24, 40));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;

        JLabel title = new JLabel("Create your account", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setForeground(PRIMARY);

        int row = 0;
        gc.gridy = row++;
        gc.insets = new Insets(0, 0, 20, 0);
        content.add(title, gc);

        gc.insets = new Insets(4, 0, 2, 0);
        gc.gridy = row++;
        content.add(new JLabel("Username"), gc);
        gc.gridy = row++;
        gc.insets = new Insets(0, 0, 8, 0);
        content.add(usernameField, gc);

        gc.insets = new Insets(4, 0, 2, 0);
        gc.gridy = row++;
        content.add(new JLabel("Password (at least 8 characters)"), gc);
        gc.gridy = row++;
        gc.insets = new Insets(0, 0, 8, 0);
        content.add(passwordField, gc);

        gc.insets = new Insets(4, 0, 2, 0);
        gc.gridy = row++;
        content.add(new JLabel("Confirm password"), gc);
        gc.gridy = row++;
        gc.insets = new Insets(0, 0, 4, 0);
        content.add(confirmPasswordField, gc);

        gc.gridy = row++;
        content.add(showPassword, gc);

        gc.gridy = row++;
        gc.insets = new Insets(6, 0, 6, 0);
        content.add(messageLabel, gc);

        gc.gridy = row++;
        gc.insets = new Insets(0, 0, 4, 0);
        gc.ipady = 8;
        content.add(registerButton, gc);

        gc.gridy = row;
        gc.ipady = 0;
        gc.insets = new Insets(2, 0, 0, 0);
        content.add(backButton, gc);

        setContentPane(content);
    }

    private void wireInternalBehavior() {
        getRootPane().setDefaultButton(registerButton);

        passwordEcho = passwordField.getEchoChar();
        confirmationPasswordEcho = confirmPasswordField.getEchoChar();
        showPassword.addActionListener(event -> {
            passwordField.setEchoChar(showPassword.isSelected() ? (char) 0 : passwordEcho);
            confirmPasswordField.setEchoChar(
                    showPassword.isSelected() ? (char) 0 : confirmationPasswordEcho);
        });
    }

    public String getUsername() {
        return usernameField.getText().trim();
    }

    public String getPassword() {
        return new String(passwordField.getPassword());
    }

    public String getConfirmedPassword() {
        return new String(confirmPasswordField.getPassword());
    }

    public void setUsername(String username) {
        usernameField.setText(username);
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    public void clearPasswordFields() {
        passwordField.setText("");
        confirmPasswordField.setText("");
        showPassword.setSelected(false);
        passwordField.setEchoChar(passwordEcho);
        confirmPasswordField.setEchoChar(confirmationPasswordEcho);
    }

    public void addRegisterListener(ActionListener listener) {
        registerButton.addActionListener(listener);
    }

    public void addBackListener(ActionListener listener) {
        backButton.addActionListener(listener);
    }

    public void showView() {
        setVisible(true);
    }

    public void closeView() {
        dispose();
    }
}
