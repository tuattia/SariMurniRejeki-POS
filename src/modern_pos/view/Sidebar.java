package modern_pos.view;

import java.awt.Color;
import java.awt.Dimension;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import modern_pos.controller.DashboardController;
import modern_pos.controller.LogTransaksiController;
import modern_pos.controller.StockController;
import modern_pos.controller.TransaksiController;
import modern_pos.controller.UtangController;
import modern_pos.utils.Session;
import modern_pos.utils.SwingHelper;
import modern_pos.utils.UITheme;

// Sidebar navigasi yang sama untuk semua layar utama.
final class Sidebar {
    private Sidebar() {}

    static final String DASHBOARD = "Dashboard", TRANSAKSI = "Transaksi", STOCK = "Stock",
            UTANG = "Utang / Piutang", LOG = "Log Transaksi";

    static JPanel buat(JFrame frame, String aktif, Runnable logout, JButton... tambahan) {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UITheme.COLOR_PRIMARY_DARK);
        sidebar.setPreferredSize(new Dimension(200, frame.getHeight()));

        JLabel brand = SwingHelper.createLabel(" SARI MURNI", UITheme.FONT_TITLE, Color.WHITE);
        brand.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        sidebar.add(brand);
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(170, 1));
        sep.setForeground(new Color(255, 255, 255, 40));
        sep.setBackground(new Color(255, 255, 255, 40));
        sidebar.add(sep);
        sidebar.add(Box.createRigidArea(new Dimension(0, 15)));

        menu(sidebar, frame, aktif, DASHBOARD, () -> new DashboardView(new DashboardController(Session.currentUser)));
        menu(sidebar, frame, aktif, TRANSAKSI, () -> new TransaksiView(new TransaksiController()));
        menu(sidebar, frame, aktif, STOCK, () -> new StockView(new StockController()));
        menu(sidebar, frame, aktif, UTANG, () -> new UtangView(new UtangController()));
        menu(sidebar, frame, aktif, LOG, () -> new LogTransaksiView(new LogTransaksiController()));
        for (JButton b : tambahan) sidebar.add(b);

        sidebar.add(Box.createVerticalGlue());
        JButton btnLogout = SwingHelper.createSidebarButton("Logout");
        btnLogout.addActionListener(e -> logout.run());
        sidebar.add(btnLogout);
        return sidebar;
    }

    // Logout standar: tutup layar, kembali ke login (LoginView mengosongkan Session).
    static Runnable keLogin(JFrame frame) {
        return () -> { frame.dispose(); new LoginView().setVisible(true); };
    }

    private static void menu(JPanel sidebar, JFrame frame, String aktif, String label, Supplier<JFrame> buka) {
        JButton b = SwingHelper.createSidebarButton(label);
        if (label.equals(aktif)) {
            b.setBackground(UITheme.COLOR_PRIMARY);
            b.setForeground(Color.WHITE);
            b.putClientProperty("active_menu", true);
        } else {
            b.addActionListener(e -> { frame.dispose(); buka.get().setVisible(true); });
        }
        sidebar.add(b);
    }
}
