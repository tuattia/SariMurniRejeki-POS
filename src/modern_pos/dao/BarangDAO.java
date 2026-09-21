package modern_pos.dao;
import config.koneksi;
import modern_pos.model.Barang;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class BarangDAO {
    public List<Barang> getAllBarang(String keyword) throws Exception {
        List<Barang> list = new ArrayList<>();
        Connection con = koneksi.getConnection();
        String sql = "SELECT * FROM barang ";
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql += "WHERE kode_barang LIKE ? OR nama_barang LIKE ? OR nama LIKE ?";
        }
        sql += " ORDER BY kode_barang ASC";
        
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (keyword != null && !keyword.trim().isEmpty()) {
                ps.setString(1, "%" + keyword + "%");
                ps.setString(2, "%" + keyword + "%");
                ps.setString(3, "%" + keyword + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Barang b = new Barang();
                    b.setKodeBarang(rs.getString("kode_barang"));
                    try { b.setNamaBarang(rs.getString("nama_barang")); } catch (Exception e) { b.setNamaBarang(rs.getString("nama")); }
                    try { b.setHarga(rs.getInt("harga_jual")); } catch (Exception e) { b.setHarga(rs.getInt("harga")); }
                    try { b.setStok(rs.getInt("stok")); } catch (Exception e) { try { b.setStok(rs.getInt("jumlah")); } catch (Exception ex) { b.setStok(0); } }
                    list.add(b);
                }
            }
        }
        return list;
    }

    public void tambahBarang(Barang b) throws Exception {
        Connection con = koneksi.getConnection();
        String sql = "INSERT INTO barang (kode_barang, nama_barang, harga_jual, stok) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, b.getKodeBarang());
            ps.setString(2, b.getNamaBarang());
            ps.setInt(3, b.getHarga());
            ps.setInt(4, b.getStok());
            ps.executeUpdate();
        } catch (Exception e) {
            // Fallback nama kolom
            String sqlFallback = "INSERT INTO barang (kode_barang, nama, harga, jumlah) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps2 = con.prepareStatement(sqlFallback)) {
                ps2.setString(1, b.getKodeBarang());
                ps2.setString(2, b.getNamaBarang());
                ps2.setInt(3, b.getHarga());
                ps2.setInt(4, b.getStok());
                ps2.executeUpdate();
            }
        }
    }

    public void updateBarang(Barang b) throws Exception {
        Connection con = koneksi.getConnection();
        String sql = "UPDATE barang SET nama_barang=?, harga_jual=?, stok=? WHERE kode_barang=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, b.getNamaBarang());
            ps.setInt(2, b.getHarga());
            ps.setInt(3, b.getStok());
            ps.setString(4, b.getKodeBarang());
            ps.executeUpdate();
        } catch (Exception e) {
            String sqlFallback = "UPDATE barang SET nama=?, harga=?, jumlah=? WHERE kode_barang=?";
            try (PreparedStatement ps2 = con.prepareStatement(sqlFallback)) {
                ps2.setString(1, b.getNamaBarang());
                ps2.setInt(2, b.getHarga());
                ps2.setInt(3, b.getStok());
                ps2.setString(4, b.getKodeBarang());
                ps2.executeUpdate();
            }
        }
    }

    public void hapusBarang(String kodeBarang) throws Exception {
        Connection con = koneksi.getConnection();
        String sql = "DELETE FROM barang WHERE kode_barang = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, kodeBarang);
            ps.executeUpdate();
        }
    }
}