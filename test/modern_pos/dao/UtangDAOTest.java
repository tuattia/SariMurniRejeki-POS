package modern_pos.dao;

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
