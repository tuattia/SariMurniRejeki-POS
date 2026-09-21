package controller;

import config.koneksi;
import model.modelstocksnapshot;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller untuk periodic stock snapshot bulanan.
 * Snapshot diambil secara manual oleh user melalui GUI.
 *
 * @author attia
 */
public class snapshotcontroller {

    // ============================================================
    // AMBIL SNAPSHOT SATU BULAN (IDEMPOTENT)
    // Mengambil stok semua barang dan menyimpannya ke stock_snapshot
    // untuk bulan yang dipilih. Jika sudah ada, stok di-update.
    // ============================================================
    public boolean ambilSnapshotBulan(YearMonth bulan) {
        LocalDate periodeDate = bulan.atDay(1);

        String sql = "INSERT INTO stock_snapshot "
                + "(periode, kode_barang, nama_barang, harga_jual, satuan, stok_snapshot) "
                + "SELECT ?, kode_barang, nama_barang, harga_jual, 'Pcs' AS satuan, stok FROM barang "
                + "ON DUPLICATE KEY UPDATE "
                + "stok_snapshot = VALUES(stok_snapshot), "
                + "nama_barang   = VALUES(nama_barang), "
                + "harga_jual    = VALUES(harga_jual), "
                + "satuan        = VALUES(satuan), "
                + "diperbarui_pada = CURRENT_TIMESTAMP";

        Connection con = null;
        try {
            con = koneksi.getConnection();
            if (con == null) return false;

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setDate(1, Date.valueOf(periodeDate));
                int affected = ps.executeUpdate();
                System.out.println("Snapshot " + bulan + ": " + affected + " baris diproses");
                return affected >= 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (con != null) try { con.close(); } catch (Exception e) {}
        }
    }

    // ============================================================
    // CEK APAKAH SNAPSHOT BULAN TERTENTU SUDAH ADA
    // ============================================================
    public boolean sudahAdaSnapshot(YearMonth bulan) {
        LocalDate periodeDate = bulan.atDay(1);
        String sql = "SELECT COUNT(*) FROM stock_snapshot WHERE periode = ?";

        Connection con = null;
        try {
            con = koneksi.getConnection();
            if (con == null) return false;

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setDate(1, Date.valueOf(periodeDate));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (con != null) try { con.close(); } catch (Exception e) {}
        }
        return false;
    }

    // ============================================================
    // GET SNAPSHOT SATU BULAN
    // ============================================================
    public List<modelstocksnapshot> getSnapshotByBulan(YearMonth bulan) {
        LocalDate periodeDate = bulan.atDay(1);
        List<modelstocksnapshot> list = new ArrayList<>();
        String sql = "SELECT * FROM stock_snapshot WHERE periode = ? ORDER BY kode_barang ASC";

        Connection con = null;
        try {
            con = koneksi.getConnection();
            if (con == null) return list;

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setDate(1, Date.valueOf(periodeDate));
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (con != null) try { con.close(); } catch (Exception e) {}
        }
        return list;
    }

    // ============================================================
    // GET DAFTAR BULAN YANG SUDAH PUNYA SNAPSHOT
    // ============================================================
    public List<YearMonth> getDaftarPeriodeTersedia() {
        List<YearMonth> list = new ArrayList<>();
        String sql = "SELECT DISTINCT periode FROM stock_snapshot ORDER BY periode DESC";

        Connection con = null;
        try {
            con = koneksi.getConnection();
            if (con == null) return list;

            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    LocalDate tgl = rs.getDate("periode").toLocalDate();
                    list.add(YearMonth.from(tgl));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (con != null) try { con.close(); } catch (Exception e) {}
        }
        return list;
    }

    // ============================================================
    // PRIVATE HELPER
    // ============================================================
    private modelstocksnapshot mapRow(ResultSet rs) throws Exception {
        modelstocksnapshot snap = new modelstocksnapshot();
        snap.setIdSnapshot(rs.getInt("id_snapshot"));
        snap.setPeriode(rs.getDate("periode").toLocalDate());
        snap.setKodeBarang(rs.getString("kode_barang"));
        snap.setNamaBarang(rs.getString("nama_barang"));
        snap.setHargaJual(rs.getInt("harga_jual"));
        snap.setSatuan(rs.getString("satuan"));
        snap.setStokSnapshot(rs.getInt("stok_snapshot"));
        Timestamp ts = rs.getTimestamp("dibuat_pada");
        if (ts != null) snap.setDibuatPada(ts.toLocalDateTime());
        return snap;
    }
}