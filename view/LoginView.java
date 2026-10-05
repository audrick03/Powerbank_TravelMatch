package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * LoginView - the login screen of TravelMatch.
 *
 * MVC role: this class only DISPLAYS things and exposes its components to the
 * controller. It contains no login logic. LoginController reads the input
 * through the getters, attaches listeners, and calls showError() when needed.
 */
public class LoginView extends JFrame {

    private static final Color PRIMARY = new Color(0x1D9E75);
    private static final Color ERROR   = new Color(0xC0392B);

    private final JTextField     usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JCheckBox      showPassword  = new JCheckBox("Show password");
    private final JLabel         errorLabel    = new JLabel(" ");
    private final JButton        loginButton   = new JButton("Log in");
    private final JButton        registerButton = new JButton("Create an account");
    private final JButton       backToHomeButton = new JButton("Back to Home");

    public LoginView() {
        super("TravelMatch - Login");
        initComponents();
        layoutComponents();
        wireInternalBehavior();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(420, 480);
        setResizable(false);
        setLocationRelativeTo(null); // center on screen
    }

    // ---------- Setup ----------

    private void initComponents() {
        errorLabel.setForeground(ERROR);
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);

        loginButton.setBackground(PRIMARY);
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setOpaque(true);
        loginButton.setBorderPainted(false);

        registerButton.setBorderPainted(false);
        registerButton.setContentAreaFilled(false);
        registerButton.setForeground(PRIMARY);
        registerButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        backToHomeButton.setBorderPainted(false);
        backToHomeButton.setContentAreaFilled(false);
        backToHomeButton.setForeground(new Color(128, 128, 128));
        backToHomeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        showPassword.setFocusPainted(false);
    }

    private void layoutComponents() {
        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.insets = new Insets(4, 0, 4, 0);

        JLabel title = new JLabel("TravelMatch", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        title.setForeground(PRIMARY);

        JLabel subtitle = new JLabel("Find destinations that fit you", SwingConstants.CENTER);
        subtitle.setForeground(Color.GRAY);

        int row = 0;
        gc.gridy = row++; gc.insets = new Insets(0, 0, 2, 0);  content.add(title, gc);
        gc.gridy = row++; gc.insets = new Insets(0, 0, 24, 0); content.add(subtitle, gc);

        gc.insets = new Insets(4, 0, 2, 0);
        gc.gridy = row++; content.add(new JLabel("Username"), gc);
        gc.gridy = row++; gc.insets = new Insets(0, 0, 10, 0); content.add(usernameField, gc);

        gc.insets = new Insets(4, 0, 2, 0);
        gc.gridy = row++; content.add(new JLabel("Password"), gc);
        gc.gridy = row++; gc.insets = new Insets(0, 0, 4, 0); content.add(passwordField, gc);

        gc.gridy = row++; content.add(showPassword, gc);
        gc.gridy = row++; gc.insets = new Insets(8, 0, 8, 0); content.add(errorLabel, gc);

        gc.gridy = row++; gc.insets = new Insets(0, 0, 6, 0); gc.ipady = 8;
        content.add(loginButton, gc);

        gc.ipady = 0;
        gc.gridy = row++; gc.insets = new Insets(4, 0, 0, 0);
        content.add(registerButton, gc);

        gc.gridy = row++; gc.insets = new Insets(20, 0, 0, 0);
        content.add(backToHomeButton, gc);


        setContentPane(content);
    }

    /** Behavior that belongs to the view itself (pure UI, no business logic). */
    private void wireInternalBehavior() {
        // Pressing Enter anywhere in the window triggers the Log in button
        getRootPane().setDefaultButton(loginButton);

        // Show / hide the password characters
        char defaultEcho = passwordField.getEchoChar();
        showPassword.addActionListener(e ->
            passwordField.setEchoChar(showPassword.isSelected() ? (char) 0 : defaultEcho));
    }

    // ---------- Read input (used by LoginController) ----------

    public String getUsername() {
        return usernameField.getText().trim();
    }

    public String getPassword() {
        return new String(passwordField.getPassword());
    }

    // ---------- Register listeners (used by LoginController) ----------

    public void addLoginListener(ActionListener listener) {
        loginButton.addActionListener(listener);
    }

    public void addRegisterListener(ActionListener listener) {
        registerButton.addActionListener(listener);
    }

    // ---------- Update the screen (called by LoginController) ----------

    public void showError(String message) {
        errorLabel.setText(message);
    }

    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    public void clearError() {
        errorLabel.setText(" ");
    }

    public void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        showPassword.setSelected(false);
        passwordField.setEchoChar('\u2022');
        clearError();
        usernameField.requestFocusInWindow();
    }

    public void setUsername(String username) {
        usernameField.setText(username);
    }

    public void showView() {
        setVisible(true);
    }

    public void closeView() {
        dispose();
    }

    public void addBackToHomeListener(ActionListener listener) {
        backToHomeButton.addActionListener(listener);
    }
}