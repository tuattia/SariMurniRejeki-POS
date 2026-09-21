package modern_pos.dao;
import config.koneksi;
import modern_pos.model.StockSnapshot;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class SnapshotDAO {

    // Idempotent: snapshot bulan yang sama di-update, bukan diduplikasi (UNIQUE periode+kode_barang).
    public void ambilSnapshotBulan(YearMonth bulan) throws SQLException {
        String sql = "INSERT INTO stock_snapshot (periode, kode_barang, nama_barang, harga_jual, satuan, stok_snapshot) "
                + "SELECT ?, kode_barang, nama_barang, harga_jual, COALESCE(satuan, 'pcs'), stok FROM barang "
                + "ON DUPLICATE KEY UPDATE stok_snapshot = VALUES(stok_snapshot), nama_barang = VALUES(nama_barang), "
                + "harga_jual = VALUES(harga_jual), satuan = VALUES(satuan), diperbarui_pada = CURRENT_TIMESTAMP";
        try (Connection con = koneksi.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(bulan.atDay(1)));
            ps.executeUpdate();
        }
    }

    public boolean sudahAdaSnapshot(YearMonth bulan) throws SQLException {
        try (Connection con = koneksi.open();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM stock_snapshot WHERE periode = ?")) {
            ps.setDate(1, Date.valueOf(bulan.atDay(1)));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public List<StockSnapshot> getSnapshotByBulan(YearMonth bulan) throws SQLException {
        List<StockSnapshot> list = new ArrayList<>();
        String sql = "SELECT periode, kode_barang, nama_barang, harga_jual, satuan, stok_snapshot "
                + "FROM stock_snapshot WHERE periode = ? ORDER BY kode_barang ASC";
        try (Connection con = koneksi.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(bulan.atDay(1)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StockSnapshot s = new StockSnapshot();
                    s.setPeriode(rs.getDate("periode").toLocalDate());
                    s.setKodeBarang(rs.getString("kode_barang"));
                    s.setNamaBarang(rs.getString("nama_barang"));
                    s.setHargaJual(rs.getInt("harga_jual"));
                    s.setSatuan(rs.getString("satuan"));
                    s.setStokSnapshot(rs.getInt("stok_snapshot"));
                    list.add(s);
                }
            }
        }
        return list;
    }
}
