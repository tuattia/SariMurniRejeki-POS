package modern_pos.dao;

import java.util.List;
import modern_pos.TestDb;
import modern_pos.model.Barang;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class BarangDAOTest {
    private final BarangDAO dao = new BarangDAO();

    @Before
    public void setUp() throws Exception {
        TestDb.reset();
    }

    @Test
    public void tidakMembocorkanKoneksi() throws Exception {
        int before = TestDb.queryInt("SELECT VARIABLE_VALUE FROM performance_schema.global_status WHERE VARIABLE_NAME='Threads_connected'");
        for (int i = 0; i < 50; i++) dao.getAllBarang("");
        int after = TestDb.queryInt("SELECT VARIABLE_VALUE FROM performance_schema.global_status WHERE VARIABLE_NAME='Threads_connected'");
        assertTrue("koneksi bocor: " + before + " -> " + after, after - before <= 2);
    }

    @Test
    public void cariBerdasarkanNamaBarang() throws Exception {
        List<Barang> hasil = dao.getAllBarang("Gula");
        assertEquals(1, hasil.size());
        assertEquals("B002", hasil.get(0).getKodeBarang());
        assertEquals(2, hasil.get(0).getStok());
        assertEquals(15000, hasil.get(0).getHarga());
    }

    @Test
    public void keywordKosongAtauSpasiMengembalikanSemua() throws Exception {
        assertEquals(2, dao.getAllBarang("").size());
        assertEquals(2, dao.getAllBarang("   ").size());
        assertEquals(2, dao.getAllBarang(null).size());
    }
}
