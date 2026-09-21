package modern_pos.dao;

import config.koneksi;
import java.sql.Connection;
import java.sql.SQLException;
import modern_pos.TestDb;
import org.junit.Test;
import static org.junit.Assert.*;

public class TxTest {
    @Test
    public void rollbackGagalTidakMenutupiErrorAsli() throws Exception {
        TestDb.reset();
        Connection con = koneksi.open();
        con.close(); // rollback pada koneksi tertutup pasti gagal
        SQLException asli = new SQLException("Stok Gula 1kg tidak cukup");

        SQLException hasil = Tx.rollbackQuietly(con, asli);

        assertSame(asli, hasil);
        assertEquals(1, hasil.getSuppressed().length);
    }
}
