# modern_pos Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build portabel + test harness JUnit, lalu perbaiki 6 bug di `modern_pos` (koneksi bocor, utang salah barang, user hilang, stok minus, fallback kolom palsu, kode transaksi bentrok).

**Architecture:** Aplikasi Swing + JDBC MySQL tanpa framework. DAO `modern_pos` membuka koneksi per method lewat `koneksi.open()` dan menutupnya dengan try-with-resources. Test JUnit 4 memukul DB terpisah `sarimurnirejeki_test` yang skemanya disalin dari DB utama.

**Tech Stack:** Java (source/target 1.8, dijalankan JDK 17), NetBeans Ant project, MySQL 8.4 (Laragon), mysql-connector-j 9.5.0, JUnit 4.13.2, Hamcrest 1.3.

**Spec:** `docs/superpowers/specs/2026-09-21-modern-pos-foundation-design.md`

## Global Constraints

- Source level Java 1.8: tidak boleh `var`, `List.of`, text block, `String.isBlank`, switch expression.
- DB utama `sarimurnirejeki` tidak boleh ditulis oleh test. `TestDb.reset()` menolak jalan bila URL tidak berakhiran `_test`.
- Nama kolom mengikuti `schema.sql` (hasil dump DB lokal). `barang`: `kode_barang, nama_barang, harga_jual, stok`.
- `kode_transaksi` maksimal 20 karakter (`varchar(20)`).
- Stack lama (`gui/`, `controller/`, `model/`, `login/`) tidak diubah perilakunya. `koneksi.getConnection()` tetap ada dan tetap `return null` bila gagal.
- Entry point tetap `gui.login`.
- Commit pakai `git -c user.name="attia" -c user.email="attiaitem@gmail.com" commit ...` dan pesan diakhiri baris `Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>`.
- Shell: PowerShell 5.1 di Windows. Working dir = root project.

## Review Focus

1. Keyword pencarian kosong atau cuma spasi harus menampilkan semua barang, bukan error/kosong → assertion di Task 2.
2. Checkout gagal di tengah (item kedua stok kurang) tidak boleh meninggalkan baris setengah jadi di `transaksi`/`transaksi_detail`/`log_transaksi` dan stok item pertama harus kembali → test di Task 4.
3. Edit utang lalu ganti barang harus tersimpan (`kode_barang` ikut ter-update) → test di Task 5.
4. Burst transaksi (> 100 dalam 1 ms) tetap menghasilkan kode unik ≤ 20 karakter → test di Task 3.
5. Pelunasan utang yang sisanya 0 (DP = harga) tidak membuat transaksi Rp 0 → test di Task 5.

---

## File Structure

| File | Aksi | Tanggung jawab |
|---|---|---|
| `lib/*.jar` | Create | Semua dependency, path relatif |
| `nbproject/project.properties` | Modify | Classpath ke `lib/`, JUnit di test classpath |
| `schema.sql` | Create | Skema DB (sumber kebenaran kolom) |
| `run-tests.ps1` | Create | Compile + jalankan test tanpa Ant |
| `src/config/koneksi.java` | Modify | `open()` baru, `getConnection()` delegasi |
| `test/modern_pos/TestDb.java` | Create | Arahkan ke DB test, reset + seed, helper query |
| `test/config/KoneksiTest.java` | Create | Test `open()` |
| `src/modern_pos/dao/*.java` | Modify | try-with-resources, kolom asli |
| `test/modern_pos/dao/BarangDAOTest.java` | Create | Leak + pencarian |
| `test/modern_pos/dao/TransaksiDAOTest.java` | Create | Kode transaksi + stok |
| `test/modern_pos/dao/UtangDAOTest.java` | Create | Utang barang/transaksi |
| `src/modern_pos/controller/TransaksiController.java` | Modify | Pakai `BarangDAO` |
| `src/modern_pos/controller/UtangController.java` | Modify | `daftarBarang()` |
| `src/modern_pos/model/Barang.java` | Modify | `toString()` untuk combo box |
| `src/modern_pos/model/Utang.java` | — | `kodeBarang` sudah ada, mulai dipakai |
| `src/modern_pos/view/UtangView.java` | Modify | Combo box barang |
| `src/modern_pos/utils/Session.java` | Create | User yang sedang login |
| `src/modern_pos/controller/LoginController.java`, `src/modern_pos/view/{LoginView,StockView,TransaksiView,UtangView,LogTransaksiView}.java` | Modify | Pakai `Session` |

---

### Task 1: Build portabel, DB test, dan `koneksi.open()`

**Files:**
- Create: `lib/` (5 jar), `schema.sql`, `run-tests.ps1`, `test/modern_pos/TestDb.java`, `test/config/KoneksiTest.java`
- Modify: `nbproject/project.properties:36-57`, `src/config/koneksi.java` (seluruh file)

**Interfaces:**
- Produces:
  - `config.koneksi.open() throws java.sql.SQLException` → `Connection` baru; baca `db.url`/`db.user`/`db.pass` dari system property saat dipanggil.
  - `config.koneksi.getConnection()` → `Connection` atau `null` (perilaku lama).
  - `modern_pos.TestDb.reset() throws SQLException` → set `db.url` ke DB test, kosongkan semua tabel, seed `B001 'Beras 5kg' 65000 stok 10` dan `B002 'Gula 1kg' 15000 stok 2`.
  - `modern_pos.TestDb.queryInt(String sql, Object... params) throws SQLException` → `int` kolom pertama baris pertama.
  - `modern_pos.TestDb.queryString(String sql, Object... params) throws SQLException` → `String` kolom pertama baris pertama (`null` bila tidak ada baris).
  - `./run-tests.ps1` → compile `src` + `test` ke `build/test-run`, jalankan semua `*Test` via JUnitCore, exit code 0 bila semua lulus.

- [ ] **Step 1: Salin jar ke `lib/`**

```powershell
New-Item -ItemType Directory -Force lib | Out-Null
Copy-Item dist\lib\AbsoluteLayout.jar, dist\lib\DateChooser.jar, dist\lib\mysql-connector-j-9.5.0.jar lib\
$u = "C:\Program Files\Unity\Hub\Editor\6000.3.10f1\Editor\Data\PlaybackEngines\AndroidPlayer\Tools\gradle\lib"
Copy-Item "$u\junit-4.13.2.jar", "$u\hamcrest-core-1.3.jar" lib\
Get-ChildItem lib | Select Name, Length
```

Expected: 5 file jar.

- [ ] **Step 2: Arahkan `nbproject/project.properties` ke `lib/`**

Ganti baris `file.reference.DateChooser.jar=...` dan `file.reference.mysql-connector-j-9.5.0.jar=...` (baris 36-37) dengan:

```properties
file.reference.AbsoluteLayout.jar=lib/AbsoluteLayout.jar
file.reference.DateChooser.jar=lib/DateChooser.jar
file.reference.hamcrest-core-1.3.jar=lib/hamcrest-core-1.3.jar
file.reference.junit-4.13.2.jar=lib/junit-4.13.2.jar
file.reference.mysql-connector-j-9.5.0.jar=lib/mysql-connector-j-9.5.0.jar
```

Ganti blok `javac.classpath` (baris 40-43) dengan:

```properties
javac.classpath=\
    ${file.reference.AbsoluteLayout.jar}:\
    ${file.reference.mysql-connector-j-9.5.0.jar}:\
    ${file.reference.DateChooser.jar}
```

Ganti blok `javac.test.classpath` (baris 55-57) dengan:

```properties
javac.test.classpath=\
    ${javac.classpath}:\
    ${build.classes.dir}:\
    ${file.reference.junit-4.13.2.jar}:\
    ${file.reference.hamcrest-core-1.3.jar}
```

Verifikasi tidak ada path Downloads tersisa:

```powershell
Select-String -Path nbproject\project.properties -Pattern 'Downloads|libs.absolutelayout'
```

Expected: tidak ada output.

- [ ] **Step 3: Dump skema dan buat DB test**

```powershell
$m = "C:\laragon\bin\mysql\mysql-8.4.3-winx64\bin"
& "$m\mysqldump.exe" -uroot --no-data --skip-comments --result-file=schema.sql sarimurnirejeki
& "$m\mysql.exe" -uroot -e "CREATE DATABASE IF NOT EXISTS sarimurnirejeki_test"
& "$m\mysql.exe" -uroot sarimurnirejeki_test -e "source schema.sql"
& "$m\mysql.exe" -uroot -N -e "SHOW TABLES FROM sarimurnirejeki_test"
```

Expected: 8 tabel (`barang, log_transaksi, stock_movement_log, stock_snapshot, transaksi, transaksi_detail, user, utang`).

- [ ] **Step 4: Tulis `run-tests.ps1`**

```powershell
# Compile src + test lalu jalankan semua test JUnit (DB: sarimurnirejeki_test).
# Dipakai karena Ant tidak ada di PATH; di NetBeans cukup "Test Project".
$ErrorActionPreference = 'Stop'
$out = 'build/test-run'
if (Test-Path $out) { Remove-Item -Recurse -Force $out }
New-Item -ItemType Directory -Force $out | Out-Null
$sources = Get-ChildItem -Recurse src, test -Filter *.java | ForEach-Object { $_.FullName }
& javac -encoding UTF-8 -nowarn -d $out -cp 'lib/*' $sources
if ($LASTEXITCODE -ne 0) { exit 1 }
$root = (Resolve-Path test).Path
$tests = Get-ChildItem -Recurse test -Filter *Test.java | ForEach-Object {
    $_.FullName.Substring($root.Length + 1).Replace('.java', '').Replace('\', '.')
}
& java -cp "$out;lib/*" org.junit.runner.JUnitCore $tests
exit $LASTEXITCODE
```

- [ ] **Step 5: Tulis `test/modern_pos/TestDb.java`**

```java
package modern_pos;

import config.koneksi;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// Helper test: semua test DAO memakai DB sarimurnirejeki_test, bukan DB utama.
public class TestDb {
    public static final String URL = "jdbc:mysql://localhost:3306/sarimurnirejeki_test";

    public static void reset() throws SQLException {
        System.setProperty("db.url", URL);
        if (!System.getProperty("db.url").endsWith("_test")) {
            throw new IllegalStateException("Menolak reset DB non-test: " + System.getProperty("db.url"));
        }
        try (Connection c = koneksi.open(); Statement s = c.createStatement()) {
            s.execute("SET FOREIGN_KEY_CHECKS=0");
            for (String t : new String[]{"log_transaksi", "transaksi_detail", "utang", "transaksi",
                    "barang", "user", "stock_movement_log", "stock_snapshot"}) {
                s.execute("TRUNCATE TABLE " + t);
            }
            s.execute("SET FOREIGN_KEY_CHECKS=1");
            s.execute("INSERT INTO barang (kode_barang, nama_barang, harga_jual, stok) VALUES "
                    + "('B001', 'Beras 5kg', 65000, 10), ('B002', 'Gula 1kg', 15000, 2)");
        }
    }

    public static int queryInt(String sql, Object... params) throws SQLException {
        String v = queryString(sql, params);
        return v == null ? 0 : Integer.parseInt(v);
    }

    public static String queryString(String sql, Object... params) throws SQLException {
        try (Connection c = koneksi.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }
}
```

- [ ] **Step 6: Tulis test gagal `test/config/KoneksiTest.java`**

```java
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
```

- [ ] **Step 7: Jalankan, pastikan gagal**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: compile error `cannot find symbol ... method open()`.

- [ ] **Step 8: Tulis ulang `src/config/koneksi.java`**

```java
package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author attia
 */
public class koneksi {
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/sarimurnirejeki";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASS = "";

    // Koneksi baru tiap panggilan; pemanggil wajib menutupnya (try-with-resources).
    // Bisa di-override lewat -Ddb.url / -Ddb.user / -Ddb.pass (dipakai test).
    public static Connection open() throws SQLException {
        return DriverManager.getConnection(
                System.getProperty("db.url", DEFAULT_URL),
                System.getProperty("db.user", DEFAULT_USER),
                System.getProperty("db.pass", DEFAULT_PASS));
    }

    // Dipakai stack lama (gui/, controller/): return null bila gagal. Hapus di sub-proyek 4.
    public static Connection getConnection() {
        try {
            return open();
        } catch (SQLException e) {
            System.out.println("Koneksi gagal: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
```

- [ ] **Step 9: Jalankan, pastikan lulus**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: `OK (3 tests)`. Bila compile stack lama gagal karena library, cek pesan `package ... does not exist` dan pastikan jar-nya ada di `lib/`.

- [ ] **Step 10: Commit**

```powershell
git add lib schema.sql run-tests.ps1 nbproject/project.properties src/config/koneksi.java test
git -c user.name="attia" -c user.email="attiaitem@gmail.com" commit -m "build: portable lib/, JUnit test harness, koneksi.open()" -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 2: DAO tanpa kebocoran koneksi dan tanpa fallback kolom (B1 + B5)

**Files:**
- Modify: `src/modern_pos/dao/BarangDAO.java`, `DashboardDAO.java`, `LogTransaksiDAO.java`, `UserDAO.java` (seluruh file), `src/modern_pos/dao/TransaksiDAO.java` (hapus `getBarangList`), `src/modern_pos/controller/TransaksiController.java:9-10,24-29`
- Test: `test/modern_pos/dao/BarangDAOTest.java`

**Interfaces:**
- Consumes: `koneksi.open()`, `TestDb.reset()`, `TestDb.queryInt(...)` (Task 1)
- Produces: `BarangDAO.getAllBarang(String keyword) throws SQLException` → `List<Barang>` urut `kode_barang`, cari di `kode_barang` dan `nama_barang`. `TransaksiDAO.getBarangList` dihapus.

- [ ] **Step 1: Tulis test gagal `test/modern_pos/dao/BarangDAOTest.java`**

```java
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
```

- [ ] **Step 2: Jalankan, pastikan gagal**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: `tidakMembocorkanKoneksi` FAIL (`koneksi bocor`), `cariBerdasarkanNamaBarang` FAIL (`Unknown column 'nama'`).

- [ ] **Step 3: Tulis ulang `src/modern_pos/dao/BarangDAO.java`**

```java
package modern_pos.dao;
import config.koneksi;
import modern_pos.model.Barang;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BarangDAO {
    public List<Barang> getAllBarang(String keyword) throws SQLException {
        List<Barang> list = new ArrayList<>();
        boolean isSearch = keyword != null && !keyword.trim().isEmpty();
        String sql = "SELECT kode_barang, nama_barang, harga_jual, stok FROM barang "
                + (isSearch ? "WHERE kode_barang LIKE ? OR nama_barang LIKE ? " : "")
                + "ORDER BY kode_barang ASC";
        try (Connection con = koneksi.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (isSearch) {
                ps.setString(1, "%" + keyword + "%");
                ps.setString(2, "%" + keyword + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Barang b = new Barang();
                    b.setKodeBarang(rs.getString("kode_barang"));
                    b.setNamaBarang(rs.getString("nama_barang"));
                    b.setHarga(rs.getInt("harga_jual"));
                    b.setStok(rs.getInt("stok"));
                    list.add(b);
                }
            }
        }
        return list;
    }

    public void tambahBarang(Barang b) throws SQLException {
        String sql = "INSERT INTO barang (kode_barang, nama_barang, harga_jual, stok) VALUES (?, ?, ?, ?)";
        try (Connection con = koneksi.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, b.getKodeBarang());
            ps.setString(2, b.getNamaBarang());
            ps.setInt(3, b.getHarga());
            ps.setInt(4, b.getStok());
            ps.executeUpdate();
        }
    }

    public void updateBarang(Barang b) throws SQLException {
        String sql = "UPDATE barang SET nama_barang=?, harga_jual=?, stok=? WHERE kode_barang=?";
        try (Connection con = koneksi.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, b.getNamaBarang());
            ps.setInt(2, b.getHarga());
            ps.setInt(3, b.getStok());
            ps.setString(4, b.getKodeBarang());
            ps.executeUpdate();
        }
    }

    public void hapusBarang(String kodeBarang) throws SQLException {
        String sql = "DELETE FROM barang WHERE kode_barang = ?";
        try (Connection con = koneksi.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, kodeBarang);
            ps.executeUpdate();
        }
    }
}
```

- [ ] **Step 4: Tulis ulang `src/modern_pos/dao/DashboardDAO.java`**

```java
package modern_pos.dao;
import config.koneksi;
import modern_pos.model.DashboardSummary;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DashboardDAO {
    public DashboardSummary getSummary() throws SQLException {
        DashboardSummary summary = new DashboardSummary();
        try (Connection con = koneksi.open()) {
            // 1. Pendapatan & Trx Hari Ini
            String sqlTrx = "SELECT SUM(total) as pendapatan, COUNT(kode_transaksi) as total_trx FROM transaksi WHERE DATE(tanggal) = CURDATE()";
            try (PreparedStatement ps = con.prepareStatement(sqlTrx); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    summary.setPendapatanHariIni(rs.getInt("pendapatan")); // jika null otomatis 0 di getInt
                    summary.setTotalTransaksiHariIni(rs.getInt("total_trx"));
                }
            }

            // 2. Utang Aktif (Status Belum Lunas)
            String sqlUtang = "SELECT COUNT(kode_utang) as total_utang FROM utang WHERE status = 'belum'";
            try (PreparedStatement ps = con.prepareStatement(sqlUtang); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) { summary.setJumlahUtangAktif(rs.getInt("total_utang")); }
            }

            // 3. Stok Menipis (Stok <= 5)
            String sqlStok = "SELECT COUNT(kode_barang) as stok_kritis FROM barang WHERE stok <= 5";
            try (PreparedStatement ps = con.prepareStatement(sqlStok); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) { summary.setStokMenipis(rs.getInt("stok_kritis")); }
            }
        }
        return summary;
    }
}
```

- [ ] **Step 5: Tulis ulang `src/modern_pos/dao/LogTransaksiDAO.java`**

```java
package modern_pos.dao;
import config.koneksi;
import modern_pos.model.LogTransaksi;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LogTransaksiDAO {

    public List<LogTransaksi> getLogList(String keyword) throws SQLException {
        List<LogTransaksi> list = new ArrayList<>();
        boolean isSearch = keyword != null && !keyword.trim().isEmpty();
        String sql = "SELECT * FROM log_transaksi "
                + (isSearch ? "WHERE kode_transaksi LIKE ? OR nama_pelanggan LIKE ? OR tanggal LIKE ? " : "")
                + "ORDER BY tanggal DESC LIMIT 500"; // Tampilkan 500 terbaru agar aplikasi tidak berat

        try (Connection con = koneksi.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (isSearch) {
                ps.setString(1, "%" + keyword + "%");
                ps.setString(2, "%" + keyword + "%");
                ps.setString(3, "%" + keyword + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LogTransaksi log = new LogTransaksi();
                    log.setKodeTransaksi(rs.getString("kode_transaksi"));
                    log.setTanggal(rs.getString("tanggal"));
                    log.setNamaPelanggan(rs.getString("nama_pelanggan"));
                    log.setTotal(rs.getInt("total"));
                    log.setBayar(rs.getInt("bayar"));
                    log.setKembali(rs.getInt("kembali"));
                    log.setTipeTransaksi(rs.getString("tipe_transaksi"));
                    log.setKeterangan(rs.getString("keterangan"));
                    list.add(log);
                }
            }
        }
        return list;
    }

    public void hapusLog(String kode) throws SQLException {
        String sql = "DELETE FROM log_transaksi WHERE kode_transaksi = ?";
        try (Connection con = koneksi.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, kode);
            ps.executeUpdate();
        }
        // Idealnya hapus juga dari tabel 'transaksi' dan 'transaksi_detail',
        // tapi log bisa saja dihapus tanpa menghapus master transaksinya (tergantung aturan bisnis)
    }
}
```

- [ ] **Step 6: Tulis ulang `src/modern_pos/dao/UserDAO.java`**

```java
package modern_pos.dao;
import config.koneksi;
import login.enkripsi;
import modern_pos.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {
    public User authenticate(String username, String passwordPlain) throws SQLException {
        String passwordHash = enkripsi.sha256(passwordPlain);
        String sql = "SELECT id_user, nama, username, hakakses FROM user WHERE username = ? AND password = ?";
        try (Connection con = koneksi.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Username atau Password salah!");
                User u = new User();
                u.setId(rs.getInt("id_user"));
                u.setNama(rs.getString("nama"));
                u.setUsername(rs.getString("username"));
                u.setHakAkses(rs.getString("hakakses"));
                return u;
            }
        }
    }
}
```

- [ ] **Step 7: Hapus `TransaksiDAO.getBarangList` dan pakai `BarangDAO` di controller**

Di `src/modern_pos/dao/TransaksiDAO.java`, hapus seluruh method `getBarangList` (dari `public List<Barang> getBarangList(String keyword) throws Exception {` sampai `}` penutupnya, sebelum `public void simpanTransaksi`). Hapus juga `import modern_pos.model.Barang;` dan `import java.sql.ResultSet;` dan `import java.util.ArrayList;` bila tidak dipakai lagi. (Sisa file dirombak di Task 3–4.)

Di `src/modern_pos/controller/TransaksiController.java`:

Tambah import di bawah `import modern_pos.dao.TransaksiDAO;`:
```java
import modern_pos.dao.BarangDAO;
```

Tambah field di bawah `private final TransaksiDAO dao;`:
```java
    private final BarangDAO barangDAO = new BarangDAO();
```

Ganti isi `doInBackground` di `loadBarang`:
```java
                return barangDAO.getAllBarang(keyword);
```

- [ ] **Step 8: Jalankan, pastikan lulus**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: `OK (6 tests)`.

- [ ] **Step 9: Commit**

```powershell
git add src/modern_pos test/modern_pos/dao/BarangDAOTest.java
git -c user.name="attia" -c user.email="attiaitem@gmail.com" commit -m "fix(modern_pos): close DB connections, drop fake column fallbacks" -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 3: Kode transaksi unik 20 karakter (B6)

**Files:**
- Modify: `src/modern_pos/dao/TransaksiDAO.java` (tambah method statis)
- Test: `test/modern_pos/dao/TransaksiDAOTest.java`

**Interfaces:**
- Produces: `public static synchronized String TransaksiDAO.newKodeTransaksi()` → `"TRX" + yyMMddHHmmssSSS + 2 digit`, panjang tepat 20, unik dalam JVM.

- [ ] **Step 1: Tulis test gagal `test/modern_pos/dao/TransaksiDAOTest.java`**

```java
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
```

- [ ] **Step 2: Jalankan, pastikan gagal**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: compile error `cannot find symbol ... newKodeTransaksi()`.

- [ ] **Step 3: Tambah generator di `TransaksiDAO`**

Tambah di awal body class `TransaksiDAO`:

```java
    private static long lastKode;

    // TRX + yyMMddHHmmssSSS + 2 digit urutan = 20 char (kolom kode_transaksi varchar(20)).
    // Nilai selalu naik, jadi unik dalam satu JVM walau > 100 panggilan per milidetik.
    public static synchronized String newKodeTransaksi() {
        lastKode = Math.max(System.currentTimeMillis() * 100, lastKode + 1);
        String waktu = new SimpleDateFormat("yyMMddHHmmssSSS").format(new Date(lastKode / 100));
        return "TRX" + waktu + String.format("%02d", lastKode % 100);
    }
```

(`SimpleDateFormat` dan `Date` sudah di-import di file ini.)

- [ ] **Step 4: Jalankan, pastikan lulus**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: `OK (7 tests)`.

- [ ] **Step 5: Commit**

```powershell
git add src/modern_pos/dao/TransaksiDAO.java test/modern_pos/dao/TransaksiDAOTest.java
git -c user.name="attia" -c user.email="attiaitem@gmail.com" commit -m "fix(modern_pos): unique 20-char transaction codes" -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 4: Checkout menolak stok kurang (B4)

**Files:**
- Modify: `src/modern_pos/dao/TransaksiDAO.java` (method `simpanTransaksi`)
- Test: `test/modern_pos/dao/TransaksiDAOTest.java` (tambah test)

**Interfaces:**
- Consumes: `newKodeTransaksi()` (Task 3), `koneksi.open()`, `TestDb` (Task 1)
- Produces: `TransaksiDAO.simpanTransaksi(List<CartItem> cart, int total, int bayar, int kembali, String pelanggan) throws SQLException` — atomik; melempar `SQLException` dengan pesan `"Stok <nama_barang> tidak cukup"` bila stok DB < qty, tanpa perubahan data apa pun. Signature sama dengan sekarang (controller tidak berubah).

- [ ] **Step 1: Tambah test gagal ke `TransaksiDAOTest.java`**

Tambah import:
```java
import java.sql.SQLException;
import java.util.Arrays;
import modern_pos.TestDb;
import modern_pos.model.Barang;
import modern_pos.model.CartItem;
```

Tambah helper dan test di dalam class:

```java
    private static Barang barang(String kode, String nama, int harga, int stokDiLayar) {
        Barang b = new Barang();
        b.setKodeBarang(kode);
        b.setNamaBarang(nama);
        b.setHarga(harga);
        b.setStok(stokDiLayar);
        return b;
    }

    @Test
    public void checkoutSuksesMengurangiStokDanMencatatSemuaTabel() throws Exception {
        TestDb.reset();
        CartItem item = new CartItem(barang("B001", "Beras 5kg", 65000, 10), 3);
        new TransaksiDAO().simpanTransaksi(Arrays.asList(item), 195000, 200000, 5000, "Budi");

        assertEquals(7, TestDb.queryInt("SELECT stok FROM barang WHERE kode_barang='B001'"));
        assertEquals(1, TestDb.queryInt("SELECT COUNT(*) FROM transaksi"));
        assertEquals(1, TestDb.queryInt("SELECT COUNT(*) FROM transaksi_detail"));
        assertEquals(1, TestDb.queryInt("SELECT COUNT(*) FROM log_transaksi"));
    }

    @Test
    public void checkoutStokKurangDitolakTanpaSisaData() throws Exception {
        TestDb.reset();
        // Layar masih menampilkan stok lama (5), padahal di DB tinggal 2.
        CartItem beras = new CartItem(barang("B001", "Beras 5kg", 65000, 10), 1);
        CartItem gula = new CartItem(barang("B002", "Gula 1kg", 15000, 5), 3);
        try {
            new TransaksiDAO().simpanTransaksi(Arrays.asList(beras, gula), 110000, 110000, 0, "Budi");
            fail("harus menolak stok kurang");
        } catch (SQLException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Stok Gula 1kg tidak cukup"));
        }
        assertEquals(2, TestDb.queryInt("SELECT stok FROM barang WHERE kode_barang='B002'"));
        assertEquals(10, TestDb.queryInt("SELECT stok FROM barang WHERE kode_barang='B001'"));
        assertEquals(0, TestDb.queryInt("SELECT COUNT(*) FROM transaksi"));
        assertEquals(0, TestDb.queryInt("SELECT COUNT(*) FROM transaksi_detail"));
        assertEquals(0, TestDb.queryInt("SELECT COUNT(*) FROM log_transaksi"));
    }
```

- [ ] **Step 2: Jalankan, pastikan gagal**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: `checkoutStokKurangDitolakTanpaSisaData` FAIL (`harus menolak stok kurang`). `checkoutSukses...` boleh lulus.

- [ ] **Step 3: Ganti method `simpanTransaksi`**

Ganti seluruh method `simpanTransaksi` dengan:

```java
    public void simpanTransaksi(List<CartItem> cart, int total, int bayar, int kembali, String pelanggan) throws SQLException {
        String kodeTrx = newKodeTransaksi();
        String sqlT = "INSERT INTO transaksi (kode_transaksi, tanggal, nama_pelanggan, total, jenis_transaksi, created_at) VALUES (?, CURDATE(), ?, ?, 'TUNAI', NOW())";
        String sqlD = "INSERT INTO transaksi_detail (kode_transaksi, kode_barang, harga, qty, subtotal) VALUES (?,?,?,?,?)";
        String sqlL = "INSERT INTO log_transaksi (kode_transaksi, tanggal, nama_pelanggan, total, bayar, kembali, tipe_transaksi, keterangan) VALUES (?, NOW(), ?, ?, ?, ?, 'TUNAI', 'Selesai')";
        // Kondisi stok >= qty mencegah stok minus walau layar menampilkan stok lama.
        String sqlStok = "UPDATE barang SET stok = stok - ? WHERE kode_barang = ? AND stok >= ?";

        try (Connection con = koneksi.open()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement psT = con.prepareStatement(sqlT)) {
                    psT.setString(1, kodeTrx);
                    psT.setString(2, pelanggan);
                    psT.setInt(3, total);
                    psT.executeUpdate();
                }

                try (PreparedStatement psD = con.prepareStatement(sqlD);
                     PreparedStatement psS = con.prepareStatement(sqlStok)) {
                    for (CartItem item : cart) {
                        Barang b = item.getBarang();
                        psS.setInt(1, item.getQty());
                        psS.setString(2, b.getKodeBarang());
                        psS.setInt(3, item.getQty());
                        if (psS.executeUpdate() == 0) {
                            throw new SQLException("Stok " + b.getNamaBarang() + " tidak cukup");
                        }

                        psD.setString(1, kodeTrx);
                        psD.setString(2, b.getKodeBarang());
                        psD.setInt(3, b.getHarga());
                        psD.setInt(4, item.getQty());
                        psD.setInt(5, item.getSubtotal());
                        psD.executeUpdate();
                    }
                }

                try (PreparedStatement psL = con.prepareStatement(sqlL)) {
                    psL.setString(1, kodeTrx);
                    psL.setString(2, pelanggan);
                    psL.setInt(3, total);
                    psL.setInt(4, bayar);
                    psL.setInt(5, kembali);
                    psL.executeUpdate();
                }

                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }
```

Pastikan import di `TransaksiDAO.java` persis:

```java
import config.koneksi;
import modern_pos.model.Barang;
import modern_pos.model.CartItem;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
```

- [ ] **Step 4: Jalankan, pastikan lulus**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: `OK (9 tests)`.

- [ ] **Step 5: Commit**

```powershell
git add src/modern_pos/dao/TransaksiDAO.java test/modern_pos/dao/TransaksiDAOTest.java
git -c user.name="attia" -c user.email="attiaitem@gmail.com" commit -m "fix(modern_pos): reject checkout when DB stock is insufficient" -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 5: Utang menyimpan barang dan transaksi yang benar (B2)

**Files:**
- Modify: `src/modern_pos/dao/UtangDAO.java` (seluruh file), `src/modern_pos/controller/UtangController.java`, `src/modern_pos/view/UtangView.java:174-233`, `src/modern_pos/model/Barang.java`
- Test: `test/modern_pos/dao/UtangDAOTest.java`

**Interfaces:**
- Consumes: `TransaksiDAO.newKodeTransaksi()` (Task 3), `BarangDAO.getAllBarang(String)` (Task 2), `TestDb` (Task 1)
- Produces:
  - `UtangDAO.tambahUtang(Utang u) throws SQLException` — insert `transaksi` → `utang` (dengan `u.getKodeBarang()` dan kode transaksi baru) → `log_transaksi`, atomik.
  - `UtangDAO.updateUtang(Utang u)` ikut meng-update `kode_barang`.
  - `UtangDAO.getUtangList(String)` mengisi `Utang.kodeBarang`.
  - `UtangController.daftarBarang() throws SQLException` → `List<Barang>`.
  - `Barang.toString()` → `"<kode> - <nama>"`.

- [ ] **Step 1: Tulis test gagal `test/modern_pos/dao/UtangDAOTest.java`**

```java
package modern_pos.dao;

import java.time.LocalDate;
import modern_pos.TestDb;
import modern_pos.model.Utang;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class UtangDAOTest {
    private final UtangDAO dao = new UtangDAO();

    @Before
    public void setUp() throws Exception {
        TestDb.reset();
    }

    private static Utang utang(String kode, String kodeBarang, int harga, int dp) {
        Utang u = new Utang();
        u.setKodeUtang(kode);
        u.setNama("Siti");
        u.setAlamat("-");
        u.setTelepon("0812");
        u.setHargaBarang(harga);
        u.setDp(dp);
        u.setJumlahCicilan(3);
        u.setJatuhTempo(LocalDate.of(2026, 12, 1));
        u.setKodeBarang(kodeBarang);
        return u;
    }

    @Test
    public void tambahUtangMenyimpanBarangYangDipilihDanKodeTransaksi() throws Exception {
        dao.tambahUtang(utang("UTG-1", "B002", 15000, 5000));

        assertEquals("B002", TestDb.queryString("SELECT kode_barang FROM utang WHERE kode_utang='UTG-1'"));
        String kodeTrx = TestDb.queryString("SELECT kode_transaksi FROM utang WHERE kode_utang='UTG-1'");
        assertNotNull(kodeTrx);
        assertEquals("KREDIT", TestDb.queryString("SELECT jenis_transaksi FROM transaksi WHERE kode_transaksi=?", kodeTrx));
        assertEquals(1, TestDb.queryInt("SELECT COUNT(*) FROM log_transaksi WHERE kode_transaksi=?", kodeTrx));
    }

    @Test
    public void getUtangListMengisiKodeBarang() throws Exception {
        dao.tambahUtang(utang("UTG-1", "B002", 15000, 5000));
        assertEquals("B002", dao.getUtangList("").get(0).getKodeBarang());
    }

    @Test
    public void editUtangBisaGantiBarang() throws Exception {
        dao.tambahUtang(utang("UTG-1", "B002", 15000, 5000));
        dao.updateUtang(utang("UTG-1", "B001", 65000, 5000));
        assertEquals("B001", TestDb.queryString("SELECT kode_barang FROM utang WHERE kode_utang='UTG-1'"));
    }

    @Test
    public void lunasTanpaSisaTidakMembuatTransaksiNol() throws Exception {
        dao.tambahUtang(utang("UTG-1", "B002", 15000, 15000));
        dao.tandaiLunas("UTG-1");
        assertEquals("lunas", TestDb.queryString("SELECT status FROM utang WHERE kode_utang='UTG-1'"));
        assertEquals(1, TestDb.queryInt("SELECT COUNT(*) FROM transaksi"));
    }

    @Test
    public void lunasDenganSisaMencatatPemasukanTunai() throws Exception {
        dao.tambahUtang(utang("UTG-1", "B002", 15000, 5000));
        dao.tandaiLunas("UTG-1");
        assertEquals(10000, TestDb.queryInt("SELECT total FROM transaksi WHERE jenis_transaksi='TUNAI'"));
    }
}
```

- [ ] **Step 2: Jalankan, pastikan gagal**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: `tambahUtangMenyimpan...` FAIL (`expected:<B00[2]> but was:<B00[1]>`), `getUtangListMengisiKodeBarang` FAIL (`null`), `editUtangBisaGantiBarang` FAIL.

- [ ] **Step 3: Tulis ulang `src/modern_pos/dao/UtangDAO.java`**

```java
package modern_pos.dao;
import config.koneksi;
import modern_pos.model.Utang;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UtangDAO {

    public List<Utang> getUtangList(String keyword) throws SQLException {
        List<Utang> list = new ArrayList<>();
        boolean isSearch = keyword != null && !keyword.trim().isEmpty();
        String sql = "SELECT * FROM utang "
                + (isSearch ? "WHERE kode_utang LIKE ? OR nama LIKE ? " : "")
                + "ORDER BY status ASC, id_utang DESC";

        try (Connection con = koneksi.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (isSearch) {
                ps.setString(1, "%" + keyword + "%");
                ps.setString(2, "%" + keyword + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Utang u = new Utang();
                    u.setKodeUtang(rs.getString("kode_utang"));
                    u.setNama(rs.getString("nama"));
                    u.setAlamat(rs.getString("alamat"));
                    u.setTelepon(rs.getString("telepon"));
                    u.setHargaBarang(rs.getInt("harga_brng"));
                    u.setDp(rs.getInt("dp"));
                    u.setJumlahCicilan(rs.getInt("jumlah_cicilan"));
                    u.setStatus(rs.getString("status"));
                    u.setKodeBarang(rs.getString("kode_barang"));
                    if (rs.getDate("jatuh_tempo") != null) u.setJatuhTempo(rs.getDate("jatuh_tempo").toLocalDate());
                    list.add(u);
                }
            }
        }
        return list;
    }

    public void tambahUtang(Utang u) throws SQLException {
        String kodeTrx = TransaksiDAO.newKodeTransaksi();
        try (Connection con = koneksi.open()) {
            con.setAutoCommit(false);
            try {
                // 1. Master transaksi dulu: utang dan log_transaksi punya FK ke kode_transaksi
                String sqlTrx = "INSERT INTO transaksi (kode_transaksi, tanggal, nama_pelanggan, total, jenis_transaksi, created_at) VALUES (?, NOW(), ?, ?, 'KREDIT', NOW())";
                try (PreparedStatement ps = con.prepareStatement(sqlTrx)) {
                    ps.setString(1, kodeTrx);
                    ps.setString(2, u.getNama());
                    ps.setInt(3, u.getHargaBarang());
                    ps.executeUpdate();
                }

                // 2. Utang, terhubung ke barang yang dipilih dan transaksi di atas
                String sql = "INSERT INTO utang (kode_utang, nama, alamat, telepon, harga_brng, dp, jumlah_cicilan, jatuh_tempo, status, kode_barang, kode_transaksi) VALUES (?,?,?,?,?,?,?,?,'belum',?,?)";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, u.getKodeUtang());
                    ps.setString(2, u.getNama());
                    ps.setString(3, u.getAlamat());
                    ps.setString(4, u.getTelepon());
                    ps.setInt(5, u.getHargaBarang());
                    ps.setInt(6, u.getDp());
                    ps.setInt(7, u.getJumlahCicilan());
                    ps.setDate(8, u.getJatuhTempo() != null ? java.sql.Date.valueOf(u.getJatuhTempo()) : null);
                    ps.setString(9, u.getKodeBarang());
                    ps.setString(10, kodeTrx);
                    ps.executeUpdate();
                }

                // 3. Log
                String sqlLog = "INSERT INTO log_transaksi (kode_transaksi, tanggal, nama_pelanggan, total, bayar, kembali, tipe_transaksi, keterangan) VALUES (?, NOW(), ?, ?, ?, 0, 'KREDIT', 'Utang Baru (DP)')";
                try (PreparedStatement ps = con.prepareStatement(sqlLog)) {
                    ps.setString(1, kodeTrx);
                    ps.setString(2, u.getNama());
                    ps.setInt(3, u.getHargaBarang());
                    ps.setInt(4, u.getDp());
                    ps.executeUpdate();
                }

                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public void updateUtang(Utang u) throws SQLException {
        String sql = "UPDATE utang SET nama=?, alamat=?, telepon=?, harga_brng=?, dp=?, jumlah_cicilan=?, jatuh_tempo=?, kode_barang=? WHERE kode_utang=?";
        try (Connection con = koneksi.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u.getNama());
            ps.setString(2, u.getAlamat());
            ps.setString(3, u.getTelepon());
            ps.setInt(4, u.getHargaBarang());
            ps.setInt(5, u.getDp());
            ps.setInt(6, u.getJumlahCicilan());
            ps.setDate(7, u.getJatuhTempo() != null ? java.sql.Date.valueOf(u.getJatuhTempo()) : null);
            ps.setString(8, u.getKodeBarang());
            ps.setString(9, u.getKodeUtang());
            ps.executeUpdate();
        }
    }

    public void hapusUtang(String kode) throws SQLException {
        String sql = "DELETE FROM utang WHERE kode_utang = ?";
        try (Connection con = koneksi.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, kode);
            ps.executeUpdate();
        }
    }

    public void tandaiLunas(String kode) throws SQLException {
        try (Connection con = koneksi.open()) {
            con.setAutoCommit(false);
            try {
                // 1. Sisa utang sebelum ditandai lunas
                String nama = "Pelanggan Utang";
                int sisa = 0;
                try (PreparedStatement ps = con.prepareStatement("SELECT nama, harga_brng, dp FROM utang WHERE kode_utang = ?")) {
                    ps.setString(1, kode);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            nama = rs.getString("nama");
                            sisa = rs.getInt("harga_brng") - rs.getInt("dp");
                        }
                    }
                }

                // 2. Status LUNAS
                try (PreparedStatement ps = con.prepareStatement("UPDATE utang SET status = 'lunas' WHERE kode_utang = ?")) {
                    ps.setString(1, kode);
                    ps.executeUpdate();
                }

                // 3. Catat pemasukan sisa sebagai TUNAI
                if (sisa > 0) {
                    String kodeTrx = TransaksiDAO.newKodeTransaksi();
                    String sqlTrx = "INSERT INTO transaksi (kode_transaksi, tanggal, nama_pelanggan, total, jenis_transaksi, created_at) VALUES (?, NOW(), ?, ?, 'TUNAI', NOW())";
                    try (PreparedStatement ps = con.prepareStatement(sqlTrx)) {
                        ps.setString(1, kodeTrx);
                        ps.setString(2, nama);
                        ps.setInt(3, sisa);
                        ps.executeUpdate();
                    }
                    String sqlLog = "INSERT INTO log_transaksi (kode_transaksi, tanggal, nama_pelanggan, total, bayar, kembali, tipe_transaksi, keterangan) VALUES (?, NOW(), ?, ?, ?, 0, 'TUNAI', 'Pelunasan Sisa Utang')";
                    try (PreparedStatement ps = con.prepareStatement(sqlLog)) {
                        ps.setString(1, kodeTrx);
                        ps.setString(2, nama);
                        ps.setInt(3, sisa);
                        ps.setInt(4, sisa);
                        ps.executeUpdate();
                    }
                }

                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }
}
```

- [ ] **Step 4: Jalankan, pastikan test DAO lulus**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: `OK (14 tests)`.

- [ ] **Step 5: `Barang.toString()` untuk combo box**

Tambah di akhir class `src/modern_pos/model/Barang.java`:

```java
    @Override
    public String toString() { return kodeBarang + " - " + namaBarang; }
```

- [ ] **Step 6: `UtangController.daftarBarang()`**

Di `src/modern_pos/controller/UtangController.java` tambah import:

```java
import java.sql.SQLException;
import modern_pos.dao.BarangDAO;
import modern_pos.model.Barang;
```

Tambah method:

```java
    // ponytail: dipanggil sinkron di EDT saat form dibuka; tabel barang kecil. Pindah ke SwingWorker bila terasa lambat.
    public List<Barang> daftarBarang() throws SQLException {
        return new BarangDAO().getAllBarang("");
    }
```

- [ ] **Step 7: Combo box barang di form `UtangView`**

Di `src/modern_pos/view/UtangView.java` tambah import:

```java
import javax.swing.JComboBox;
import modern_pos.model.Barang;
```

Di `showFormDialog`, setelah baris `JTextField txtJatuhTempo = SwingHelper.createMaterialTextField();` tambah:

```java
        JComboBox<Barang> cmbBarang = new JComboBox<>();
        try {
            for (Barang b : controller.daftarBarang()) {
                cmbBarang.addItem(b);
                if (isEdit && b.getKodeBarang().equals(u.getKodeBarang())) cmbBarang.setSelectedItem(b);
            }
        } catch (Exception ex) {
            showError("Gagal memuat daftar barang: " + ex.getMessage());
            return;
        }
```

Ganti `new GridLayout(7, 2, 10, 10)` jadi `new GridLayout(8, 2, 10, 10)`, lalu setelah baris label "Telepon:" tambah:

```java
        panel.add(SwingHelper.createLabel("Barang:", UITheme.FONT_BODY, UITheme.COLOR_TEXT_PRIMARY)); panel.add(cmbBarang);
```

Di blok validasi, setelah `if (txtNama.getText().trim().isEmpty()) throw new Exception("Nama pelanggan tidak boleh kosong!");` tambah:

```java
                    Barang barangDipilih = (Barang) cmbBarang.getSelectedItem();
                    if (barangDipilih == null) throw new Exception("Pilih barang yang diutang!");
```

dan setelah `newU.setAlamat("-"); // Optional` tambah:

```java
                    newU.setKodeBarang(barangDipilih.getKodeBarang());
```

- [ ] **Step 8: Compile + test**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: `OK (14 tests)`.

- [ ] **Step 9: Commit**

```powershell
git add src/modern_pos test/modern_pos/dao/UtangDAOTest.java
git -c user.name="attia" -c user.email="attiaitem@gmail.com" commit -m "fix(modern_pos): store selected item and transaction on utang" -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 6: User tetap terbawa saat pindah menu (B3)

**Files:**
- Create: `src/modern_pos/utils/Session.java`
- Modify: `src/modern_pos/controller/LoginController.java`, `src/modern_pos/view/LoginView.java:27-31`, `StockView.java:79`, `TransaksiView.java:85`, `UtangView.java:80`, `LogTransaksiView.java:78`

**Interfaces:**
- Produces: `modern_pos.utils.Session.currentUser` (`public static User`), di-set saat login sukses, di-clear saat `LoginView` dibuat.

Tidak ada test otomatis (murni wiring UI, sesuai spec B3).

- [ ] **Step 1: Buat `src/modern_pos/utils/Session.java`**

```java
package modern_pos.utils;

import modern_pos.model.User;

// Aplikasi desktop: satu user login per proses, jadi cukup field statis.
public class Session {
    public static User currentUser;
}
```

- [ ] **Step 2: Set saat login**

Di `LoginController.handleLogin`, di dalam `done()`, setelah `User loggedInUser = get();` tambah:

```java
                    modern_pos.utils.Session.currentUser = loggedInUser;
```

- [ ] **Step 3: Clear di `LoginView`**

Di constructor `LoginView()`, sebagai baris pertama tambah:

```java
        modern_pos.utils.Session.currentUser = null; // semua jalur logout membuka LoginView
```

- [ ] **Step 4: Ganti `new modern_pos.model.User()` di 4 view**

Di `StockView.java`, `TransaksiView.java`, `UtangView.java`, `LogTransaksiView.java`, ganti teks

```java
new modern_pos.controller.DashboardController(new modern_pos.model.User())
```

dengan

```java
new modern_pos.controller.DashboardController(modern_pos.utils.Session.currentUser)
```

- [ ] **Step 5: Verifikasi**

```powershell
Select-String -Path src\modern_pos\view\*.java -Pattern 'new modern_pos\.model\.User\(\)'
powershell -ExecutionPolicy Bypass -File run-tests.ps1
```

Expected: grep tanpa output; test `OK (14 tests)`.

- [ ] **Step 6: Cek manual**

Jalankan `modern_pos.view.LoginView` dari NetBeans (Run File), login, buka Stock, klik Dashboard. Expected: nama user tampil di dashboard (sebelumnya kosong).

- [ ] **Step 7: Commit**

```powershell
git add src/modern_pos
git -c user.name="attia" -c user.email="attiaitem@gmail.com" commit -m "fix(modern_pos): keep logged-in user across navigation" -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 7: Verifikasi akhir

- [ ] **Step 1: Tidak ada sisa pola lama di modern_pos**

```powershell
Select-String -Path src\modern_pos\*\*.java -Pattern 'koneksi\.getConnection|con == null|getInt\("(harga|jumlah)"\)|setStok\(999\)|LIMIT 1\)'
```

Expected: tidak ada output.

- [ ] **Step 2: Semua test**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: `OK (14 tests)`.

- [ ] **Step 3: Build dari NetBeans**

Clean and Build Project di NetBeans; pastikan tidak ada error referensi library (bukti path `lib/` benar).
