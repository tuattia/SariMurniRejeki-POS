package modern_pos.dao;
import config.koneksi;
import modern_pos.model.Utang;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UtangDAO {
    
    public List<Utang> getUtangList(String keyword) throws Exception {
        List<Utang> list = new ArrayList<>();
        Connection con = koneksi.getConnection();
        if (con == null) throw new Exception("Koneksi database terputus");

        String sql = "SELECT * FROM utang ";
        boolean isSearch = (keyword != null && !keyword.trim().isEmpty());
        if (isSearch) { sql += "WHERE kode_utang LIKE ? OR nama LIKE ? "; }
        sql += "ORDER BY status ASC, id_utang DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (isSearch) {
                ps.setString(1, "%" + keyword + "%");
                ps.setString(2, "%" + keyword + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Utang u = new Utang();
                    u.setKodeUtang(rs.getString("kode_utang"));
                    u.setNama(rs.getString("nama"));
                    u.setAlamat(rs.getString("alamat"));
                    u.setTelepon(rs.getString("telepon"));
                    u.setHargaBarang(rs.getInt("harga_brng"));
                    u.setDp(rs.getInt("dp"));
                    u.setJumlahCicilan(rs.getInt("jumlah_cicilan"));
                    try { u.setStatus(rs.getString("status")); } catch (Exception e) { u.setStatus("-"); }
                    if(rs.getDate("jatuh_tempo") != null) u.setJatuhTempo(rs.getDate("jatuh_tempo").toLocalDate());
                    list.add(u);
                }
            }
        }
        return list;
    }

    public void tambahUtang(Utang u) throws Exception {
        Connection con = koneksi.getConnection();
        boolean autoCommit = con.getAutoCommit();
        try {
            con.setAutoCommit(false); // Memulai Database Transaction

            // 1. Simpan ke tabel utang
            String sql = "INSERT INTO utang (kode_utang, nama, alamat, telepon, harga_brng, dp, jumlah_cicilan, jatuh_tempo, status, kode_barang) VALUES (?,?,?,?,?,?,?,?,'belum', (SELECT kode_barang FROM barang LIMIT 1))";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, u.getKodeUtang());
                ps.setString(2, u.getNama());
                ps.setString(3, u.getAlamat());
                ps.setString(4, u.getTelepon());
                ps.setInt(5, u.getHargaBarang());
                ps.setInt(6, u.getDp());
                ps.setInt(7, u.getJumlahCicilan());
                ps.setDate(8, (u.getJatuhTempo() != null) ? java.sql.Date.valueOf(u.getJatuhTempo()) : null);
                ps.executeUpdate();
            }

            // 2. Simpan ke tabel MASTER transaksi dulu (Syarat mutlak Foreign Key dari log_transaksi)
            String kodeTrx = "TRX-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String sqlTrx = "INSERT INTO transaksi (kode_transaksi, tanggal, nama_pelanggan, total, jenis_transaksi, created_at) VALUES (?, NOW(), ?, ?, 'KREDIT', NOW())";
            try (PreparedStatement psT = con.prepareStatement(sqlTrx)) {
                psT.setString(1, kodeTrx);
                psT.setString(2, u.getNama());
                psT.setInt(3, u.getHargaBarang());
                psT.executeUpdate();
            }

            // 3. Baru simpan detailnya ke tabel log_transaksi
            String sqlLog = "INSERT INTO log_transaksi (kode_transaksi, tanggal, nama_pelanggan, total, bayar, kembali, tipe_transaksi, keterangan) VALUES (?, NOW(), ?, ?, ?, 0, 'KREDIT', 'Utang Baru (DP)')";
            try (PreparedStatement psLog = con.prepareStatement(sqlLog)) {
                psLog.setString(1, kodeTrx);
                psLog.setString(2, u.getNama());
                psLog.setInt(3, u.getHargaBarang());
                psLog.setInt(4, u.getDp());
                psLog.executeUpdate();
            }

            con.commit();
        } catch (Exception e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(autoCommit);
        }
    }

    public void updateUtang(Utang u) throws Exception {
        Connection con = koneksi.getConnection();
        String sql = "UPDATE utang SET nama=?, alamat=?, telepon=?, harga_brng=?, dp=?, jumlah_cicilan=?, jatuh_tempo=? WHERE kode_utang=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u.getNama());
            ps.setString(2, u.getAlamat());
            ps.setString(3, u.getTelepon());
            ps.setInt(4, u.getHargaBarang());
            ps.setInt(5, u.getDp());
            ps.setInt(6, u.getJumlahCicilan());
            ps.setDate(7, (u.getJatuhTempo() != null) ? java.sql.Date.valueOf(u.getJatuhTempo()) : null);
            ps.setString(8, u.getKodeUtang());
            ps.executeUpdate();
        }
    }

    public void hapusUtang(String kode) throws Exception {
        Connection con = koneksi.getConnection();
        String sql = "DELETE FROM utang WHERE kode_utang = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, kode);
            ps.executeUpdate();
        }
    }

    public void tandaiLunas(String kode) throws Exception {
        Connection con = koneksi.getConnection();
        boolean autoCommit = con.getAutoCommit();
        try {
            con.setAutoCommit(false);

            // 1. Dapatkan informasi nominal sisa utang sebelum ditandai lunas
            String sqlInfo = "SELECT nama, harga_brng, dp FROM utang WHERE kode_utang = ?";
            String nama = "Pelanggan Utang";
            int sisa = 0;
            try(PreparedStatement psInfo = con.prepareStatement(sqlInfo)) {
                psInfo.setString(1, kode);
                ResultSet rs = psInfo.executeQuery();
                if(rs.next()) {
                    nama = rs.getString("nama");
                    sisa = rs.getInt("harga_brng") - rs.getInt("dp");
                }
            }

            // 2. Ubah status utang menjadi LUNAS
            String sql = "UPDATE utang SET status = 'lunas' WHERE kode_utang = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, kode);
                ps.executeUpdate();
            }

            // 3. Catat pemasukan di log_transaksi sebagai "TUNAI"
            if (sisa > 0) {
                String kodeTrx = "TRX-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                
                // Simpan ke tabel master transaksi terlebih dahulu (Syarat FK)
                String sqlTrx = "INSERT INTO transaksi (kode_transaksi, tanggal, nama_pelanggan, total, jenis_transaksi, created_at) VALUES (?, NOW(), ?, ?, 'TUNAI', NOW())";
                try (PreparedStatement psT = con.prepareStatement(sqlTrx)) {
                    psT.setString(1, kodeTrx);
                    psT.setString(2, nama);
                    psT.setInt(3, sisa);
                    psT.executeUpdate();
                }

                // Baru simpan ke log_transaksi
                String sqlLog = "INSERT INTO log_transaksi (kode_transaksi, tanggal, nama_pelanggan, total, bayar, kembali, tipe_transaksi, keterangan) VALUES (?, NOW(), ?, ?, ?, 0, 'TUNAI', 'Pelunasan Sisa Utang')";
                try (PreparedStatement psLog = con.prepareStatement(sqlLog)) {
                    psLog.setString(1, kodeTrx);
                    psLog.setString(2, nama);
                    psLog.setInt(3, sisa);
                    psLog.setInt(4, sisa);
                    psLog.executeUpdate();
                }
            }

            con.commit();
        } catch (Exception e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(autoCommit);
        }
    }
}