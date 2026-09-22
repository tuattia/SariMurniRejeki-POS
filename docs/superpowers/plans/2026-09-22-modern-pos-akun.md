# modern_pos Akun (Sub-proyek 2c) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Admin mengelola akun dari `modern_pos` tanpa risiko terkunci; `member` hanya melihat tombol operasional; username unik.

**Architecture:** `Akses.admin()` membaca `Session.currentUser`. `UserDAO` mendapat CRUD akun dengan validasi di Java dan aturan "minimal satu admin" di transaksi DB (`FOR UPDATE`). `UserDialog` + `UserController` dibuka dari Dashboard. Tombol non-member disembunyikan via `setVisible(Akses.admin())`.

**Tech Stack:** Java 1.8 source (JDK 17), Swing, JDBC MySQL 8.4, JUnit 4.13.2. Test: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`.

**Spec:** `docs/superpowers/specs/2026-09-22-modern-pos-akun-design.md`

## Global Constraints

- Source level Java 1.8.
- Role hanya `admin` / `member`, disimpan huruf kecil. Password minimal 6 karakter (tambah/reset). Hash tetap `login.enkripsi.sha256`.
- Pesan error persis: `"Username <u> sudah dipakai"`, `"User tidak ditemukan"`, `"Tidak bisa menghapus akun sendiri"`, `"Minimal harus ada satu admin"`, `"Hak akses harus admin atau member"`, `"Password minimal 6 karakter"`, `"<Label> wajib diisi"`.
- DB utama tidak disentuh sampai Task 5 (izin eksplisit user).
- Stack lama tidak diubah.
- Commit: `git -c user.name="attia" -c user.email="attiaitem@gmail.com" commit -m "<pesan>" -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"`.
- `TestDb.reset()` mengosongkan tabel `user`.

## Review Focus

1. Dua admin tersisa saling menurunkan role hampir bersamaan: harus tetap ada ≥1 admin → dijaga `FOR UPDATE` (Task 2), test sekuensial `adminTerakhirTidakBisaDiturunkan`.
2. Username dengan spasi di depan/belakang atau huruf besar dianggap sama saat cek dobel? (MySQL collation `ai_ci` → case-insensitive) → test `usernameDobelBedaHurufDitolak` di Task 2.
3. Admin mereset password dirinya sendiri: tetap bisa login dengan password baru → test `resetPasswordMenggantiPassword` (Task 2) memakai user yang sama.
4. Member yang dibuka sebagai `Session.currentUser == null` (layar dibuka langsung via `main`) tidak melihat tombol admin → `Akses.admin()` false (Task 1).
5. Role lama tersimpan `"Admin"` / `" admin "` di DB tetap dihitung admin oleh `Akses` dan aturan "minimal satu admin" → test `adminDenganHurufBesarTetapDihitung` di Task 2.

---

### Task 1: `Akses` + migrasi username unik (DB test)

**Files:**
- Create: `src/modern_pos/utils/Akses.java`, `test/modern_pos/utils/AksesTest.java`, `migrations/2026-09-22-user-username-unique.sql`
- Modify: `schema.sql` (dump ulang)

**Interfaces:**
- Produces: `Akses.ADMIN = "admin"`, `Akses.MEMBER = "member"`, `static boolean Akses.admin()`, `static boolean Akses.isAdmin(String hakAkses)`.

- [ ] **Step 1: Migrasi DB test + schema**

`migrations/2026-09-22-user-username-unique.sql`:
```sql
-- Sub-proyek 2c: username unik (juga menolak duplikat dari register stack lama).
ALTER TABLE user ADD UNIQUE KEY uq_user_username (username);
```
```powershell
$m = "C:\laragon\bin\mysql\mysql-8.4.3-winx64\bin"
& "$m\mysql.exe" -uroot sarimurnirejeki_test -e "source migrations/2026-09-22-user-username-unique.sql"
& "$m\mysqldump.exe" -uroot --no-data --skip-comments --result-file=schema.sql sarimurnirejeki_test
Select-String -Path schema.sql -Pattern 'uq_user_username'
```
Expected: 1 baris cocok.

- [ ] **Step 2: Test gagal `test/modern_pos/utils/AksesTest.java`**

```java
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
```

- [ ] **Step 3: Jalankan → gagal** (`cannot find symbol ... Akses`).

- [ ] **Step 4: `src/modern_pos/utils/Akses.java`**

```java
package modern_pos.utils;

public final class Akses {
    private Akses() {}

    public static final String ADMIN = "admin";
    public static final String MEMBER = "member";

    public static boolean isAdmin(String hakAkses) {
        return hakAkses != null && ADMIN.equals(hakAkses.trim().toLowerCase());
    }

    // Pembatasan di UI: aplikasi desktop terhubung langsung ke DB.
    public static boolean admin() {
        return Session.currentUser != null && isAdmin(Session.currentUser.getHakAkses());
    }
}
```

- [ ] **Step 5: Jalankan → `OK (71 tests)`.**

- [ ] **Step 6: Commit**
```powershell
git add migrations schema.sql src/modern_pos/utils/Akses.java test/modern_pos/utils/AksesTest.java
git -c user.name="attia" -c user.email="attiaitem@gmail.com" commit -m "feat(modern_pos): role check helper; unique username migration" -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 2: `UserDAO` — kelola akun

**Files:**
- Modify: `src/modern_pos/dao/UserDAO.java`
- Create: `test/modern_pos/dao/UserDAOTest.java`

**Interfaces:**
- Consumes: `Akses.ADMIN/MEMBER/isAdmin`, `Tx.rollbackQuietly`, `enkripsi.sha256`.
- Produces: `List<User> listUser()`, `void tambahUser(String nama, String username, String password, String hakAkses)`, `void ubahUser(int id, String nama, String hakAkses)`, `void resetPassword(int id, String passwordBaru)`, `void hapusUser(int id, int idPelaku)` — semua `throws SQLException`; validasi input → `IllegalArgumentException`.

- [ ] **Step 1: Test gagal `test/modern_pos/dao/UserDAOTest.java`**

```java
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
        dao.ubahUser(id("diva"), "Diva Kasir", "member");
        User u = dao.authenticate("diva", "rahasia1");
        assertEquals("Diva Kasir", u.getNama());
        assertEquals("member", u.getHakAkses());
    }

    @Test
    public void adminTerakhirTidakBisaDiturunkan() throws Exception {
        dao.tambahUser("Admin", "admin", "rahasia1", "admin");
        harusSQL("Minimal harus ada satu admin", r(() -> dao.ubahUser(id("admin"), "Admin", "member")));
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
        harusSQL("User tidak ditemukan", r(() -> dao.ubahUser(99999, "X", "member")));
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
```

- [ ] **Step 2: Jalankan → gagal** (compile: `cannot find symbol tambahUser/listUser/...`).

- [ ] **Step 3: Tambah ke `UserDAO.java`**

Import tambahan:
```java
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import modern_pos.utils.Akses;
```

Method:
```java
    public List<User> listUser() throws SQLException {
        List<User> list = new ArrayList<>();
        try (Connection con = koneksi.open();
             PreparedStatement ps = con.prepareStatement("SELECT id_user, nama, username, hakakses FROM user ORDER BY nama");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id_user"));
                u.setNama(rs.getString("nama"));
                u.setUsername(rs.getString("username"));
                u.setHakAkses(rs.getString("hakakses"));
                list.add(u);
            }
        }
        return list;
    }

    public void tambahUser(String nama, String username, String password, String hakAkses) throws SQLException {
        String n = wajib(nama, "Nama"), u = wajib(username, "Username"), r = role(hakAkses);
        cekPassword(password);
        try (Connection con = koneksi.open()) {
            con.setAutoCommit(false);
            try {
                // Cek eksplisit (pesan jelas) + UNIQUE KEY di DB sebagai jaring pengaman.
                try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM user WHERE username = ? FOR UPDATE")) {
                    ps.setString(1, u);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        if (rs.getInt(1) > 0) throw new SQLException("Username " + u + " sudah dipakai");
                    }
                }
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO user (nama, username, password, hakakses) VALUES (?, ?, ?, ?)")) {
                    ps.setString(1, n);
                    ps.setString(2, u);
                    ps.setString(3, enkripsi.sha256(password));
                    ps.setString(4, r);
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLIntegrityConstraintViolationException e) {
                throw Tx.rollbackQuietly(con, new SQLException("Username " + u + " sudah dipakai", e));
            } catch (SQLException e) {
                throw Tx.rollbackQuietly(con, e);
            }
        }
    }

    public void ubahUser(int id, String nama, String hakAkses) throws SQLException {
        String n = wajib(nama, "Nama"), r = role(hakAkses);
        try (Connection con = koneksi.open()) {
            con.setAutoCommit(false);
            try {
                boolean adminSekarang = kunciDanCekAdmin(con, id);
                if (adminSekarang && !Akses.isAdmin(r) && jumlahAdmin(con) <= 1) {
                    throw new SQLException("Minimal harus ada satu admin");
                }
                try (PreparedStatement ps = con.prepareStatement("UPDATE user SET nama = ?, hakakses = ? WHERE id_user = ?")) {
                    ps.setString(1, n);
                    ps.setString(2, r);
                    ps.setInt(3, id);
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) {
                throw Tx.rollbackQuietly(con, e);
            }
        }
    }

    public void resetPassword(int id, String passwordBaru) throws SQLException {
        cekPassword(passwordBaru);
        try (Connection con = koneksi.open();
             PreparedStatement ps = con.prepareStatement("UPDATE user SET password = ? WHERE id_user = ?")) {
            ps.setString(1, enkripsi.sha256(passwordBaru));
            ps.setInt(2, id);
            if (ps.executeUpdate() == 0) throw new SQLException("User tidak ditemukan");
        }
    }

    public void hapusUser(int id, int idPelaku) throws SQLException {
        if (id == idPelaku) throw new SQLException("Tidak bisa menghapus akun sendiri");
        try (Connection con = koneksi.open()) {
            con.setAutoCommit(false);
            try {
                boolean admin = kunciDanCekAdmin(con, id);
                if (admin && jumlahAdmin(con) <= 1) throw new SQLException("Minimal harus ada satu admin");
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM user WHERE id_user = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) {
                throw Tx.rollbackQuietly(con, e);
            }
        }
    }

    // Kunci semua baris user (tabel kecil) supaya dua admin yang saling menurunkan
    // bersamaan tidak sama-sama lolos cek "minimal satu admin".
    private static boolean kunciDanCekAdmin(Connection con, int id) throws SQLException {
        Boolean admin = null;
        try (PreparedStatement ps = con.prepareStatement("SELECT id_user, hakakses FROM user FOR UPDATE");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                if (rs.getInt("id_user") == id) admin = Akses.isAdmin(rs.getString("hakakses"));
            }
        }
        if (admin == null) throw new SQLException("User tidak ditemukan");
        return admin;
    }

    private static int jumlahAdmin(Connection con) throws SQLException {
        int n = 0;
        try (PreparedStatement ps = con.prepareStatement("SELECT hakakses FROM user");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) if (Akses.isAdmin(rs.getString("hakakses"))) n++;
        }
        return n;
    }

    private static String wajib(String nilai, String label) {
        if (nilai == null || nilai.trim().isEmpty()) throw new IllegalArgumentException(label + " wajib diisi");
        return nilai.trim();
    }

    private static String role(String hakAkses) {
        String r = hakAkses == null ? "" : hakAkses.trim().toLowerCase();
        if (!r.equals(Akses.ADMIN) && !r.equals(Akses.MEMBER)) throw new IllegalArgumentException("Hak akses harus admin atau member");
        return r;
    }

    private static void cekPassword(String password) {
        if (password == null || password.length() < 6) throw new IllegalArgumentException("Password minimal 6 karakter");
    }
```

- [ ] **Step 4: Jalankan → `OK (88 tests)`.**

- [ ] **Step 5: Commit**
```powershell
git add src/modern_pos/dao/UserDAO.java test/modern_pos/dao/UserDAOTest.java
git -c user.name="attia" -c user.email="attiaitem@gmail.com" commit -m "feat(modern_pos): user management with last-admin and self-delete guards" -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 3: `UserDialog` + `UserController`, dibuka dari Dashboard

**Files:**
- Create: `src/modern_pos/controller/UserController.java`, `src/modern_pos/view/UserDialog.java`
- Modify: `src/modern_pos/view/DashboardView.java` (sidebar)

Tidak ada test otomatis (UI).

- [ ] **Step 1: `UserController`**

```java
package modern_pos.controller;

import java.util.List;
import javax.swing.SwingWorker;
import modern_pos.dao.UserDAO;
import modern_pos.model.User;
import modern_pos.view.UserDialog;

public class UserController {
    private final UserDAO dao = new UserDAO();
    private UserDialog view;

    public void setView(UserDialog view) {
        this.view = view;
        muat();
    }

    private static String pesan(Exception ex) {
        Throwable c = ex.getCause() != null ? ex.getCause() : ex;
        return c.getMessage() != null ? c.getMessage() : c.toString();
    }

    public void muat() {
        new SwingWorker<List<User>, Void>() {
            @Override protected List<User> doInBackground() throws Exception { return dao.listUser(); }
            @Override protected void done() {
                try { view.tampilkan(get()); }
                catch (Exception ex) { view.showError("Gagal memuat user: " + pesan(ex)); }
            }
        }.execute();
    }

    private interface Aksi { void jalan() throws Exception; }

    private void jalankan(final Aksi aksi, final String sukses) {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception { aksi.jalan(); return null; }
            @Override protected void done() {
                try { get(); view.showSuccess(sukses); }
                catch (Exception ex) { view.showError(pesan(ex)); }
                muat();
            }
        }.execute();
    }

    public void tambah(String nama, String username, String password, String role) {
        jalankan(() -> dao.tambahUser(nama, username, password, role), "User berhasil ditambahkan!");
    }

    public void ubah(int id, String nama, String role) {
        jalankan(() -> dao.ubahUser(id, nama, role), "User berhasil diubah!");
    }

    public void resetPassword(int id, String password) {
        jalankan(() -> dao.resetPassword(id, password), "Password berhasil direset!");
    }

    public void hapus(int id, int idPelaku) {
        jalankan(() -> dao.hapusUser(id, idPelaku), "User berhasil dihapus!");
    }
}
```

- [ ] **Step 2: `UserDialog`**

```java
package modern_pos.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import modern_pos.controller.UserController;
import modern_pos.model.User;
import modern_pos.utils.Akses;
import modern_pos.utils.Session;
import modern_pos.utils.SwingHelper;

public class UserDialog extends JDialog {
    private final UserController controller;
    private final DefaultTableModel model;
    private final JTable tabel;
    private List<User> daftar;

    public UserDialog(Frame parent, UserController controller) {
        super(parent, "Kelola User", true);
        this.controller = controller;

        model = new DefaultTableModel(new String[]{"Nama", "Username", "Hak Akses"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tabel = new JTable(model);
        SwingHelper.styleTable(tabel);

        JButton btnTambah = new JButton("Tambah");
        JButton btnEdit = new JButton("Edit");
        JButton btnReset = new JButton("Reset Password");
        JButton btnHapus = new JButton("Hapus");
        JButton btnTutup = new JButton("Tutup");
        btnTambah.addActionListener(e -> formTambah());
        btnEdit.addActionListener(e -> { User u = dipilih(); if (u != null) formEdit(u); });
        btnReset.addActionListener(e -> { User u = dipilih(); if (u != null) formReset(u); });
        btnHapus.addActionListener(e -> {
            User u = dipilih();
            if (u == null) return;
            int ok = JOptionPane.showConfirmDialog(this, "Hapus user " + u.getNama() + "?", "Hapus", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) controller.hapus(u.getId(), Session.currentUser != null ? Session.currentUser.getId() : -1);
        });
        btnTutup.addActionListener(e -> dispose());

        JPanel tombol = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        tombol.add(btnTambah);
        tombol.add(btnEdit);
        tombol.add(btnReset);
        tombol.add(btnHapus);
        tombol.add(btnTutup);

        setLayout(new BorderLayout());
        add(new JScrollPane(tabel), BorderLayout.CENTER);
        add(tombol, BorderLayout.SOUTH);
        setSize(640, 420);
        setLocationRelativeTo(parent);
        controller.setView(this);
    }

    private User dipilih() {
        int row = tabel.getSelectedRow();
        if (row == -1) { showError("Pilih user terlebih dahulu!"); return null; }
        return daftar.get(row);
    }

    private static JComboBox<String> comboRole(String pilih) {
        JComboBox<String> c = new JComboBox<>(new String[]{Akses.MEMBER, Akses.ADMIN});
        if (pilih != null) c.setSelectedItem(pilih.trim().toLowerCase());
        return c;
    }

    private void formTambah() {
        JTextField nama = new JTextField();
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        JComboBox<String> role = comboRole(null);
        JPanel p = new JPanel(new GridLayout(4, 2, 8, 8));
        p.add(new JLabel("Nama:")); p.add(nama);
        p.add(new JLabel("Username:")); p.add(username);
        p.add(new JLabel("Password (min 6):")); p.add(password);
        p.add(new JLabel("Hak Akses:")); p.add(role);
        if (JOptionPane.showConfirmDialog(this, p, "Tambah User", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            controller.tambah(nama.getText(), username.getText(), new String(password.getPassword()), (String) role.getSelectedItem());
        }
    }

    private void formEdit(User u) {
        JTextField nama = new JTextField(u.getNama());
        JComboBox<String> role = comboRole(u.getHakAkses());
        JPanel p = new JPanel(new GridLayout(3, 2, 8, 8));
        p.add(new JLabel("Username:")); p.add(new JLabel(u.getUsername()));
        p.add(new JLabel("Nama:")); p.add(nama);
        p.add(new JLabel("Hak Akses:")); p.add(role);
        if (JOptionPane.showConfirmDialog(this, p, "Edit User", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            controller.ubah(u.getId(), nama.getText(), (String) role.getSelectedItem());
        }
    }

    private void formReset(User u) {
        JPasswordField baru = new JPasswordField();
        JPasswordField ulang = new JPasswordField();
        JPanel p = new JPanel(new GridLayout(2, 2, 8, 8));
        p.add(new JLabel("Password baru (min 6):")); p.add(baru);
        p.add(new JLabel("Ulangi password:")); p.add(ulang);
        if (JOptionPane.showConfirmDialog(this, p, "Reset Password " + u.getUsername(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        String a = new String(baru.getPassword()), b = new String(ulang.getPassword());
        if (!a.equals(b)) { showError("Password dan ulangan tidak sama!"); return; }
        controller.resetPassword(u.getId(), a);
    }

    public void tampilkan(List<User> list) {
        this.daftar = list;
        model.setRowCount(0);
        for (User u : list) model.addRow(new Object[]{u.getNama(), u.getUsername(), u.getHakAkses()});
    }

    public void showError(String msg) { JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE); }
    public void showSuccess(String msg) { JOptionPane.showMessageDialog(this, msg, "Sukses", JOptionPane.INFORMATION_MESSAGE); }
}
```

- [ ] **Step 3: `DashboardView` — tombol sidebar "Kelola User"**

Setelah baris `JButton btnLogout = SwingHelper.createSidebarButton("Logout");` tambah:
```java
        JButton btnUser = SwingHelper.createSidebarButton("Kelola User");
        btnUser.addActionListener(e -> new UserDialog(this, new modern_pos.controller.UserController()).setVisible(true));
        btnUser.setVisible(modern_pos.utils.Akses.admin());
```
Setelah `sidebar.add(btnLog);` tambah `sidebar.add(btnUser);`.

- [ ] **Step 4: Compile + suite → `OK (88 tests)`.**

- [ ] **Step 5: Commit**
```powershell
git add src/modern_pos
git -c user.name="attia" -c user.email="attiaitem@gmail.com" commit -m "feat(modern_pos): Kelola User dialog for admins" -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 4: Sembunyikan tombol admin dari member

**Files:**
- Modify: `src/modern_pos/view/StockView.java`, `src/modern_pos/view/UtangView.java`, `src/modern_pos/view/LogTransaksiView.java`

Tidak ada test otomatis (UI); `Akses.admin()` sudah dites.

- [ ] **Step 1: `StockView`** — setelah blok `actionPanel.add(...)` tambah:
```java
        // Member: operasional saja (restock, riwayat).
        boolean admin = modern_pos.utils.Akses.admin();
        btnTambah.setVisible(admin);
        btnEdit.setVisible(admin);
        btnHapus.setVisible(admin);
        btnSnapshot.setVisible(admin);
```

- [ ] **Step 2: `UtangView`** — setelah blok `actionPanel.add(...)` tambah:
```java
        // Member: tambah, lunas, kartu saja.
        boolean admin = modern_pos.utils.Akses.admin();
        btnEdit.setVisible(admin);
        btnHapus.setVisible(admin);
```

- [ ] **Step 3: `LogTransaksiView`** — setelah `actionPanel.add(btnHapus);` tambah:
```java
        btnHapus.setVisible(modern_pos.utils.Akses.admin()); // member hanya boleh cetak
```

- [ ] **Step 4: Compile + suite + verifikasi**
```powershell
powershell -ExecutionPolicy Bypass -File run-tests.ps1
Select-String -Path src\modern_pos\view\*.java -Pattern 'Akses\.admin\(\)' | Measure-Object | % Count
```
Expected: `OK (88 tests)`; count 4 (Dashboard, Stock, Utang, Log).

- [ ] **Step 5: Commit**
```powershell
git add src/modern_pos/view
git -c user.name="attia" -c user.email="attiaitem@gmail.com" commit -m "feat(modern_pos): hide admin-only buttons from member role" -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 5: Verifikasi akhir + migrasi DB utama (izin user)

- [ ] **Step 1:** `powershell -ExecutionPolicy Bypass -File run-tests.ps1` → `OK (88 tests)`.
- [ ] **Step 2: STOP — minta izin user** untuk `migrations/2026-09-22-user-username-unique.sql` ke `sarimurnirejeki`. Setelah izin: cek duplikat dulu (`SELECT username, COUNT(*) FROM user GROUP BY username HAVING COUNT(*) > 1` harus kosong), backup `mysqldump` ke `%TEMP%\sarimurnirejeki-before-user-unique.sql`, jalankan migrasi, verifikasi `SHOW INDEX FROM user` memuat `uq_user_username`.
- [ ] **Step 3: Cek manual (user):** login admin → Dashboard ada "Kelola User" → tambah member → logout → login member → Stock tanpa Tambah/Edit/Hapus/Snapshot, Utang tanpa Edit/Hapus, Log tanpa Hapus, Dashboard tanpa Kelola User.
