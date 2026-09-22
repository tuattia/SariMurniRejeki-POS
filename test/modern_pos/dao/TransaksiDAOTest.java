package modern_pos.dao;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import modern_pos.TestDb;
import modern_pos.model.Barang;
import modern_pos.model.CartItem;
import org.junit.Test;
import static org.junit.Assert.*;

public class TransaksiDAOTest {

    private static Barang barang(String kode, String nama, int harga, int stokDiLayar) {
        Barang b = new Barang();
        b.setKodeBarang(kode);
        b.setNamaBarang(nama);
        b.setHarga(harga);
        b.setStok(stokDiLayar);
        return b;
    }

    @Test
    public void checkoutSuksesMengurangiStokDanMencatatSemuaTabel() throws Exception {
        TestDb.reset();
        CartItem item = new CartItem(barang("B001", "Beras 5kg", 65000, 10), 3);
        new TransaksiDAO().simpanTransaksi(Arrays.asList(item), 195000, 200000, 5000, "Budi");

        assertEquals(7, TestDb.queryInt("SELECT stok FROM barang WHERE kode_barang='B001'"));
        assertEquals(1, TestDb.queryInt("SELECT COUNT(*) FROM transaksi"));
        assertEquals(1, TestDb.queryInt("SELECT COUNT(*) FROM transaksi_detail"));
        assertEquals(1, TestDb.queryInt("SELECT COUNT(*) FROM log_transaksi"));
    }

    @Test
    public void checkoutStokKurangDitolakTanpaSisaData() throws Exception {
        TestDb.reset();
        // Layar masih menampilkan stok lama (5), padahal di DB tinggal 2.
        CartItem beras = new CartItem(barang("B001", "Beras 5kg", 65000, 10), 1);
        CartItem gula = new CartItem(barang("B002", "Gula 1kg", 15000, 5), 3);
        try {
            new TransaksiDAO().simpanTransaksi(Arrays.asList(beras, gula), 110000, 110000, 0, "Budi");
            fail("harus menolak stok kurang");
        } catch (SQLException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Stok Gula 1kg tidak cukup"));
        }
        assertEquals(2, TestDb.queryInt("SELECT stok FROM barang WHERE kode_barang='B002'"));
        assertEquals(10, TestDb.queryInt("SELECT stok FROM barang WHERE kode_barang='B001'"));
        assertEquals(0, TestDb.queryInt("SELECT COUNT(*) FROM transaksi"));
        assertEquals(0, TestDb.queryInt("SELECT COUNT(*) FROM transaksi_detail"));
        assertEquals(0, TestDb.queryInt("SELECT COUNT(*) FROM log_transaksi"));
        assertEquals(0, TestDb.queryInt("SELECT COUNT(*) FROM stock_movement_log"));
    }

    @Test
    public void simpanTransaksiMengembalikanKodeDanGetStrukLengkap() throws Exception {
        TestDb.reset();
        CartItem beras = new CartItem(barang("B001", "Beras 5kg", 65000, 10), 2);
        CartItem gula = new CartItem(barang("B002", "Gula 1kg", 15000, 2), 1);

        String kode = new TransaksiDAO().simpanTransaksi(Arrays.asList(beras, gula), 145000, 150000, 5000, "Budi");

        assertEquals(kode, TestDb.queryString("SELECT kode_transaksi FROM transaksi"));
        modern_pos.model.Struk s = new TransaksiDAO().getStruk(kode);
        assertEquals(kode, s.getKodeTransaksi());
        assertEquals("Budi", s.getPelanggan());
        assertEquals("TUNAI", s.getJenis());
        assertEquals(2, s.getItems().size());
        assertEquals("Beras 5kg", s.getItems().get(0).getNama());
        assertEquals(2, s.getItems().get(0).getQty());
        assertEquals(130000, s.getItems().get(0).getSubtotal());
        assertEquals(145000, s.getTotal());
        assertEquals(150000, s.getBayar());
        assertEquals(5000, s.getKembali());
        assertNotNull(s.getWaktu());
    }

    @Test
    public void getStrukTanpaLogBayarSamaDenganTotal() throws Exception {
        TestDb.reset();
        CartItem beras = new CartItem(barang("B001", "Beras 5kg", 65000, 10), 1);
        String kode = new TransaksiDAO().simpanTransaksi(Arrays.asList(beras), 65000, 100000, 35000, "Budi");
        new LogTransaksiDAO().hapusLog(kode);

        modern_pos.model.Struk s = new TransaksiDAO().getStruk(kode);
        assertEquals(65000, s.getBayar());
        assertEquals(0, s.getKembali());
        assertEquals(1, s.getItems().size());
    }

    @Test(expected = SQLException.class)
    public void getStrukKodeTidakAdaDitolak() throws Exception {
        TestDb.reset();
        new TransaksiDAO().getStruk("TRX-TIDAK-ADA");
    }

    @Test
    public void checkoutMencatatGerakanKeluarDenganKodeTransaksi() throws Exception {
        TestDb.reset();
        CartItem item = new CartItem(barang("B001", "Beras 5kg", 65000, 10), 3);
        new TransaksiDAO().simpanTransaksi(Arrays.asList(item), 195000, 200000, 5000, "Budi");

        String kodeTrx = TestDb.queryString("SELECT kode_transaksi FROM transaksi");
        assertEquals(1, TestDb.queryInt("SELECT COUNT(*) FROM stock_movement_log WHERE tipe_gerakan='KELUAR' AND kode_transaksi=?", kodeTrx));
        assertEquals(3, TestDb.queryInt("SELECT qty FROM stock_movement_log"));
        assertEquals("Penjualan", TestDb.queryString("SELECT keterangan FROM stock_movement_log"));
    }

    @Test
    public void kodeTransaksiUnikDanMuatVarchar20() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String kode = TransaksiDAO.newKodeTransaksi();
            assertEquals(kode, 20, kode.length());
            assertTrue(kode, kode.startsWith("TRX"));
            assertTrue("duplikat: " + kode, seen.add(kode));
        }
    }
}
