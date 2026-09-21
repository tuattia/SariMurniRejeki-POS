package modern_pos.view;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
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
import modern_pos.controller.StockController;
import modern_pos.model.Barang;
import modern_pos.utils.SwingHelper;
import modern_pos.utils.UITheme;

public class StockView extends JFrame {
    private final StockController controller;
    private JTable tblStock;
    private DefaultTableModel tableModel;
    private JTextField txtSearch;
    private List<Barang> currentList;

    public StockView(StockController controller) {
        this.controller = controller;
        initUI();
        controller.setView(this);
    }

    private void initUI() {
        setTitle("Sari Murni Rejeki - Stock Management");
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
        JButton btnUtang = SwingHelper.createSidebarButton("Utang / Piutang"); btnUtang.addActionListener(e -> { this.dispose(); new modern_pos.view.UtangView(new modern_pos.controller.UtangController()).setVisible(true); });
        JButton btnLog = SwingHelper.createSidebarButton("Log Transaksi"); btnLog.addActionListener(e -> { this.dispose(); new modern_pos.view.LogTransaksiView(new modern_pos.controller.LogTransaksiController()).setVisible(true); });
        JButton btnLogout = SwingHelper.createSidebarButton("Logout");

        btnStock.setBackground(UITheme.COLOR_PRIMARY); btnStock.setForeground(Color.WHITE); btnStock.putClientProperty("active_menu", true); // Active state

        sidebar.add(btnDash); sidebar.add(btnTrans); sidebar.add(btnStock);
        sidebar.add(btnUtang); sidebar.add(btnLog); sidebar.add(Box.createVerticalGlue()); 
        sidebar.add(btnLogout);
        add(sidebar, BorderLayout.WEST);

        // --- NAVIGASI ---
        btnDash.addActionListener(e -> { this.dispose(); new DashboardView(new modern_pos.controller.DashboardController(modern_pos.utils.Session.currentUser)).setVisible(true); });
        btnTrans.addActionListener(e -> { this.dispose(); new TransaksiView(new modern_pos.controller.TransaksiController()).setVisible(true); });
        btnLogout.addActionListener(e -> { this.dispose(); new LoginView().setVisible(true); });

        // --- MAIN CONTENT ---
        JPanel mainContent = new JPanel(new BorderLayout(20, 20));
        mainContent.setBackground(UITheme.COLOR_BG_APP);
        mainContent.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // HEADER (Judul + Tombol Aksi)
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(UITheme.COLOR_BG_APP);
        
        JLabel lblTitle = SwingHelper.createLabel("Manajemen Stok Barang", UITheme.FONT_TITLE, UITheme.COLOR_TEXT_PRIMARY);
        topPanel.add(lblTitle, BorderLayout.WEST);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionPanel.setBackground(UITheme.COLOR_BG_APP);
        
        JButton btnTambah = SwingHelper.createFlatButton("+ Tambah", UITheme.COLOR_SUCCESS, new Color(56, 142, 60));
        JButton btnEdit = SwingHelper.createFlatButton("Edit", UITheme.COLOR_PRIMARY, UITheme.COLOR_PRIMARY_DARK);
        JButton btnHapus = SwingHelper.createFlatButton("Hapus", UITheme.COLOR_DANGER, new Color(198, 40, 40));
        JButton btnSnapshot = SwingHelper.createFlatButton("Stock Snapshot", new Color(156, 39, 176), new Color(123, 31, 162));
        
        // Atur ukuran lebar tombol secara manual agar proporsional
        btnTambah.setPreferredSize(new Dimension(120, 35));
        btnEdit.setPreferredSize(new Dimension(100, 35));
        btnHapus.setPreferredSize(new Dimension(100, 35));
        btnSnapshot.setPreferredSize(new Dimension(150, 35));

        actionPanel.add(btnSnapshot);
        actionPanel.add(btnHapus);
        actionPanel.add(btnEdit);
        actionPanel.add(btnTambah);
        topPanel.add(actionPanel, BorderLayout.EAST);
        
        mainContent.add(topPanel, BorderLayout.NORTH);

        // AREA TABEL & PENCARIAN
        JPanel tableContainer = new JPanel(new BorderLayout(10, 10));
        tableContainer.setBackground(Color.WHITE);
        tableContainer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230,230,230)), BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        searchPanel.setBackground(Color.WHITE);
        txtSearch = SwingHelper.createMaterialTextField();
        searchPanel.add(SwingHelper.createLabel("Cari Barang: ", UITheme.FONT_BODY, UITheme.COLOR_TEXT_PRIMARY));
        searchPanel.add(Box.createHorizontalStrut(10));
        searchPanel.add(txtSearch);
        tableContainer.add(searchPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"Kode Barang", "Nama Barang", "Harga Jual", "Stok Tersedia"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tblStock = new JTable(tableModel);
        SwingHelper.styleTable(tblStock);
        
        JScrollPane scrollPane = new JScrollPane(tblStock);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        tableContainer.add(scrollPane, BorderLayout.CENTER);

        mainContent.add(tableContainer, BorderLayout.CENTER);
        add(mainContent, BorderLayout.CENTER);

        // --- EVENT LISTENERS CRUD ---
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { controller.loadData(txtSearch.getText()); }
        });

        btnTambah.addActionListener(e -> showFormDialog(null));
        
        btnEdit.addActionListener(e -> {
            int row = tblStock.getSelectedRow();
            if (row == -1) {
                showError("Pilih barang yang ingin diedit terlebih dahulu!");
                return;
            }
            showFormDialog(currentList.get(row));
        });

        btnHapus.addActionListener(e -> {
            int row = tblStock.getSelectedRow();
            if (row == -1) {
                showError("Pilih barang yang ingin dihapus!");
                return;
            }
            Barang b = currentList.get(row);
            int confirm = JOptionPane.showConfirmDialog(this, "Yakin ingin menghapus " + b.getNamaBarang() + "?", "Hapus Data", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                controller.hapusBarang(b.getKodeBarang());
            }
        });

        btnSnapshot.addActionListener(e -> {
            // TODO: Integrasi dengan snapshotDialog yang dibuat pada sesi sebelumnya
            try { new gui.snapshotDialog(this, true).setVisible(true); } catch(Exception ex) { showError("Gagal memuat Snapshot: " + ex.getMessage()); }
        });
    }

    private void showFormDialog(Barang b) {
        boolean isEdit = (b != null);
        JTextField txtKode = SwingHelper.createMaterialTextField();
        JTextField txtNama = SwingHelper.createMaterialTextField();
        JTextField txtHarga = SwingHelper.createMaterialTextField();
        JTextField txtStok = SwingHelper.createMaterialTextField();

        if (isEdit) {
            txtKode.setText(b.getKodeBarang());
            txtKode.setEnabled(false); // Kode tidak boleh diubah
            txtNama.setText(b.getNamaBarang());
            txtHarga.setText(String.valueOf(b.getHarga()));
            txtStok.setText(String.valueOf(b.getStok()));
        }

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 15));
        panel.add(SwingHelper.createLabel("Kode Barang:", UITheme.FONT_BODY, UITheme.COLOR_TEXT_PRIMARY)); panel.add(txtKode);
        panel.add(SwingHelper.createLabel("Nama Barang:", UITheme.FONT_BODY, UITheme.COLOR_TEXT_PRIMARY)); panel.add(txtNama);
        panel.add(SwingHelper.createLabel("Harga Jual:", UITheme.FONT_BODY, UITheme.COLOR_TEXT_PRIMARY)); panel.add(txtHarga);
        panel.add(SwingHelper.createLabel("Stok Awal:", UITheme.FONT_BODY, UITheme.COLOR_TEXT_PRIMARY)); panel.add(txtStok);

        int result = JOptionPane.showConfirmDialog(this, panel, isEdit ? "Edit Barang" : "Tambah Barang Baru", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            try {
                Barang newB = new Barang();
                newB.setKodeBarang(txtKode.getText());
                newB.setNamaBarang(txtNama.getText());
                newB.setHarga(Integer.parseInt(txtHarga.getText()));
                newB.setStok(Integer.parseInt(txtStok.getText()));
                controller.simpanBarang(newB, isEdit);
            } catch (Exception ex) {
                showError("Input harga dan stok harus berupa angka!");
            }
        }
    }

    public void populateTable(List<Barang> list) {
        this.currentList = list;
        tableModel.setRowCount(0);
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("id", "ID"));
        for (Barang b : list) {
            tableModel.addRow(new Object[]{ b.getKodeBarang(), b.getNamaBarang(), "Rp " + nf.format(b.getHarga()), b.getStok() });
        }
    }

    public void setLoading(boolean isLoading) {
        if(isLoading) setTitle("Sari Murni Rejeki - Stock (Memuat...)");
        else setTitle("Sari Murni Rejeki - Stock Management");
    }
    public void showError(String msg) { JOptionPane.showMessageDialog(this, "<html><body style='width: 350px; font-family: Segoe UI, sans-serif;'>" + msg.replace("\n", "<br>") + "</body></html>", "Error", JOptionPane.ERROR_MESSAGE); }
    public void showSuccess(String msg) { JOptionPane.showMessageDialog(this, msg, "Sukses", JOptionPane.INFORMATION_MESSAGE); }
}