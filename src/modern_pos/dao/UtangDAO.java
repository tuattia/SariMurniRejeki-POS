package modern_pos.dao;
import config.koneksi;
import modern_pos.model.Utang;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UtangDAO {

    public List<Utang> getUtangList(String keyword) throws SQLException {
        List<Utang> list = new ArrayList<>();
        boolean isSearch = keyword != null && !keyword.trim().isEmpty();
        String sql = "SELECT * FROM utang "
                + (isSearch ? "WHERE kode_utang LIKE ? OR nama LIKE ? " : "")
                + "ORDER BY status ASC, id_utang DESC";

        try (Connection con = koneksi.open(); PreparedStatement ps = con.prepareStatement(sql)) {
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
                    u.setStatus(rs.getString("status"));
                    u.setKodeBarang(rs.getString("kode_barang"));
                    if (rs.getDate("jatuh_tempo") != null) u.setJatuhTempo(rs.getDate("jatuh_tempo").toLocalDate());
                    list.add(u);
                }
            }
        }
        return list;
    }

    public void tambahUtang(Utang u) throws SQLException {
        String kodeTrx = TransaksiDAO.newKodeTransaksi();
        try (Connection con = koneksi.open()) {
            con.setAutoCommit(false);
            try {
                // 1. Master transaksi dulu: utang dan log_transaksi punya FK ke kode_transaksi
                String sqlTrx = "INSERT INTO transaksi (kode_transaksi, tanggal, nama_pelanggan, total, jenis_transaksi, created_at) VALUES (?, NOW(), ?, ?, 'KREDIT', NOW())";
                try (PreparedStatement ps = con.prepareStatement(sqlTrx)) {
                    ps.setString(1, kodeTrx);
                    ps.setString(2, u.getNama());
                    ps.setInt(3, u.getHargaBarang());
                    ps.executeUpdate();
                }

                // 2. Utang, terhubung ke barang yang dipilih dan transaksi di atas
                String sql = "INSERT INTO utang (kode_utang, nama, alamat, telepon, harga_brng, dp, jumlah_cicilan, jatuh_tempo, status, kode_barang, kode_transaksi) VALUES (?,?,?,?,?,?,?,?,'belum',?,?)";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, u.getKodeUtang());
                    ps.setString(2, u.getNama());
                    ps.setString(3, u.getAlamat());
                    ps.setString(4, u.getTelepon());
                    ps.setInt(5, u.getHargaBarang());
                    ps.setInt(6, u.getDp());
                    ps.setInt(7, u.getJumlahCicilan());
                    ps.setDate(8, u.getJatuhTempo() != null ? java.sql.Date.valueOf(u.getJatuhTempo()) : null);
                    ps.setString(9, u.getKodeBarang());
                    ps.setString(10, kodeTrx);
                    ps.executeUpdate();
                }

                // 3. Log
                String sqlLog = "INSERT INTO log_transaksi (kode_transaksi, tanggal, nama_pelanggan, total, bayar, kembali, tipe_transaksi, keterangan) VALUES (?, NOW(), ?, ?, ?, 0, 'KREDIT', 'Utang Baru (DP)')";
                try (PreparedStatement ps = con.prepareStatement(sqlLog)) {
                    ps.setString(1, kodeTrx);
                    ps.setString(2, u.getNama());
                    ps.setInt(3, u.getHargaBarang());
                    ps.setInt(4, u.getDp());
                    ps.executeUpdate();
                }

                con.commit();
            } catch (SQLException e) {
                throw Tx.rollbackQuietly(con, e);
            }
        }
    }

    public void updateUtang(Utang u) throws SQLException {
        String sql = "UPDATE utang SET nama=?, alamat=?, telepon=?, harga_brng=?, dp=?, jumlah_cicilan=?, jatuh_tempo=?, kode_barang=? WHERE kode_utang=?";
        try (Connection con = koneksi.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u.getNama());
            ps.setString(2, u.getAlamat());
            ps.setString(3, u.getTelepon());
            ps.setInt(4, u.getHargaBarang());
            ps.setInt(5, u.getDp());
            ps.setInt(6, u.getJumlahCicilan());
            ps.setDate(7, u.getJatuhTempo() != null ? java.sql.Date.valueOf(u.getJatuhTempo()) : null);
            ps.setString(8, u.getKodeBarang());
            ps.setString(9, u.getKodeUtang());
            ps.executeUpdate();
        }
    }

    public void hapusUtang(String kode) throws SQLException {
        String sql = "DELETE FROM utang WHERE kode_utang = ?";
        try (Connection con = koneksi.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, kode);
            ps.executeUpdate();
        }
    }

    public void tandaiLunas(String kode) throws SQLException {
        try (Connection con = koneksi.open()) {
            con.setAutoCommit(false);
            try {
                // 1. Sisa utang sebelum ditandai lunas
                String nama = "Pelanggan Utang";
                int sisa = 0;
                try (PreparedStatement ps = con.prepareStatement("SELECT nama, harga_brng, dp FROM utang WHERE kode_utang = ?")) {
                    ps.setString(1, kode);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            nama = rs.getString("nama");
                            sisa = rs.getInt("harga_brng") - rs.getInt("dp");
                        }
                    }
                }

                // 2. Status LUNAS; hanya dari 'belum' supaya klik ganda tidak mencatat pemasukan dua kali
                try (PreparedStatement ps = con.prepareStatement("UPDATE utang SET status = 'lunas' WHERE kode_utang = ? AND status = 'belum'")) {
                    ps.setString(1, kode);
                    if (ps.executeUpdate() == 0) {
                        con.rollback();
                        return;
                    }
                }

                // 3. Catat pemasukan sisa sebagai TUNAI
                if (sisa > 0) {
                    String kodeTrx = TransaksiDAO.newKodeTransaksi();
                    String sqlTrx = "INSERT INTO transaksi (kode_transaksi, tanggal, nama_pelanggan, total, jenis_transaksi, created_at) VALUES (?, NOW(), ?, ?, 'TUNAI', NOW())";
                    try (PreparedStatement ps = con.prepareStatement(sqlTrx)) {
                        ps.setString(1, kodeTrx);
                        ps.setString(2, nama);
                        ps.setInt(3, sisa);
                        ps.executeUpdate();
                    }
                    String sqlLog = "INSERT INTO log_transaksi (kode_transaksi, tanggal, nama_pelanggan, total, bayar, kembali, tipe_transaksi, keterangan) VALUES (?, NOW(), ?, ?, ?, 0, 'TUNAI', 'Pelunasan Sisa Utang')";
                    try (PreparedStatement ps = con.prepareStatement(sqlLog)) {
                        ps.setString(1, kodeTrx);
                        ps.setString(2, nama);
                        ps.setInt(3, sisa);
                        ps.setInt(4, sisa);
                        ps.executeUpdate();
                    }
                }

                con.commit();
            } catch (SQLException e) {
                throw Tx.rollbackQuietly(con, e);
            }
        }
    }
}
