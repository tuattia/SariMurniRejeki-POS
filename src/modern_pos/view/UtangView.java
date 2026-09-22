package modern_pos.view;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import modern_pos.model.Barang;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import modern_pos.controller.UtangController;
import modern_pos.model.Utang;
import modern_pos.utils.SwingHelper;
import modern_pos.utils.UITheme;

public class UtangView extends JFrame {
    private final UtangController controller;
    private JTable tblUtang;
    private DefaultTableModel tableModel;
    private JTextField txtSearch;
    private List<Utang> currentList;

    public UtangView(UtangController controller) {
        this.controller = controller;
        initUI();
        controller.setView(this);
    }

    private void initUI() {
        setTitle("Sari Murni Rejeki - Utang / Piutang");
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
        JButton btnLog = SwingHelper.createSidebarButton("Log Transaksi"); btnLog.addActionListener(e -> { this.dispose(); new modern_pos.view.LogTransaksiView(new modern_pos.controller.LogTransaksiController()).setVisible(true); });
        JButton btnLogout = SwingHelper.createSidebarButton("Logout");

        btnUtang.setBackground(UITheme.COLOR_PRIMARY); btnUtang.setForeground(Color.WHITE); btnUtang.putClientProperty("active_menu", true);

        sidebar.add(btnDash); sidebar.add(btnTrans); sidebar.add(btnStock);
        sidebar.add(btnUtang); sidebar.add(btnLog); sidebar.add(Box.createVerticalGlue()); 
        sidebar.add(btnLogout);
        add(sidebar, BorderLayout.WEST);

        // --- NAVIGASI ---
        btnDash.addActionListener(e -> { this.dispose(); new DashboardView(new modern_pos.controller.DashboardController(modern_pos.utils.Session.currentUser)).setVisible(true); });
        btnTrans.addActionListener(e -> { this.dispose(); new TransaksiView(new modern_pos.controller.TransaksiController()).setVisible(true); });
        btnStock.addActionListener(e -> { this.dispose(); new StockView(new modern_pos.controller.StockController()).setVisible(true); });
        btnLogout.addActionListener(e -> { this.dispose(); new LoginView().setVisible(true); });

        // --- MAIN CONTENT ---
        JPanel mainContent = new JPanel(new BorderLayout(20, 20));
        mainContent.setBackground(UITheme.COLOR_BG_APP);
        mainContent.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // HEADER
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(UITheme.COLOR_BG_APP);
        
        JLabel lblTitle = SwingHelper.createLabel("Manajemen Utang Pelanggan", UITheme.FONT_TITLE, UITheme.COLOR_TEXT_PRIMARY);
        topPanel.add(lblTitle, BorderLayout.WEST);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionPanel.setBackground(UITheme.COLOR_BG_APP);
        
        JButton btnTambah = SwingHelper.createFlatButton("+ Tambah", UITheme.COLOR_PRIMARY, UITheme.COLOR_PRIMARY_DARK);
        JButton btnEdit = SwingHelper.createFlatButton("Edit", UITheme.COLOR_WARNING, new Color(230, 81, 0));
        JButton btnHapus = SwingHelper.createFlatButton("Hapus", UITheme.COLOR_DANGER, new Color(198, 40, 40));
        JButton btnLunas = SwingHelper.createFlatButton("Tandai Lunas", UITheme.COLOR_SUCCESS, new Color(56, 142, 60));
        JButton btnKartu = SwingHelper.createFlatButton("Kartu", new Color(96, 125, 139), new Color(69, 90, 100));

        btnLunas.setPreferredSize(new Dimension(140, 35));
        
        actionPanel.add(btnKartu);
        actionPanel.add(btnLunas);
        actionPanel.add(btnHapus);
        actionPanel.add(btnEdit);
        actionPanel.add(btnTambah);
        // Member: tambah, lunas, kartu saja.
        boolean admin = modern_pos.utils.Akses.admin();
        btnEdit.setVisible(admin);
        btnHapus.setVisible(admin);
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
        searchPanel.add(SwingHelper.createLabel("Cari Pelanggan: ", UITheme.FONT_BODY, UITheme.COLOR_TEXT_PRIMARY));
        searchPanel.add(Box.createHorizontalStrut(10));
        searchPanel.add(txtSearch);
        tableContainer.add(searchPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"Kode", "Nama", "Telepon", "Qty", "Total", "DP", "Sisa Cicilan", "Jatuh Tempo", "Status"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tblUtang = new JTable(tableModel);
        SwingHelper.styleTable(tblUtang);
        
        JScrollPane scrollPane = new JScrollPane(tblUtang);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        tableContainer.add(scrollPane, BorderLayout.CENTER);

        mainContent.add(tableContainer, BorderLayout.CENTER);
        add(mainContent, BorderLayout.CENTER);

        // --- EVENT LISTENERS ---
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { controller.loadData(txtSearch.getText()); }
        });

        btnTambah.addActionListener(e -> showFormDialog(null));
        btnEdit.addActionListener(e -> {
            int row = tblUtang.getSelectedRow();
            if (row == -1) { showError("Pilih data yang ingin diedit!"); return; }
            showFormDialog(currentList.get(row));
        });

        btnHapus.addActionListener(e -> {
            int row = tblUtang.getSelectedRow();
            if (row == -1) { showError("Pilih data yang ingin dihapus!"); return; }
            Utang u = currentList.get(row);
            int confirm = JOptionPane.showConfirmDialog(this, "Yakin hapus utang " + u.getNama() + "?", "Hapus", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) controller.hapusUtang(u.getKodeUtang());
        });

        btnLunas.addActionListener(e -> {
            int row = tblUtang.getSelectedRow();
            if (row == -1) { showError("Pilih utang yang ingin dilunaskan!"); return; }
            Utang u = currentList.get(row);
            if("lunas".equalsIgnoreCase(u.getStatus())) { showError("Utang ini sudah lunas!"); return; }
            int confirm = JOptionPane.showConfirmDialog(this, "Tandai utang " + u.getNama() + " sebagai LUNAS?", "Pelunasan", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) controller.tandaiLunas(u.getKodeUtang());
        });

        btnKartu.addActionListener(e -> {
            int row = tblUtang.getSelectedRow();
            if (row == -1) { showError("Pilih utang yang ingin dilihat kartunya!"); return; }
            controller.tampilKartu(currentList.get(row).getKodeUtang());
        });
    }

    public void showKartu(modern_pos.model.KartuAngsuran k) { new KartuAngsuranDialog(this, k).setVisible(true); }

    private void showFormDialog(Utang u) {
        boolean isEdit = (u != null);
        JTextField txtKode = SwingHelper.createMaterialTextField();
        JTextField txtNama = SwingHelper.createMaterialTextField();
        JTextField txtTelp = SwingHelper.createMaterialTextField();
        JTextField txtHarga = SwingHelper.createMaterialTextField();
        JTextField txtDp = SwingHelper.createMaterialTextField();
        JTextField txtCicilan = SwingHelper.createMaterialTextField();
        JTextField txtJatuhTempo = SwingHelper.createMaterialTextField();
        JComboBox<Barang> cmbBarang = new JComboBox<>();
        try {
            for (Barang b : controller.daftarBarang()) {
                cmbBarang.addItem(b);
                if (isEdit && b.getKodeBarang().equals(u.getKodeBarang())) cmbBarang.setSelectedItem(b);
            }
        } catch (Exception ex) {
            showError("Gagal memuat daftar barang: " + ex.getMessage());
            return;
        }
        JTextField txtQty = SwingHelper.createMaterialTextField();
        txtQty.setText(isEdit ? String.valueOf(u.getQty()) : "1");

        if (isEdit) {
            txtKode.setText(u.getKodeUtang()); txtKode.setEnabled(false);
            txtNama.setText(u.getNama());
            txtTelp.setText(u.getTelepon());
            txtHarga.setText(String.valueOf(u.getHargaBarang()));
            txtDp.setText(String.valueOf(u.getDp()));
            txtCicilan.setText(String.valueOf(u.getJumlahCicilan()));
            if(u.getJatuhTempo() != null) txtJatuhTempo.setText(u.getJatuhTempo().toString());
        } else {
            txtKode.setText("UTG-" + new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date())); txtKode.setEnabled(false); txtJatuhTempo.setText(LocalDate.now().plusMonths(1).toString()); // Default 1 bulan kedepan
        }

        pasangTotalOtomatis(cmbBarang, txtQty, txtHarga, !isEdit);

        JPanel panel = new JPanel(new GridLayout(9, 2, 10, 10));
        panel.add(SwingHelper.createLabel("Kode Utang:", UITheme.FONT_BODY, UITheme.COLOR_TEXT_PRIMARY)); panel.add(txtKode);
        panel.add(SwingHelper.createLabel("Nama Pelanggan:", UITheme.FONT_BODY, UITheme.COLOR_TEXT_PRIMARY)); panel.add(txtNama);
        panel.add(SwingHelper.createLabel("Telepon:", UITheme.FONT_BODY, UITheme.COLOR_TEXT_PRIMARY)); panel.add(txtTelp);
        panel.add(SwingHelper.createLabel("Barang:", UITheme.FONT_BODY, UITheme.COLOR_TEXT_PRIMARY)); panel.add(cmbBarang);
        panel.add(SwingHelper.createLabel("Qty:", UITheme.FONT_BODY, UITheme.COLOR_TEXT_PRIMARY)); panel.add(txtQty);
        panel.add(SwingHelper.createLabel("Total Utang (Rp):", UITheme.FONT_BODY, UITheme.COLOR_TEXT_PRIMARY)); panel.add(txtHarga);
        panel.add(SwingHelper.createLabel("DP (Rp):", UITheme.FONT_BODY, UITheme.COLOR_TEXT_PRIMARY)); panel.add(txtDp);
        panel.add(SwingHelper.createLabel("Jumlah Cicilan (x):", UITheme.FONT_BODY, UITheme.COLOR_TEXT_PRIMARY)); panel.add(txtCicilan);
        panel.add(SwingHelper.createLabel("Jatuh Tempo (YYYY-MM-DD):", UITheme.FONT_BODY, UITheme.COLOR_TEXT_PRIMARY)); panel.add(txtJatuhTempo);

        boolean formValid = false;
        while (!formValid) {
            int result = JOptionPane.showConfirmDialog(this, panel, isEdit ? "Edit Utang" : "Tambah Utang", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                try {
                    // Validasi Dasar
                    if (txtNama.getText().trim().isEmpty()) throw new Exception("Nama pelanggan tidak boleh kosong!");
                    Barang barangDipilih = (Barang) cmbBarang.getSelectedItem();
                    if (barangDipilih == null) throw new Exception("Pilih barang yang diutang!");
                    
                    Utang newU = new Utang();
                    newU.setKodeUtang(txtKode.getText());
                    newU.setNama(txtNama.getText());
                    newU.setTelepon(txtTelp.getText());
                    newU.setAlamat("-"); // Optional
                    newU.setKodeBarang(barangDipilih.getKodeBarang());
                    newU.setQty(modern_pos.utils.Angka.parseQty(txtQty.getText()));
                    String hStr = txtHarga.getText().replaceAll("[^0-9]", ""); newU.setHargaBarang(hStr.isEmpty() ? 0 : Integer.parseInt(hStr));
                    String dStr = txtDp.getText().replaceAll("[^0-9]", ""); newU.setDp(dStr.isEmpty() ? 0 : Integer.parseInt(dStr));
                    String cStr = txtCicilan.getText().replaceAll("[^0-9]", ""); newU.setJumlahCicilan(cStr.isEmpty() ? 0 : Integer.parseInt(cStr));
                    try { newU.setJatuhTempo(LocalDate.parse(txtJatuhTempo.getText())); } catch(Exception e) { newU.setJatuhTempo(null); }
                    
                    controller.simpanUtang(newU, isEdit);
                    formValid = true; // Sukses simpan, keluar dari loop (modal hilang selamanya)
                } catch (Exception ex) {
                    showError(ex.getMessage() != null ? ex.getMessage() : "Terjadi kesalahan saat menyimpan!");
                    // formValid tetap false, sehingga kotak dialog JOptionPane akan dimunculkan ulang!
                }
            } else {
                formValid = true; // Tombol Batal/Silang ditekan, keluar dari loop 
            }
        }
    }

    // Total utang otomatis harga_jual x qty saat barang/qty diubah; tetap bisa diketik manual (harga kredit).
    // Hanya dihitung ulang saat qty (angka valid) atau barang benar-benar berubah, supaya Tab/klik
    // tidak menimpa total yang diketik manual.
    static void pasangTotalOtomatis(JComboBox<Barang> cmb, JTextField txtQty, JTextField txtHarga, boolean hitungSekarang) {
        final int[] qtyTerakhir = { qtyAtauNol(txtQty) };
        Runnable isiTotal = () -> {
            Barang dipilih = (Barang) cmb.getSelectedItem();
            int q = qtyAtauNol(txtQty);
            if (dipilih != null && q > 0) txtHarga.setText(String.valueOf(dipilih.getHarga() * q));
        };
        cmb.addItemListener(e -> { if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) isiTotal.run(); });
        txtQty.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void cek() {
                int q = qtyAtauNol(txtQty);
                if (q > 0 && q != qtyTerakhir[0]) {
                    qtyTerakhir[0] = q;
                    isiTotal.run();
                }
            }
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { cek(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { cek(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { cek(); }
        });
        if (hitungSekarang) isiTotal.run();
    }

    private static int qtyAtauNol(JTextField f) {
        try {
            return modern_pos.utils.Angka.parseQty(f.getText());
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    public void populateTable(List<Utang> list) {
        this.currentList = list;
        tableModel.setRowCount(0);
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("id", "ID"));
        for (Utang u : list) {
            String statusLunas = "lunas".equalsIgnoreCase(u.getStatus()) ? "[ LUNAS ]" : "[ BELUM ]";
            tableModel.addRow(new Object[]{ 
                u.getKodeUtang(), u.getNama(), u.getTelepon(), u.getQty(),
                "Rp " + nf.format(u.getHargaBarang()), "Rp " + nf.format(u.getDp()), 
                u.getJumlahCicilan() + "x", (u.getJatuhTempo() != null) ? u.getJatuhTempo().toString() : "-",
                statusLunas
            });
        }
    }

    public void setLoading(boolean isLoading) {
        setTitle(isLoading ? "Sari Murni Rejeki - Utang (Memuat...)" : "Sari Murni Rejeki - Utang / Piutang");
    }
    public void showError(String msg) { JOptionPane.showMessageDialog(this, "<html><body style='width: 350px; font-family: Segoe UI, sans-serif;'>" + msg.replace("\n", "<br>") + "</body></html>", "Error", JOptionPane.ERROR_MESSAGE); }
    public void showSuccess(String msg) { JOptionPane.showMessageDialog(this, msg, "Sukses", JOptionPane.INFORMATION_MESSAGE); }
}