package modern_pos.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import modern_pos.model.KartuAngsuran;
import modern_pos.model.Struk;
import modern_pos.model.Utang;
import org.junit.Test;
import static org.junit.Assert.*;

// Laptop kasir umum 1366x768: dialog harus muat supaya tombol Cetak terjangkau.
public class DialogCetakTest {
    private static final int TINGGI_MAKS = 700;

    @Test
    public void strukBanyakItemTetapMuatLayar() {
        Struk s = new Struk();
        s.setKodeTransaksi("TRX26092214050000001");
        s.setWaktu(LocalDateTime.of(2026, 9, 22, 14, 5));
        for (int i = 0; i < 25; i++) s.getItems().add(new Struk.Item("Barang " + i, 1, 1000, 1000));
        s.setTotal(25000);
        s.setBayar(25000);

        StrukDialog d = new StrukDialog(null, s);
        try {
            assertTrue("tinggi " + d.getHeight(), d.getHeight() <= TINGGI_MAKS);
        } finally {
            d.dispose();
        }
    }

    @Test
    public void kartuAngsuranMuatLayar() {
        Utang u = new Utang();
        u.setKodeUtang("UTG-1");
        u.setNama("Siti");
        u.setHargaBarang(1500000);
        u.setDp(500000);
        u.setJumlahCicilan(5);
        u.setJatuhTempo(LocalDate.of(2026, 12, 1));

        KartuAngsuranDialog d = new KartuAngsuranDialog(null, new KartuAngsuran(u, "Kulkas"));
        try {
            assertTrue("tinggi " + d.getHeight(), d.getHeight() <= TINGGI_MAKS);
        } finally {
            d.dispose();
        }
    }
}
