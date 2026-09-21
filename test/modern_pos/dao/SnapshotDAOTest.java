package modern_pos.dao;

import java.time.YearMonth;
import java.util.List;
import modern_pos.TestDb;
import modern_pos.model.StockSnapshot;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class SnapshotDAOTest {
    private final SnapshotDAO dao = new SnapshotDAO();
    private final YearMonth sept = YearMonth.of(2026, 9);

    @Before
    public void setUp() throws Exception {
        TestDb.reset();
    }

    @Test
    public void ambilSnapshotMenyalinSemuaBarang() throws Exception {
        assertFalse(dao.sudahAdaSnapshot(sept));
        dao.ambilSnapshotBulan(sept);
        assertTrue(dao.sudahAdaSnapshot(sept));

        List<StockSnapshot> list = dao.getSnapshotByBulan(sept);
        assertEquals(2, list.size());
        assertEquals("B001", list.get(0).getKodeBarang());
        assertEquals(10, list.get(0).getStokSnapshot());
        assertEquals(65000, list.get(0).getHargaJual());
        assertEquals(sept.atDay(1), list.get(0).getPeriode());
    }

    @Test
    public void ambilUlangMengupdateTanpaDuplikat() throws Exception {
        dao.ambilSnapshotBulan(sept);
        try (java.sql.Connection c = config.koneksi.open(); java.sql.Statement s = c.createStatement()) {
            s.executeUpdate("UPDATE barang SET stok = 4 WHERE kode_barang='B001'");
        }
        dao.ambilSnapshotBulan(sept);

        assertEquals(2, TestDb.queryInt("SELECT COUNT(*) FROM stock_snapshot"));
        assertEquals(4, dao.getSnapshotByBulan(sept).get(0).getStokSnapshot());
    }

    @Test
    public void satuanDiambilDariBarang() throws Exception {
        try (java.sql.Connection c = config.koneksi.open(); java.sql.Statement s = c.createStatement()) {
            s.executeUpdate("UPDATE barang SET satuan = 'karung' WHERE kode_barang='B001'");
        }
        dao.ambilSnapshotBulan(sept);
        List<StockSnapshot> list = dao.getSnapshotByBulan(sept);
        assertEquals("karung", list.get(0).getSatuan());
        assertEquals("pcs", list.get(1).getSatuan());
    }

    @Test
    public void bulanLainTidakTercampur() throws Exception {
        dao.ambilSnapshotBulan(sept);
        assertTrue(dao.getSnapshotByBulan(YearMonth.of(2026, 8)).isEmpty());
    }
}
