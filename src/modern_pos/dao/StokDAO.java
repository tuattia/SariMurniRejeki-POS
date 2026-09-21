package modern_pos.dao;
import config.koneksi;
import modern_pos.model.StockMovement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StokDAO {

    // Satu-satunya jalan mengubah stok di modern_pos. Dipanggil di dalam transaksi milik
    // pemanggil (tidak commit/rollback sendiri). FOR UPDATE mengunci baris barang supaya
    // dua kasir bersamaan tidak saling menimpa.
    public static void ubahStok(Connection con, String kodeBarang, int delta, String tipe,
                                String kodeTransaksi, String keterangan) throws SQLException {
        if (delta == 0) return;

        String nama;
        int sebelum;
        try (PreparedStatement ps = con.prepareStatement("SELECT nama_barang, stok FROM barang WHERE kode_barang = ? FOR UPDATE")) {
            ps.setString(1, kodeBarang);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Barang " + kodeBarang + " tidak ditemukan");
                nama = rs.getString("nama_barang");
                sebelum = rs.getInt("stok");
            }
        }

        int sesudah = sebelum + delta;
        if (sesudah < 0) throw new SQLException("Stok " + nama + " tidak cukup");

        try (PreparedStatement ps = con.prepareStatement("UPDATE barang SET stok = ? WHERE kode_barang = ?")) {
            ps.setInt(1, sesudah);
            ps.setString(2, kodeBarang);
            ps.executeUpdate();
        }

        String sqlLog = "INSERT INTO stock_movement_log (kode_barang, kode_transaksi, tipe_gerakan, qty, stok_sebelum, stok_sesudah, keterangan) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sqlLog)) {
            ps.setString(1, kodeBarang);
            ps.setString(2, kodeTransaksi);
            ps.setString(3, tipe);
            ps.setInt(4, Math.abs(delta));
            ps.setInt(5, sebelum);
            ps.setInt(6, sesudah);
            ps.setString(7, keterangan);
            ps.executeUpdate();
        }
    }

    public void restock(String kodeBarang, int qty, String keterangan) throws SQLException {
        if (qty <= 0) throw new IllegalArgumentException("Qty restock harus lebih dari 0");
        String ket = (keterangan == null || keterangan.trim().isEmpty()) ? "Restock" : keterangan.trim();
        try (Connection con = koneksi.open()) {
            con.setAutoCommit(false);
            try {
                ubahStok(con, kodeBarang, qty, "MASUK", null, ket);
                con.commit();
            } catch (SQLException e) {
                throw Tx.rollbackQuietly(con, e);
            }
        }
    }

    public List<StockMovement> riwayat(String kodeBarang) throws SQLException {
        List<StockMovement> list = new ArrayList<>();
        String sql = "SELECT waktu, tipe_gerakan, qty, stok_sebelum, stok_sesudah, kode_transaksi, keterangan "
                + "FROM stock_movement_log WHERE kode_barang = ? ORDER BY waktu DESC, id_log DESC LIMIT 200";
        try (Connection con = koneksi.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, kodeBarang);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StockMovement m = new StockMovement();
                    m.setWaktu(rs.getString("waktu"));
                    m.setTipe(rs.getString("tipe_gerakan"));
                    m.setQty(rs.getInt("qty"));
                    m.setStokSebelum(rs.getInt("stok_sebelum"));
                    m.setStokSesudah(rs.getInt("stok_sesudah"));
                    m.setKodeTransaksi(rs.getString("kode_transaksi"));
                    m.setKeterangan(rs.getString("keterangan"));
                    list.add(m);
                }
            }
        }
        return list;
    }
}
