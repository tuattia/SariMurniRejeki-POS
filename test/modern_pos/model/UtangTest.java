package modern_pos.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class UtangTest {
    private static Utang utang(int harga, int dp, int cicilan) {
        Utang u = new Utang();
        u.setHargaBarang(harga);
        u.setDp(dp);
        u.setJumlahCicilan(cicilan);
        return u;
    }

    @Test
    public void bayarPerBulanPasTanpaSisa() {
        assertEquals(200000, utang(1500000, 500000, 5).bayarPerBulan());
    }

    @Test
    public void bayarPerBulanDibulatkanKeAtas() {
        // 1.999.000 / 12 = 166.583,33 -> 166.584 supaya total angsuran tidak kurang dari sisa utang
        assertEquals(166584, utang(2000000, 1000, 12).bayarPerBulan());
    }

    @Test
    public void cicilanNolAtauTanpaSisaHasilNol() {
        assertEquals(0, utang(1000000, 0, 0).bayarPerBulan());
        assertEquals(0, utang(1000000, 1000000, 5).bayarPerBulan());
        assertEquals(0, utang(1000000, 1200000, 5).bayarPerBulan());
    }
}
