package modern_pos.dao;

import static modern_pos.TestDb.utang;

import java.util.Arrays;
import modern_pos.TestDb;
import modern_pos.model.Barang;
import modern_pos.model.CartItem;
import modern_pos.model.DashboardSummary;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class DashboardDAOTest {

    @Before
    public void setUp() throws Exception {
        TestDb.reset();
    }


    private static void jualTunai(int total) throws Exception {
        Barang b = new Barang();
        b.setKodeBarang("B001");
        b.setNamaBarang("Beras 5kg");
        b.setHarga(total);
        b.setStok(10);
        new TransaksiDAO().simpanTransaksi(Arrays.asList(new CartItem(b, 1)), total, total, 0, "Budi");
    }

    @Test
    public void pendapatanAdalahUangMasukTanpaDobel() throws Exception {
        jualTunai(100000);                                            // tunai 100.000
        new UtangDAO().tambahUtang(utang("UTG-A", "B001", 500000, 50000)); // DP 50.000 (bukan 500.000)
        new UtangDAO().tambahUtang(utang("UTG-B", "B002", 40000, 10000));  // DP 10.000
        new UtangDAO().tandaiLunas("UTG-B");                          // pelunasan 30.000

        DashboardSummary s = new DashboardDAO().getSummary();
        assertEquals(190000, s.getPendapatanHariIni());
        assertEquals(4, s.getTotalTransaksiHariIni());
    }

    @Test
    public void utangKemarinTidakDihitungHariIni() throws Exception {
        new UtangDAO().tambahUtang(utang("UTG-A", "B001", 500000, 50000));
        try (java.sql.Connection c = config.koneksi.open(); java.sql.Statement st = c.createStatement()) {
            st.executeUpdate("UPDATE transaksi SET tanggal = DATE_SUB(NOW(), INTERVAL 1 DAY)");
        }
        new UtangDAO().tandaiLunas("UTG-A");                          // pelunasan hari ini 450.000

        assertEquals(450000, new DashboardDAO().getSummary().getPendapatanHariIni());
    }
}
