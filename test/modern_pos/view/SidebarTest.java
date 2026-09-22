package modern_pos.view;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import modern_pos.TestDb;
import modern_pos.controller.DashboardController;
import modern_pos.controller.LogTransaksiController;
import modern_pos.controller.StockController;
import modern_pos.controller.TransaksiController;
import modern_pos.controller.UtangController;
import modern_pos.model.User;
import modern_pos.utils.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class SidebarTest {
    @Before
    public void setUp() throws Exception {
        TestDb.reset();
        User admin = new User();
        admin.setNama("Admin");
        admin.setHakAkses("admin");
        Session.currentUser = admin;
    }

    @After
    public void bersihkan() {
        Session.currentUser = null;
    }

    private static void kumpulkan(Container c, List<JButton> out) {
        for (Component k : c.getComponents()) {
            if (k instanceof JButton) out.add((JButton) k);
            if (k instanceof Container) kumpulkan((Container) k, out);
        }
    }

    private static List<String> tombolTerlihat(JFrame f) {
        List<JButton> semua = new ArrayList<>();
        kumpulkan(f.getContentPane(), semua);
        List<String> teks = new ArrayList<>();
        for (JButton b : semua) if (b.isVisible()) teks.add(b.getText());
        return teks;
    }

    private static void cekSidebar(JFrame f) {
        try {
            List<String> t = tombolTerlihat(f);
            for (String menu : new String[]{"Dashboard", "Transaksi", "Stock", "Utang / Piutang", "Log Transaksi", "Logout"}) {
                assertTrue(f.getClass().getSimpleName() + " tanpa menu " + menu, t.contains(menu));
            }
        } finally {
            f.dispose();
        }
    }

    @Test
    public void semuaLayarPunyaSidebarLengkap() {
        cekSidebar(new StockView(new StockController()));
        cekSidebar(new UtangView(new UtangController()));
        cekSidebar(new LogTransaksiView(new LogTransaksiController()));
        cekSidebar(new TransaksiView(new TransaksiController()));
        cekSidebar(new DashboardView(new DashboardController(Session.currentUser)));
    }

    @Test
    public void kelolaUserHanyaUntukAdmin() {
        DashboardView admin = new DashboardView(new DashboardController(Session.currentUser));
        assertTrue(tombolTerlihat(admin).contains("Kelola User"));
        admin.dispose();

        Session.currentUser.setHakAkses("member");
        DashboardView member = new DashboardView(new DashboardController(Session.currentUser));
        assertFalse(tombolTerlihat(member).contains("Kelola User"));
        member.dispose();
    }
}
