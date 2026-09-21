package gui;

import controller.snapshotcontroller;
import model.modelstocksnapshot;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Dialog untuk fitur Snapshot Stok Bulanan.
 * Dibuka dari tombol "Snapshot Bulanan" di halaman Stock.
 *
 * @author attia
 */
public class snapshotDialog extends JDialog {

    private snapshotcontroller sc = new snapshotcontroller();

    private JComboBox<String> cboBulan;
    private JButton btnSimpan;
    private JButton btnLihat;
    private JLabel lblStatus;
    private JTable tblSnapshot;
    private DefaultTableModel tblModel;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("id", "ID"));

    public snapshotDialog(Frame parent, boolean modal) {
        super(parent, "Snapshot Stok Bulanan", modal);
        initUI();
        setSize(900, 540);
        setLocationRelativeTo(parent);
        setResizable(true);
        updateStatus(YearMonth.now());
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(242, 246, 250));

        // ── Header ──────────────────────────────────────────────
        JLabel lblTitle = new JLabel("📅 Snapshot Stok Bulanan", JLabel.LEFT);
        lblTitle.setFont(new Font("Poppins", Font.BOLD, 18));
        lblTitle.setForeground(new Color(23, 118, 211));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(12, 16, 4, 0));
        add(lblTitle, BorderLayout.NORTH);

        // ── Toolbar ─────────────────────────────────────────────
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        toolbar.setBackground(new Color(220, 235, 255));
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(180, 200, 230)));

        JLabel lblBulan = new JLabel("Pilih Bulan:");
        lblBulan.setFont(new Font("Poppins", Font.PLAIN, 13));

        cboBulan = new JComboBox<>();
        cboBulan.setPreferredSize(new Dimension(200, 30));
        cboBulan.setFont(new Font("Poppins", Font.PLAIN, 12));
        populateCbo();

        btnSimpan = buatTombol("💾  Simpan Snapshot", new Color(23, 118, 211));
        btnLihat  = buatTombol("🔍  Lihat Data",      new Color(40, 167, 69));

        lblStatus = new JLabel("Pilih bulan lalu klik tombol.");
        lblStatus.setFont(new Font("Poppins", Font.ITALIC, 11));
        lblStatus.setForeground(new Color(90, 90, 90));

        toolbar.add(lblBulan);
        toolbar.add(cboBulan);
        toolbar.add(btnSimpan);
        toolbar.add(btnLihat);
        toolbar.add(Box.createHorizontalStrut(16));
        toolbar.add(lblStatus);

        add(toolbar, BorderLayout.AFTER_LAST_LINE);

        // ── Tabel ───────────────────────────────────────────────
        tblModel = new DefaultTableModel(
                new String[]{"Kode Barang", "Nama Barang", "Satuan", "Stok Snapshot", "Harga Jual", "Periode"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblSnapshot = new JTable(tblModel);
        tblSnapshot.setRowHeight(26);
        tblSnapshot.setFont(new Font("Poppins", Font.PLAIN, 12));
        tblSnapshot.getTableHeader().setFont(new Font("Poppins", Font.BOLD, 12));
        tblSnapshot.getTableHeader().setBackground(new Color(23, 118, 211));
        tblSnapshot.getTableHeader().setForeground(Color.WHITE);
        tblSnapshot.setSelectionBackground(new Color(180, 215, 255));
        tblSnapshot.setGridColor(new Color(210, 220, 230));

        // Lebar kolom
        tblSnapshot.getColumnModel().getColumn(0).setPreferredWidth(100);
        tblSnapshot.getColumnModel().getColumn(1).setPreferredWidth(220);
        tblSnapshot.getColumnModel().getColumn(2).setPreferredWidth(80);
        tblSnapshot.getColumnModel().getColumn(3).setPreferredWidth(110);
        tblSnapshot.getColumnModel().getColumn(4).setPreferredWidth(110);
        tblSnapshot.getColumnModel().getColumn(5).setPreferredWidth(150);

        JScrollPane sp = new JScrollPane(tblSnapshot);
        sp.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));
        add(sp, BorderLayout.CENTER);

        // ── Action Listeners ─────────────────────────────────────
        btnSimpan.addActionListener(e -> aksiSimpan());
        btnLihat.addActionListener(e -> aksiLihat());

        cboBulan.addActionListener(e -> updateStatus(getSelectedYM()));
    }

    // ── Actions ──────────────────────────────────────────────────

    private void aksiSimpan() {
        YearMonth ym = getSelectedYM();
        if (ym == null) return;

        if (sc.sudahAdaSnapshot(ym)) {
            int c = JOptionPane.showConfirmDialog(this,
                    "Snapshot " + ym.format(FMT) + " sudah ada.\nTimpa dengan data stok terkini?",
                    "Konfirmasi Timpa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (c != JOptionPane.YES_OPTION) return;
        }

        boolean ok = sc.ambilSnapshotBulan(ym);
        if (ok) {
            JOptionPane.showMessageDialog(this,
                    "✓ Snapshot " + ym.format(FMT) + " berhasil disimpan!",
                    "Berhasil", JOptionPane.INFORMATION_MESSAGE);
            loadTabel(ym);
        } else {
            JOptionPane.showMessageDialog(this,
                    "✗ Gagal menyimpan snapshot.\nPastikan tabel stock_snapshot sudah dibuat di database.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
        updateStatus(ym);
    }

    private void aksiLihat() {
        YearMonth ym = getSelectedYM();
        if (ym == null) return;
        loadTabel(ym);
        updateStatus(ym);
    }

    // ── Helpers ──────────────────────────────────────────────────

    private void populateCbo() {
        YearMonth now = YearMonth.now();
        for (int i = 0; i < 12; i++) {
            cboBulan.addItem(now.minusMonths(i).format(FMT));
        }
        cboBulan.setSelectedIndex(0);
    }

    private YearMonth getSelectedYM() {
        int idx = cboBulan.getSelectedIndex();
        return idx >= 0 ? YearMonth.now().minusMonths(idx) : null;
    }

    private void loadTabel(YearMonth ym) {
        tblModel.setRowCount(0);
        List<modelstocksnapshot> list = sc.getSnapshotByBulan(ym);
        for (modelstocksnapshot s : list) {
            tblModel.addRow(new Object[]{
                s.getKodeBarang(),
                s.getNamaBarang(),
                s.getSatuan(),
                s.getStokSnapshot(),
                s.getHargaJual(),
                s.getPeriode() != null ? s.getPeriode().format(FMT) : "-"
            });
        }
        if (list.isEmpty()) {
            setStatus("⚠ Belum ada snapshot " + ym.format(FMT) + ". Klik 'Simpan Snapshot'.", new Color(180, 80, 0));
        } else {
            setStatus("✓ " + list.size() + " item ditampilkan untuk " + ym.format(FMT) + ".", new Color(30, 140, 60));
        }
    }

    private void updateStatus(YearMonth ym) {
        if (ym == null) return;
        boolean ada = sc.sudahAdaSnapshot(ym);
        if (ada) {
            setStatus("✓ Snapshot " + ym.format(FMT) + " sudah tersedia.", new Color(30, 140, 60));
        } else {
            setStatus("⚠ Belum ada snapshot " + ym.format(FMT) + ".", new Color(180, 80, 0));
        }
    }

    private void setStatus(String teks, Color warna) {
        lblStatus.setText(teks);
        lblStatus.setForeground(warna);
    }

    private JButton buatTombol(String teks, Color bg) {
        JButton btn = new JButton(teks);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Poppins", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(170, 30));
        return btn;
    }
}