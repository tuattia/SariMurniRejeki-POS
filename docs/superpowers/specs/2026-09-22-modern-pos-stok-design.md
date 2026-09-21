# Sub-proyek 2a: Stok di modern_pos

Tanggal: 2026-09-22
Status: menunggu review
Sebelumnya: `2026-09-21-modern-pos-foundation-design.md` (sub-proyek 1, sudah di `main`)

## Konteks

Sub-proyek 2 (fitur yang belum ada di `modern_pos`) dipecah jadi 2a Stok,
2b Cetak (struk, detail utang/cicilan), 2c Akun (register, hak akses).
Dokumen ini hanya 2a.

Kondisi saat ini (dicek di DB `sarimurnirejeki`, read-only):
- `stock_movement_log` kosong: belum pernah ada gerakan stok tercatat.
- Restock di stack lama (`barangcontroller.tambahStok`) tidak dipanggil dari
  UI mana pun; restock dilakukan dengan edit angka stok.
- `stock_snapshot` berisi 11 baris periode 2026-09.
- Utang tidak mengurangi stok (lama maupun modern).
- `utang` tidak punya kolom qty.

## Tujuan & kriteria sukses

- Setiap perubahan stok lewat `modern_pos` tercatat 1 baris di
  `stock_movement_log` dengan `stok_sebelum`/`stok_sesudah` yang benar, dalam
  transaksi DB yang sama dengan perubahan stoknya.
- Stok tidak pernah minus, termasuk dua kasir bersamaan.
- Utang punya qty dan mengurangi stok sebanyak qty.
- Layar Stock punya Restock dan Riwayat per barang.
- Snapshot jalan tanpa kelas stack lama.
- 5 minor tertunda dari sub-proyek 1 selesai.

## A. `StokDAO` (baru, `modern_pos.dao`)

```java
public static void ubahStok(Connection con, String kodeBarang, int delta, String tipe,
                            String kodeTransaksi, String keterangan) throws SQLException
```
- Dipanggil di dalam transaksi milik pemanggil (tidak commit/rollback sendiri).
- `SELECT nama_barang, stok FROM barang WHERE kode_barang=? FOR UPDATE`.
- Barang tidak ada → `SQLException("Barang <kode> tidak ditemukan")`.
- `stok + delta < 0` → `SQLException("Stok <nama_barang> tidak cukup")`.
- `delta == 0` → tidak melakukan apa pun (tidak ada baris log).
- `UPDATE barang SET stok=?` lalu `INSERT INTO stock_movement_log`
  (`qty` = |delta|, `stok_sebelum`, `stok_sesudah`, `kode_transaksi` boleh null).
- `tipe` salah satu `KELUAR`, `MASUK`, `KOREKSI`.

```java
public void restock(String kodeBarang, int qty, String keterangan) throws SQLException
```
- `qty <= 0` → `IllegalArgumentException("Qty restock harus lebih dari 0")`.
- Transaksi sendiri; `MASUK`; keterangan kosong → `"Restock"`.

```java
public List<StockMovement> riwayat(String kodeBarang) throws SQLException
```
- 200 baris terbaru, urut `waktu DESC, id_log DESC`.
- Model baru `modern_pos.model.StockMovement`: `waktu` (String), `tipe`,
  `qty`, `stokSebelum`, `stokSesudah`, `kodeTransaksi`, `keterangan`.

## B. Pemakai `ubahStok`

| Kejadian | Gerakan | Keterangan log |
|---|---|---|
| `TransaksiDAO.simpanTransaksi` | KELUAR qty per item, `kode_transaksi` terisi | `Penjualan` |
| `UtangDAO.tambahUtang` | KELUAR `qty` | `Utang <kode_utang>` |
| `UtangDAO.updateUtang`, barang sama, qty berubah | selisih: KELUAR bila naik, MASUK bila turun | `Edit utang <kode_utang>` |
| `UtangDAO.updateUtang`, barang berubah | MASUK qty lama ke barang lama, KELUAR qty baru dari barang baru | `Edit utang <kode_utang>` |
| `UtangDAO.hapusUtang`, status `belum` | MASUK qty | `Utang dihapus <kode_utang>` |
| `UtangDAO.hapusUtang`, status `lunas` | tidak ada (barang sudah milik pelanggan) | — |
| `BarangDAO.tambahBarang`, stok > 0 | MASUK stok | `Stok awal` |
| `BarangDAO.updateBarang(Barang, int stokSebelumEdit)`, stok berubah | KOREKSI `b.stok - stokSebelumEdit` (relatif, jadi penjualan yang terjadi selama form terbuka tidak tertimpa) | `Koreksi manual` |

- `simpanTransaksi` mengganti `UPDATE ... AND stok >= ?` dengan `ubahStok`;
  pesan error tetap `"Stok <nama> tidak cukup"` (test sub-proyek 1 tetap lulus).
- `updateBarang`, `updateUtang`, `hapusUtang`, `tambahBarang` menjadi transaksi
  DB (satu koneksi, commit/rollback).
- `updateUtang` membaca `kode_barang` dan `qty` lama dengan `FOR UPDATE`
  sebelum menghitung gerakan. Gerakan berlaku apa pun status utangnya
  (edit dianggap koreksi atas barang yang benar-benar dibawa pelanggan).
  Kode utang tidak ada → `SQLException("Utang <kode> tidak ditemukan")`.
- `hapusBarang`: tidak ada gerakan (FK `transaksi_detail`/`utang` sudah
  mencegah hapus barang yang dipakai). Log lama untuk barang itu tetap ada
  (tabel log tidak punya FK).

## C. Utang punya qty

- Migrasi: `ALTER TABLE utang ADD COLUMN qty INT NOT NULL DEFAULT 1 AFTER kode_barang;`
  - File `migrations/2026-09-22-utang-qty.sql`.
  - `schema.sql` diperbarui (dump ulang setelah migrasi) dan DB test dibuat ulang.
  - Migrasi ke DB utama `sarimurnirejeki` dijalankan hanya setelah user
    mengizinkan secara eksplisit saat eksekusi. Utang lama otomatis qty 1.
  - Stack lama tidak memakai kolom ini; `INSERT` lamanya tetap jalan karena ada DEFAULT.
- `Utang.qty` (int) + getter/setter. `getUtangList` membaca `qty`.
- `UtangView` form: field `Qty` (default 1, harus ≥ 1). Saat barang atau qty
  berubah, `Total Utang` diisi otomatis `harga_jual × qty` (masih bisa diubah
  manual, misal untuk harga kredit). Tabel utang menampilkan kolom `Qty`.
- `tambahUtang`/`updateUtang` menolak `qty < 1`
  (`IllegalArgumentException("Qty minimal 1")`).

## D. Layar Stock

- Tombol **Restock**: pilih baris → input qty (angka > 0) dan keterangan
  opsional → `StockController.restock` (SwingWorker) → reload tabel.
- Tombol **Riwayat**: pilih baris → `RiwayatStokDialog` (modal, JTable:
  Waktu, Tipe, Qty, Stok Sebelum, Stok Sesudah, Kode Transaksi, Keterangan).
  Data dimuat lewat `StockController.riwayat` (SwingWorker).
- Label form "Stok Awal" jadi "Stok" (edit memakai KOREKSI).

## E. Snapshot

- `modern_pos.dao.SnapshotDAO`, port dari `controller.snapshotcontroller`:
  `ambilSnapshotBulan(YearMonth)`, `sudahAdaSnapshot(YearMonth)`,
  `getSnapshotByBulan(YearMonth)` → `List<StockSnapshot>`. Semua
  `throws SQLException`, `koneksi.open()` + try-with-resources.
  (`getDaftarPeriodeTersedia` lama tidak dipakai dialog, tidak di-port.)
- `tambahBarang` dengan stok awal negatif ditolak
  (`IllegalArgumentException("Stok tidak boleh negatif")`).
- `satuan` diambil dari `barang.satuan` (`COALESCE(satuan, 'pcs')`), bukan
  literal `'Pcs'`.
- Model `modern_pos.model.StockSnapshot` (field sesuai tabel yang ditampilkan dialog).
- `modern_pos.view.SnapshotDialog`: salinan `gui.snapshotDialog` yang memakai
  `SnapshotDAO`; error DB ditampilkan sebagai dialog pesan.
- `StockView` memanggil `SnapshotDialog`. File stack lama tidak diubah.

## F. Minor tertunda dari sub-proyek 1

1. `BarangDAO.getAllBarang`: keyword di-trim sebelum LIKE.
2. `TransaksiController.processPayment`: `loadBarang("")` juga saat gagal.
3. Rollback yang gagal tidak menutupi error asli: helper
   `modern_pos.dao.Tx.rollbackQuietly(Connection, SQLException)` →
   `addSuppressed`; dipakai di semua DAO bertransaksi.
4. `run-tests.ps1` men-set `-Ddb.url=...sarimurnirejeki_test`; `project.properties`
   `test-sys-prop.db.url` sama (mekanisme NetBeans untuk system property test).
   `TestDb` tetap menjaga `_test`.
5. `DashboardController.setView`: `currentUser == null` → tampil `"-"`.

## Test

- `StokDAOTest`: ubahStok mencatat log benar; stok kurang ditolak tanpa
  perubahan; delta 0 tanpa log; barang tidak ada ditolak; restock qty ≤ 0
  ditolak; riwayat urut terbaru dulu.
- `TransaksiDAOTest`: checkout mencatat KELUAR dengan `kode_transaksi`;
  checkout gagal tidak meninggalkan log.
- `UtangDAOTest`: tambah utang qty 2 mengurangi stok 2 + log; stok kurang
  ditolak tanpa baris utang/transaksi; edit qty naik/turun; edit ganti barang;
  hapus `belum` mengembalikan stok; hapus `lunas` tidak; qty 0 ditolak.
- `BarangDAOTest`: tambah barang stok awal → log MASUK; edit stok → KOREKSI;
  edit tanpa ubah stok → tanpa log; pencarian `" Gula "` ketemu.
- `SnapshotDAOTest`: ambil snapshot dua kali idempotent (baris tidak dobel,
  stok ter-update); `satuan` dari barang; daftar periode.
- Semua test di DB `sarimurnirejeki_test`.

## Di luar cakupan

- Struk, detail utang/cicilan (2b); register, hak akses (2c).
- Pendapatan KREDIT terhitung dobel di dashboard (dicatat reviewer sub-proyek 1) → 2b.
- Mengubah stack lama.
