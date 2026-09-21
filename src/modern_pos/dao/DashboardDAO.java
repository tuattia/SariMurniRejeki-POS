package modern_pos.dao;
import config.koneksi;
import modern_pos.model.DashboardSummary;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DashboardDAO {
    public DashboardSummary getSummary() throws SQLException {
        DashboardSummary summary = new DashboardSummary();
        try (Connection con = koneksi.open()) {
            // 1. Pendapatan & Trx Hari Ini
            String sqlTrx = "SELECT SUM(total) as pendapatan, COUNT(kode_transaksi) as total_trx FROM transaksi WHERE DATE(tanggal) = CURDATE()";
            try (PreparedStatement ps = con.prepareStatement(sqlTrx); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    summary.setPendapatanHariIni(rs.getInt("pendapatan")); // jika null otomatis 0 di getInt
                    summary.setTotalTransaksiHariIni(rs.getInt("total_trx"));
                }
            }

            // 2. Utang Aktif (Status Belum Lunas)
            String sqlUtang = "SELECT COUNT(kode_utang) as total_utang FROM utang WHERE status = 'belum'";
            try (PreparedStatement ps = con.prepareStatement(sqlUtang); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) { summary.setJumlahUtangAktif(rs.getInt("total_utang")); }
            }

            // 3. Stok Menipis (Stok <= 5)
            String sqlStok = "SELECT COUNT(kode_barang) as stok_kritis FROM barang WHERE stok <= 5";
            try (PreparedStatement ps = con.prepareStatement(sqlStok); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) { summary.setStokMenipis(rs.getInt("stok_kritis")); }
            }
        }
        return summary;
    }
}
