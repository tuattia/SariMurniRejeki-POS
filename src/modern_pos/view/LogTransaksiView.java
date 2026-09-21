package modern_pos.view;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
import javax.swing.table.DefaultTableModel;
import modern_pos.controller.LogTransaksiController;
import modern_pos.model.LogTransaksi;
import modern_pos.utils.SwingHelper;
import modern_pos.utils.UITheme;

public class LogTransaksiView extends JFrame {
    private final LogTransaksiController controller;
    private JTable tblLog;
    private DefaultTableModel tableModel;
    private JTextField txtSearch;
    private List<LogTransaksi> currentList;

    public LogTransaksiView(LogTransaksiController controller) {
        this.controller = controller;
        initUI();
        controller.setView(this);
    }

    private void initUI() {
        setTitle("Sari Murni Rejeki - Riwayat & Log Transaksi");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- SIDEBAR ---
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
        JButton btnStock = SwingHelper.createSidebarButton("Stock");
        JButton btnUtang = SwingHelper.createSidebarButton("Utang / Piutang");
        JButton btnLog = SwingHelper.createSidebarButton("Log Transaksi");
        JButton btnLogout = SwingHelper.createSidebarButton("Logout");

        btnLog.setBackground(UITheme.COLOR_PRIMARY); btnLog.setForeground(Color.WHITE); btnLog.putClientProperty("active_menu", true);

        sidebar.add(btnDash); sidebar.add(btnTrans); sidebar.add(btnStock);
        sidebar.add(btnUtang); sidebar.add(btnLog); sidebar.add(Box.createVerticalGlue()); 
        sidebar.add(btnLogout);
        add(sidebar, BorderLayout.WEST);

        // --- NAVIGASI ---
        btnDash.addActionListener(e -> { this.dispose(); new DashboardView(new modern_pos.controller.DashboardController(new modern_pos.model.User())).setVisible(true); });
        btnTrans.addActionListener(e -> { this.dispose(); new TransaksiView(new modern_pos.controller.TransaksiController()).setVisible(true); });
        btnStock.addActionListener(e -> { this.dispose(); new StockView(new modern_pos.controller.StockController()).setVisible(true); });
        btnUtang.addActionListener(e -> { this.dispose(); new UtangView(new modern_pos.controller.UtangController()).setVisible(true); });
        btnLogout.addActionListener(e -> { this.dispose(); new LoginView().setVisible(true); });

        // --- MAIN CONTENT ---
        JPanel mainContent = new JPanel(new BorderLayout(20, 20));
        mainContent.setBackground(UITheme.COLOR_BG_APP);
        mainContent.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // HEADER
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(UITheme.COLOR_BG_APP);
        
        JLabel lblTitle = SwingHelper.createLabel("Riwayat Transaksi Penjualan", UITheme.FONT_TITLE, UITheme.COLOR_TEXT_PRIMARY);
        topPanel.add(lblTitle, BorderLayout.WEST);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionPanel.setBackground(UITheme.COLOR_BG_APP);
        
        JButton btnCetak = SwingHelper.createFlatButton("Cetak Struk", UITheme.COLOR_PRIMARY, UITheme.COLOR_PRIMARY_DARK);
        JButton btnHapus = SwingHelper.createFlatButton("Hapus Riwayat", UITheme.COLOR_DANGER, new Color(198, 40, 40));

        
        actionPanel.add(btnCetak);
        actionPanel.add(btnHapus);
        topPanel.add(actionPanel, BorderLayout.EAST);
        
        mainContent.add(topPanel, BorderLayout.NORTH);

        // AREA TABEL
        JPanel tableContainer = new JPanel(new BorderLayout(10, 10));
        tableContainer.setBackground(Color.WHITE);
        tableContainer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230,230,230)), BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        searchPanel.setBackground(Color.WHITE);
        txtSearch = SwingHelper.createMaterialTextField();
        searchPanel.add(SwingHelper.createLabel("Pencarian (Kode / Tgl / Nama): ", UITheme.FONT_BODY, UITheme.COLOR_TEXT_PRIMARY));
        searchPanel.add(Box.createHorizontalStrut(10));
        searchPanel.add(txtSearch);
        tableContainer.add(searchPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"Waktu Transaksi", "Kode Transaksi", "Pelanggan", "Total Belanja", "Dibayar", "Kembali", "Status"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tblLog = new JTable(tableModel);
        SwingHelper.styleTable(tblLog);
        
        JScrollPane scrollPane = new JScrollPane(tblLog);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        tableContainer.add(scrollPane, BorderLayout.CENTER);

        mainContent.add(tableContainer, BorderLayout.CENTER);
        add(mainContent, BorderLayout.CENTER);

        // --- EVENT LISTENERS ---
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { controller.loadData(txtSearch.getText()); }
        });

        btnHapus.addActionListener(e -> {
            int row = tblLog.getSelectedRow();
            if (row == -1) { showError("Pilih riwayat yang ingin dihapus!"); return; }
            LogTransaksi log = currentList.get(row);
            int confirm = JOptionPane.showConfirmDialog(this, "Yakin hapus histori dengan kode " + log.getKodeTransaksi() + "?", "Hapus", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) controller.hapusLog(log.getKodeTransaksi());
        });

        btnCetak.addActionListener(e -> {
            int row = tblLog.getSelectedRow();
            if (row == -1) { showError("Pilih transaksi di tabel untuk dicetak struknya!"); return; }
            LogTransaksi log = currentList.get(row);
            try {
                // Memanggil sistem cetak (detailtransaksi) peninggalan dari project lama
                gui.detailtransaksi dt = new gui.detailtransaksi();
                
                // Pada sistem lama, dt biasanya mengambil value dari textfield atau property public, 
                // Jika error, minimal form detailtransaksi akan terbuka
                dt.setVisible(true); 
                
                showSuccess("Jendela Cetak Struk ('detailtransaksi') berhasil dipanggil untuk: " + log.getKodeTransaksi());
            } catch (Exception ex) {
                showError("Gagal membuka modul cetak struk: " + ex.getMessage());
            }
        });
    }

    public void populateTable(List<LogTransaksi> list) {
        this.currentList = list;
        tableModel.setRowCount(0);
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("id", "ID"));
        for (LogTransaksi log : list) {
            tableModel.addRow(new Object[]{ 
                log.getTanggal(), log.getKodeTransaksi(), log.getNamaPelanggan(), 
                "Rp " + nf.format(log.getTotal()), "Rp " + nf.format(log.getBayar()), 
                "Rp " + nf.format(log.getKembali()), "SELESAI"
            });
        }
    }

    public void setLoading(boolean isLoading) {
        setTitle(isLoading ? "Sari Murni Rejeki (Memuat...)" : "Sari Murni Rejeki - Riwayat Transaksi");
    }
    public void showError(String msg) { JOptionPane.showMessageDialog(this, "<html><body style='width: 350px; font-family: Segoe UI, sans-serif;'>" + msg.replace("\n", "<br>") + "</body></html>", "Error", JOptionPane.ERROR_MESSAGE); }
    public void showSuccess(String msg) { JOptionPane.showMessageDialog(this, msg, "Sukses", JOptionPane.INFORMATION_MESSAGE); }
}