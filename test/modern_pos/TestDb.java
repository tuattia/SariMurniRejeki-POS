package modern_pos;

import config.koneksi;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// Helper test: semua test DAO memakai DB sarimurnirejeki_test, bukan DB utama.
public class TestDb {
    public static final String URL = "jdbc:mysql://localhost:3306/sarimurnirejeki_test";

    public static void reset() throws SQLException {
        System.setProperty("db.url", URL);
        if (!System.getProperty("db.url").endsWith("_test")) {
            throw new IllegalStateException("Menolak reset DB non-test: " + System.getProperty("db.url"));
        }
        try (Connection c = koneksi.open(); Statement s = c.createStatement()) {
            s.execute("SET FOREIGN_KEY_CHECKS=0");
            for (String t : new String[]{"log_transaksi", "transaksi_detail", "utang", "transaksi",
                    "barang", "user", "stock_movement_log", "stock_snapshot"}) {
                s.execute("TRUNCATE TABLE " + t);
            }
            s.execute("SET FOREIGN_KEY_CHECKS=1");
            s.execute("INSERT INTO barang (kode_barang, nama_barang, harga_jual, stok) VALUES "
                    + "('B001', 'Beras 5kg', 65000, 10), ('B002', 'Gula 1kg', 15000, 2)");
        }
    }

    // Data uji bersama untuk test DAO.
    public static modern_pos.model.Barang barang(String kode, String nama, int harga, int stok) {
        modern_pos.model.Barang b = new modern_pos.model.Barang();
        b.setKodeBarang(kode);
        b.setNamaBarang(nama);
        b.setHarga(harga);
        b.setStok(stok);
        return b;
    }

    public static modern_pos.model.Utang utang(String kode, String kodeBarang, int harga, int dp) {
        return utang(kode, kodeBarang, harga, dp, 1);
    }

    public static modern_pos.model.Utang utang(String kode, String kodeBarang, int harga, int dp, int qty) {
        modern_pos.model.Utang u = new modern_pos.model.Utang();
        u.setKodeUtang(kode);
        u.setNama("Siti");
        u.setAlamat("-");
        u.setTelepon("0812");
        u.setHargaBarang(harga);
        u.setDp(dp);
        u.setJumlahCicilan(3);
        u.setJatuhTempo(java.time.LocalDate.of(2026, 12, 1));
        u.setKodeBarang(kodeBarang);
        u.setQty(qty);
        return u;
    }

    public static int queryInt(String sql, Object... params) throws SQLException {
        String v = queryString(sql, params);
        return v == null ? 0 : Integer.parseInt(v);
    }

    public static String queryString(String sql, Object... params) throws SQLException {
        try (Connection c = koneksi.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }
}
