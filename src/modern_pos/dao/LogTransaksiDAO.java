package modern_pos.dao;
import config.koneksi;
import modern_pos.model.LogTransaksi;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LogTransaksiDAO {

    public List<LogTransaksi> getLogList(String keyword) throws SQLException {
        List<LogTransaksi> list = new ArrayList<>();
        boolean isSearch = keyword != null && !keyword.trim().isEmpty();
        String sql = "SELECT * FROM log_transaksi "
                + (isSearch ? "WHERE kode_transaksi LIKE ? OR nama_pelanggan LIKE ? OR tanggal LIKE ? " : "")
                + "ORDER BY tanggal DESC LIMIT 500"; // Tampilkan 500 terbaru agar aplikasi tidak berat

        try (Connection con = koneksi.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (isSearch) {
                ps.setString(1, "%" + keyword + "%");
                ps.setString(2, "%" + keyword + "%");
                ps.setString(3, "%" + keyword + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LogTransaksi log = new LogTransaksi();
                    log.setKodeTransaksi(rs.getString("kode_transaksi"));
                    log.setTanggal(rs.getString("tanggal"));
                    log.setNamaPelanggan(rs.getString("nama_pelanggan"));
                    log.setTotal(rs.getInt("total"));
                    log.setBayar(rs.getInt("bayar"));
                    log.setKembali(rs.getInt("kembali"));
                    log.setTipeTransaksi(rs.getString("tipe_transaksi"));
                    log.setKeterangan(rs.getString("keterangan"));
                    list.add(log);
                }
            }
        }
        return list;
    }

    public void hapusLog(String kode) throws SQLException {
        String sql = "DELETE FROM log_transaksi WHERE kode_transaksi = ?";
        try (Connection con = koneksi.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, kode);
            ps.executeUpdate();
        }
        // Idealnya hapus juga dari tabel 'transaksi' dan 'transaksi_detail',
        // tapi log bisa saja dihapus tanpa menghapus master transaksinya (tergantung aturan bisnis)
    }
}
