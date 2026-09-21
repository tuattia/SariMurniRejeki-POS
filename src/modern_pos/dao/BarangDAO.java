package modern_pos.dao;
import config.koneksi;
import modern_pos.model.Barang;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BarangDAO {
    public List<Barang> getAllBarang(String keyword) throws SQLException {
        List<Barang> list = new ArrayList<>();
        boolean isSearch = keyword != null && !keyword.trim().isEmpty();
        String sql = "SELECT kode_barang, nama_barang, harga_jual, stok FROM barang "
                + (isSearch ? "WHERE kode_barang LIKE ? OR nama_barang LIKE ? " : "")
                + "ORDER BY kode_barang ASC";
        try (Connection con = koneksi.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (isSearch) {
                ps.setString(1, "%" + keyword + "%");
                ps.setString(2, "%" + keyword + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Barang b = new Barang();
                    b.setKodeBarang(rs.getString("kode_barang"));
                    b.setNamaBarang(rs.getString("nama_barang"));
                    b.setHarga(rs.getInt("harga_jual"));
                    b.setStok(rs.getInt("stok"));
                    list.add(b);
                }
            }
        }
        return list;
    }

    public void tambahBarang(Barang b) throws SQLException {
        String sql = "INSERT INTO barang (kode_barang, nama_barang, harga_jual, stok) VALUES (?, ?, ?, ?)";
        try (Connection con = koneksi.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, b.getKodeBarang());
            ps.setString(2, b.getNamaBarang());
            ps.setInt(3, b.getHarga());
            ps.setInt(4, b.getStok());
            ps.executeUpdate();
        }
    }

    public void updateBarang(Barang b) throws SQLException {
        String sql = "UPDATE barang SET nama_barang=?, harga_jual=?, stok=? WHERE kode_barang=?";
        try (Connection con = koneksi.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, b.getNamaBarang());
            ps.setInt(2, b.getHarga());
            ps.setInt(3, b.getStok());
            ps.setString(4, b.getKodeBarang());
            ps.executeUpdate();
        }
    }

    public void hapusBarang(String kodeBarang) throws SQLException {
        String sql = "DELETE FROM barang WHERE kode_barang = ?";
        try (Connection con = koneksi.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, kodeBarang);
            ps.executeUpdate();
        }
    }
}
