# Sub-proyek 2b: Cetak (struk, kartu angsuran) dan pendapatan dashboard

Tanggal: 2026-09-22
Status: menunggu review
Sebelumnya: 2a Stok (`2026-09-22-modern-pos-stok-design.md`, sudah di `main`)

## Konteks

- `TransaksiController` (setelah checkout) dan `LogTransaksiView` (tombol cetak)
  membuka `new gui.detailtransaksi()` tanpa data → struk kosong. Ini dua
  referensi terakhir `modern_pos` ke stack lama.
- `gui.detailtransaksi`: struk thermal 58mm; header toko, kode, waktu, kasir,
  item (nama, qty, harga, total), subtotal, bayar, kembalian, "Terima Kasih",
  "Selamat Datang Kembali".
- `gui.detailutang`: kartu angsuran kertas 15×20 cm; data utang + 15 baris
  kosong untuk ditulis tangan + tanda tangan Pengawas "(Ni Wayan Suerni)" /
  Peminjam. Tidak ada pencatatan pembayaran per angsuran (keputusan user:
  port apa adanya, tanpa tabel baru).
- `DashboardDAO` menghitung pendapatan `SUM(transaksi.total)` hari ini →
  utang dihitung penuh saat dibuat dan sisanya dihitung lagi saat lunas.
  Keputusan user: pendapatan = **uang masuk**.

## Tujuan & kriteria sukses

- Setelah checkout, struk berisi data transaksi itu dan bisa dicetak ke
  printer thermal 58mm.
- Log Transaksi bisa menampilkan/mencetak struk transaksi TUNAI yang dipilih.
- Layar Utang bisa menampilkan/mencetak kartu angsuran 15×20 cm.
- Dashboard "Pendapatan Hari Ini" = tunai + DP utang baru + pelunasan, tanpa dobel.
- `modern_pos` tidak lagi mereferensi `gui.*`.

## A. Data toko

`modern_pos.utils.Toko` (konstanta, satu tempat):
- `NAMA = "SARI MURNI REJEKI"`
- `ALAMAT = "Jln. Raya Katung Payangan Kintamani, Bangli"`
- `TELEPON = "Telp. 083851003084"`
- `PENGAWAS = "Ni Wayan Suerni"`

## B. Struk

Model `modern_pos.model.Struk`: `kodeTransaksi`, `waktu` (`LocalDateTime`),
`pelanggan`, `jenis` (`TUNAI`/`KREDIT`), `keterangan`, `items`
(`List<Struk.Item>`: `nama`, `qty`, `harga`, `subtotal`), `total`, `bayar`, `kembali`.

`TransaksiDAO`:
- `simpanTransaksi(...)` sekarang **mengembalikan `String` kode transaksi**.
- `Struk getStruk(String kodeTransaksi) throws SQLException`:
  - `transaksi` → kode, pelanggan, total, jenis; waktu awal = `created_at`.
  - `transaksi_detail` JOIN `barang` (LEFT, nama `"-"` bila barang tidak ada)
    → items, urut `id_detail`.
  - `log_transaksi` (baris pertama per kode) → `bayar`, `kembali`,
    `keterangan`, dan `waktu` (menggantikan `created_at` bila ada).
  - Log tidak ada → `bayar = total`, `kembali = 0`, `keterangan = null`.
  - Kode tidak ada → `SQLException("Transaksi <kode> tidak ditemukan")`.

`modern_pos.utils.StrukFormatter.format(Struk) → List<String>`:
- Semua baris panjang ≤ 32 karakter (58mm, font monospace).
- Urutan: nama toko, alamat (dibungkus ke 32 kolom), telepon (tengah),
  garis `-` 32, `Kode: <kode>`, `Waktu: dd-MM-yyyy HH:mm`, `Pelanggan: <nama>`
  (hanya bila tidak kosong), garis, per item: baris nama (dipotong 32) lalu
  baris `  <qty> x <harga>` kiri dan `<subtotal>` rata kanan, garis,
  `Total`, `Bayar`, `Kembali` (label kiri, nominal rata kanan, format
  `Rp 1.234.567`), bila `keterangan` ada dan items kosong: baris keterangan
  sebelum garis total, garis, "Terima Kasih" dan "Selamat Datang Kembali" (tengah).
- Angka memakai titik ribuan (`String.format("%,d")` dengan `,` → `.`).

`modern_pos.view.StrukDialog(Frame, Struk)`: modal, `JTextArea` read-only
font `Monospaced`, isi = baris formatter; tombol **Cetak** dan **Tutup**.
Cetak lewat `modern_pos.utils.Cetak.cetakBaris(List<String>, String judulJob)`:
`PrinterJob` + `Printable` yang menggambar baris dengan font Monospaced 7pt,
kertas lebar 58mm (165pt), tinggi = jumlah baris × tinggi baris + margin.
Printer dipilih lewat dialog printer bawaan (`job.printDialog()`); batal →
tidak mencetak.

Pemanggil:
- `TransaksiController.processPayment`: setelah sukses, `dao.getStruk(kode)`
  di background lalu `view.showStruk(struk)`; gagal memuat struk → pesan
  error, transaksi tetap tersimpan.
- `LogTransaksiView` tombol cetak: baris `tipe_transaksi = KREDIT` → pesan
  "Transaksi kredit: lihat kartu angsuran di menu Utang"; selain itu
  `LogTransaksiController.tampilStruk(kode)` → `StrukDialog`.

## C. Kartu angsuran

Model `modern_pos.model.KartuAngsuran`: `utang` (`Utang`), `namaBarang`.

`UtangDAO.getKartu(String kodeUtang) throws SQLException` → utang (semua
kolom termasuk `qty`) + `barang.nama_barang` (LEFT JOIN, `"-"` bila tidak
ada). Kode tidak ada → `SQLException("Utang <kode> tidak ditemukan")`.

`Utang.bayarPerBulan()` → `ceil((hargaBarang - dp) / jumlahCicilan)`;
`jumlahCicilan <= 0` → `0`; sisa ≤ 0 → `0`. (Stack lama membulatkan ke
bawah sehingga total angsuran bisa kurang dari sisa utang.)

`modern_pos.view.KartuAngsuranDialog(Frame, KartuAngsuran)`: panel putih
567×756 px (15×20 cm @96dpi):
- "KARTU ANGSURAN", `Toko.NAMA`.
- Kode utang, nama, telepon, alamat, nama barang, qty, harga barang, DP,
  jumlah cicilan (`<n>x`), jatuh tempo (`dd-MM-yyyy`), bayar per bulan.
- Tabel 15 baris kosong: Angsuran, Tanggal, Nominal, Sisa, Paraf (grid hitam).
- Tanda tangan: "Pengawas" `(Toko.PENGAWAS)` dan "Peminjam" `(....)`.
- Tombol **Cetak** dan **Tutup** di luar panel (tidak ikut tercetak).

Cetak lewat `Cetak.cetakKomponen(JComponent, double lebarMm, double tinggiMm,
String judulJob)`: `Printable` yang men-skala komponen agar muat di area
cetak kertas 150×200 mm; dialog printer bawaan.

Pemanggil: tombol **Kartu** di `UtangView` (pilih baris →
`UtangController.tampilKartu(kode)` → background `getKartu` → dialog).

## D. Pendapatan dashboard (uang masuk)

`DashboardDAO.getSummary` pendapatan hari ini:
```sql
SELECT COALESCE(SUM(CASE WHEN t.jenis_transaksi = 'KREDIT' THEN COALESCE(u.dp, 0)
                         ELSE t.total END), 0)
FROM transaksi t LEFT JOIN utang u ON u.kode_transaksi = t.kode_transaksi
WHERE DATE(t.tanggal) = CURDATE()
```
Jumlah transaksi hari ini tetap `COUNT(*)` dari `transaksi`.
Keterbatasan: utang lama dengan `kode_transaksi` NULL, atau utang yang
dihapus, dianggap DP 0.

## E. Minor tertunda

`UtangController`: pesan error diambil dari `getCause()` (tidak lagi
`"java.sql.SQLException: ..."`), seperti `StockController`.

## Test

- `StrukFormatterTest`: semua baris ≤ 32; memuat kode, item, total, bayar,
  kembali dengan format `Rp 1.234`; nama barang panjang dipotong; baris
  pelanggan tidak muncul bila kosong; keterangan muncul untuk struk tanpa item.
- `TransaksiDAOTest`: `simpanTransaksi` mengembalikan kode yang ada di DB;
  `getStruk` sesudah checkout (items, nama barang, bayar, kembali);
  `getStruk` tanpa log (bayar = total); kode tidak ada → exception.
- `UtangDAOTest`: `getKartu` memuat nama barang dan qty; kode tidak ada → exception.
- `UtangTest`: `bayarPerBulan` (bulat ke atas, cicilan 0, sisa 0).
- `DashboardDAOTest`: tunai 100.000 + utang baru (harga 500.000, DP 50.000)
  + pelunasan utang lain sisa 30.000 = 180.000; utang tidak dihitung penuh.
- Cetak ke printer dan tampilan dialog: cek manual.

## Di luar cakupan

- Pencatatan pembayaran per angsuran (keputusan user).
- Register, hak akses (2c).
- Minor tertunda lain dari 2a (log `kode_transaksi` null saat edit utang,
  urutan kunci dua barang).
