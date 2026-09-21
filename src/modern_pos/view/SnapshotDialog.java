package modern_pos.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.sql.SQLException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import modern_pos.dao.SnapshotDAO;
import modern_pos.model.StockSnapshot;

// Port gui.snapshotDialog ke SnapshotDAO.
// ponytail: query DB sinkron di EDT; tabel barang kecil. Pindah ke SwingWorker bila terasa lambat.
public class SnapshotDialog extends JDialog {

    private final SnapshotDAO dao = new SnapshotDAO();

    private JComboBox<String> cboBulan;
    private JLabel lblStatus;
    private DefaultTableModel tblModel;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("id", "ID"));

    public SnapshotDialog(Frame parent) {
        super(parent, "Snapshot Stok Bulanan", true);
        initUI();
        setSize(900, 540);
        setLocationRelativeTo(parent);
        updateStatus(YearMonth.now());
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(242, 246, 250));

        JLabel lblTitle = new JLabel("Snapshot Stok Bulanan", JLabel.LEFT);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(new Color(23, 118, 211));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(12, 16, 4, 0));
        add(lblTitle, BorderLayout.NORTH);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        toolbar.setBackground(new Color(220, 235, 255));

        cboBulan = new JComboBox<>();
        cboBulan.setPreferredSize(new Dimension(200, 30));
        YearMonth now = YearMonth.now();
        for (int i = 0; i < 12; i++) cboBulan.addItem(now.minusMonths(i).format(FMT));

        JButton btnSimpan = buatTombol("Simpan Snapshot", new Color(23, 118, 211));
        JButton btnLihat = buatTombol("Lihat Data", new Color(40, 167, 69));

        lblStatus = new JLabel("Pilih bulan lalu klik tombol.");
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 11));

        toolbar.add(new JLabel("Pilih Bulan:"));
        toolbar.add(cboBulan);
        toolbar.add(btnSimpan);
        toolbar.add(btnLihat);
        toolbar.add(Box.createHorizontalStrut(16));
        toolbar.add(lblStatus);
        add(toolbar, BorderLayout.AFTER_LAST_LINE);

        tblModel = new DefaultTableModel(
                new String[]{"Kode Barang", "Nama Barang", "Satuan", "Stok Snapshot", "Harga Jual", "Periode"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tbl = new JTable(tblModel);
        tbl.setRowHeight(26);
        JScrollPane sp = new JScrollPane(tbl);
        sp.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));
        add(sp, BorderLayout.CENTER);

        btnSimpan.addActionListener(e -> aksiSimpan());
        btnLihat.addActionListener(e -> aksiLihat());
        cboBulan.addActionListener(e -> updateStatus(getSelectedYM()));
    }

    private void aksiSimpan() {
        YearMonth ym = getSelectedYM();
        try {
            if (dao.sudahAdaSnapshot(ym)) {
                int c = JOptionPane.showConfirmDialog(this,
                        "Snapshot " + ym.format(FMT) + " sudah ada.\nTimpa dengan data stok terkini?",
                        "Konfirmasi Timpa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (c != JOptionPane.YES_OPTION) return;
            }
            dao.ambilSnapshotBulan(ym);
            JOptionPane.showMessageDialog(this, "Snapshot " + ym.format(FMT) + " berhasil disimpan!",
                    "Berhasil", JOptionPane.INFORMATION_MESSAGE);
            loadTabel(ym);
        } catch (SQLException ex) {
            tampilError(ex);
        }
        updateStatus(ym);
    }

    private void aksiLihat() {
        YearMonth ym = getSelectedYM();
        try {
            loadTabel(ym);
        } catch (SQLException ex) {
            tampilError(ex);
        }
    }

    private YearMonth getSelectedYM() {
        return YearMonth.now().minusMonths(Math.max(0, cboBulan.getSelectedIndex()));
    }

    private void loadTabel(YearMonth ym) throws SQLException {
        tblModel.setRowCount(0);
        List<StockSnapshot> list = dao.getSnapshotByBulan(ym);
        for (StockSnapshot s : list) {
            tblModel.addRow(new Object[]{ s.getKodeBarang(), s.getNamaBarang(), s.getSatuan(),
                    s.getStokSnapshot(), s.getHargaJual(), s.getPeriode().format(FMT) });
        }
        if (list.isEmpty()) setStatus("Belum ada snapshot " + ym.format(FMT) + ". Klik 'Simpan Snapshot'.", new Color(180, 80, 0));
        else setStatus(list.size() + " item ditampilkan untuk " + ym.format(FMT) + ".", new Color(30, 140, 60));
    }

    private void updateStatus(YearMonth ym) {
        try {
            if (dao.sudahAdaSnapshot(ym)) setStatus("Snapshot " + ym.format(FMT) + " sudah tersedia.", new Color(30, 140, 60));
            else setStatus("Belum ada snapshot " + ym.format(FMT) + ".", new Color(180, 80, 0));
        } catch (SQLException ex) {
            setStatus("Gagal membaca snapshot: " + ex.getMessage(), new Color(198, 40, 40));
        }
    }

    private void tampilError(SQLException ex) {
        JOptionPane.showMessageDialog(this, "Gagal: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void setStatus(String teks, Color warna) {
        lblStatus.setText(teks);
        lblStatus.setForeground(warna);
    }

    private JButton buatTombol(String teks, Color bg) {
        JButton btn = new JButton(teks);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(170, 30));
        return btn;
    }
}
