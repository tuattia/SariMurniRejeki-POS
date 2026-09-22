package modern_pos.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import modern_pos.model.KartuAngsuran;
import modern_pos.model.Utang;
import modern_pos.utils.Cetak;
import modern_pos.utils.StrukFormatter;
import modern_pos.utils.Toko;

// Kartu angsuran kertas 15x20 cm (567x756 px @96dpi): baris kosong ditulis tangan saat pelanggan membayar.
public class KartuAngsuranDialog extends JDialog {
    private static final int BARIS_ANGSURAN = 15;

    public KartuAngsuranDialog(Frame parent, KartuAngsuran k) {
        super(parent, "Kartu Angsuran " + k.getUtang().getKodeUtang(), true);
        Utang u = k.getUtang();

        JPanel kartu = new JPanel();
        kartu.setLayout(new BoxLayout(kartu, BoxLayout.Y_AXIS));
        kartu.setBackground(Color.WHITE);
        kartu.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        kartu.setPreferredSize(new Dimension(567, 756));

        kartu.add(judul("KARTU ANGSURAN", 16));
        kartu.add(judul(Toko.NAMA, 13));

        JPanel info = new JPanel(new GridLayout(0, 2, 6, 2));
        info.setBackground(Color.WHITE);
        info.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        baris(info, "Kode Utang", u.getKodeUtang());
        baris(info, "Nama", u.getNama());
        baris(info, "Telepon", u.getTelepon());
        baris(info, "Alamat", u.getAlamat());
        baris(info, "Nama Barang", k.getNamaBarang());
        baris(info, "Qty", String.valueOf(u.getQty()));
        baris(info, "Harga Barang", StrukFormatter.rupiah(u.getHargaBarang()));
        baris(info, "DP", StrukFormatter.rupiah(u.getDp()));
        baris(info, "Jumlah Cicilan", u.getJumlahCicilan() + "x");
        baris(info, "Jatuh Tempo", u.getJatuhTempo() != null ? u.getJatuhTempo().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "-");
        baris(info, "Bayar Perbulan", StrukFormatter.rupiah(u.bayarPerBulan()));
        kartu.add(info);

        DefaultTableModel model = new DefaultTableModel(new String[]{"Angsuran", "Tanggal", "Nominal", "Sisa", "Paraf"}, BARIS_ANGSURAN) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabel = new JTable(model);
        tabel.setRowHeight(22);
        tabel.setShowGrid(true);
        tabel.setGridColor(Color.BLACK);
        JPanel wadahTabel = new JPanel(new BorderLayout());
        wadahTabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        wadahTabel.add(tabel.getTableHeader(), BorderLayout.NORTH);
        wadahTabel.add(tabel, BorderLayout.CENTER);
        kartu.add(wadahTabel);

        JPanel ttd = new JPanel(new GridLayout(3, 2));
        ttd.setBackground(Color.WHITE);
        ttd.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        ttd.add(tengah("Pengawas"));
        ttd.add(tengah("Peminjam"));
        ttd.add(tengah(" "));
        ttd.add(tengah(" "));
        ttd.add(tengah("(" + Toko.PENGAWAS + ")"));
        ttd.add(tengah("(..............................)"));
        kartu.add(ttd);

        JButton btnCetak = new JButton("Cetak");
        JButton btnTutup = new JButton("Tutup");
        btnCetak.addActionListener(e -> {
            try {
                Cetak.cetakKomponen(kartu, 150, 200, "Kartu Angsuran " + u.getKodeUtang());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Gagal mencetak: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        btnTutup.addActionListener(e -> dispose());
        JPanel tombol = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        tombol.add(btnCetak);
        tombol.add(btnTutup);

        setLayout(new BorderLayout());
        add(kartu, BorderLayout.CENTER);
        add(tombol, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(parent);
    }

    private static JLabel judul(String teks, int ukuran) {
        JLabel l = tengah(teks);
        l.setFont(new Font("Segoe UI", Font.BOLD, ukuran));
        l.setAlignmentX(0.5f);
        return l;
    }

    private static JLabel tengah(String teks) {
        JLabel l = new JLabel(teks, SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return l;
    }

    private static void baris(JPanel p, String label, String nilai) {
        JLabel l = new JLabel(label + " :");
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JLabel v = new JLabel(nilai != null ? nilai : "-");
        v.setFont(new Font("Segoe UI", Font.BOLD, 12));
        p.add(l);
        p.add(v);
    }
}
