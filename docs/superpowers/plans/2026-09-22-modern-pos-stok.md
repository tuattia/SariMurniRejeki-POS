# modern_pos Stok (Sub-proyek 2a) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Setiap perubahan stok di `modern_pos` tercatat atomik di `stock_movement_log`, utang punya qty yang mengurangi stok, layar Stock punya Restock/Riwayat, snapshot lepas dari stack lama, dan 5 minor sub-proyek 1 selesai.

**Architecture:** Satu fungsi pusat `StokDAO.ubahStok(Connection, ...)` yang dipanggil di dalam transaksi DB milik pemanggil (penjualan, utang, barang, restock). Fungsi ini mengunci baris barang (`FOR UPDATE`), menolak stok minus, meng-update stok, dan menulis log. Helper `Tx.rollbackQuietly` menjaga error asli saat rollback gagal.

**Tech Stack:** Java 1.8 source (JDK 17), Swing, JDBC MySQL 8.4 (Laragon), JUnit 4.13.2. Test via `run-tests.ps1` ke DB `sarimurnirejeki_test`.

**Spec:** `docs/superpowers/specs/2026-09-22-modern-pos-stok-design.md`

## Global Constraints

- Source level Java 1.8: tidak boleh `var`, `List.of`, text block, `String.isBlank`, switch expression.
- DB utama `sarimurnirejeki` tidak ditulis selama Task 1–7. Migrasi ke DB utama hanya di Task 8 **setelah user mengizinkan secara eksplisit**.
- Pesan error stok tetap `"Stok <nama_barang> tidak cukup"`.
- `tipe_gerakan` hanya `KELUAR`, `MASUK`, `KOREKSI`; `qty` di log selalu positif (|delta|).
- Stack lama (`gui/`, `controller/`, `model/`, `login/`) tidak diubah.
- Commit: `git -c user.name="attia" -c user.email="attiaitem@gmail.com" commit -m "<pesan>" -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"`.
- Shell PowerShell 5.1, working dir root project. Test: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`.
- Data seed test (dari `TestDb.reset()`): `B001 'Beras 5kg' 65000 stok 10`, `B002 'Gula 1kg' 15000 stok 2`, `satuan` NULL.

## Review Focus

1. Edit barang saat penjualan terjadi setelah form dibuka: stok hasil = stok DB + selisih yang diketik, bukan angka mutlak yang basi → test `editMemakaiSelisihBukanAngkaMutlak` di Task 4.
2. Stok awal negatif saat tambah barang ditolak dengan pesan jelas → test `stokAwalNegatifDitolak` di Task 4.
3. Edit utang menaikkan qty melebihi stok: ditolak dan data utang tidak berubah → test `editQtyMelebihiStokDitolakTanpaPerubahan` di Task 5.
4. Restock dengan keterangan kosong/spasi tercatat `"Restock"` → test `restockKeteranganKosongJadiRestock` di Task 2.
5. Snapshot diambil ulang setelah stok berubah meng-update, tidak menduplikasi → test `ambilUlangMengupdateTanpaDuplikat` di Task 7.

---

## File Structure

| File | Aksi | Tanggung jawab |
|---|---|---|
| `src/modern_pos/dao/Tx.java` | Create | Rollback tanpa menutupi error asli |
| `src/modern_pos/dao/StokDAO.java` | Create | `ubahStok`, `restock`, `riwayat` |
| `src/modern_pos/model/StockMovement.java` | Create | Baris log gerakan stok |
| `src/modern_pos/dao/TransaksiDAO.java` | Modify | Penjualan via `ubahStok` |
| `src/modern_pos/dao/BarangDAO.java` | Modify | Stok awal MASUK, edit KOREKSI relatif, trim |
| `src/modern_pos/dao/UtangDAO.java` | Modify | Qty + gerakan stok |
| `src/modern_pos/model/Utang.java` | Modify | Field `qty` |
| `migrations/2026-09-22-utang-qty.sql` | Create | `ALTER TABLE utang ADD qty` |
| `schema.sql` | Modify | Dump ulang (ada `qty`) |
| `src/modern_pos/dao/SnapshotDAO.java` | Create | Port `snapshotcontroller` |
| `src/modern_pos/model/StockSnapshot.java` | Create | Baris snapshot |
| `src/modern_pos/view/SnapshotDialog.java` | Create | Port `gui.snapshotDialog` |
| `src/modern_pos/view/RiwayatStokDialog.java` | Create | Tabel riwayat per barang |
| `src/modern_pos/controller/StockController.java` | Modify | `simpanBarang(baru, lama)`, `restock`, `tampilRiwayat` |
| `src/modern_pos/view/StockView.java` | Modify | Tombol Restock/Riwayat, snapshot modern |
| `src/modern_pos/view/UtangView.java` | Modify | Field Qty, total otomatis, kolom Qty |
| `src/modern_pos/controller/{TransaksiController,DashboardController}.java` | Modify | Minor F2, F5 |
| `run-tests.ps1`, `nbproject/project.properties` | Modify | `db.url` test |
| `test/modern_pos/dao/{TxTest,StokDAOTest,SnapshotDAOTest}.java` | Create | — |
| `test/modern_pos/dao/{BarangDAOTest,TransaksiDAOTest,UtangDAOTest}.java` | Modify | — |

---

### Task 1: `Tx` helper dan minor tertunda (F1–F5)

**Files:**
- Create: `src/modern_pos/dao/Tx.java`, `test/modern_pos/dao/TxTest.java`
- Modify: `run-tests.ps1:14`, `nbproject/project.properties:91`, `src/modern_pos/dao/BarangDAO.java` (getAllBarang), `src/modern_pos/dao/TransaksiDAO.java` (catch), `src/modern_pos/dao/UtangDAO.java` (3 catch), `src/modern_pos/controller/TransaksiController.java`, `src/modern_pos/controller/DashboardController.java:25`
- Test: `test/modern_pos/dao/BarangDAOTest.java`

**Interfaces:**
- Produces: `static SQLException Tx.rollbackQuietly(Connection con, SQLException e)` (package-private class di `modern_pos.dao`) → rollback, kegagalan rollback ditempel `e.addSuppressed`, mengembalikan `e`. Pola pakai: `catch (SQLException e) { throw Tx.rollbackQuietly(con, e); }`.

- [ ] **Step 1: Tulis test gagal `test/modern_pos/dao/TxTest.java`**

```java
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
```

Tambah test ke `test/modern_pos/dao/BarangDAOTest.java`:

```java
    @Test
    public void keywordDenganSpasiDiTrim() throws Exception {
        assertEquals(1, dao.getAllBarang(" Gula ").size());
    }
```

- [ ] **Step 2: Jalankan, pastikan gagal**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: compile error `cannot find symbol ... Tx`.

- [ ] **Step 3: Buat `src/modern_pos/dao/Tx.java`**

```java
package modern_pos.dao;

import java.sql.Connection;
import java.sql.SQLException;

final class Tx {
    private Tx() {}

    // Rollback tanpa menutupi error asli: kegagalan rollback ditempel sebagai suppressed.
    static SQLException rollbackQuietly(Connection con, SQLException e) {
        try {
            con.rollback();
        } catch (SQLException r) {
            e.addSuppressed(r);
        }
        return e;
    }
}
```

- [ ] **Step 4: Jalankan, pastikan `TxTest` lulus dan `keywordDenganSpasiDiTrim` masih gagal**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: `Tests run: 17,  Failures: 1` — hanya `keywordDenganSpasiDiTrim` (`expected:<1> but was:<0>`).

- [ ] **Step 5: Trim keyword di `BarangDAO.getAllBarang`**

Ganti:
```java
            if (isSearch) {
                ps.setString(1, "%" + keyword + "%");
                ps.setString(2, "%" + keyword + "%");
            }
```
dengan:
```java
            if (isSearch) {
                ps.setString(1, "%" + keyword.trim() + "%");
                ps.setString(2, "%" + keyword.trim() + "%");
            }
```

- [ ] **Step 6: Pakai `Tx` di semua catch transaksi**

Di `TransaksiDAO.java` (1 tempat) dan `UtangDAO.java` (2 tempat: `tambahUtang`, `tandaiLunas`), ganti setiap:
```java
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
```
dengan:
```java
            } catch (SQLException e) {
                throw Tx.rollbackQuietly(con, e);
            }
```
Verifikasi: `Select-String -Path src\modern_pos\dao\*.java -Pattern 'con\.rollback\(\);\s*$'` hanya menyisakan `UtangDAO.tandaiLunas` di cabang `executeUpdate() == 0` (yang diikuti `return;`).

- [ ] **Step 7: `db.url` test di luar `TestDb`**

`run-tests.ps1` baris `java`:
```powershell
& java "-Ddb.url=jdbc:mysql://localhost:3306/sarimurnirejeki_test" -cp "$out;lib/*" org.junit.runner.JUnitCore $tests
```

`nbproject/project.properties`, tepat di bawah baris komentar `# To set system properties for unit tests define test-sys-prop.name=value:` tambah:
```properties
test-sys-prop.db.url=jdbc:mysql://localhost:3306/sarimurnirejeki_test
```

- [ ] **Step 8: Minor UI (F2, F5)**

`TransaksiController.processPayment`, blok `catch` terluar di `done()`:
```java
                } catch (Exception ex) {
                    view.showError("Gagal menyimpan: " + ex.getMessage());
                    loadBarang(""); // stok di layar mungkin basi
                }
```

`DashboardController.setView`, ganti `view.setUserInfo(currentUser.getNama());` dengan:
```java
        view.setUserInfo(currentUser != null ? currentUser.getNama() : "-");
```

- [ ] **Step 9: Jalankan, pastikan lulus**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: `OK (17 tests)`.

- [ ] **Step 10: Commit**

```powershell
git add src/modern_pos run-tests.ps1 nbproject/project.properties test/modern_pos/dao
git -c user.name="attia" -c user.email="attiaitem@gmail.com" commit -m "fix(modern_pos): keep original error on rollback failure, trim search, test db.url" -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 2: `StokDAO` dan `StockMovement`

**Files:**
- Create: `src/modern_pos/dao/StokDAO.java`, `src/modern_pos/model/StockMovement.java`, `test/modern_pos/dao/StokDAOTest.java`

**Interfaces:**
- Consumes: `Tx.rollbackQuietly` (Task 1), `koneksi.open()`, `TestDb`.
- Produces:
  - `public static void StokDAO.ubahStok(Connection con, String kodeBarang, int delta, String tipe, String kodeTransaksi, String keterangan) throws SQLException` — tidak commit/rollback.
  - `public void StokDAO.restock(String kodeBarang, int qty, String keterangan) throws SQLException` — `qty <= 0` → `IllegalArgumentException("Qty restock harus lebih dari 0")`.
  - `public List<StockMovement> StokDAO.riwayat(String kodeBarang) throws SQLException`.
  - `StockMovement` getter: `getWaktu()` String, `getTipe()`, `getQty()` int, `getStokSebelum()` int, `getStokSesudah()` int, `getKodeTransaksi()`, `getKeterangan()`.

- [ ] **Step 1: Tulis test gagal `test/modern_pos/dao/StokDAOTest.java`**

```java
package modern_pos.dao;

import config.koneksi;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import modern_pos.TestDb;
import modern_pos.model.StockMovement;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class StokDAOTest {
    private final StokDAO dao = new StokDAO();

    @Before
    public void setUp() throws Exception {
        TestDb.reset();
    }

    private static void ubahDalamTransaksi(String kode, int delta, String tipe) throws SQLException {
        try (Connection con = koneksi.open()) {
            con.setAutoCommit(false);
            try {
                StokDAO.ubahStok(con, kode, delta, tipe, "TRXTEST", "uji");
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    @Test
    public void ubahStokMengubahStokDanMencatatLog() throws Exception {
        ubahDalamTransaksi("B001", -3, "KELUAR");

        assertEquals(7, TestDb.queryInt("SELECT stok FROM barang WHERE kode_barang='B001'"));
        assertEquals("KELUAR", TestDb.queryString("SELECT tipe_gerakan FROM stock_movement_log"));
        assertEquals(3, TestDb.queryInt("SELECT qty FROM stock_movement_log"));
        assertEquals(10, TestDb.queryInt("SELECT stok_sebelum FROM stock_movement_log"));
        assertEquals(7, TestDb.queryInt("SELECT stok_sesudah FROM stock_movement_log"));
        assertEquals("TRXTEST", TestDb.queryString("SELECT kode_transaksi FROM stock_movement_log"));
    }

    @Test
    public void stokKurangDitolakTanpaPerubahan() throws Exception {
        try {
            ubahDalamTransaksi("B002", -3, "KELUAR");
            fail("harus ditolak");
        } catch (SQLException e) {
            assertEquals("Stok Gula 1kg tidak cukup", e.getMessage());
        }
        assertEquals(2, TestDb.queryInt("SELECT stok FROM barang WHERE kode_barang='B002'"));
        assertEquals(0, TestDb.queryInt("SELECT COUNT(*) FROM stock_movement_log"));
    }

    @Test
    public void deltaNolTidakMencatatLog() throws Exception {
        ubahDalamTransaksi("B001", 0, "KOREKSI");
        assertEquals(0, TestDb.queryInt("SELECT COUNT(*) FROM stock_movement_log"));
    }

    @Test
    public void barangTidakAdaDitolak() throws Exception {
        try {
            ubahDalamTransaksi("X999", 1, "MASUK");
            fail("harus ditolak");
        } catch (SQLException e) {
            assertEquals("Barang X999 tidak ditemukan", e.getMessage());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void restockQtyNolDitolak() throws Exception {
        dao.restock("B001", 0, "x");
    }

    @Test
    public void restockMenambahStokDanMencatatMasuk() throws Exception {
        dao.restock("B001", 5, "Kiriman supplier");
        assertEquals(15, TestDb.queryInt("SELECT stok FROM barang WHERE kode_barang='B001'"));
        assertEquals("MASUK", TestDb.queryString("SELECT tipe_gerakan FROM stock_movement_log"));
        assertEquals("Kiriman supplier", TestDb.queryString("SELECT keterangan FROM stock_movement_log"));
        assertNull(TestDb.queryString("SELECT kode_transaksi FROM stock_movement_log"));
    }

    @Test
    public void restockKeteranganKosongJadiRestock() throws Exception {
        dao.restock("B001", 1, "   ");
        assertEquals("Restock", TestDb.queryString("SELECT keterangan FROM stock_movement_log"));
    }

    @Test
    public void riwayatTerbaruDulu() throws Exception {
        dao.restock("B001", 1, "pertama");
        dao.restock("B001", 2, "kedua");
        dao.restock("B002", 9, "barang lain");

        List<StockMovement> r = dao.riwayat("B001");
        assertEquals(2, r.size());
        assertEquals("kedua", r.get(0).getKeterangan());
        assertEquals(2, r.get(0).getQty());
        assertEquals(11, r.get(0).getStokSebelum());
        assertEquals(13, r.get(0).getStokSesudah());
        assertEquals("MASUK", r.get(0).getTipe());
        assertNotNull(r.get(0).getWaktu());
    }
}
```

- [ ] **Step 2: Jalankan, pastikan gagal**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: compile error `cannot find symbol ... StokDAO` / `StockMovement`.

- [ ] **Step 3: Buat `src/modern_pos/model/StockMovement.java`**

```java
package modern_pos.model;

public class StockMovement {
    private String waktu, tipe, kodeTransaksi, keterangan;
    private int qty, stokSebelum, stokSesudah;

    public String getWaktu() { return waktu; }
    public void setWaktu(String waktu) { this.waktu = waktu; }
    public String getTipe() { return tipe; }
    public void setTipe(String tipe) { this.tipe = tipe; }
    public String getKodeTransaksi() { return kodeTransaksi; }
    public void setKodeTransaksi(String kodeTransaksi) { this.kodeTransaksi = kodeTransaksi; }
    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }
    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
    public int getStokSebelum() { return stokSebelum; }
    public void setStokSebelum(int stokSebelum) { this.stokSebelum = stokSebelum; }
    public int getStokSesudah() { return stokSesudah; }
    public void setStokSesudah(int stokSesudah) { this.stokSesudah = stokSesudah; }
}
```

- [ ] **Step 4: Buat `src/modern_pos/dao/StokDAO.java`**

```java
package modern_pos.dao;
import config.koneksi;
import modern_pos.model.StockMovement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StokDAO {

    // Satu-satunya jalan mengubah stok di modern_pos. Dipanggil di dalam transaksi milik
    // pemanggil (tidak commit/rollback sendiri). FOR UPDATE mengunci baris barang supaya
    // dua kasir bersamaan tidak saling menimpa.
    public static void ubahStok(Connection con, String kodeBarang, int delta, String tipe,
                                String kodeTransaksi, String keterangan) throws SQLException {
        if (delta == 0) return;

        String nama;
        int sebelum;
        try (PreparedStatement ps = con.prepareStatement("SELECT nama_barang, stok FROM barang WHERE kode_barang = ? FOR UPDATE")) {
            ps.setString(1, kodeBarang);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Barang " + kodeBarang + " tidak ditemukan");
                nama = rs.getString("nama_barang");
                sebelum = rs.getInt("stok");
            }
        }

        int sesudah = sebelum + delta;
        if (sesudah < 0) throw new SQLException("Stok " + nama + " tidak cukup");

        try (PreparedStatement ps = con.prepareStatement("UPDATE barang SET stok = ? WHERE kode_barang = ?")) {
            ps.setInt(1, sesudah);
            ps.setString(2, kodeBarang);
            ps.executeUpdate();
        }

        String sqlLog = "INSERT INTO stock_movement_log (kode_barang, kode_transaksi, tipe_gerakan, qty, stok_sebelum, stok_sesudah, keterangan) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sqlLog)) {
            ps.setString(1, kodeBarang);
            ps.setString(2, kodeTransaksi);
            ps.setString(3, tipe);
            ps.setInt(4, Math.abs(delta));
            ps.setInt(5, sebelum);
            ps.setInt(6, sesudah);
            ps.setString(7, keterangan);
            ps.executeUpdate();
        }
    }

    public void restock(String kodeBarang, int qty, String keterangan) throws SQLException {
        if (qty <= 0) throw new IllegalArgumentException("Qty restock harus lebih dari 0");
        String ket = (keterangan == null || keterangan.trim().isEmpty()) ? "Restock" : keterangan.trim();
        try (Connection con = koneksi.open()) {
            con.setAutoCommit(false);
            try {
                ubahStok(con, kodeBarang, qty, "MASUK", null, ket);
                con.commit();
            } catch (SQLException e) {
                throw Tx.rollbackQuietly(con, e);
            }
        }
    }

    public List<StockMovement> riwayat(String kodeBarang) throws SQLException {
        List<StockMovement> list = new ArrayList<>();
        String sql = "SELECT waktu, tipe_gerakan, qty, stok_sebelum, stok_sesudah, kode_transaksi, keterangan "
                + "FROM stock_movement_log WHERE kode_barang = ? ORDER BY waktu DESC, id_log DESC LIMIT 200";
        try (Connection con = koneksi.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, kodeBarang);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StockMovement m = new StockMovement();
                    m.setWaktu(rs.getString("waktu"));
                    m.setTipe(rs.getString("tipe_gerakan"));
                    m.setQty(rs.getInt("qty"));
                    m.setStokSebelum(rs.getInt("stok_sebelum"));
                    m.setStokSesudah(rs.getInt("stok_sesudah"));
                    m.setKodeTransaksi(rs.getString("kode_transaksi"));
                    m.setKeterangan(rs.getString("keterangan"));
                    list.add(m);
                }
            }
        }
        return list;
    }
}
```

- [ ] **Step 5: Jalankan, pastikan lulus**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: `OK (25 tests)`.

- [ ] **Step 6: Commit**

```powershell
git add src/modern_pos/dao/StokDAO.java src/modern_pos/model/StockMovement.java test/modern_pos/dao/StokDAOTest.java
git -c user.name="attia" -c user.email="attiaitem@gmail.com" commit -m "feat(modern_pos): StokDAO with atomic stock change + movement log" -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 3: Penjualan mencatat gerakan KELUAR

**Files:**
- Modify: `src/modern_pos/dao/TransaksiDAO.java` (method `simpanTransaksi`)
- Test: `test/modern_pos/dao/TransaksiDAOTest.java`

**Interfaces:**
- Consumes: `StokDAO.ubahStok(...)` (Task 2), `Tx.rollbackQuietly` (Task 1).
- Produces: signature `simpanTransaksi` tidak berubah.

- [ ] **Step 1: Tambah test gagal ke `TransaksiDAOTest.java`**

```java
    @Test
    public void checkoutMencatatGerakanKeluarDenganKodeTransaksi() throws Exception {
        TestDb.reset();
        CartItem item = new CartItem(barang("B001", "Beras 5kg", 65000, 10), 3);
        new TransaksiDAO().simpanTransaksi(Arrays.asList(item), 195000, 200000, 5000, "Budi");

        String kodeTrx = TestDb.queryString("SELECT kode_transaksi FROM transaksi");
        assertEquals(1, TestDb.queryInt("SELECT COUNT(*) FROM stock_movement_log WHERE tipe_gerakan='KELUAR' AND kode_transaksi=?", kodeTrx));
        assertEquals(3, TestDb.queryInt("SELECT qty FROM stock_movement_log"));
        assertEquals("Penjualan", TestDb.queryString("SELECT keterangan FROM stock_movement_log"));
    }
```

Di test `checkoutStokKurangDitolakTanpaSisaData`, tambah assertion terakhir:
```java
        assertEquals(0, TestDb.queryInt("SELECT COUNT(*) FROM stock_movement_log"));
```

- [ ] **Step 2: Jalankan, pastikan gagal**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: `checkoutMencatatGerakanKeluarDenganKodeTransaksi` FAIL (`expected:<1> but was:<0>`).

- [ ] **Step 3: Pakai `ubahStok` di `simpanTransaksi`**

Hapus deklarasi `String sqlStok = ...` beserta komentar di atasnya. Ganti blok:
```java
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

```
dengan:
```java
                try (PreparedStatement psD = con.prepareStatement(sqlD)) {
                    for (CartItem item : cart) {
                        Barang b = item.getBarang();
                        StokDAO.ubahStok(con, b.getKodeBarang(), -item.getQty(), "KELUAR", kodeTrx, "Penjualan");

```
(Sisa loop — `psD.setString(...)` s.d. `psD.executeUpdate();` — tetap.)

- [ ] **Step 4: Jalankan, pastikan lulus**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: `OK (26 tests)`.

- [ ] **Step 5: Commit**

```powershell
git add src/modern_pos/dao/TransaksiDAO.java test/modern_pos/dao/TransaksiDAOTest.java
git -c user.name="attia" -c user.email="attiaitem@gmail.com" commit -m "feat(modern_pos): log KELUAR movement on sale" -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 4: Barang — stok awal MASUK, edit stok KOREKSI relatif

**Files:**
- Modify: `src/modern_pos/dao/BarangDAO.java` (`tambahBarang`, `updateBarang`), `src/modern_pos/controller/StockController.java` (`simpanBarang`), `src/modern_pos/view/StockView.java` (`showFormDialog`)
- Test: `test/modern_pos/dao/BarangDAOTest.java`

**Interfaces:**
- Consumes: `StokDAO.ubahStok`, `Tx.rollbackQuietly`.
- Produces:
  - `BarangDAO.tambahBarang(Barang b) throws SQLException` — stok < 0 → `IllegalArgumentException("Stok tidak boleh negatif")`.
  - `BarangDAO.updateBarang(Barang b, int stokSebelumEdit) throws SQLException` — **signature baru**.
  - `StockController.simpanBarang(Barang baru, Barang lama)` — `lama == null` berarti tambah.

- [ ] **Step 1: Tambah test gagal ke `BarangDAOTest.java`**

Tambah import `import modern_pos.model.Barang;` (sudah ada) dan helper + test:

```java
    private static Barang barang(String kode, String nama, int harga, int stok) {
        Barang b = new Barang();
        b.setKodeBarang(kode);
        b.setNamaBarang(nama);
        b.setHarga(harga);
        b.setStok(stok);
        return b;
    }

    @Test
    public void tambahBarangStokAwalMencatatMasuk() throws Exception {
        dao.tambahBarang(barang("B003", "Minyak 1L", 18000, 5));
        assertEquals(5, TestDb.queryInt("SELECT stok FROM barang WHERE kode_barang='B003'"));
        assertEquals("MASUK", TestDb.queryString("SELECT tipe_gerakan FROM stock_movement_log WHERE kode_barang='B003'"));
        assertEquals(0, TestDb.queryInt("SELECT stok_sebelum FROM stock_movement_log WHERE kode_barang='B003'"));
        assertEquals("Stok awal", TestDb.queryString("SELECT keterangan FROM stock_movement_log WHERE kode_barang='B003'"));
    }

    @Test
    public void tambahBarangStokNolTanpaLog() throws Exception {
        dao.tambahBarang(barang("B003", "Minyak 1L", 18000, 0));
        assertEquals(0, TestDb.queryInt("SELECT COUNT(*) FROM stock_movement_log"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void stokAwalNegatifDitolak() throws Exception {
        dao.tambahBarang(barang("B003", "Minyak 1L", 18000, -1));
    }

    @Test
    public void editStokMencatatKoreksi() throws Exception {
        dao.updateBarang(barang("B001", "Beras 5kg", 65000, 7), 10);
        assertEquals(7, TestDb.queryInt("SELECT stok FROM barang WHERE kode_barang='B001'"));
        assertEquals("KOREKSI", TestDb.queryString("SELECT tipe_gerakan FROM stock_movement_log"));
        assertEquals(3, TestDb.queryInt("SELECT qty FROM stock_movement_log"));
        assertEquals(10, TestDb.queryInt("SELECT stok_sebelum FROM stock_movement_log"));
        assertEquals(7, TestDb.queryInt("SELECT stok_sesudah FROM stock_movement_log"));
    }

    @Test
    public void editTanpaUbahStokTanpaLog() throws Exception {
        dao.updateBarang(barang("B001", "Beras Premium 5kg", 70000, 10), 10);
        assertEquals("Beras Premium 5kg", TestDb.queryString("SELECT nama_barang FROM barang WHERE kode_barang='B001'"));
        assertEquals(70000, TestDb.queryInt("SELECT harga_jual FROM barang WHERE kode_barang='B001'"));
        assertEquals(0, TestDb.queryInt("SELECT COUNT(*) FROM stock_movement_log"));
    }

    @Test
    public void editMemakaiSelisihBukanAngkaMutlak() throws Exception {
        // Form dibuka saat stok 10; sebelum disimpan, kasir lain menjual 2 (stok DB jadi 8).
        try (java.sql.Connection c = config.koneksi.open(); java.sql.Statement s = c.createStatement()) {
            s.executeUpdate("UPDATE barang SET stok = 8 WHERE kode_barang='B001'");
        }
        // User mengetik 12 (niatnya +2 dari yang ia lihat).
        dao.updateBarang(barang("B001", "Beras 5kg", 65000, 12), 10);
        assertEquals(10, TestDb.queryInt("SELECT stok FROM barang WHERE kode_barang='B001'"));
    }
```

- [ ] **Step 2: Jalankan, pastikan gagal**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: compile error `method updateBarang in class BarangDAO cannot be applied to given types`.

- [ ] **Step 3: Ganti `tambahBarang` dan `updateBarang` di `BarangDAO.java`**

```java
    public void tambahBarang(Barang b) throws SQLException {
        if (b.getStok() < 0) throw new IllegalArgumentException("Stok tidak boleh negatif");
        String sql = "INSERT INTO barang (kode_barang, nama_barang, harga_jual, stok) VALUES (?, ?, ?, 0)";
        try (Connection con = koneksi.open()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, b.getKodeBarang());
                    ps.setString(2, b.getNamaBarang());
                    ps.setInt(3, b.getHarga());
                    ps.executeUpdate();
                }
                StokDAO.ubahStok(con, b.getKodeBarang(), b.getStok(), "MASUK", null, "Stok awal");
                con.commit();
            } catch (SQLException e) {
                throw Tx.rollbackQuietly(con, e);
            }
        }
    }

    // stokSebelumEdit = stok yang tampil saat form dibuka. Yang diterapkan hanya selisihnya,
    // jadi penjualan yang terjadi selama form terbuka tidak tertimpa.
    public void updateBarang(Barang b, int stokSebelumEdit) throws SQLException {
        String sql = "UPDATE barang SET nama_barang=?, harga_jual=? WHERE kode_barang=?";
        try (Connection con = koneksi.open()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, b.getNamaBarang());
                    ps.setInt(2, b.getHarga());
                    ps.setString(3, b.getKodeBarang());
                    ps.executeUpdate();
                }
                StokDAO.ubahStok(con, b.getKodeBarang(), b.getStok() - stokSebelumEdit, "KOREKSI", null, "Koreksi manual");
                con.commit();
            } catch (SQLException e) {
                throw Tx.rollbackQuietly(con, e);
            }
        }
    }
```

- [ ] **Step 4: `StockController.simpanBarang(Barang baru, Barang lama)`**

Ganti seluruh method `simpanBarang` dengan:

```java
    // lama == null berarti tambah barang baru.
    public void simpanBarang(final Barang baru, final Barang lama) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                if (lama != null) dao.updateBarang(baru, lama.getStok());
                else dao.tambahBarang(baru);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    view.showSuccess("Data barang berhasil disimpan!");
                    loadData("");
                } catch (Exception ex) {
                    Throwable c = ex.getCause() != null ? ex.getCause() : ex;
                    view.showError("Gagal menyimpan: " + c.getMessage());
                    loadData("");
                }
            }
        };
        worker.execute();
    }
```

- [ ] **Step 5: `StockView.showFormDialog`**

Ganti label `"Stok Awal:"` dengan `isEdit ? "Stok:" : "Stok Awal:"`, dan ganti `controller.simpanBarang(newB, isEdit);` dengan:

```java
                controller.simpanBarang(newB, b);
```

- [ ] **Step 6: Jalankan, pastikan lulus**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: `OK (32 tests)`.

- [ ] **Step 7: Commit**

```powershell
git add src/modern_pos test/modern_pos/dao/BarangDAOTest.java
git -c user.name="attia" -c user.email="attiaitem@gmail.com" commit -m "feat(modern_pos): log initial stock and relative stock corrections" -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 5: Utang punya qty dan menggerakkan stok

**Files:**
- Create: `migrations/2026-09-22-utang-qty.sql`
- Modify: `schema.sql` (dump ulang), `src/modern_pos/model/Utang.java`, `src/modern_pos/dao/UtangDAO.java` (`getUtangList`, `tambahUtang`, `updateUtang`, `hapusUtang`), `src/modern_pos/view/UtangView.java`
- Test: `test/modern_pos/dao/UtangDAOTest.java`

**Interfaces:**
- Consumes: `StokDAO.ubahStok`, `Tx.rollbackQuietly`, `TransaksiDAO.newKodeTransaksi()`.
- Produces: `Utang.getQty()/setQty(int)` (default 1). `tambahUtang`/`updateUtang` menolak `qty < 1` dengan `IllegalArgumentException("Qty minimal 1")`. `updateUtang` kode tidak ada → `SQLException("Utang <kode> tidak ditemukan")`.

- [ ] **Step 1: Migrasi DB test + schema**

Buat `migrations/2026-09-22-utang-qty.sql`:
```sql
-- Sub-proyek 2a: utang punya qty. Utang lama otomatis qty 1.
ALTER TABLE utang ADD COLUMN qty INT NOT NULL DEFAULT 1 AFTER kode_barang;
```

Terapkan ke DB **test saja** dan dump ulang skema:
```powershell
$m = "C:\laragon\bin\mysql\mysql-8.4.3-winx64\bin"
& "$m\mysql.exe" -uroot sarimurnirejeki_test -e "source migrations/2026-09-22-utang-qty.sql"
& "$m\mysqldump.exe" -uroot --no-data --skip-comments --result-file=schema.sql sarimurnirejeki_test
Select-String -Path schema.sql -Pattern '`qty` int NOT NULL DEFAULT'
```
Expected: 1 baris cocok.

- [ ] **Step 2: Tulis test gagal di `UtangDAOTest.java`**

Ganti helper `utang(...)` dengan dua overload:

```java
    private static Utang utang(String kode, String kodeBarang, int harga, int dp) {
        return utang(kode, kodeBarang, harga, dp, 1);
    }

    private static Utang utang(String kode, String kodeBarang, int harga, int dp, int qty) {
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
        u.setQty(qty);
        return u;
    }

    private static int stok(String kode) throws Exception {
        return TestDb.queryInt("SELECT stok FROM barang WHERE kode_barang=?", kode);
    }
```

Tambah import `import java.sql.SQLException;` lalu test:

```java
    @Test
    public void tambahUtangMengurangiStokSebanyakQty() throws Exception {
        dao.tambahUtang(utang("UTG-1", "B001", 130000, 30000, 2));

        assertEquals(8, stok("B001"));
        assertEquals(2, TestDb.queryInt("SELECT qty FROM utang WHERE kode_utang='UTG-1'"));
        String kodeTrx = TestDb.queryString("SELECT kode_transaksi FROM utang WHERE kode_utang='UTG-1'");
        assertEquals(1, TestDb.queryInt("SELECT COUNT(*) FROM stock_movement_log WHERE tipe_gerakan='KELUAR' AND qty=2 AND kode_transaksi=?", kodeTrx));
        assertEquals(2, dao.getUtangList("").get(0).getQty());
    }

    @Test
    public void tambahUtangStokKurangDitolakTanpaSisaData() throws Exception {
        try {
            dao.tambahUtang(utang("UTG-1", "B002", 45000, 0, 3));
            fail("harus ditolak");
        } catch (SQLException e) {
            assertEquals("Stok Gula 1kg tidak cukup", e.getMessage());
        }
        assertEquals(2, stok("B002"));
        assertEquals(0, TestDb.queryInt("SELECT COUNT(*) FROM utang"));
        assertEquals(0, TestDb.queryInt("SELECT COUNT(*) FROM transaksi"));
        assertEquals(0, TestDb.queryInt("SELECT COUNT(*) FROM log_transaksi"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void qtyNolDitolak() throws Exception {
        dao.tambahUtang(utang("UTG-1", "B001", 65000, 0, 0));
    }

    @Test
    public void editQtyNaikTurunMenggerakkanSelisih() throws Exception {
        dao.tambahUtang(utang("UTG-1", "B001", 130000, 0, 2));   // 10 -> 8
        dao.updateUtang(utang("UTG-1", "B001", 325000, 0, 5));   // +3 -> 5
        assertEquals(5, stok("B001"));
        dao.updateUtang(utang("UTG-1", "B001", 65000, 0, 1));    // -4 -> 9
        assertEquals(9, stok("B001"));
        assertEquals(1, TestDb.queryInt("SELECT qty FROM utang WHERE kode_utang='UTG-1'"));
    }

    @Test
    public void editQtyMelebihiStokDitolakTanpaPerubahan() throws Exception {
        dao.tambahUtang(utang("UTG-1", "B002", 15000, 0, 1));    // 2 -> 1
        try {
            dao.updateUtang(utang("UTG-1", "B002", 45000, 0, 3)); // butuh +2, sisa 1
            fail("harus ditolak");
        } catch (SQLException e) {
            assertEquals("Stok Gula 1kg tidak cukup", e.getMessage());
        }
        assertEquals(1, stok("B002"));
        assertEquals(1, TestDb.queryInt("SELECT qty FROM utang WHERE kode_utang='UTG-1'"));
        assertEquals(15000, TestDb.queryInt("SELECT harga_brng FROM utang WHERE kode_utang='UTG-1'"));
    }

    @Test
    public void editGantiBarangMemindahkanStok() throws Exception {
        dao.tambahUtang(utang("UTG-1", "B001", 130000, 0, 2));   // B001 10 -> 8
        dao.updateUtang(utang("UTG-1", "B002", 15000, 0, 1));    // B001 -> 10, B002 2 -> 1
        assertEquals(10, stok("B001"));
        assertEquals(1, stok("B002"));
    }

    @Test(expected = SQLException.class)
    public void editUtangTidakAdaDitolak() throws Exception {
        dao.updateUtang(utang("TIDAK-ADA", "B001", 65000, 0, 1));
    }

    @Test
    public void hapusUtangBelumLunasMengembalikanStok() throws Exception {
        dao.tambahUtang(utang("UTG-1", "B001", 130000, 0, 2));
        dao.hapusUtang("UTG-1");
        assertEquals(10, stok("B001"));
        assertEquals(1, TestDb.queryInt("SELECT COUNT(*) FROM stock_movement_log WHERE tipe_gerakan='MASUK' AND qty=2"));
    }

    @Test
    public void hapusUtangLunasTidakMengembalikanStok() throws Exception {
        dao.tambahUtang(utang("UTG-1", "B001", 130000, 0, 2));
        dao.tandaiLunas("UTG-1");
        dao.hapusUtang("UTG-1");
        assertEquals(8, stok("B001"));
    }
```

- [ ] **Step 3: Jalankan, pastikan gagal**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: compile error `cannot find symbol ... setQty`.

- [ ] **Step 4: `Utang.qty`**

Di `src/modern_pos/model/Utang.java` tambah field dan accessor:

```java
    private int qty = 1;

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
```

- [ ] **Step 5: Jalankan, pastikan gagal di assertion (bukan compile)**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: FAIL di antaranya `tambahUtangMengurangiStokSebanyakQty` (`expected:<8> but was:<10>`) dan `qtyNolDitolak`.

- [ ] **Step 6: `UtangDAO` — qty dan stok**

Di `getUtangList`, setelah `u.setKodeBarang(rs.getString("kode_barang"));` tambah:
```java
                    u.setQty(rs.getInt("qty"));
```

Ganti `tambahUtang`:

```java
    public void tambahUtang(Utang u) throws SQLException {
        if (u.getQty() < 1) throw new IllegalArgumentException("Qty minimal 1");
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
                String sql = "INSERT INTO utang (kode_utang, nama, alamat, telepon, harga_brng, dp, jumlah_cicilan, jatuh_tempo, status, kode_barang, qty, kode_transaksi) VALUES (?,?,?,?,?,?,?,?,'belum',?,?,?)";
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
                    ps.setInt(10, u.getQty());
                    ps.setString(11, kodeTrx);
                    ps.executeUpdate();
                }

                // 3. Barang dibawa pulang pelanggan: stok berkurang
                StokDAO.ubahStok(con, u.getKodeBarang(), -u.getQty(), "KELUAR", kodeTrx, "Utang " + u.getKodeUtang());

                // 4. Log
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
                throw Tx.rollbackQuietly(con, e);
            }
        }
    }
```

Ganti `updateUtang`:

```java
    public void updateUtang(Utang u) throws SQLException {
        if (u.getQty() < 1) throw new IllegalArgumentException("Qty minimal 1");
        String ket = "Edit utang " + u.getKodeUtang();
        try (Connection con = koneksi.open()) {
            con.setAutoCommit(false);
            try {
                String barangLama;
                int qtyLama;
                try (PreparedStatement ps = con.prepareStatement("SELECT kode_barang, qty FROM utang WHERE kode_utang = ? FOR UPDATE")) {
                    ps.setString(1, u.getKodeUtang());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Utang " + u.getKodeUtang() + " tidak ditemukan");
                        barangLama = rs.getString("kode_barang");
                        qtyLama = rs.getInt("qty");
                    }
                }

                String sql = "UPDATE utang SET nama=?, alamat=?, telepon=?, harga_brng=?, dp=?, jumlah_cicilan=?, jatuh_tempo=?, kode_barang=?, qty=? WHERE kode_utang=?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, u.getNama());
                    ps.setString(2, u.getAlamat());
                    ps.setString(3, u.getTelepon());
                    ps.setInt(4, u.getHargaBarang());
                    ps.setInt(5, u.getDp());
                    ps.setInt(6, u.getJumlahCicilan());
                    ps.setDate(7, u.getJatuhTempo() != null ? java.sql.Date.valueOf(u.getJatuhTempo()) : null);
                    ps.setString(8, u.getKodeBarang());
                    ps.setInt(9, u.getQty());
                    ps.setString(10, u.getKodeUtang());
                    ps.executeUpdate();
                }

                // Edit = koreksi atas barang yang dibawa pelanggan, apa pun status utangnya.
                if (barangLama.equals(u.getKodeBarang())) {
                    int kembali = qtyLama - u.getQty();
                    StokDAO.ubahStok(con, barangLama, kembali, kembali > 0 ? "MASUK" : "KELUAR", null, ket);
                } else {
                    StokDAO.ubahStok(con, barangLama, qtyLama, "MASUK", null, ket);
                    StokDAO.ubahStok(con, u.getKodeBarang(), -u.getQty(), "KELUAR", null, ket);
                }

                con.commit();
            } catch (SQLException e) {
                throw Tx.rollbackQuietly(con, e);
            }
        }
    }
```

Ganti `hapusUtang`:

```java
    public void hapusUtang(String kode) throws SQLException {
        try (Connection con = koneksi.open()) {
            con.setAutoCommit(false);
            try {
                String kodeBarang = null, status = null;
                int qty = 0;
                try (PreparedStatement ps = con.prepareStatement("SELECT kode_barang, qty, status FROM utang WHERE kode_utang = ? FOR UPDATE")) {
                    ps.setString(1, kode);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            kodeBarang = rs.getString("kode_barang");
                            qty = rs.getInt("qty");
                            status = rs.getString("status");
                        }
                    }
                }

                try (PreparedStatement ps = con.prepareStatement("DELETE FROM utang WHERE kode_utang = ?")) {
                    ps.setString(1, kode);
                    ps.executeUpdate();
                }

                // Belum lunas: barang dianggap batal dibawa, stok kembali. Lunas: barang sudah milik pelanggan.
                if ("belum".equals(status)) {
                    StokDAO.ubahStok(con, kodeBarang, qty, "MASUK", null, "Utang dihapus " + kode);
                }

                con.commit();
            } catch (SQLException e) {
                throw Tx.rollbackQuietly(con, e);
            }
        }
    }
```

- [ ] **Step 7: Jalankan, pastikan lulus**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: `OK (41 tests)`.

- [ ] **Step 8: `UtangView` — field Qty, total otomatis, kolom Qty**

Di `showFormDialog`, setelah blok `try { for (Barang b : controller.daftarBarang()) ... }` tambah:

```java
        JTextField txtQty = SwingHelper.createMaterialTextField();
        txtQty.setText(isEdit ? String.valueOf(u.getQty()) : "1");
```

Setelah blok `if (isEdit) { ... } else { ... }` (pengisian nilai awal) tambah:

```java
        // Total utang otomatis harga_jual x qty saat barang/qty diubah; tetap bisa diketik manual (harga kredit).
        Runnable isiTotal = () -> {
            Barang dipilih = (Barang) cmbBarang.getSelectedItem();
            String q = txtQty.getText().replaceAll("[^0-9]", "");
            if (dipilih != null && !q.isEmpty()) txtHarga.setText(String.valueOf(dipilih.getHarga() * Integer.parseInt(q)));
        };
        cmbBarang.addActionListener(e -> isiTotal.run());
        txtQty.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { isiTotal.run(); }
        });
        if (!isEdit) isiTotal.run();
```

Ganti `new GridLayout(8, 2, 10, 10)` jadi `new GridLayout(9, 2, 10, 10)`. Setelah baris label `"Barang:"` tambah:

```java
        panel.add(SwingHelper.createLabel("Qty:", UITheme.FONT_BODY, UITheme.COLOR_TEXT_PRIMARY)); panel.add(txtQty);
```

Di blok validasi, setelah `newU.setKodeBarang(barangDipilih.getKodeBarang());` tambah:

```java
                    String qStr = txtQty.getText().replaceAll("[^0-9]", "");
                    int qty = qStr.isEmpty() ? 0 : Integer.parseInt(qStr);
                    if (qty < 1) throw new Exception("Qty minimal 1!");
                    newU.setQty(qty);
```

Tabel: ganti header `new String[]{"Kode", "Nama", "Telepon", "Total", ...}` jadi `new String[]{"Kode", "Nama", "Telepon", "Qty", "Total", "DP", "Sisa Cicilan", "Jatuh Tempo", "Status"}`, dan di `populateTable` ganti `u.getKodeUtang(), u.getNama(), u.getTelepon(),` jadi `u.getKodeUtang(), u.getNama(), u.getTelepon(), u.getQty(),`.

- [ ] **Step 9: Compile + test**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: `OK (41 tests)`.

- [ ] **Step 10: Commit**

```powershell
git add migrations schema.sql src/modern_pos test/modern_pos/dao/UtangDAOTest.java
git -c user.name="attia" -c user.email="attiaitem@gmail.com" commit -m "feat(modern_pos): utang qty moves stock on add/edit/delete" -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 6: Layar Stock — Restock dan Riwayat

**Files:**
- Create: `src/modern_pos/view/RiwayatStokDialog.java`
- Modify: `src/modern_pos/controller/StockController.java`, `src/modern_pos/view/StockView.java`

**Interfaces:**
- Consumes: `StokDAO.restock`, `StokDAO.riwayat`, `StockMovement` (Task 2).
- Produces: `StockController.restock(String kode, int qty, String ket)`, `StockController.tampilRiwayat(Barang b)`, `StockView.showRiwayat(Barang b, List<StockMovement> list)`.

Tidak ada test otomatis (wiring UI; DAO sudah dites di Task 2).

- [ ] **Step 1: `RiwayatStokDialog`**

```java
package modern_pos.view;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import modern_pos.model.Barang;
import modern_pos.model.StockMovement;
import modern_pos.utils.SwingHelper;
import modern_pos.utils.UITheme;

public class RiwayatStokDialog extends JDialog {
    public RiwayatStokDialog(Frame parent, Barang b, List<StockMovement> list) {
        super(parent, "Riwayat Stok - " + b.getKodeBarang() + " " + b.getNamaBarang(), true);
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Waktu", "Tipe", "Qty", "Stok Sebelum", "Stok Sesudah", "Kode Transaksi", "Keterangan"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (StockMovement m : list) {
            model.addRow(new Object[]{ m.getWaktu(), m.getTipe(), m.getQty(), m.getStokSebelum(),
                    m.getStokSesudah(), m.getKodeTransaksi() != null ? m.getKodeTransaksi() : "-", m.getKeterangan() });
        }
        JTable tbl = new JTable(model);
        SwingHelper.styleTable(tbl);
        JScrollPane sp = new JScrollPane(tbl);
        sp.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setLayout(new BorderLayout());
        add(SwingHelper.createLabel(list.isEmpty() ? "  Belum ada gerakan stok." : "  " + list.size() + " gerakan terbaru",
                UITheme.FONT_BODY, UITheme.COLOR_TEXT_SECONDARY), BorderLayout.NORTH);
        add(sp, BorderLayout.CENTER);
        setSize(900, 450);
        setLocationRelativeTo(parent);
    }
}
```

- [ ] **Step 2: `StockController`**

Tambah import:
```java
import modern_pos.dao.StokDAO;
import modern_pos.model.StockMovement;
```

Tambah field `private final StokDAO stokDAO = new StokDAO();` dan method:

```java
    public void restock(final String kode, final int qty, final String ket) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                stokDAO.restock(kode, qty, ket);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    view.showSuccess("Restock berhasil!");
                } catch (Exception ex) {
                    Throwable c = ex.getCause() != null ? ex.getCause() : ex;
                    view.showError("Gagal restock: " + c.getMessage());
                }
                loadData("");
            }
        };
        worker.execute();
    }

    public void tampilRiwayat(final Barang b) {
        SwingWorker<List<StockMovement>, Void> worker = new SwingWorker<List<StockMovement>, Void>() {
            @Override protected List<StockMovement> doInBackground() throws Exception {
                return stokDAO.riwayat(b.getKodeBarang());
            }
            @Override protected void done() {
                try {
                    view.showRiwayat(b, get());
                } catch (Exception ex) {
                    Throwable c = ex.getCause() != null ? ex.getCause() : ex;
                    view.showError("Gagal memuat riwayat: " + c.getMessage());
                }
            }
        };
        worker.execute();
    }
```

- [ ] **Step 3: `StockView` — tombol dan handler**

Tambah import `import modern_pos.model.StockMovement;`.

Setelah deklarasi `btnSnapshot`, tambah:
```java
        JButton btnRestock = SwingHelper.createFlatButton("Restock", UITheme.COLOR_SUCCESS, new Color(56, 142, 60));
        JButton btnRiwayat = SwingHelper.createFlatButton("Riwayat", UITheme.COLOR_WARNING, new Color(230, 81, 0));
        btnRestock.setPreferredSize(new Dimension(110, 35));
        btnRiwayat.setPreferredSize(new Dimension(110, 35));
```

Ganti `actionPanel.add(btnSnapshot);` dengan:
```java
        actionPanel.add(btnRiwayat);
        actionPanel.add(btnRestock);
        actionPanel.add(btnSnapshot);
```

Sebelum `btnSnapshot.addActionListener(...)`, tambah:
```java
        btnRestock.addActionListener(e -> {
            int row = tblStock.getSelectedRow();
            if (row == -1) { showError("Pilih barang yang ingin di-restock!"); return; }
            Barang b = currentList.get(row);
            JTextField txtQty = SwingHelper.createMaterialTextField();
            JTextField txtKet = SwingHelper.createMaterialTextField();
            JPanel p = new JPanel(new GridLayout(2, 2, 10, 10));
            p.add(SwingHelper.createLabel("Qty masuk:", UITheme.FONT_BODY, UITheme.COLOR_TEXT_PRIMARY)); p.add(txtQty);
            p.add(SwingHelper.createLabel("Keterangan:", UITheme.FONT_BODY, UITheme.COLOR_TEXT_PRIMARY)); p.add(txtKet);
            int res = JOptionPane.showConfirmDialog(this, p, "Restock " + b.getNamaBarang(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION) return;
            String q = txtQty.getText().replaceAll("[^0-9]", "");
            int qty = q.isEmpty() ? 0 : Integer.parseInt(q);
            if (qty <= 0) { showError("Qty restock harus lebih dari 0!"); return; }
            controller.restock(b.getKodeBarang(), qty, txtKet.getText());
        });

        btnRiwayat.addActionListener(e -> {
            int row = tblStock.getSelectedRow();
            if (row == -1) { showError("Pilih barang yang ingin dilihat riwayatnya!"); return; }
            controller.tampilRiwayat(currentList.get(row));
        });
```

Tambah method publik:
```java
    public void showRiwayat(Barang b, List<StockMovement> list) {
        new RiwayatStokDialog(this, b, list).setVisible(true);
    }
```

- [ ] **Step 4: Compile + test**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: `OK (41 tests)`.

- [ ] **Step 5: Commit**

```powershell
git add src/modern_pos
git -c user.name="attia" -c user.email="attiaitem@gmail.com" commit -m "feat(modern_pos): restock and per-item stock history on Stock screen" -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 7: Snapshot tanpa stack lama

**Files:**
- Create: `src/modern_pos/dao/SnapshotDAO.java`, `src/modern_pos/model/StockSnapshot.java`, `src/modern_pos/view/SnapshotDialog.java`, `test/modern_pos/dao/SnapshotDAOTest.java`
- Modify: `src/modern_pos/view/StockView.java` (handler `btnSnapshot`)

**Interfaces:**
- Produces: `SnapshotDAO.ambilSnapshotBulan(YearMonth) throws SQLException`, `SnapshotDAO.sudahAdaSnapshot(YearMonth) throws SQLException` → boolean, `SnapshotDAO.getSnapshotByBulan(YearMonth) throws SQLException` → `List<StockSnapshot>` urut `kode_barang`.

- [ ] **Step 1: Tulis test gagal `test/modern_pos/dao/SnapshotDAOTest.java`**

```java
package modern_pos.dao;

import java.time.YearMonth;
import java.util.List;
import modern_pos.TestDb;
import modern_pos.model.StockSnapshot;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class SnapshotDAOTest {
    private final SnapshotDAO dao = new SnapshotDAO();
    private final YearMonth sept = YearMonth.of(2026, 9);

    @Before
    public void setUp() throws Exception {
        TestDb.reset();
    }

    @Test
    public void ambilSnapshotMenyalinSemuaBarang() throws Exception {
        assertFalse(dao.sudahAdaSnapshot(sept));
        dao.ambilSnapshotBulan(sept);
        assertTrue(dao.sudahAdaSnapshot(sept));

        List<StockSnapshot> list = dao.getSnapshotByBulan(sept);
        assertEquals(2, list.size());
        assertEquals("B001", list.get(0).getKodeBarang());
        assertEquals(10, list.get(0).getStokSnapshot());
        assertEquals(65000, list.get(0).getHargaJual());
        assertEquals(sept.atDay(1), list.get(0).getPeriode());
    }

    @Test
    public void ambilUlangMengupdateTanpaDuplikat() throws Exception {
        dao.ambilSnapshotBulan(sept);
        try (java.sql.Connection c = config.koneksi.open(); java.sql.Statement s = c.createStatement()) {
            s.executeUpdate("UPDATE barang SET stok = 4 WHERE kode_barang='B001'");
        }
        dao.ambilSnapshotBulan(sept);

        assertEquals(2, TestDb.queryInt("SELECT COUNT(*) FROM stock_snapshot"));
        assertEquals(4, dao.getSnapshotByBulan(sept).get(0).getStokSnapshot());
    }

    @Test
    public void satuanDiambilDariBarang() throws Exception {
        try (java.sql.Connection c = config.koneksi.open(); java.sql.Statement s = c.createStatement()) {
            s.executeUpdate("UPDATE barang SET satuan = 'karung' WHERE kode_barang='B001'");
        }
        dao.ambilSnapshotBulan(sept);
        List<StockSnapshot> list = dao.getSnapshotByBulan(sept);
        assertEquals("karung", list.get(0).getSatuan());
        assertEquals("pcs", list.get(1).getSatuan());
    }

    @Test
    public void bulanLainTidakTercampur() throws Exception {
        dao.ambilSnapshotBulan(sept);
        assertTrue(dao.getSnapshotByBulan(YearMonth.of(2026, 8)).isEmpty());
    }
}
```

- [ ] **Step 2: Jalankan, pastikan gagal**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: compile error `cannot find symbol ... SnapshotDAO`.

- [ ] **Step 3: `StockSnapshot` model**

```java
package modern_pos.model;

import java.time.LocalDate;

public class StockSnapshot {
    private LocalDate periode;
    private String kodeBarang, namaBarang, satuan;
    private int hargaJual, stokSnapshot;

    public LocalDate getPeriode() { return periode; }
    public void setPeriode(LocalDate periode) { this.periode = periode; }
    public String getKodeBarang() { return kodeBarang; }
    public void setKodeBarang(String kodeBarang) { this.kodeBarang = kodeBarang; }
    public String getNamaBarang() { return namaBarang; }
    public void setNamaBarang(String namaBarang) { this.namaBarang = namaBarang; }
    public String getSatuan() { return satuan; }
    public void setSatuan(String satuan) { this.satuan = satuan; }
    public int getHargaJual() { return hargaJual; }
    public void setHargaJual(int hargaJual) { this.hargaJual = hargaJual; }
    public int getStokSnapshot() { return stokSnapshot; }
    public void setStokSnapshot(int stokSnapshot) { this.stokSnapshot = stokSnapshot; }
}
```

- [ ] **Step 4: `SnapshotDAO`**

```java
package modern_pos.dao;
import config.koneksi;
import modern_pos.model.StockSnapshot;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class SnapshotDAO {

    // Idempotent: snapshot bulan yang sama di-update, bukan diduplikasi (UNIQUE periode+kode_barang).
    public void ambilSnapshotBulan(YearMonth bulan) throws SQLException {
        String sql = "INSERT INTO stock_snapshot (periode, kode_barang, nama_barang, harga_jual, satuan, stok_snapshot) "
                + "SELECT ?, kode_barang, nama_barang, harga_jual, COALESCE(satuan, 'pcs'), stok FROM barang "
                + "ON DUPLICATE KEY UPDATE stok_snapshot = VALUES(stok_snapshot), nama_barang = VALUES(nama_barang), "
                + "harga_jual = VALUES(harga_jual), satuan = VALUES(satuan), diperbarui_pada = CURRENT_TIMESTAMP";
        try (Connection con = koneksi.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(bulan.atDay(1)));
            ps.executeUpdate();
        }
    }

    public boolean sudahAdaSnapshot(YearMonth bulan) throws SQLException {
        try (Connection con = koneksi.open();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM stock_snapshot WHERE periode = ?")) {
            ps.setDate(1, Date.valueOf(bulan.atDay(1)));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public List<StockSnapshot> getSnapshotByBulan(YearMonth bulan) throws SQLException {
        List<StockSnapshot> list = new ArrayList<>();
        String sql = "SELECT periode, kode_barang, nama_barang, harga_jual, satuan, stok_snapshot "
                + "FROM stock_snapshot WHERE periode = ? ORDER BY kode_barang ASC";
        try (Connection con = koneksi.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(bulan.atDay(1)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StockSnapshot s = new StockSnapshot();
                    s.setPeriode(rs.getDate("periode").toLocalDate());
                    s.setKodeBarang(rs.getString("kode_barang"));
                    s.setNamaBarang(rs.getString("nama_barang"));
                    s.setHargaJual(rs.getInt("harga_jual"));
                    s.setSatuan(rs.getString("satuan"));
                    s.setStokSnapshot(rs.getInt("stok_snapshot"));
                    list.add(s);
                }
            }
        }
        return list;
    }
}
```

- [ ] **Step 5: Jalankan, pastikan lulus**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: `OK (45 tests)`.

- [ ] **Step 6: `SnapshotDialog` (port `gui.snapshotDialog`)**

Buat `src/modern_pos/view/SnapshotDialog.java`:

```java
package modern_pos.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.sql.SQLException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import modern_pos.dao.SnapshotDAO;
import modern_pos.model.StockSnapshot;

// Port gui.snapshotDialog ke SnapshotDAO.
// ponytail: query DB sinkron di EDT; tabel barang kecil. Pindah ke SwingWorker bila terasa lambat.
public class SnapshotDialog extends JDialog {

    private final SnapshotDAO dao = new SnapshotDAO();

    private JComboBox<String> cboBulan;
    private JLabel lblStatus;
    private DefaultTableModel tblModel;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("id", "ID"));

    public SnapshotDialog(Frame parent) {
        super(parent, "Snapshot Stok Bulanan", true);
        initUI();
        setSize(900, 540);
        setLocationRelativeTo(parent);
        updateStatus(YearMonth.now());
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(242, 246, 250));

        JLabel lblTitle = new JLabel("Snapshot Stok Bulanan", JLabel.LEFT);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(new Color(23, 118, 211));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(12, 16, 4, 0));
        add(lblTitle, BorderLayout.NORTH);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        toolbar.setBackground(new Color(220, 235, 255));

        cboBulan = new JComboBox<>();
        cboBulan.setPreferredSize(new Dimension(200, 30));
        YearMonth now = YearMonth.now();
        for (int i = 0; i < 12; i++) cboBulan.addItem(now.minusMonths(i).format(FMT));

        JButton btnSimpan = buatTombol("Simpan Snapshot", new Color(23, 118, 211));
        JButton btnLihat = buatTombol("Lihat Data", new Color(40, 167, 69));

        lblStatus = new JLabel("Pilih bulan lalu klik tombol.");
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 11));

        toolbar.add(new JLabel("Pilih Bulan:"));
        toolbar.add(cboBulan);
        toolbar.add(btnSimpan);
        toolbar.add(btnLihat);
        toolbar.add(Box.createHorizontalStrut(16));
        toolbar.add(lblStatus);
        add(toolbar, BorderLayout.AFTER_LAST_LINE);

        tblModel = new DefaultTableModel(
                new String[]{"Kode Barang", "Nama Barang", "Satuan", "Stok Snapshot", "Harga Jual", "Periode"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tbl = new JTable(tblModel);
        tbl.setRowHeight(26);
        JScrollPane sp = new JScrollPane(tbl);
        sp.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));
        add(sp, BorderLayout.CENTER);

        btnSimpan.addActionListener(e -> aksiSimpan());
        btnLihat.addActionListener(e -> aksiLihat());
        cboBulan.addActionListener(e -> updateStatus(getSelectedYM()));
    }

    private void aksiSimpan() {
        YearMonth ym = getSelectedYM();
        try {
            if (dao.sudahAdaSnapshot(ym)) {
                int c = JOptionPane.showConfirmDialog(this,
                        "Snapshot " + ym.format(FMT) + " sudah ada.\nTimpa dengan data stok terkini?",
                        "Konfirmasi Timpa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (c != JOptionPane.YES_OPTION) return;
            }
            dao.ambilSnapshotBulan(ym);
            JOptionPane.showMessageDialog(this, "Snapshot " + ym.format(FMT) + " berhasil disimpan!",
                    "Berhasil", JOptionPane.INFORMATION_MESSAGE);
            loadTabel(ym);
        } catch (SQLException ex) {
            tampilError(ex);
        }
        updateStatus(ym);
    }

    private void aksiLihat() {
        YearMonth ym = getSelectedYM();
        try {
            loadTabel(ym);
        } catch (SQLException ex) {
            tampilError(ex);
        }
    }

    private YearMonth getSelectedYM() {
        return YearMonth.now().minusMonths(Math.max(0, cboBulan.getSelectedIndex()));
    }

    private void loadTabel(YearMonth ym) throws SQLException {
        tblModel.setRowCount(0);
        List<StockSnapshot> list = dao.getSnapshotByBulan(ym);
        for (StockSnapshot s : list) {
            tblModel.addRow(new Object[]{ s.getKodeBarang(), s.getNamaBarang(), s.getSatuan(),
                    s.getStokSnapshot(), s.getHargaJual(), s.getPeriode().format(FMT) });
        }
        if (list.isEmpty()) setStatus("Belum ada snapshot " + ym.format(FMT) + ". Klik 'Simpan Snapshot'.", new Color(180, 80, 0));
        else setStatus(list.size() + " item ditampilkan untuk " + ym.format(FMT) + ".", new Color(30, 140, 60));
    }

    private void updateStatus(YearMonth ym) {
        try {
            if (dao.sudahAdaSnapshot(ym)) setStatus("Snapshot " + ym.format(FMT) + " sudah tersedia.", new Color(30, 140, 60));
            else setStatus("Belum ada snapshot " + ym.format(FMT) + ".", new Color(180, 80, 0));
        } catch (SQLException ex) {
            setStatus("Gagal membaca snapshot: " + ex.getMessage(), new Color(198, 40, 40));
        }
    }

    private void tampilError(SQLException ex) {
        JOptionPane.showMessageDialog(this, "Gagal: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void setStatus(String teks, Color warna) {
        lblStatus.setText(teks);
        lblStatus.setForeground(warna);
    }

    private JButton buatTombol(String teks, Color bg) {
        JButton btn = new JButton(teks);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(170, 30));
        return btn;
    }
}
```

- [ ] **Step 7: `StockView` memakai `SnapshotDialog`**

Ganti handler:
```java
        btnSnapshot.addActionListener(e -> {
            // TODO: Integrasi dengan snapshotDialog yang dibuat pada sesi sebelumnya
            try { new gui.snapshotDialog(this, true).setVisible(true); } catch(Exception ex) { showError("Gagal memuat Snapshot: " + ex.getMessage()); }
        });
```
dengan:
```java
        btnSnapshot.addActionListener(e -> new SnapshotDialog(this).setVisible(true));
```

Verifikasi: `Select-String -Path src\modern_pos\view\StockView.java -Pattern 'gui\.'` tanpa output.

- [ ] **Step 8: Compile + test**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: `OK (45 tests)`.

- [ ] **Step 9: Commit**

```powershell
git add src/modern_pos test/modern_pos/dao/SnapshotDAOTest.java
git -c user.name="attia" -c user.email="attiaitem@gmail.com" commit -m "feat(modern_pos): stock snapshot without legacy stack" -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 8: Verifikasi akhir dan migrasi DB utama

- [ ] **Step 1: Semua perubahan stok lewat `StokDAO`**

```powershell
Select-String -Path src\modern_pos\*\*.java -Pattern 'SET stok\s*=' | Where-Object { $_.Filename -ne 'StokDAO.java' }
Select-String -Path src\modern_pos\*\*.java -Pattern 'gui\.snapshotDialog|controller\.snapshotcontroller'
```
Expected: keduanya tanpa output.

- [ ] **Step 2: Semua test**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: `OK (45 tests)`.

- [ ] **Step 3: STOP — minta izin user untuk migrasi DB utama**

Tanyakan ke user: "Boleh jalankan `migrations/2026-09-22-utang-qty.sql` (ALTER TABLE utang ADD COLUMN qty ... DEFAULT 1) ke DB utama `sarimurnirejeki`?" Jangan jalankan tanpa jawaban "ya" eksplisit.

Setelah diizinkan:
```powershell
$m = "C:\laragon\bin\mysql\mysql-8.4.3-winx64\bin"
& "$m\mysqldump.exe" -uroot --result-file="$env:TEMP\sarimurnirejeki-before-utang-qty.sql" sarimurnirejeki
& "$m\mysql.exe" -uroot sarimurnirejeki -e "source migrations/2026-09-22-utang-qty.sql"
& "$m\mysql.exe" -uroot -N -e "SELECT COUNT(*), MIN(qty), MAX(qty) FROM sarimurnirejeki.utang"
```
Expected: semua utang lama qty 1. Backup ada di `%TEMP%`.

- [ ] **Step 4: Cek manual (user)**

Di NetBeans: Clean and Build; jalankan `modern_pos.view.LoginView` → Stock → Restock B001 +1 → Riwayat (baris MASUK) → Snapshot → Transaksi jual 1 → Riwayat (baris KELUAR) → Utang tambah qty 2 → stok berkurang 2.
