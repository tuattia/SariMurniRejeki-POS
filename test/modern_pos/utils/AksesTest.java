package modern_pos.utils;

import modern_pos.model.User;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class AksesTest {
    @After
    public void bersihkan() {
        Session.currentUser = null;
    }

    private static User user(String hak) {
        User u = new User();
        u.setHakAkses(hak);
        return u;
    }

    @Test
    public void belumLoginBukanAdmin() {
        Session.currentUser = null;
        assertFalse(Akses.admin());
    }

    @Test
    public void memberBukanAdmin() {
        Session.currentUser = user("member");
        assertFalse(Akses.admin());
    }

    @Test
    public void adminTanpaPedulikanHurufDanSpasi() {
        Session.currentUser = user(" Admin ");
        assertTrue(Akses.admin());
        assertFalse(Akses.isAdmin(null));
    }
}
