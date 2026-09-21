package modern_pos.dao;
import config.koneksi;
import modern_pos.model.LogTransaksi;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class LogTransaksiDAO {
    
    public List<LogTransaksi> getLogList(String keyword) throws Exception {
        List<LogTransaksi> list = new ArrayList<>();
        Connection con = koneksi.getConnection();
        if (con == null) throw new Exception("Koneksi database terputus");

        String sql = "SELECT * FROM log_transaksi ";
        boolean isSearch = (keyword != null && !keyword.trim().isEmpty());
        if (isSearch) { sql += "WHERE kode_transaksi LIKE ? OR nama_pelanggan LIKE ? OR tanggal LIKE ? "; }
        sql += "ORDER BY tanggal DESC LIMIT 500"; // Tampilkan 500 terbaru agar aplikasi tidak berat

        try (PreparedStatement ps = con.prepareStatement(sql)) {
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
                    try { log.setTipeTransaksi(rs.getString("tipe_transaksi")); } catch (Exception e) {}
                    try { log.setKeterangan(rs.getString("keterangan")); } catch (Exception e) {}
                    list.add(log);
                }
            }
        }
        return list;
    }

    public void hapusLog(String kode) throws Exception {
        Connection con = koneksi.getConnection();
        String sql = "DELETE FROM log_transaksi WHERE kode_transaksi = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, kode);
            ps.executeUpdate();
        }
        // Idealnya hapus juga dari tabel 'transaksi' dan 'transaksi_detail', 
        // tapi log bisa saja dihapus tanpa menghapus master transaksinya (tergantung aturan bisnis)
    }
}