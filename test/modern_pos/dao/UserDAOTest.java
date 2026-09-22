package modern_pos.dao;

import java.sql.SQLException;
import java.util.List;
import modern_pos.TestDb;
import modern_pos.model.User;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class UserDAOTest {
    private final UserDAO dao = new UserDAO();

    @Before
    public void setUp() throws Exception {
        TestDb.reset();
    }

    private static int id(String username) throws Exception {
        return TestDb.queryInt("SELECT id_user FROM user WHERE username=?", username);
    }

    private static void harusSQL(String pesan, Runnable r) {
        try {
            r.run();
            fail("harus ditolak: " + pesan);
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof SQLException);
            assertEquals(pesan, e.getCause().getMessage());
        }
    }

    private interface Aksi { void jalan() throws Exception; }

    private static Runnable r(Aksi a) {
        return () -> {
            try { a.jalan(); } catch (RuntimeException e) { throw e; } catch (Exception e) { throw new RuntimeException(e); }
        };
    }

    @Test
    public void tambahUserBisaLogin() throws Exception {
        dao.tambahUser("Budi", "budi", "rahasia1", "member");
        User u = dao.authenticate("budi", "rahasia1");
        assertEquals("Budi", u.getNama());
        assertEquals("member", u.getHakAkses());
    }

    @Test
    public void usernameDisimpanTrimDanRoleHurufKecil() throws Exception {
        dao.tambahUser("Budi", " budi ", "rahasia1", "Member");
        User u = dao.listUser().get(0);
        assertEquals("budi", u.getUsername());
        assertEquals("member", u.getHakAkses());
    }

    @Test
    public void usernameDobelDitolak() throws Exception {
        dao.tambahUser("Budi", "budi", "rahasia1", "member");
        harusSQL("Username budi sudah dipakai", r(() -> dao.tambahUser("Budi 2", "budi", "rahasia2", "member")));
    }

    @Test
    public void usernameDobelBedaHurufDitolak() throws Exception {
        dao.tambahUser("Budi", "budi", "rahasia1", "member");
        harusSQL("Username BUDI sudah dipakai", r(() -> dao.tambahUser("Budi 2", "BUDI", "rahasia2", "member")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void roleTidakValidDitolak() throws Exception {
        dao.tambahUser("Budi", "budi", "rahasia1", "owner");
    }

    @Test(expected = IllegalArgumentException.class)
    public void passwordPendekDitolak() throws Exception {
        dao.tambahUser("Budi", "budi", "12345", "member");
    }

    @Test(expected = IllegalArgumentException.class)
    public void namaKosongDitolak() throws Exception {
        dao.tambahUser("  ", "budi", "rahasia1", "member");
    }

    @Test
    public void resetPasswordMenggantiPassword() throws Exception {
        dao.tambahUser("Admin", "admin", "lamaaa1", "admin");
        dao.resetPassword(id("admin"), "baruuu1");
        assertEquals("Admin", dao.authenticate("admin", "baruuu1").getNama());
        harusSQL("Username atau Password salah!", r(() -> dao.authenticate("admin", "lamaaa1")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void resetPasswordPendekDitolak() throws Exception {
        dao.tambahUser("Admin", "admin", "lamaaa1", "admin");
        dao.resetPassword(id("admin"), "123");
    }

    @Test
    public void ubahNamaDanRole() throws Exception {
        dao.tambahUser("Admin", "admin", "rahasia1", "admin");
        dao.tambahUser("Diva", "diva", "rahasia1", "admin");
        dao.ubahUser(id("diva"), "Diva Kasir", "member", id("admin"));
        User u = dao.authenticate("diva", "rahasia1");
        assertEquals("Diva Kasir", u.getNama());
        assertEquals("member", u.getHakAkses());
    }

    @Test
    public void ubahRoleDiriSendiriDitolakTapiNamaBoleh() throws Exception {
        dao.tambahUser("Admin", "admin", "rahasia1", "admin");
        dao.tambahUser("Diva", "diva", "rahasia1", "admin");
        harusSQL("Tidak bisa mengubah hak akses akun sendiri", r(() -> dao.ubahUser(id("diva"), "Diva", "member", id("diva"))));
        dao.ubahUser(id("diva"), "Diva Baru", "admin", id("diva"));
        assertEquals("Diva Baru", dao.authenticate("diva", "rahasia1").getNama());
        assertEquals("admin", dao.authenticate("diva", "rahasia1").getHakAkses());
    }

    @Test
    public void adminTerakhirTidakBisaDiturunkan() throws Exception {
        dao.tambahUser("Admin", "admin", "rahasia1", "admin");
        harusSQL("Minimal harus ada satu admin", r(() -> dao.ubahUser(id("admin"), "Admin", "member", -1)));
        assertEquals("admin", dao.authenticate("admin", "rahasia1").getHakAkses());
    }

    @Test
    public void adminDenganHurufBesarTetapDihitung() throws Exception {
        dao.tambahUser("Admin", "admin", "rahasia1", "admin");
        try (java.sql.Connection c = config.koneksi.open(); java.sql.Statement s = c.createStatement()) {
            s.executeUpdate("UPDATE user SET hakakses = ' Admin ' WHERE username = 'admin'");
        }
        dao.tambahUser("Kasir", "kasir", "rahasia1", "member");
        harusSQL("Minimal harus ada satu admin", r(() -> dao.hapusUser(id("admin"), id("kasir"))));
    }

    @Test
    public void adminTerakhirTidakBisaDihapus() throws Exception {
        dao.tambahUser("Admin", "admin", "rahasia1", "admin");
        dao.tambahUser("Kasir", "kasir", "rahasia1", "member");
        harusSQL("Minimal harus ada satu admin", r(() -> dao.hapusUser(id("admin"), id("kasir"))));
    }

    @Test
    public void hapusDiriSendiriDitolak() throws Exception {
        dao.tambahUser("Admin", "admin", "rahasia1", "admin");
        dao.tambahUser("Diva", "diva", "rahasia1", "admin");
        harusSQL("Tidak bisa menghapus akun sendiri", r(() -> dao.hapusUser(id("diva"), id("diva"))));
    }

    @Test
    public void hapusMemberBerhasil() throws Exception {
        dao.tambahUser("Admin", "admin", "rahasia1", "admin");
        dao.tambahUser("Kasir", "kasir", "rahasia1", "member");
        dao.hapusUser(id("kasir"), id("admin"));
        assertEquals(1, dao.listUser().size());
    }

    @Test
    public void userTidakAdaDitolak() throws Exception {
        dao.tambahUser("Admin", "admin", "rahasia1", "admin");
        harusSQL("User tidak ditemukan", r(() -> dao.ubahUser(99999, "X", "member", -1)));
        harusSQL("User tidak ditemukan", r(() -> dao.resetPassword(99999, "rahasia1")));
    }

    @Test
    public void listUserUrutNama() throws Exception {
        dao.tambahUser("Zaki", "zaki", "rahasia1", "member");
        dao.tambahUser("Ayu", "ayu", "rahasia1", "admin");
        List<User> list = dao.listUser();
        assertEquals("Ayu", list.get(0).getNama());
        assertEquals("Zaki", list.get(1).getNama());
    }
}
