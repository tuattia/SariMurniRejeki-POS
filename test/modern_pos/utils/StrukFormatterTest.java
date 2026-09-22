package modern_pos.utils;

import java.time.LocalDateTime;
import java.util.List;
import modern_pos.model.Struk;
import org.junit.Test;
import static org.junit.Assert.*;

public class StrukFormatterTest {

    private static Struk struk(String pelanggan) {
        Struk s = new Struk();
        s.setKodeTransaksi("TRX26092214050000001");
        s.setWaktu(LocalDateTime.of(2026, 9, 22, 14, 5));
        s.setPelanggan(pelanggan);
        s.setJenis("TUNAI");
        s.getItems().add(new Struk.Item("Beras 5kg", 2, 65000, 130000));
        s.getItems().add(new Struk.Item("Minyak Goreng Kemasan Premium Super 2 Liter", 1, 30000, 30000));
        s.setTotal(160000);
        s.setBayar(200000);
        s.setKembali(40000);
        return s;
    }

    private static boolean ada(List<String> baris, String awal, String akhir) {
        for (String b : baris) if (b.startsWith(awal) && b.endsWith(akhir)) return true;
        return false;
    }

    @Test
    public void semuaBarisMuat32Kolom() {
        for (String b : StrukFormatter.format(struk("Budi"))) {
            assertTrue("terlalu panjang (" + b.length() + "): " + b, b.length() <= 32);
        }
    }

    @Test
    public void memuatHeaderItemDanPembayaran() {
        List<String> b = StrukFormatter.format(struk("Budi"));
        assertTrue(b.contains("Kode: TRX26092214050000001"));
        assertTrue(b.contains("Waktu: 22-09-2026 14:05"));
        assertTrue(b.contains("Pelanggan: Budi"));
        assertTrue(b.contains("Beras 5kg"));
        assertTrue(ada(b, "  2 x 65.000", "130.000"));
        assertTrue(ada(b, "Total", "Rp 160.000"));
        assertTrue(ada(b, "Bayar", "Rp 200.000"));
        assertTrue(ada(b, "Kembali", "Rp 40.000"));
        assertTrue(ada(b, "", "Terima Kasih"));
    }

    @Test
    public void namaBarangPanjangDipotong() {
        List<String> b = StrukFormatter.format(struk("Budi"));
        assertTrue(b.contains("Minyak Goreng Kemasan Premium Su"));
    }

    @Test
    public void pelangganKosongTidakDitampilkan() {
        for (String b : StrukFormatter.format(struk("  "))) assertFalse(b.startsWith("Pelanggan"));
        for (String b : StrukFormatter.format(struk(null))) assertFalse(b.startsWith("Pelanggan"));
    }

    @Test
    public void strukTanpaItemMenampilkanKeterangan() {
        Struk s = struk("Siti");
        s.getItems().clear();
        s.setKeterangan("Pelunasan Sisa Utang");
        assertTrue(StrukFormatter.format(s).contains("Pelunasan Sisa Utang"));
    }

    @Test
    public void nominalBesarTetapMuat() {
        Struk s = struk("Budi");
        s.getItems().clear();
        s.getItems().add(new Struk.Item("Semen", 100, 99999999, 999999900));
        s.setTotal(999999900);
        s.setBayar(999999999);
        s.setKembali(99);
        for (String b : StrukFormatter.format(s)) assertTrue(b, b.length() <= 32);
        assertTrue(ada(StrukFormatter.format(s), "Total", "Rp 999.999.900"));
    }

    @Test
    public void rupiahMemakaiTitikRibuan() {
        assertEquals("Rp 1.234.567", StrukFormatter.rupiah(1234567));
        assertEquals("Rp 0", StrukFormatter.rupiah(0));
    }
}
