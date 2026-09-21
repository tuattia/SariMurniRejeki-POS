package modern_pos.view;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import modern_pos.model.Barang;
import modern_pos.model.StockMovement;
import modern_pos.utils.SwingHelper;
import modern_pos.utils.UITheme;

public class RiwayatStokDialog extends JDialog {
    public RiwayatStokDialog(Frame parent, Barang b, List<StockMovement> list) {
        super(parent, "Riwayat Stok - " + b.getKodeBarang() + " " + b.getNamaBarang(), true);
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Waktu", "Tipe", "Qty", "Stok Sebelum", "Stok Sesudah", "Kode Transaksi", "Keterangan"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (StockMovement m : list) {
            model.addRow(new Object[]{ m.getWaktu(), m.getTipe(), m.getQty(), m.getStokSebelum(),
                    m.getStokSesudah(), m.getKodeTransaksi() != null ? m.getKodeTransaksi() : "-", m.getKeterangan() });
        }
        JTable tbl = new JTable(model);
        SwingHelper.styleTable(tbl);
        JScrollPane sp = new JScrollPane(tbl);
        sp.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setLayout(new BorderLayout());
        add(SwingHelper.createLabel(list.isEmpty() ? "  Belum ada gerakan stok." : "  " + list.size() + " gerakan terbaru",
                UITheme.FONT_BODY, UITheme.COLOR_TEXT_SECONDARY), BorderLayout.NORTH);
        add(sp, BorderLayout.CENTER);
        setSize(900, 450);
        setLocationRelativeTo(parent);
    }
}
