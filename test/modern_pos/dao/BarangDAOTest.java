package modern_pos.dao;

import static modern_pos.TestDb.barang;

import java.util.List;
import modern_pos.TestDb;
import modern_pos.model.Barang;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class BarangDAOTest {
    private final BarangDAO dao = new BarangDAO();

    @Before
    public void setUp() throws Exception {
        TestDb.reset();
    }

    @Test
    public void tidakMembocorkanKoneksi() throws Exception {
        int before = TestDb.queryInt("SELECT VARIABLE_VALUE FROM performance_schema.global_status WHERE VARIABLE_NAME='Threads_connected'");
        for (int i = 0; i < 50; i++) dao.getAllBarang("");
        int after = TestDb.queryInt("SELECT VARIABLE_VALUE FROM performance_schema.global_status WHERE VARIABLE_NAME='Threads_connected'");
        assertTrue("koneksi bocor: " + before + " -> " + after, after - before <= 2);
    }

    @Test
    public void cariBerdasarkanNamaBarang() throws Exception {
        List<Barang> hasil = dao.getAllBarang("Gula");
        assertEquals(1, hasil.size());
        assertEquals("B002", hasil.get(0).getKodeBarang());
        assertEquals(2, hasil.get(0).getStok());
        assertEquals(15000, hasil.get(0).getHarga());
    }


    @Test
    public void tambahBarangStokAwalMencatatMasuk() throws Exception {
        dao.tambahBarang(barang("B003", "Minyak 1L", 18000, 5));
        assertEquals(5, TestDb.queryInt("SELECT stok FROM barang WHERE kode_barang='B003'"));
        assertEquals("MASUK", TestDb.queryString("SELECT tipe_gerakan FROM stock_movement_log WHERE kode_barang='B003'"));
        assertEquals(0, TestDb.queryInt("SELECT stok_sebelum FROM stock_movement_log WHERE kode_barang='B003'"));
        assertEquals("Stok awal", TestDb.queryString("SELECT keterangan FROM stock_movement_log WHERE kode_barang='B003'"));
    }

    @Test
    public void tambahBarangStokNolTanpaLog() throws Exception {
        dao.tambahBarang(barang("B003", "Minyak 1L", 18000, 0));
        assertEquals(0, TestDb.queryInt("SELECT COUNT(*) FROM stock_movement_log"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void stokAwalNegatifDitolak() throws Exception {
        dao.tambahBarang(barang("B003", "Minyak 1L", 18000, -1));
    }

    @Test
    public void editStokMencatatKoreksi() throws Exception {
        dao.updateBarang(barang("B001", "Beras 5kg", 65000, 7), 10);
        assertEquals(7, TestDb.queryInt("SELECT stok FROM barang WHERE kode_barang='B001'"));
        assertEquals("KOREKSI", TestDb.queryString("SELECT tipe_gerakan FROM stock_movement_log"));
        assertEquals(3, TestDb.queryInt("SELECT qty FROM stock_movement_log"));
        assertEquals(10, TestDb.queryInt("SELECT stok_sebelum FROM stock_movement_log"));
        assertEquals(7, TestDb.queryInt("SELECT stok_sesudah FROM stock_movement_log"));
    }

    @Test
    public void editTanpaUbahStokTanpaLog() throws Exception {
        dao.updateBarang(barang("B001", "Beras Premium 5kg", 70000, 10), 10);
        assertEquals("Beras Premium 5kg", TestDb.queryString("SELECT nama_barang FROM barang WHERE kode_barang='B001'"));
        assertEquals(70000, TestDb.queryInt("SELECT harga_jual FROM barang WHERE kode_barang='B001'"));
        assertEquals(0, TestDb.queryInt("SELECT COUNT(*) FROM stock_movement_log"));
    }

    @Test
    public void editMemakaiSelisihBukanAngkaMutlak() throws Exception {
        // Form dibuka saat stok 10; sebelum disimpan, kasir lain menjual 2 (stok DB jadi 8).
        try (java.sql.Connection c = config.koneksi.open(); java.sql.Statement s = c.createStatement()) {
            s.executeUpdate("UPDATE barang SET stok = 8 WHERE kode_barang='B001'");
        }
        // User mengetik 12 (niatnya +2 dari yang ia lihat).
        dao.updateBarang(barang("B001", "Beras 5kg", 65000, 12), 10);
        assertEquals(10, TestDb.queryInt("SELECT stok FROM barang WHERE kode_barang='B001'"));
    }

    @Test
    public void keywordDenganSpasiDiTrim() throws Exception {
        assertEquals(1, dao.getAllBarang(" Gula ").size());
    }

    @Test
    public void keywordKosongAtauSpasiMengembalikanSemua() throws Exception {
        assertEquals(2, dao.getAllBarang("").size());
        assertEquals(2, dao.getAllBarang("   ").size());
        assertEquals(2, dao.getAllBarang(null).size());
    }
}
