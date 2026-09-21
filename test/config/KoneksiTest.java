package config;

import java.sql.Connection;
import java.sql.SQLException;
import modern_pos.TestDb;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class KoneksiTest {
    @After
    public void restoreUrl() {
        System.setProperty("db.url", TestDb.URL);
    }

    @Test
    public void openMemakaiUrlDariSystemProperty() throws SQLException {
        System.setProperty("db.url", TestDb.URL);
        try (Connection c = koneksi.open()) {
            assertEquals("sarimurnirejeki_test", c.getCatalog());
        }
    }

    @Test(expected = SQLException.class)
    public void openMelemparExceptionBilaGagal() throws SQLException {
        System.setProperty("db.url", "jdbc:mysql://localhost:3306/db_yang_tidak_ada_xyz");
        koneksi.open().close();
    }

    @Test
    public void getConnectionLamaTetapReturnNullBilaGagal() {
        System.setProperty("db.url", "jdbc:mysql://localhost:3306/db_yang_tidak_ada_xyz");
        assertNull(koneksi.getConnection());
    }
}
