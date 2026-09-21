package modern_pos.dao;

import config.koneksi;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import modern_pos.TestDb;
import modern_pos.model.StockMovement;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class StokDAOTest {
    private final StokDAO dao = new StokDAO();

    @Before
    public void setUp() throws Exception {
        TestDb.reset();
    }

    private static void ubahDalamTransaksi(String kode, int delta, String tipe) throws SQLException {
        try (Connection con = koneksi.open()) {
            con.setAutoCommit(false);
            try {
                StokDAO.ubahStok(con, kode, delta, tipe, "TRXTEST", "uji");
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    @Test
    public void ubahStokMengubahStokDanMencatatLog() throws Exception {
        ubahDalamTransaksi("B001", -3, "KELUAR");

        assertEquals(7, TestDb.queryInt("SELECT stok FROM barang WHERE kode_barang='B001'"));
        assertEquals("KELUAR", TestDb.queryString("SELECT tipe_gerakan FROM stock_movement_log"));
        assertEquals(3, TestDb.queryInt("SELECT qty FROM stock_movement_log"));
        assertEquals(10, TestDb.queryInt("SELECT stok_sebelum FROM stock_movement_log"));
        assertEquals(7, TestDb.queryInt("SELECT stok_sesudah FROM stock_movement_log"));
        assertEquals("TRXTEST", TestDb.queryString("SELECT kode_transaksi FROM stock_movement_log"));
    }

    @Test
    public void stokKurangDitolakTanpaPerubahan() throws Exception {
        try {
            ubahDalamTransaksi("B002", -3, "KELUAR");
            fail("harus ditolak");
        } catch (SQLException e) {
            assertEquals("Stok Gula 1kg tidak cukup", e.getMessage());
        }
        assertEquals(2, TestDb.queryInt("SELECT stok FROM barang WHERE kode_barang='B002'"));
        assertEquals(0, TestDb.queryInt("SELECT COUNT(*) FROM stock_movement_log"));
    }

    @Test
    public void deltaNolTidakMencatatLog() throws Exception {
        ubahDalamTransaksi("B001", 0, "KOREKSI");
        assertEquals(0, TestDb.queryInt("SELECT COUNT(*) FROM stock_movement_log"));
    }

    @Test
    public void barangTidakAdaDitolak() throws Exception {
        try {
            ubahDalamTransaksi("X999", 1, "MASUK");
            fail("harus ditolak");
        } catch (SQLException e) {
            assertEquals("Barang X999 tidak ditemukan", e.getMessage());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void restockQtyNolDitolak() throws Exception {
        dao.restock("B001", 0, "x");
    }

    @Test
    public void restockMenambahStokDanMencatatMasuk() throws Exception {
        dao.restock("B001", 5, "Kiriman supplier");
        assertEquals(15, TestDb.queryInt("SELECT stok FROM barang WHERE kode_barang='B001'"));
        assertEquals("MASUK", TestDb.queryString("SELECT tipe_gerakan FROM stock_movement_log"));
        assertEquals("Kiriman supplier", TestDb.queryString("SELECT keterangan FROM stock_movement_log"));
        assertNull(TestDb.queryString("SELECT kode_transaksi FROM stock_movement_log"));
    }

    @Test
    public void restockKeteranganKosongJadiRestock() throws Exception {
        dao.restock("B001", 1, "   ");
        assertEquals("Restock", TestDb.queryString("SELECT keterangan FROM stock_movement_log"));
    }

    @Test
    public void riwayatTerbaruDulu() throws Exception {
        dao.restock("B001", 1, "pertama");
        dao.restock("B001", 2, "kedua");
        dao.restock("B002", 9, "barang lain");

        List<StockMovement> r = dao.riwayat("B001");
        assertEquals(2, r.size());
        assertEquals("kedua", r.get(0).getKeterangan());
        assertEquals(2, r.get(0).getQty());
        assertEquals(11, r.get(0).getStokSebelum());
        assertEquals(13, r.get(0).getStokSesudah());
        assertEquals("MASUK", r.get(0).getTipe());
        assertNotNull(r.get(0).getWaktu());
    }
}
