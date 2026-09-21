package modern_pos.view;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import modern_pos.controller.LoginController;
import modern_pos.utils.SwingHelper;
import modern_pos.utils.UITheme;

public class LoginView extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private LoginController controller;

    public LoginView() {
        controller = new LoginController();
        controller.setView(this); 
        initUI();
    }
    private void initUI() {
        setTitle("Sari Murni Rejeki - POS Login");
        setUndecorated(true); 
        setSize(800, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        setContentPane(mainPanel);

        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setBackground(UITheme.COLOR_PRIMARY);
        leftPanel.setPreferredSize(new Dimension(350, 500));
        JLabel lblBrand = SwingHelper.createLabel("SARI MURNI", new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 36), Color.WHITE);
        JLabel lblSub = SwingHelper.createLabel("Point of Sales", UITheme.FONT_HEADING, new Color(200, 230, 255));
        GridBagConstraints gbcL = new GridBagConstraints();
        gbcL.gridx = 0; gbcL.gridy = 0; leftPanel.add(lblBrand, gbcL);
        gbcL.gridy = 1; leftPanel.add(lblSub, gbcL);
        mainPanel.add(leftPanel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBackground(UITheme.COLOR_BG_CARD);
        JLabel lblTitle = SwingHelper.createLabel("Welcome Back", UITheme.FONT_TITLE, UITheme.COLOR_TEXT_PRIMARY);
        JLabel lblDesc = SwingHelper.createLabel("Silakan login untuk melanjutkan", UITheme.FONT_BODY, UITheme.COLOR_TEXT_SECONDARY);
        txtUsername = SwingHelper.createMaterialTextField();
        txtPassword = SwingHelper.createMaterialPasswordField();
        btnLogin = SwingHelper.createFlatButton("LOGIN", UITheme.COLOR_PRIMARY, UITheme.COLOR_PRIMARY_DARK);
        
        JButton btnClose = new JButton("X");
        btnClose.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
        btnClose.setForeground(UITheme.COLOR_TEXT_SECONDARY);
        btnClose.setBackground(UITheme.COLOR_BG_CARD);
        btnClose.setBorderPainted(false);
        btnClose.setFocusPainted(false);
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> System.exit(0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 10, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0; gbc.insets = new Insets(0, 20, 0, 20); rightPanel.add(lblTitle, gbc);
        gbc.gridy = 1; gbc.insets = new Insets(0, 20, 30, 20); rightPanel.add(lblDesc, gbc);
        gbc.gridy = 2; gbc.insets = new Insets(5, 20, 0, 20); rightPanel.add(SwingHelper.createLabel("Username", UITheme.FONT_SMALL, UITheme.COLOR_TEXT_SECONDARY), gbc);
        gbc.gridy = 3; gbc.insets = new Insets(0, 20, 15, 20); rightPanel.add(txtUsername, gbc);
        gbc.gridy = 4; gbc.insets = new Insets(5, 20, 0, 20); rightPanel.add(SwingHelper.createLabel("Password", UITheme.FONT_SMALL, UITheme.COLOR_TEXT_SECONDARY), gbc);
        gbc.gridy = 5; gbc.insets = new Insets(0, 20, 30, 20); rightPanel.add(txtPassword, gbc);
        gbc.gridy = 6; gbc.insets = new Insets(10, 20, 10, 20); rightPanel.add(btnLogin, gbc);

        JPanel rightWrapper = new JPanel(new BorderLayout());
        rightWrapper.setBackground(UITheme.COLOR_BG_CARD);
        JPanel topBar = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        topBar.setBackground(UITheme.COLOR_BG_CARD);
        topBar.add(btnClose);
        rightWrapper.add(topBar, BorderLayout.NORTH);
        rightWrapper.add(rightPanel, BorderLayout.CENTER);
        mainPanel.add(rightWrapper, BorderLayout.CENTER);

        btnLogin.addActionListener(e -> controller.handleLogin());
        txtPassword.addActionListener(e -> controller.handleLogin());
    }

    public String getUsernameInput() { return txtUsername.getText(); }
    public String getPasswordInput() { return new String(txtPassword.getPassword()); }
    public void setLoading(boolean isLoading) {
        if (isLoading) {
            btnLogin.setText("LOADING...");
            btnLogin.setEnabled(false);
            setCursor(new Cursor(Cursor.WAIT_CURSOR));
        } else {
            btnLogin.setText("LOGIN");
            btnLogin.setEnabled(true);
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }
    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    public void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Sukses", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> {
            new LoginView().setVisible(true);
        });
    }
}