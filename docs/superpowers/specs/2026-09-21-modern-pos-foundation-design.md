# Sub-proyek 1: Pondasi modern_pos

Tanggal: 2026-09-21
Status: menunggu review

## Konteks

Aplikasi punya dua stack paralel: `gui/` (lama, entry point saat ini `gui.login`)
dan `modern_pos/` (MVC baru, belum jadi entry point). Tujuan besar: migrasi penuh
ke `modern_pos` lalu hapus stack lama. Migrasi dipecah 4 sub-proyek:

1. **Pondasi** (dokumen ini): build portabel, test harness, fix bug `modern_pos`.
2. Fitur yang belum ada di modern: struk, snapshot stok, detail utang/cicilan,
   register, stock_movement_log, hak akses.
3. Ganti entry point ke `modern_pos`.
4. Hapus stack lama + audit akhir.

Sub-proyek 1 tidak menambah fitur dan tidak mengubah entry point.

## Tujuan & kriteria sukses

- Project bisa di-build di mesin lain tanpa path `Downloads`.
- Ada test otomatis (JUnit 4) yang bisa dijalankan dari NetBeans "Test Project".
- Enam bug di bagian B punya test yang gagal sebelum fix dan lulus sesudahnya.
- DB utama `sarimurnirejeki` tidak disentuh oleh test.

## A. Infrastruktur

- Git sudah di-init (commit `02f0053`), `.gitignore` meng-exclude `build/`,
  `dist/`, `output/`, `nbproject/private/`, `*.exe`, `*.rar`, `*.class`.
- Folder `lib/` berisi: `mysql-connector-j-9.5.0.jar`, `DateChooser.jar`
  (disalin dari lokasi `Downloads` saat ini), `junit-4.13.2.jar`,
  `hamcrest-core-1.3.jar` (dari Maven Central).
- `nbproject/project.properties`: `file.reference.*` diganti path relatif `lib/...`;
  `javac.test.classpath` berisi `${javac.classpath}`, JUnit, Hamcrest.
- `schema.sql` di root: hasil `mysqldump --no-data sarimurnirejeki` dari DB lokal
  (read-only terhadap DB utama). Menjadi sumber kebenaran nama kolom.
- DB test `sarimurnirejeki_test` dibuat dari `schema.sql`. Test helper
  (`test/modern_pos/TestDb.java`) men-set system property koneksi ke DB test,
  dan mengosongkan tabel sebelum tiap test (urutan hapus menghormati FK).
- Ant tidak ada di PATH; verifikasi selama pengerjaan pakai `javac` + `java
  org.junit.runner.JUnitCore` langsung. NetBeans tetap bisa menjalankan test.

## B. Perbaikan bug

Setiap bug: tulis test gagal dulu, lalu fix (TDD).

### B1. Koneksi bocor + `null` connection
- Method baru `config.koneksi.open() throws SQLException`: buka koneksi,
  lempar exception bila gagal. URL/USER/PASS dibaca saat dipanggil dari system
  property `db.url`, `db.user`, `db.pass`; default tetap nilai sekarang.
- `getConnection()` lama tetap ada dengan perilaku sama (`return null` bila
  gagal), tapi isinya mendelegasikan ke `open()`. Alasan: stack lama masih
  entry point dan banyak memakai `private Connection con = koneksi.getConnection()`
  di field initializer; mengubah perilakunya berisiko merusak aplikasi yang
  sedang dipakai. `getConnection()` dihapus di sub-proyek 4.
- Semua DAO `modern_pos` memakai `try (Connection con = koneksi.open())`.
- Pengecekan `if (con == null)` di DAO dihapus.
- Teks log dengan encoding rusak (`âœ“`) diganti ASCII.
- Test: ukur `SHOW STATUS LIKE 'Threads_connected'` sebelum dan sesudah
  50 panggilan `BarangDAO.getAllBarang("")`; selisih harus ≤ 2.

### B2. `kode_barang` dan `kode_transaksi` utang
- `tambahUtang` saat ini mengisi `kode_barang` dengan `SELECT ... LIMIT 1`
  (barang sembarang) dan tidak mengisi `utang.kode_transaksi`.
- Fix: `Utang.kodeBarang` wajib diisi; urutan insert jadi `transaksi` →
  `utang` (dengan `kode_barang` dari input dan `kode_transaksi` dari transaksi
  yang baru dibuat) → `log_transaksi`. Semua dalam satu transaksi DB.
- `UtangView` form tambah/edit mendapat combo box barang (dari `BarangDAO`).
- `getUtangList` membaca `kode_barang`.
- Test: tambah utang untuk barang kedua di DB test → baris utang punya
  `kode_barang` barang kedua dan `kode_transaksi` yang ada di tabel `transaksi`.

### B3. User hilang saat navigasi
- Kelas baru `modern_pos.utils.Session` dengan field statis `currentUser`.
- `LoginController` men-set `Session.currentUser` setelah login sukses.
  Constructor `LoginView` men-clear-nya (semua jalur logout membuka
  `LoginView`, jadi satu titik cukup).
- Semua tombol sidebar/dashboard yang memakai `new User()` diganti
  `Session.currentUser`.
- Test: tidak ada test otomatis (murni wiring UI); verifikasi dengan grep
  `new modern_pos.model.User()` = 0 hasil di `view/` dan cek manual.

### B4. Stok bisa minus
- `TransaksiDAO.simpanTransaksi`: update stok jadi
  `UPDATE barang SET stok = stok - ? WHERE kode_barang = ? AND stok >= ?`,
  dieksekusi per item (bukan batch) supaya jumlah baris ter-update bisa dicek.
  Bila 0 baris → rollback, lempar exception `"Stok <nama> tidak cukup"`.
- Koneksi yang dipakai transaksi ditutup setelah commit/rollback (ikut B1).
- Test: barang stok 2, checkout qty 3 → exception, stok tetap 2, tidak ada
  baris baru di `transaksi`.

### B5. Fallback nama kolom palsu
- Hapus semua `try { getX("a") } catch { getX("b") }` dan SQL fallback di
  `BarangDAO`, `TransaksiDAO`, `LogTransaksiDAO`, `UtangDAO`; pakai nama kolom
  dari `schema.sql`. Nilai stok palsu `999` hilang.
- Duplikasi `TransaksiDAO.getBarangList` dihapus; `TransaksiController` memakai
  `BarangDAO.getAllBarang`.
- Test: `getAllBarang("")` mengembalikan stok sesuai data seed.

### B6. Kode transaksi bentrok
- Kolom `kode_transaksi` di `transaksi`, `transaksi_detail`, `utang` adalah
  `varchar(20)`, jadi kode maksimal 20 karakter.
- Satu helper `newKodeTransaksi()` (static di `TransaksiDAO`) dipakai oleh
  `TransaksiDAO` dan `UtangDAO`: `TRX` + `yyMMddHHmmssSSS` + 2 digit urutan
  = tepat 20 karakter. Generator `synchronized` menyimpan nilai terakhir
  (`millis*100 + urutan`) dan selalu naik, jadi unik dalam satu JVM walau
  dipanggil > 100 kali per milidetik. Antar-JVM (dua kasir) bentrok ditolak
  UNIQUE KEY di DB (transaksi rollback, bukan data korup).
- `jdbc` driver auto-load (JDBC 4), `Class.forName` dan `testConnection()`
  (tidak dipakai di mana pun) dihapus.
- Library tambahan `AbsoluteLayout.jar` (dipakai form lama) ikut ke `lib/`;
  ketiga jar lama disalin dari `dist/lib/`. JUnit 4.13.2 + Hamcrest 1.3 disalin
  dari salinan lokal yang sudah ada di mesin (tidak perlu download).
- Test: 1000 kode berturut-turut → semua unik dan panjang 20.

## C. Di luar cakupan

- Hash password tetap SHA-256 (ganti ke algoritma bersalt butuh migrasi
  password yang ada — sub-proyek terpisah).
- `stock_movement_log`, struk modern, snapshot modern, register, detail utang,
  hak akses → sub-proyek 2.
- Entry point tetap `gui.login` → sub-proyek 3.
- Refactor `SwingWorker` berulang di controller, tampilan UI → tidak disentuh.

## Risiko

- `schema.sql` dari DB lokal bisa berbeda dengan `output/sarimurnirejeki.sql`
  lama; DB lokal dianggap benar.
- Stack lama tidak berubah perilaku: `getConnection()` dipertahankan (B1).
