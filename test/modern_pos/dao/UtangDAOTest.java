package modern_pos.dao;

import java.sql.SQLException;
import java.time.LocalDate;
import modern_pos.TestDb;
import modern_pos.model.Utang;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class UtangDAOTest {
    private final UtangDAO dao = new UtangDAO();

    @Before
    public void setUp() throws Exception {
        TestDb.reset();
    }

    private static Utang utang(String kode, String kodeBarang, int harga, int dp) {
        return utang(kode, kodeBarang, harga, dp, 1);
    }

    private static int stok(String kode) throws Exception {
        return TestDb.queryInt("SELECT stok FROM barang WHERE kode_barang=?", kode);
    }

    @Test
    public void tambahUtangMengurangiStokSebanyakQty() throws Exception {
        dao.tambahUtang(utang("UTG-1", "B001", 130000, 30000, 2));

        assertEquals(8, stok("B001"));
        assertEquals(2, TestDb.queryInt("SELECT qty FROM utang WHERE kode_utang='UTG-1'"));
        String kodeTrx = TestDb.queryString("SELECT kode_transaksi FROM utang WHERE kode_utang='UTG-1'");
        assertEquals(1, TestDb.queryInt("SELECT COUNT(*) FROM stock_movement_log WHERE tipe_gerakan='KELUAR' AND qty=2 AND kode_transaksi=?", kodeTrx));
        assertEquals(2, dao.getUtangList("").get(0).getQty());
    }

    @Test
    public void tambahUtangStokKurangDitolakTanpaSisaData() throws Exception {
        try {
            dao.tambahUtang(utang("UTG-1", "B002", 45000, 0, 3));
            fail("harus ditolak");
        } catch (SQLException e) {
            assertEquals("Stok Gula 1kg tidak cukup", e.getMessage());
        }
        assertEquals(2, stok("B002"));
        assertEquals(0, TestDb.queryInt("SELECT COUNT(*) FROM utang"));
        assertEquals(0, TestDb.queryInt("SELECT COUNT(*) FROM transaksi"));
        assertEquals(0, TestDb.queryInt("SELECT COUNT(*) FROM log_transaksi"));
    }

    @Test
    public void duaKasirBersamaanTidakDeadlock() throws Exception {
        // 8 utang untuk barang yang sama, dilepas bersamaan; stok 10 cukup untuk semuanya.
        final int n = 8;
        final java.util.concurrent.CyclicBarrier start = new java.util.concurrent.CyclicBarrier(n);
        final java.util.List<Throwable> gagal = java.util.Collections.synchronizedList(new java.util.ArrayList<Throwable>());
        Thread[] ts = new Thread[n];
        for (int i = 0; i < n; i++) {
            final String kode = "UTG-" + i;
            ts[i] = new Thread(() -> {
                try {
                    start.await();
                    dao.tambahUtang(utang(kode, "B001", 65000, 0, 1));
                } catch (Throwable t) {
                    gagal.add(t);
                }
            });
            ts[i].start();
        }
        for (Thread t : ts) t.join();
        assertTrue("gagal: " + gagal, gagal.isEmpty());
        assertEquals(2, stok("B001"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void qtyNolDitolak() throws Exception {
        dao.tambahUtang(utang("UTG-1", "B001", 65000, 0, 0));
    }

    @Test
    public void editQtyNaikTurunMenggerakkanSelisih() throws Exception {
        dao.tambahUtang(utang("UTG-1", "B001", 130000, 0, 2));   // 10 -> 8
        dao.updateUtang(utang("UTG-1", "B001", 325000, 0, 5));   // +3 -> 5
        assertEquals(5, stok("B001"));
        dao.updateUtang(utang("UTG-1", "B001", 65000, 0, 1));    // -4 -> 9
        assertEquals(9, stok("B001"));
        assertEquals(1, TestDb.queryInt("SELECT qty FROM utang WHERE kode_utang='UTG-1'"));
    }

    @Test
    public void editQtyMelebihiStokDitolakTanpaPerubahan() throws Exception {
        dao.tambahUtang(utang("UTG-1", "B002", 15000, 0, 1));    // 2 -> 1
        try {
            dao.updateUtang(utang("UTG-1", "B002", 45000, 0, 3)); // butuh +2, sisa 1
            fail("harus ditolak");
        } catch (SQLException e) {
            assertEquals("Stok Gula 1kg tidak cukup", e.getMessage());
        }
        assertEquals(1, stok("B002"));
        assertEquals(1, TestDb.queryInt("SELECT qty FROM utang WHERE kode_utang='UTG-1'"));
        assertEquals(15000, TestDb.queryInt("SELECT harga_brng FROM utang WHERE kode_utang='UTG-1'"));
    }

    @Test
    public void editGantiBarangMemindahkanStok() throws Exception {
        dao.tambahUtang(utang("UTG-1", "B001", 130000, 0, 2));   // B001 10 -> 8
        dao.updateUtang(utang("UTG-1", "B002", 15000, 0, 1));    // B001 -> 10, B002 2 -> 1
        assertEquals(10, stok("B001"));
        assertEquals(1, stok("B002"));
    }

    @Test(expected = SQLException.class)
    public void editUtangTidakAdaDitolak() throws Exception {
        dao.updateUtang(utang("TIDAK-ADA", "B001", 65000, 0, 1));
    }

    @Test
    public void hapusUtangBelumLunasMengembalikanStok() throws Exception {
        dao.tambahUtang(utang("UTG-1", "B001", 130000, 0, 2));
        dao.hapusUtang("UTG-1");
        assertEquals(10, stok("B001"));
        assertEquals(1, TestDb.queryInt("SELECT COUNT(*) FROM stock_movement_log WHERE tipe_gerakan='MASUK' AND qty=2"));
    }

    @Test
    public void hapusUtangLunasTidakMengembalikanStok() throws Exception {
        dao.tambahUtang(utang("UTG-1", "B001", 130000, 0, 2));
        dao.tandaiLunas("UTG-1");
        dao.hapusUtang("UTG-1");
        assertEquals(8, stok("B001"));
    }

    private static Utang utang(String kode, String kodeBarang, int harga, int dp, int qty) {
        Utang u = new Utang();
        u.setKodeUtang(kode);
        u.setNama("Siti");
        u.setAlamat("-");
        u.setTelepon("0812");
        u.setHargaBarang(harga);
        u.setDp(dp);
        u.setJumlahCicilan(3);
        u.setJatuhTempo(LocalDate.of(2026, 12, 1));
        u.setKodeBarang(kodeBarang);
        u.setQty(qty);
        return u;
    }

    @Test
    public void tambahUtangMenyimpanBarangYangDipilihDanKodeTransaksi() throws Exception {
        dao.tambahUtang(utang("UTG-1", "B002", 15000, 5000));

        assertEquals("B002", TestDb.queryString("SELECT kode_barang FROM utang WHERE kode_utang='UTG-1'"));
        String kodeTrx = TestDb.queryString("SELECT kode_transaksi FROM utang WHERE kode_utang='UTG-1'");
        assertNotNull(kodeTrx);
        assertEquals("KREDIT", TestDb.queryString("SELECT jenis_transaksi FROM transaksi WHERE kode_transaksi=?", kodeTrx));
        assertEquals(1, TestDb.queryInt("SELECT COUNT(*) FROM log_transaksi WHERE kode_transaksi=?", kodeTrx));
    }

    @Test
    public void getUtangListMengisiKodeBarang() throws Exception {
        dao.tambahUtang(utang("UTG-1", "B002", 15000, 5000));
        assertEquals("B002", dao.getUtangList("").get(0).getKodeBarang());
    }

    @Test
    public void editUtangBisaGantiBarang() throws Exception {
        dao.tambahUtang(utang("UTG-1", "B001", 65000, 5000));
        dao.updateUtang(utang("UTG-1", "B002", 15000, 5000));
        assertEquals("B002", TestDb.queryString("SELECT kode_barang FROM utang WHERE kode_utang='UTG-1'"));
    }

    @Test
    public void lunasTanpaSisaTidakMembuatTransaksiNol() throws Exception {
        dao.tambahUtang(utang("UTG-1", "B002", 15000, 15000));
        dao.tandaiLunas("UTG-1");
        assertEquals("lunas", TestDb.queryString("SELECT status FROM utang WHERE kode_utang='UTG-1'"));
        assertEquals(1, TestDb.queryInt("SELECT COUNT(*) FROM transaksi"));
    }

    @Test
    public void lunasDuaKaliTidakMencatatPemasukanDobel() throws Exception {
        dao.tambahUtang(utang("UTG-1", "B002", 15000, 5000));
        dao.tandaiLunas("UTG-1");
        dao.tandaiLunas("UTG-1"); // klik ganda / daftar di layar sudah basi
        assertEquals(1, TestDb.queryInt("SELECT COUNT(*) FROM transaksi WHERE jenis_transaksi='TUNAI'"));
        assertEquals(1, TestDb.queryInt("SELECT COUNT(*) FROM log_transaksi WHERE tipe_transaksi='TUNAI'"));
    }

    @Test
    public void lunasDenganSisaMencatatPemasukanTunai() throws Exception {
        dao.tambahUtang(utang("UTG-1", "B002", 15000, 5000));
        dao.tandaiLunas("UTG-1");
        assertEquals(10000, TestDb.queryInt("SELECT total FROM transaksi WHERE jenis_transaksi='TUNAI'"));
    }
}
