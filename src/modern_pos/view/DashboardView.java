package modern_pos.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import modern_pos.controller.DashboardController;
import modern_pos.model.DashboardSummary;
import modern_pos.model.Utang;
import modern_pos.utils.SwingHelper;
import modern_pos.utils.UITheme;

public class DashboardView extends JFrame {

    private final DashboardController controller;
    private JLabel lblUser, lblJam, lblTanggal;
    private JTextField txtSearch;
    private JTable tblUtang;
    private DefaultTableModel tableModel;
    private JPanel summaryPanel; // Tempat kartu-kartu metrik

    public DashboardView(DashboardController controller) {
        this.controller = controller;
        initUI();
        startClock();
        controller.setView(this);
    }

    private void initUI() {
        setTitle("Sari Murni Rejeki - Dashboard");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- SIDEBAR --- (Diperkecil jadi 200px)
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UITheme.COLOR_PRIMARY_DARK);
        sidebar.setPreferredSize(new Dimension(200, getHeight()));

        JLabel lblBrand = SwingHelper.createLabel(" SARI MURNI", UITheme.FONT_TITLE, Color.WHITE);
        lblBrand.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
                sidebar.add(lblBrand);
        
        // Garis Pembatas (Separator)
        javax.swing.JSeparator sep = new javax.swing.JSeparator();
        sep.setMaximumSize(new Dimension(170, 1));
        sep.setForeground(new Color(255, 255, 255, 40));
        sep.setBackground(new Color(255, 255, 255, 40));
        sidebar.add(sep);
        sidebar.add(Box.createRigidArea(new Dimension(0, 15)));

        JButton btnDash = SwingHelper.createSidebarButton("Dashboard");
        JButton btnTrans = SwingHelper.createSidebarButton("Transaksi");
        btnTrans.addActionListener(e -> { this.dispose(); new modern_pos.view.TransaksiView(new modern_pos.controller.TransaksiController()).setVisible(true); });
        JButton btnStock = SwingHelper.createSidebarButton("Stock"); btnStock.addActionListener(e -> { this.dispose(); new modern_pos.view.StockView(new modern_pos.controller.StockController()).setVisible(true); });
        JButton btnUtang = SwingHelper.createSidebarButton("Utang / Piutang"); btnUtang.addActionListener(e -> { this.dispose(); new modern_pos.view.UtangView(new modern_pos.controller.UtangController()).setVisible(true); });
        JButton btnLog = SwingHelper.createSidebarButton("Log Transaksi"); btnLog.addActionListener(e -> { this.dispose(); new modern_pos.view.LogTransaksiView(new modern_pos.controller.LogTransaksiController()).setVisible(true); });
        JButton btnLogout = SwingHelper.createSidebarButton("Logout");

        btnDash.setBackground(UITheme.COLOR_PRIMARY); btnDash.setForeground(Color.WHITE); btnDash.putClientProperty("active_menu", true); 

        sidebar.add(btnDash);
        sidebar.add(btnTrans);
        sidebar.add(btnStock);
        sidebar.add(btnUtang);
        sidebar.add(btnLog);
        sidebar.add(Box.createVerticalGlue()); 
        sidebar.add(btnLogout);
        add(sidebar, BorderLayout.WEST);

        // --- HEADER --- (Tinggi diperkecil jadi 50px)
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)));
        topBar.setPreferredSize(new Dimension(getWidth(), 50));

        JLabel lblTitle = SwingHelper.createLabel("  Ringkasan Bisnis", UITheme.FONT_HEADING, UITheme.COLOR_TEXT_PRIMARY);
        topBar.add(lblTitle, BorderLayout.WEST);

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        infoPanel.setBackground(Color.WHITE);
        lblTanggal = SwingHelper.createLabel("Tanggal", UITheme.FONT_SMALL, UITheme.COLOR_TEXT_SECONDARY);
        lblJam = SwingHelper.createLabel("Jam", UITheme.FONT_HEADING, UITheme.COLOR_PRIMARY);
        lblUser = SwingHelper.createLabel("User", UITheme.FONT_HEADING, UITheme.COLOR_TEXT_PRIMARY);

        infoPanel.add(lblTanggal);
        infoPanel.add(lblJam);
        infoPanel.add(SwingHelper.createLabel("|", UITheme.FONT_BODY, Color.LIGHT_GRAY));
        infoPanel.add(lblUser);
        topBar.add(infoPanel, BorderLayout.EAST);

        // --- MAIN CONTENT ---
        JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
        contentPanel.setBackground(UITheme.COLOR_BG_APP);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 1. KPI Summary Cards (4 kolom)
        summaryPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        summaryPanel.setBackground(UITheme.COLOR_BG_APP);
        summaryPanel.setPreferredSize(new Dimension(getWidth(), 90)); // Padat untuk 14"
        
        summaryPanel.add(SwingHelper.createSummaryCard("Pendapatan Hari Ini", "valPendapatan", UITheme.COLOR_SUCCESS));
        summaryPanel.add(SwingHelper.createSummaryCard("Transaksi Hari Ini", "valTrx", UITheme.COLOR_PRIMARY));
        summaryPanel.add(SwingHelper.createSummaryCard("Utang Aktif", "valUtang", UITheme.COLOR_WARNING));
        summaryPanel.add(SwingHelper.createSummaryCard("Stok Menipis", "valStok", UITheme.COLOR_DANGER));
        
        contentPanel.add(summaryPanel, BorderLayout.NORTH);

        // 2. Data Table Panel (Diberi border membulat putih)
        JPanel tableContainer = new JPanel(new BorderLayout(10, 10));
        tableContainer.setBackground(Color.WHITE);
        tableContainer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230,230,230)),
            BorderFactory.createEmptyBorder(10, 15, 15, 15)
        ));

        // Search Bar
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        searchPanel.setBackground(Color.WHITE);
        JLabel lblSearch = SwingHelper.createLabel("Pencarian Kredit: ", UITheme.FONT_BODY, UITheme.COLOR_TEXT_PRIMARY);
        txtSearch = SwingHelper.createMaterialTextField();
        searchPanel.add(lblSearch);
        searchPanel.add(Box.createHorizontalStrut(10));
        searchPanel.add(txtSearch);
        tableContainer.add(searchPanel, BorderLayout.NORTH);

        // Tabel Data
        tableModel = new DefaultTableModel(new String[]{
            "Kode", "Nama", "Telepon", "Barang", "DP", "Cicilan", "Jatuh Tempo"
        }, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tblUtang = new JTable(tableModel);
        SwingHelper.styleTable(tblUtang);
        
        JScrollPane scrollPane = new JScrollPane(tblUtang);
        scrollPane.setBorder(BorderFactory.createEmptyBorder()); // Hilangkan double border
        scrollPane.getViewport().setBackground(Color.WHITE);
        tableContainer.add(scrollPane, BorderLayout.CENTER);

        contentPanel.add(tableContainer, BorderLayout.CENTER);

        // BUNGKUS
        JPanel rightWrapper = new JPanel(new BorderLayout());
        rightWrapper.add(topBar, BorderLayout.NORTH);
        rightWrapper.add(contentPanel, BorderLayout.CENTER);
        add(rightWrapper, BorderLayout.CENTER);

        // --- EVENTS ---
        btnLogout.addActionListener(e -> controller.logout());
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                controller.loadData(txtSearch.getText());
            }
        });
    }

    private void startClock() {
        Timer timer = new Timer(1000, e -> {
            Date now = new Date();
            lblJam.setText(new SimpleDateFormat("HH:mm:ss").format(now));
            lblTanggal.setText(new SimpleDateFormat("dd MMM yyyy").format(now));
        });
        timer.start();
    }

    // --- METHODS ---
    public void setUserInfo(String nama) { lblUser.setText(nama); }

    public void updateSummaryCards(DashboardSummary summary) {
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("id", "ID"));
        
        // Loop mencari Label berdasarkan namanya yang sudah diset di SwingHelper
        for (Component panel : summaryPanel.getComponents()) {
            if (panel instanceof JPanel) {
                for (Component c : ((JPanel) panel).getComponents()) {
                    if (c instanceof JLabel) {
                        JLabel lbl = (JLabel) c;
                        if ("valPendapatan".equals(lbl.getName())) lbl.setText("Rp " + nf.format(summary.getPendapatanHariIni()));
                        else if ("valTrx".equals(lbl.getName())) lbl.setText(String.valueOf(summary.getTotalTransaksiHariIni()));
                        else if ("valUtang".equals(lbl.getName())) lbl.setText(String.valueOf(summary.getJumlahUtangAktif()));
                        else if ("valStok".equals(lbl.getName())) lbl.setText(String.valueOf(summary.getStokMenipis()));
                    }
                }
            }
        }
    }

    public void populateTable(List<Utang> list) {
        tableModel.setRowCount(0);
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("id", "ID"));
        
        for (Utang u : list) {
            tableModel.addRow(new Object[]{
                u.getKodeUtang(),
                u.getNama(),
                u.getTelepon(),
                "Rp " + nf.format(u.getHargaBarang()),
                "Rp " + nf.format(u.getDp()),
                u.getJumlahCicilan() + "x",
                (u.getJatuhTempo() != null) ? u.getJatuhTempo().toString() : "-"
            });
        }
    }

    public int showConfirm(String message) {
        return JOptionPane.showConfirmDialog(this, message, "Konfirmasi", JOptionPane.YES_NO_OPTION);
    }

    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
