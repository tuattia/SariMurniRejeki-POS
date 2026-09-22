package modern_pos.utils;

import org.junit.Test;
import static org.junit.Assert.*;

public class HashTest {
    @Test
    public void sha256HexSamaDenganFormatLama() {
        // Nilai yang tersimpan di tabel user untuk password "admin".
        assertEquals("8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918", Hash.sha256("admin"));
    }
}
