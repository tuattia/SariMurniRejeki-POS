package modern_pos.utils;

import org.junit.Test;
import static org.junit.Assert.*;

public class AngkaTest {
    @Test
    public void bilanganBulatPositifDiterima() {
        assertEquals(5, Angka.parseQty("5"));
        assertEquals(12, Angka.parseQty("  12 "));
    }

    private static void harusDitolak(String input) {
        try {
            Angka.parseQty(input);
            fail("harus ditolak: " + input);
        } catch (IllegalArgumentException e) {
            assertEquals("Qty harus bilangan bulat lebih dari 0", e.getMessage());
        }
    }

    @Test
    public void inputSalahDitolakBukanDitebak() {
        harusDitolak("-5");
        harusDitolak("1.5");
        harusDitolak("1,5");
        harusDitolak("0");
        harusDitolak("");
        harusDitolak("abc");
        harusDitolak("99999999999");
        harusDitolak(null);
    }
}
