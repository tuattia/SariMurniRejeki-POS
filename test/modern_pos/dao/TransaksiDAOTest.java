package modern_pos.dao;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class TransaksiDAOTest {

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
