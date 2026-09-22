# modern_pos Cetak (Sub-proyek 2b) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Struk transaksi dan kartu angsuran `modern_pos` berisi data yang benar dan bisa dicetak tanpa stack lama; dashboard menampilkan uang masuk tanpa dobel.

**Architecture:** Struk dibentuk sebagai baris teks 32 kolom oleh `StrukFormatter` (murni, dites) dan dicetak baris-per-baris dengan font monospace ke kertas 58mm. Kartu angsuran adalah panel Swing 15×20 cm yang dicetak dengan skala. Satu helper `Cetak` untuk keduanya. Pendapatan dashboard dihitung dari `transaksi` LEFT JOIN `utang` (TUNAI = total, KREDIT = DP).

**Tech Stack:** Java 1.8 source (JDK 17), Swing, `java.awt.print`, JDBC MySQL 8.4, JUnit 4.13.2. Test: `powershell -ExecutionPolicy Bypass -File run-tests.ps1` (DB `sarimurnirejeki_test`).

**Spec:** `docs/superpowers/specs/2026-09-22-modern-pos-cetak-design.md`

## Global Constraints

- Source level Java 1.8: tidak boleh `var`, `List.of`, text block, `String.isBlank`, `String.repeat`, switch expression.
- Semua baris struk ≤ 32 karakter. Format uang `Rp 1.234.567` (titik ribuan).
- Data toko hanya di `modern_pos.utils.Toko`.
- DB utama `sarimurnirejeki` tidak disentuh; tidak ada migrasi di sub-proyek ini.
- Stack lama tidak diubah. Setelah Task 3, `modern_pos` tidak mereferensi `gui.*` (kecuali komentar).
- Commit: `git -c user.name="attia" -c user.email="attiaitem@gmail.com" commit -m "<pesan>" -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"`.
- Seed test (`TestDb.reset()`): `B001 'Beras 5kg' 65000 stok 10`, `B002 'Gula 1kg' 15000 stok 2`.

## Review Focus

1. Nominal besar (puluhan juta, qty ratusan) tetap muat 32 kolom di struk → test `nominalBesarTetapMuat` di Task 1.
2. Log transaksi sudah dihapus user: struk tetap bisa dibuka dengan bayar = total → test `getStrukTanpaLogBayarSamaDenganTotal` di Task 2.
3. Utang yang dibuat kemarin lalu dilunasi hari ini: hari ini hanya pelunasannya yang terhitung → test `utangKemarinTidakDihitungHariIni` di Task 6.
4. Kartu untuk utang dengan jumlah cicilan 0 atau DP ≥ harga tidak crash dan menampilkan 0 → test di Task 4 (`UtangTest`).
5. Checkout sukses tapi struk gagal dimuat: transaksi tetap tersimpan, keranjang dikosongkan, user diberi tahu → ditangani di Task 3 Step 3 (cek manual; jalur error dites lewat `getStruk` kode tidak ada di Task 2).

---

## File Structure

| File | Aksi | Tanggung jawab |
|---|---|---|
| `src/modern_pos/utils/Toko.java` | Create | Nama, alamat, telepon, pengawas |
| `src/modern_pos/model/Struk.java` | Create | Data struk + `Struk.Item` |
| `src/modern_pos/utils/StrukFormatter.java` | Create | Struk → baris 32 kolom |
| `src/modern_pos/utils/Cetak.java` | Create | `cetakBaris`, `cetakKomponen` |
| `src/modern_pos/dao/TransaksiDAO.java` | Modify | `simpanTransaksi` return kode, `getStruk` |
| `src/modern_pos/view/StrukDialog.java` | Create | Tampil + cetak struk |
| `src/modern_pos/controller/TransaksiController.java` | Modify | Buka struk modern setelah checkout |
| `src/modern_pos/controller/LogTransaksiController.java` | Modify | `tampilStruk` |
| `src/modern_pos/view/{TransaksiView,LogTransaksiView}.java` | Modify | `showStruk`, tombol cetak |
| `src/modern_pos/model/Utang.java` | Modify | `bayarPerBulan()` |
| `src/modern_pos/model/KartuAngsuran.java` | Create | Utang + nama barang |
| `src/modern_pos/dao/UtangDAO.java` | Modify | `getKartu`, mapping bersama |
| `src/modern_pos/view/KartuAngsuranDialog.java` | Create | Kartu 15×20 cm + cetak |
| `src/modern_pos/controller/UtangController.java` | Modify | `tampilKartu`, pesan error dibuka |
| `src/modern_pos/view/UtangView.java` | Modify | Tombol Kartu, `showKartu` |
| `src/modern_pos/dao/DashboardDAO.java` | Modify | Pendapatan uang masuk |
| `test/modern_pos/utils/StrukFormatterTest.java`, `test/modern_pos/model/UtangTest.java`, `test/modern_pos/dao/DashboardDAOTest.java` | Create | — |
| `test/modern_pos/dao/{TransaksiDAOTest,UtangDAOTest}.java` | Modify | — |

---

### Task 1: `Toko`, `Struk`, `StrukFormatter`

**Files:**
- Create: `src/modern_pos/utils/Toko.java`, `src/modern_pos/model/Struk.java`, `src/modern_pos/utils/StrukFormatter.java`, `test/modern_pos/utils/StrukFormatterTest.java`

**Interfaces:**
- Produces:
  - `Toko.NAMA`, `Toko.ALAMAT`, `Toko.TELEPON`, `Toko.PENGAWAS` (`public static final String`).
  - `Struk` getter/setter: `kodeTransaksi`, `waktu` (`LocalDateTime`), `pelanggan`, `jenis`, `keterangan`, `total`, `bayar`, `kembali` (int), `getItems()` → `List<Struk.Item>` (tidak pernah null). `Struk.Item(String nama, int qty, int harga, int subtotal)` dengan getter.
  - `StrukFormatter.LEBAR = 32`, `static List<String> format(Struk)`, `static String rupiah(int)` → `"Rp 1.234"`.

- [ ] **Step 1: Tulis test gagal `test/modern_pos/utils/StrukFormatterTest.java`**

```java
package modern_pos.utils;

import java.time.LocalDateTime;
import java.util.List;
import modern_pos.model.Struk;
import org.junit.Test;
import static org.junit.Assert.*;

public class StrukFormatterTest {

    private static Struk struk(String pelanggan) {
        Struk s = new Struk();
        s.setKodeTransaksi("TRX26092214050000001");
        s.setWaktu(LocalDateTime.of(2026, 9, 22, 14, 5));
        s.setPelanggan(pelanggan);
        s.setJenis("TUNAI");
        s.getItems().add(new Struk.Item("Beras 5kg", 2, 65000, 130000));
        s.getItems().add(new Struk.Item("Minyak Goreng Kemasan Premium Super 2 Liter", 1, 30000, 30000));
        s.setTotal(160000);
        s.setBayar(200000);
        s.setKembali(40000);
        return s;
    }

    private static boolean ada(List<String> baris, String awal, String akhir) {
        for (String b : baris) if (b.startsWith(awal) && b.endsWith(akhir)) return true;
        return false;
    }

    @Test
    public void semuaBarisMuat32Kolom() {
        for (String b : StrukFormatter.format(struk("Budi"))) {
            assertTrue("terlalu panjang (" + b.length() + "): " + b, b.length() <= 32);
        }
    }

    @Test
    public void memuatHeaderItemDanPembayaran() {
        List<String> b = StrukFormatter.format(struk("Budi"));
        assertTrue(b.contains("Kode: TRX26092214050000001"));
        assertTrue(b.contains("Waktu: 22-09-2026 14:05"));
        assertTrue(b.contains("Pelanggan: Budi"));
        assertTrue(b.contains("Beras 5kg"));
        assertTrue(ada(b, "  2 x 65.000", "130.000"));
        assertTrue(ada(b, "Total", "Rp 160.000"));
        assertTrue(ada(b, "Bayar", "Rp 200.000"));
        assertTrue(ada(b, "Kembali", "Rp 40.000"));
        assertTrue(ada(b, "", "Terima Kasih"));
    }

    @Test
    public void namaBarangPanjangDipotong() {
        List<String> b = StrukFormatter.format(struk("Budi"));
        assertTrue(b.contains("Minyak Goreng Kemasan Premium Su"));
    }

    @Test
    public void pelangganKosongTidakDitampilkan() {
        for (String b : StrukFormatter.format(struk("  "))) assertFalse(b.startsWith("Pelanggan"));
        for (String b : StrukFormatter.format(struk(null))) assertFalse(b.startsWith("Pelanggan"));
    }

    @Test
    public void strukTanpaItemMenampilkanKeterangan() {
        Struk s = struk("Siti");
        s.getItems().clear();
        s.setKeterangan("Pelunasan Sisa Utang");
        assertTrue(StrukFormatter.format(s).contains("Pelunasan Sisa Utang"));
    }

    @Test
    public void nominalBesarTetapMuat() {
        Struk s = struk("Budi");
        s.getItems().clear();
        s.getItems().add(new Struk.Item("Semen", 100, 99999999, 999999900));
        s.setTotal(999999900);
        s.setBayar(999999999);
        s.setKembali(99);
        for (String b : StrukFormatter.format(s)) assertTrue(b, b.length() <= 32);
        assertTrue(ada(StrukFormatter.format(s), "Total", "Rp 999.999.900"));
    }

    @Test
    public void rupiahMemakaiTitikRibuan() {
        assertEquals("Rp 1.234.567", StrukFormatter.rupiah(1234567));
        assertEquals("Rp 0", StrukFormatter.rupiah(0));
    }
}
```

- [ ] **Step 2: Jalankan, pastikan gagal**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: compile error `cannot find symbol ... Struk` / `StrukFormatter`.

- [ ] **Step 3: `src/modern_pos/utils/Toko.java`**

```java
package modern_pos.utils;

// Data toko untuk struk dan kartu angsuran. Satu tempat supaya mudah diganti.
public final class Toko {
    private Toko() {}

    public static final String NAMA = "SARI MURNI REJEKI";
    public static final String ALAMAT = "Jln. Raya Katung Payangan Kintamani, Bangli";
    public static final String TELEPON = "Telp. 083851003084";
    public static final String PENGAWAS = "Ni Wayan Suerni";
}
```

- [ ] **Step 4: `src/modern_pos/model/Struk.java`**

```java
package modern_pos.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Struk {
    public static class Item {
        private final String nama;
        private final int qty, harga, subtotal;

        public Item(String nama, int qty, int harga, int subtotal) {
            this.nama = nama;
            this.qty = qty;
            this.harga = harga;
            this.subtotal = subtotal;
        }
        public String getNama() { return nama; }
        public int getQty() { return qty; }
        public int getHarga() { return harga; }
        public int getSubtotal() { return subtotal; }
    }

    private String kodeTransaksi, pelanggan, jenis, keterangan;
    private LocalDateTime waktu;
    private int total, bayar, kembali;
    private final List<Item> items = new ArrayList<>();

    public String getKodeTransaksi() { return kodeTransaksi; }
    public void setKodeTransaksi(String kodeTransaksi) { this.kodeTransaksi = kodeTransaksi; }
    public String getPelanggan() { return pelanggan; }
    public void setPelanggan(String pelanggan) { this.pelanggan = pelanggan; }
    public String getJenis() { return jenis; }
    public void setJenis(String jenis) { this.jenis = jenis; }
    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }
    public LocalDateTime getWaktu() { return waktu; }
    public void setWaktu(LocalDateTime waktu) { this.waktu = waktu; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getBayar() { return bayar; }
    public void setBayar(int bayar) { this.bayar = bayar; }
    public int getKembali() { return kembali; }
    public void setKembali(int kembali) { this.kembali = kembali; }
    public List<Item> getItems() { return items; }
}
```

- [ ] **Step 5: `src/modern_pos/utils/StrukFormatter.java`**

```java
package modern_pos.utils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import modern_pos.model.Struk;

// Struk thermal 58mm = 32 karakter monospace per baris.
public final class StrukFormatter {
    private StrukFormatter() {}

    public static final int LEBAR = 32;
    private static final DateTimeFormatter WAKTU = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public static List<String> format(Struk s) {
        List<String> out = new ArrayList<>();
        out.add(tengah(Toko.NAMA));
        for (String b : bungkus(Toko.ALAMAT)) out.add(tengah(b));
        out.add(tengah(Toko.TELEPON));
        out.add(garis());
        out.add(potong("Kode: " + s.getKodeTransaksi()));
        out.add(potong("Waktu: " + s.getWaktu().format(WAKTU)));
        if (s.getPelanggan() != null && !s.getPelanggan().trim().isEmpty()) {
            out.add(potong("Pelanggan: " + s.getPelanggan().trim()));
        }
        out.add(garis());
        for (Struk.Item i : s.getItems()) {
            out.add(potong(i.getNama()));
            out.add(kiriKanan("  " + i.getQty() + " x " + angka(i.getHarga()), angka(i.getSubtotal())));
        }
        if (s.getItems().isEmpty() && s.getKeterangan() != null) out.add(potong(s.getKeterangan()));
        out.add(garis());
        out.add(kiriKanan("Total", rupiah(s.getTotal())));
        out.add(kiriKanan("Bayar", rupiah(s.getBayar())));
        out.add(kiriKanan("Kembali", rupiah(s.getKembali())));
        out.add(garis());
        out.add(tengah("Terima Kasih"));
        out.add(tengah("Selamat Datang Kembali"));
        return out;
    }

    public static String rupiah(int nilai) {
        return "Rp " + angka(nilai);
    }

    private static String angka(int nilai) {
        return String.format("%,d", nilai).replace(',', '.');
    }

    private static String potong(String s) {
        return s.length() <= LEBAR ? s : s.substring(0, LEBAR);
    }

    private static String spasi(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(' ');
        return sb.toString();
    }

    private static String garis() {
        return spasi(LEBAR).replace(' ', '-');
    }

    private static String tengah(String s) {
        String p = potong(s);
        return spasi((LEBAR - p.length()) / 2) + p;
    }

    private static String kiriKanan(String kiri, String kanan) {
        return potong(kiri + spasi(Math.max(1, LEBAR - kiri.length() - kanan.length())) + kanan);
    }

    private static List<String> bungkus(String teks) {
        List<String> hasil = new ArrayList<>();
        StringBuilder baris = new StringBuilder();
        for (String kata : teks.split(" ")) {
            if (baris.length() > 0 && baris.length() + 1 + kata.length() > LEBAR) {
                hasil.add(baris.toString());
                baris.setLength(0);
            }
            if (baris.length() > 0) baris.append(' ');
            baris.append(kata);
        }
        if (baris.length() > 0) hasil.add(potong(baris.toString()));
        return hasil;
    }
}
```

- [ ] **Step 6: Jalankan, pastikan lulus**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: `OK (56 tests)`.

- [ ] **Step 7: Commit**

```powershell
git add src/modern_pos/utils/Toko.java src/modern_pos/model/Struk.java src/modern_pos/utils/StrukFormatter.java test/modern_pos/utils/StrukFormatterTest.java
git -c user.name="attia" -c user.email="attiaitem@gmail.com" commit -m "feat(modern_pos): 58mm receipt text formatter" -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 2: `TransaksiDAO` — kode dikembalikan dan `getStruk`

**Files:**
- Modify: `src/modern_pos/dao/TransaksiDAO.java`
- Test: `test/modern_pos/dao/TransaksiDAOTest.java`

**Interfaces:**
- Consumes: `Struk`, `Struk.Item` (Task 1).
- Produces: `String TransaksiDAO.simpanTransaksi(List<CartItem>, int total, int bayar, int kembali, String pelanggan) throws SQLException`; `Struk TransaksiDAO.getStruk(String kodeTransaksi) throws SQLException` — kode tidak ada → `SQLException("Transaksi <kode> tidak ditemukan")`.

- [ ] **Step 1: Tambah test gagal ke `TransaksiDAOTest.java`**

Tambah import `import modern_pos.model.Struk;` lalu:

```java
    @Test
    public void simpanTransaksiMengembalikanKodeDanGetStrukLengkap() throws Exception {
        TestDb.reset();
        CartItem beras = new CartItem(barang("B001", "Beras 5kg", 65000, 10), 2);
        CartItem gula = new CartItem(barang("B002", "Gula 1kg", 15000, 2), 1);

        String kode = new TransaksiDAO().simpanTransaksi(Arrays.asList(beras, gula), 145000, 150000, 5000, "Budi");

        assertEquals(kode, TestDb.queryString("SELECT kode_transaksi FROM transaksi"));
        Struk s = new TransaksiDAO().getStruk(kode);
        assertEquals(kode, s.getKodeTransaksi());
        assertEquals("Budi", s.getPelanggan());
        assertEquals("TUNAI", s.getJenis());
        assertEquals(2, s.getItems().size());
        assertEquals("Beras 5kg", s.getItems().get(0).getNama());
        assertEquals(2, s.getItems().get(0).getQty());
        assertEquals(130000, s.getItems().get(0).getSubtotal());
        assertEquals(145000, s.getTotal());
        assertEquals(150000, s.getBayar());
        assertEquals(5000, s.getKembali());
        assertNotNull(s.getWaktu());
    }

    @Test
    public void getStrukTanpaLogBayarSamaDenganTotal() throws Exception {
        TestDb.reset();
        CartItem beras = new CartItem(barang("B001", "Beras 5kg", 65000, 10), 1);
        String kode = new TransaksiDAO().simpanTransaksi(Arrays.asList(beras), 65000, 100000, 35000, "Budi");
        new LogTransaksiDAO().hapusLog(kode);

        Struk s = new TransaksiDAO().getStruk(kode);
        assertEquals(65000, s.getBayar());
        assertEquals(0, s.getKembali());
        assertEquals(1, s.getItems().size());
    }

    @Test(expected = SQLException.class)
    public void getStrukKodeTidakAdaDitolak() throws Exception {
        TestDb.reset();
        new TransaksiDAO().getStruk("TRX-TIDAK-ADA");
    }
```

- [ ] **Step 2: Jalankan, pastikan gagal**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: compile error (`incompatible types: void cannot be converted to String` / `cannot find symbol getStruk`).

- [ ] **Step 3: `simpanTransaksi` mengembalikan kode**

Ganti signature `public void simpanTransaksi(` menjadi `public String simpanTransaksi(`, dan tambah `return kodeTrx;` tepat sebelum `}` penutup method (setelah blok `try (Connection con = koneksi.open()) { ... }`).

- [ ] **Step 4: Tambah `getStruk`**

Tambah import:
```java
import modern_pos.model.Struk;
import java.sql.ResultSet;
import java.sql.Timestamp;
```

Tambah method:
```java
    public Struk getStruk(String kodeTransaksi) throws SQLException {
        Struk s = new Struk();
        try (Connection con = koneksi.open()) {
            String sqlT = "SELECT kode_transaksi, nama_pelanggan, total, jenis_transaksi, created_at, tanggal FROM transaksi WHERE kode_transaksi = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlT)) {
                ps.setString(1, kodeTransaksi);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Transaksi " + kodeTransaksi + " tidak ditemukan");
                    s.setKodeTransaksi(rs.getString("kode_transaksi"));
                    s.setPelanggan(rs.getString("nama_pelanggan"));
                    s.setTotal(rs.getInt("total"));
                    s.setJenis(rs.getString("jenis_transaksi"));
                    Timestamp dibuat = rs.getTimestamp("created_at");
                    s.setWaktu((dibuat != null ? dibuat : rs.getTimestamp("tanggal")).toLocalDateTime());
                    // Default bila log sudah dihapus: dianggap uang pas.
                    s.setBayar(s.getTotal());
                    s.setKembali(0);
                }
            }

            String sqlD = "SELECT COALESCE(b.nama_barang, '-') AS nama, d.qty, d.harga, d.subtotal FROM transaksi_detail d "
                    + "LEFT JOIN barang b ON b.kode_barang = d.kode_barang WHERE d.kode_transaksi = ? ORDER BY d.id_detail";
            try (PreparedStatement ps = con.prepareStatement(sqlD)) {
                ps.setString(1, kodeTransaksi);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        s.getItems().add(new Struk.Item(rs.getString("nama"), rs.getInt("qty"), rs.getInt("harga"), rs.getInt("subtotal")));
                    }
                }
            }

            String sqlL = "SELECT tanggal, bayar, kembali, keterangan FROM log_transaksi WHERE kode_transaksi = ? ORDER BY id_log LIMIT 1";
            try (PreparedStatement ps = con.prepareStatement(sqlL)) {
                ps.setString(1, kodeTransaksi);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        s.setBayar(rs.getInt("bayar"));
                        s.setKembali(rs.getInt("kembali"));
                        s.setKeterangan(rs.getString("keterangan"));
                        Timestamp t = rs.getTimestamp("tanggal");
                        if (t != null) s.setWaktu(t.toLocalDateTime());
                    }
                }
            }
        }
        return s;
    }
```

- [ ] **Step 5: Jalankan, pastikan lulus**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: `OK (59 tests)`.

- [ ] **Step 6: Commit**

```powershell
git add src/modern_pos/dao/TransaksiDAO.java test/modern_pos/dao/TransaksiDAOTest.java
git -c user.name="attia" -c user.email="attiaitem@gmail.com" commit -m "feat(modern_pos): return transaction code and load receipt data" -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 3: `Cetak`, `StrukDialog`, dan pemanggilnya

**Files:**
- Create: `src/modern_pos/utils/Cetak.java`, `src/modern_pos/view/StrukDialog.java`
- Modify: `src/modern_pos/controller/TransaksiController.java` (`processPayment`), `src/modern_pos/view/TransaksiView.java`, `src/modern_pos/controller/LogTransaksiController.java`, `src/modern_pos/view/LogTransaksiView.java` (handler `btnCetak`)

**Interfaces:**
- Consumes: `StrukFormatter.format`, `TransaksiDAO.simpanTransaksi` (return String), `TransaksiDAO.getStruk`.
- Produces: `Cetak.cetakBaris(List<String>, String) throws PrinterException` → boolean (false = batal); `Cetak.cetakKomponen(JComponent, double lebarMm, double tinggiMm, String) throws PrinterException` → boolean; `TransaksiView.showStruk(Struk)`, `LogTransaksiView.showStruk(Struk)`, `LogTransaksiController.tampilStruk(String)`.

Tidak ada test otomatis (UI + printer); compile + suite + grep + cek manual.

- [ ] **Step 1: `src/modern_pos/utils/Cetak.java`**

```java
package modern_pos.utils;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.List;
import javax.swing.JComponent;

public final class Cetak {
    private Cetak() {}

    private static final double PT_PER_MM = 72 / 25.4;

    // Struk thermal 58mm: baris teks monospace, tinggi kertas mengikuti jumlah baris.
    // Mengembalikan false bila user membatalkan dialog printer.
    public static boolean cetakBaris(final List<String> baris, String judul) throws PrinterException {
        final Font font = new Font(Font.MONOSPACED, Font.PLAIN, 7);
        final double tinggiBaris = 9, margin = 4;
        double lebar = 58 * PT_PER_MM;
        double tinggi = baris.size() * tinggiBaris + 2 * margin;

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName(judul);
        job.setPrintable((g, format, halaman) -> {
            if (halaman > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) g;
            g2.translate(format.getImageableX(), format.getImageableY());
            g2.setFont(font);
            for (int i = 0; i < baris.size(); i++) g2.drawString(baris.get(i), 0, (float) (tinggiBaris * (i + 1)));
            return Printable.PAGE_EXISTS;
        }, halaman(lebar, tinggi, margin, job));
        if (!job.printDialog()) return false;
        job.print();
        return true;
    }

    // Komponen Swing diskalakan agar muat di kertas lebarMm x tinggiMm (misal kartu angsuran 150x200).
    public static boolean cetakKomponen(final JComponent komponen, double lebarMm, double tinggiMm, String judul) throws PrinterException {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName(judul);
        job.setPrintable((g, format, halaman) -> {
            if (halaman > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) g;
            g2.translate(format.getImageableX(), format.getImageableY());
            double skala = Math.min(format.getImageableWidth() / komponen.getWidth(),
                    format.getImageableHeight() / komponen.getHeight());
            g2.scale(skala, skala);
            komponen.printAll(g2);
            return Printable.PAGE_EXISTS;
        }, halaman(lebarMm * PT_PER_MM, tinggiMm * PT_PER_MM, 10, job));
        if (!job.printDialog()) return false;
        job.print();
        return true;
    }

    private static PageFormat halaman(double lebar, double tinggi, double margin, PrinterJob job) {
        PageFormat pf = job.defaultPage();
        Paper p = new Paper();
        p.setSize(lebar, tinggi);
        p.setImageableArea(margin, margin, lebar - 2 * margin, tinggi - 2 * margin);
        pf.setPaper(p);
        pf.setOrientation(PageFormat.PORTRAIT);
        return pf;
    }
}
```

- [ ] **Step 2: `src/modern_pos/view/StrukDialog.java`**

```java
package modern_pos.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import modern_pos.model.Struk;
import modern_pos.utils.Cetak;
import modern_pos.utils.StrukFormatter;

public class StrukDialog extends JDialog {
    public StrukDialog(Frame parent, Struk struk) {
        super(parent, "Struk " + struk.getKodeTransaksi(), true);
        final List<String> baris = StrukFormatter.format(struk);

        JTextArea area = new JTextArea(String.join("\n", baris));
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setMargin(new Insets(10, 10, 10, 10));

        JButton btnCetak = new JButton("Cetak");
        JButton btnTutup = new JButton("Tutup");
        btnCetak.addActionListener(e -> {
            try {
                Cetak.cetakBaris(baris, "Struk " + struk.getKodeTransaksi());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Gagal mencetak: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        btnTutup.addActionListener(e -> dispose());

        JPanel tombol = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        tombol.add(btnCetak);
        tombol.add(btnTutup);

        setLayout(new BorderLayout());
        add(new JScrollPane(area), BorderLayout.CENTER);
        add(tombol, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(parent);
    }
}
```

- [ ] **Step 3: `TransaksiController.processPayment` membuka struk modern**

Tambah import `import modern_pos.model.Struk;` dan `import java.sql.SQLException;`.

Ganti seluruh blok `SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() { ... };` di `processPayment` dengan:

```java
        SwingWorker<Struk, Void> worker = new SwingWorker<Struk, Void>() {
            private String strukGagal;

            @Override protected Struk doInBackground() throws Exception {
                String kode = dao.simpanTransaksi(cart, finalTotal, bayar, kembali, pelanggan);
                try {
                    return dao.getStruk(kode);
                } catch (SQLException e) {
                    strukGagal = e.getMessage(); // transaksi sudah tersimpan; hanya struk yang gagal
                    return null;
                }
            }
            @Override protected void done() {
                view.setLoading(false);
                try {
                    Struk struk = get();
                    view.showSuccess("Transaksi Berhasil! Kembalian: Rp " + kembali);
                    cart.clear();
                    updateCartView();
                    loadBarang("");
                    view.resetForm();
                    if (struk != null) view.showStruk(struk);
                    else view.showError("Transaksi tersimpan, tapi struk gagal dimuat: " + strukGagal);
                } catch (Exception ex) {
                    Throwable c = ex.getCause() != null ? ex.getCause() : ex;
                    view.showError("Gagal menyimpan: " + c.getMessage());
                    loadBarang(""); // stok di layar mungkin basi
                }
            }
        };
```

(`worker.execute();` sesudahnya tetap.)

`TransaksiView`: tambah import `import modern_pos.model.Struk;` dan method:
```java
    public void showStruk(Struk struk) { new StrukDialog(this, struk).setVisible(true); }
```

- [ ] **Step 4: Log Transaksi**

`LogTransaksiController`: tambah import `import modern_pos.dao.TransaksiDAO;` dan `import modern_pos.model.Struk;`, field `private final TransaksiDAO transaksiDAO = new TransaksiDAO();`, dan method:

```java
    public void tampilStruk(final String kode) {
        SwingWorker<Struk, Void> worker = new SwingWorker<Struk, Void>() {
            @Override protected Struk doInBackground() throws Exception {
                return transaksiDAO.getStruk(kode);
            }
            @Override protected void done() {
                try {
                    view.showStruk(get());
                } catch (Exception ex) {
                    Throwable c = ex.getCause() != null ? ex.getCause() : ex;
                    view.showError("Gagal memuat struk: " + c.getMessage());
                }
            }
        };
        worker.execute();
    }
```

`LogTransaksiView`: tambah import `import modern_pos.model.Struk;`. Ganti seluruh handler `btnCetak.addActionListener(e -> { ... });` dengan:

```java
        btnCetak.addActionListener(e -> {
            int row = tblLog.getSelectedRow();
            if (row == -1) { showError("Pilih transaksi di tabel untuk dicetak struknya!"); return; }
            LogTransaksi log = currentList.get(row);
            if ("KREDIT".equalsIgnoreCase(log.getTipeTransaksi())) {
                showSuccess("Transaksi kredit: lihat kartu angsuran di menu Utang.");
                return;
            }
            controller.tampilStruk(log.getKodeTransaksi());
        });
```

Tambah method:
```java
    public void showStruk(Struk struk) { new StrukDialog(this, struk).setVisible(true); }
```

- [ ] **Step 5: Compile, suite, grep**

```powershell
powershell -ExecutionPolicy Bypass -File run-tests.ps1
Select-String -Path src\modern_pos\*\*.java -Pattern 'gui\.' | Where-Object { $_.Line -notmatch '^\s*//' }
```
Expected: `OK (59 tests)`; grep tanpa output.

- [ ] **Step 6: Commit**

```powershell
git add src/modern_pos
git -c user.name="attia" -c user.email="attiaitem@gmail.com" commit -m "feat(modern_pos): modern receipt dialog after checkout and from log" -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 4: `bayarPerBulan` dan `UtangDAO.getKartu`

**Files:**
- Create: `src/modern_pos/model/KartuAngsuran.java`, `test/modern_pos/model/UtangTest.java`
- Modify: `src/modern_pos/model/Utang.java`, `src/modern_pos/dao/UtangDAO.java`
- Test: `test/modern_pos/dao/UtangDAOTest.java`

**Interfaces:**
- Produces: `int Utang.bayarPerBulan()`; `KartuAngsuran(Utang utang, String namaBarang)` + `getUtang()`, `getNamaBarang()`; `KartuAngsuran UtangDAO.getKartu(String kodeUtang) throws SQLException` — tidak ada → `SQLException("Utang <kode> tidak ditemukan")`.

- [ ] **Step 1: Tulis test gagal**

`test/modern_pos/model/UtangTest.java`:
```java
package modern_pos.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class UtangTest {
    private static Utang utang(int harga, int dp, int cicilan) {
        Utang u = new Utang();
        u.setHargaBarang(harga);
        u.setDp(dp);
        u.setJumlahCicilan(cicilan);
        return u;
    }

    @Test
    public void bayarPerBulanPasTanpaSisa() {
        assertEquals(200000, utang(1500000, 500000, 5).bayarPerBulan());
    }

    @Test
    public void bayarPerBulanDibulatkanKeAtas() {
        // 1.999.000 / 12 = 166.583,33 -> 166.584 supaya total angsuran tidak kurang dari sisa utang
        assertEquals(166584, utang(2000000, 1000, 12).bayarPerBulan());
    }

    @Test
    public void cicilanNolAtauTanpaSisaHasilNol() {
        assertEquals(0, utang(1000000, 0, 0).bayarPerBulan());
        assertEquals(0, utang(1000000, 1000000, 5).bayarPerBulan());
        assertEquals(0, utang(1000000, 1200000, 5).bayarPerBulan());
    }
}
```

Tambah ke `UtangDAOTest.java` (import `import modern_pos.model.KartuAngsuran;`):
```java
    @Test
    public void getKartuMemuatNamaBarangDanQty() throws Exception {
        dao.tambahUtang(utang("UTG-1", "B001", 130000, 30000, 2));
        KartuAngsuran k = dao.getKartu("UTG-1");
        assertEquals("Beras 5kg", k.getNamaBarang());
        assertEquals(2, k.getUtang().getQty());
        assertEquals("Siti", k.getUtang().getNama());
        assertEquals(130000, k.getUtang().getHargaBarang());
        assertNotNull(k.getUtang().getJatuhTempo());
    }

    @Test(expected = SQLException.class)
    public void getKartuTidakAdaDitolak() throws Exception {
        dao.getKartu("TIDAK-ADA");
    }
```

- [ ] **Step 2: Jalankan, pastikan gagal**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: compile error `cannot find symbol ... bayarPerBulan` / `KartuAngsuran` / `getKartu`.

- [ ] **Step 3: `Utang.bayarPerBulan`**

Tambah di `Utang.java`:
```java
    // Dibulatkan ke atas supaya total angsuran tidak kurang dari sisa utang.
    public int bayarPerBulan() {
        int sisa = hargaBarang - dp;
        if (jumlahCicilan <= 0 || sisa <= 0) return 0;
        return (sisa + jumlahCicilan - 1) / jumlahCicilan;
    }
```

- [ ] **Step 4: `KartuAngsuran`**

```java
package modern_pos.model;

public class KartuAngsuran {
    private final Utang utang;
    private final String namaBarang;

    public KartuAngsuran(Utang utang, String namaBarang) {
        this.utang = utang;
        this.namaBarang = namaBarang;
    }
    public Utang getUtang() { return utang; }
    public String getNamaBarang() { return namaBarang; }
}
```

- [ ] **Step 5: `UtangDAO` — mapping bersama dan `getKartu`**

Tambah import `import modern_pos.model.KartuAngsuran;`. Di `getUtangList`, ganti isi loop `while (rs.next()) { Utang u = new Utang(); ... list.add(u); }` dengan:
```java
                while (rs.next()) list.add(map(rs));
```

Tambah method:
```java
    private static Utang map(ResultSet rs) throws SQLException {
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
        u.setQty(rs.getInt("qty"));
        if (rs.getDate("jatuh_tempo") != null) u.setJatuhTempo(rs.getDate("jatuh_tempo").toLocalDate());
        return u;
    }

    public KartuAngsuran getKartu(String kodeUtang) throws SQLException {
        String sql = "SELECT u.*, COALESCE(b.nama_barang, '-') AS nama_barang FROM utang u "
                + "LEFT JOIN barang b ON b.kode_barang = u.kode_barang WHERE u.kode_utang = ?";
        try (Connection con = koneksi.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, kodeUtang);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Utang " + kodeUtang + " tidak ditemukan");
                return new KartuAngsuran(map(rs), rs.getString("nama_barang"));
            }
        }
    }
```

- [ ] **Step 6: Jalankan, pastikan lulus**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: `OK (64 tests)`.

- [ ] **Step 7: Commit**

```powershell
git add src/modern_pos test/modern_pos
git -c user.name="attia" -c user.email="attiaitem@gmail.com" commit -m "feat(modern_pos): installment amount (rounded up) and installment card data" -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 5: `KartuAngsuranDialog` dan layar Utang

**Files:**
- Create: `src/modern_pos/view/KartuAngsuranDialog.java`
- Modify: `src/modern_pos/controller/UtangController.java`, `src/modern_pos/view/UtangView.java`

**Interfaces:**
- Consumes: `UtangDAO.getKartu`, `KartuAngsuran`, `Utang.bayarPerBulan`, `Toko`, `Cetak.cetakKomponen`, `StrukFormatter.rupiah`.
- Produces: `UtangController.tampilKartu(String)`, `UtangView.showKartu(KartuAngsuran)`.

Tidak ada test otomatis (UI + printer).

- [ ] **Step 1: `src/modern_pos/view/KartuAngsuranDialog.java`**

```java
package modern_pos.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import modern_pos.model.KartuAngsuran;
import modern_pos.model.Utang;
import modern_pos.utils.Cetak;
import modern_pos.utils.StrukFormatter;
import modern_pos.utils.Toko;

// Kartu angsuran kertas 15x20 cm (567x756 px @96dpi): baris kosong ditulis tangan saat pelanggan membayar.
public class KartuAngsuranDialog extends JDialog {
    private static final int BARIS_ANGSURAN = 15;

    public KartuAngsuranDialog(Frame parent, KartuAngsuran k) {
        super(parent, "Kartu Angsuran " + k.getUtang().getKodeUtang(), true);
        Utang u = k.getUtang();

        JPanel kartu = new JPanel();
        kartu.setLayout(new BoxLayout(kartu, BoxLayout.Y_AXIS));
        kartu.setBackground(Color.WHITE);
        kartu.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        kartu.setPreferredSize(new Dimension(567, 756));

        kartu.add(judul("KARTU ANGSURAN", 16));
        kartu.add(judul(Toko.NAMA, 13));

        JPanel info = new JPanel(new GridLayout(0, 2, 6, 2));
        info.setBackground(Color.WHITE);
        info.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        baris(info, "Kode Utang", u.getKodeUtang());
        baris(info, "Nama", u.getNama());
        baris(info, "Telepon", u.getTelepon());
        baris(info, "Alamat", u.getAlamat());
        baris(info, "Nama Barang", k.getNamaBarang());
        baris(info, "Qty", String.valueOf(u.getQty()));
        baris(info, "Harga Barang", StrukFormatter.rupiah(u.getHargaBarang()));
        baris(info, "DP", StrukFormatter.rupiah(u.getDp()));
        baris(info, "Jumlah Cicilan", u.getJumlahCicilan() + "x");
        baris(info, "Jatuh Tempo", u.getJatuhTempo() != null ? u.getJatuhTempo().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "-");
        baris(info, "Bayar Perbulan", StrukFormatter.rupiah(u.bayarPerBulan()));
        kartu.add(info);

        DefaultTableModel model = new DefaultTableModel(new String[]{"Angsuran", "Tanggal", "Nominal", "Sisa", "Paraf"}, BARIS_ANGSURAN) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabel = new JTable(model);
        tabel.setRowHeight(22);
        tabel.setShowGrid(true);
        tabel.setGridColor(Color.BLACK);
        JPanel wadahTabel = new JPanel(new BorderLayout());
        wadahTabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        wadahTabel.add(tabel.getTableHeader(), BorderLayout.NORTH);
        wadahTabel.add(tabel, BorderLayout.CENTER);
        kartu.add(wadahTabel);

        JPanel ttd = new JPanel(new GridLayout(3, 2));
        ttd.setBackground(Color.WHITE);
        ttd.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        ttd.add(tengah("Pengawas"));
        ttd.add(tengah("Peminjam"));
        ttd.add(tengah(" "));
        ttd.add(tengah(" "));
        ttd.add(tengah("(" + Toko.PENGAWAS + ")"));
        ttd.add(tengah("(..............................)"));
        kartu.add(ttd);

        JButton btnCetak = new JButton("Cetak");
        JButton btnTutup = new JButton("Tutup");
        btnCetak.addActionListener(e -> {
            try {
                Cetak.cetakKomponen(kartu, 150, 200, "Kartu Angsuran " + u.getKodeUtang());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Gagal mencetak: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        btnTutup.addActionListener(e -> dispose());
        JPanel tombol = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        tombol.add(btnCetak);
        tombol.add(btnTutup);

        setLayout(new BorderLayout());
        add(kartu, BorderLayout.CENTER);
        add(tombol, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(parent);
    }

    private static JLabel judul(String teks, int ukuran) {
        JLabel l = tengah(teks);
        l.setFont(new Font("Segoe UI", Font.BOLD, ukuran));
        l.setAlignmentX(0.5f);
        return l;
    }

    private static JLabel tengah(String teks) {
        JLabel l = new JLabel(teks, SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return l;
    }

    private static void baris(JPanel p, String label, String nilai) {
        JLabel l = new JLabel(label + " :");
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JLabel v = new JLabel(nilai != null ? nilai : "-");
        v.setFont(new Font("Segoe UI", Font.BOLD, 12));
        p.add(l);
        p.add(v);
    }
}
```

- [ ] **Step 2: `UtangController` — `tampilKartu` dan pesan error**

Tambah import `import modern_pos.model.KartuAngsuran;`. Tambah helper dan method:

```java
    private static String pesan(Exception ex) {
        Throwable c = ex.getCause() != null ? ex.getCause() : ex;
        return c.getMessage();
    }

    public void tampilKartu(final String kode) {
        SwingWorker<KartuAngsuran, Void> worker = new SwingWorker<KartuAngsuran, Void>() {
            @Override protected KartuAngsuran doInBackground() throws Exception {
                return dao.getKartu(kode);
            }
            @Override protected void done() {
                try { view.showKartu(get()); }
                catch (Exception ex) { view.showError("Gagal memuat kartu: " + pesan(ex)); }
            }
        };
        worker.execute();
    }
```

Lalu di semua `view.showError("... " + ex.getMessage())` dalam `UtangController` ganti `ex.getMessage()` dengan `pesan(ex)`.

Verifikasi: `Select-String -Path src\modern_pos\controller\UtangController.java -Pattern 'ex\.getMessage\(\)'` tanpa output.

- [ ] **Step 3: `UtangView` — tombol Kartu**

Tambah import `import modern_pos.model.KartuAngsuran;`. Setelah deklarasi `btnLunas`, tambah:
```java
        JButton btnKartu = SwingHelper.createFlatButton("Kartu", new Color(96, 125, 139), new Color(69, 90, 100));
```
Ganti `actionPanel.add(btnLunas);` dengan:
```java
        actionPanel.add(btnKartu);
        actionPanel.add(btnLunas);
```
Setelah handler `btnLunas.addActionListener(...)`, tambah:
```java
        btnKartu.addActionListener(e -> {
            int row = tblUtang.getSelectedRow();
            if (row == -1) { showError("Pilih utang yang ingin dilihat kartunya!"); return; }
            controller.tampilKartu(currentList.get(row).getKodeUtang());
        });
```
Tambah method:
```java
    public void showKartu(KartuAngsuran k) { new KartuAngsuranDialog(this, k).setVisible(true); }
```

- [ ] **Step 4: Compile + suite**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: `OK (64 tests)`.

- [ ] **Step 5: Commit**

```powershell
git add src/modern_pos
git -c user.name="attia" -c user.email="attiaitem@gmail.com" commit -m "feat(modern_pos): printable 15x20cm installment card from Utang screen" -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 6: Pendapatan dashboard = uang masuk

**Files:**
- Modify: `src/modern_pos/dao/DashboardDAO.java` (query pendapatan)
- Create: `test/modern_pos/dao/DashboardDAOTest.java`

**Interfaces:**
- Consumes: `TransaksiDAO.simpanTransaksi`, `UtangDAO.tambahUtang/tandaiLunas`, `TestDb`.
- Produces: `DashboardSummary.getPendapatanHariIni()` = TUNAI total + KREDIT DP, hari ini.

- [ ] **Step 1: Tulis test gagal `test/modern_pos/dao/DashboardDAOTest.java`**

```java
package modern_pos.dao;

import java.time.LocalDate;
import java.util.Arrays;
import modern_pos.TestDb;
import modern_pos.model.Barang;
import modern_pos.model.CartItem;
import modern_pos.model.DashboardSummary;
import modern_pos.model.Utang;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class DashboardDAOTest {

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

    private static void jualTunai(int total) throws Exception {
        Barang b = new Barang();
        b.setKodeBarang("B001");
        b.setNamaBarang("Beras 5kg");
        b.setHarga(total);
        b.setStok(10);
        new TransaksiDAO().simpanTransaksi(Arrays.asList(new CartItem(b, 1)), total, total, 0, "Budi");
    }

    @Test
    public void pendapatanAdalahUangMasukTanpaDobel() throws Exception {
        jualTunai(100000);                                            // tunai 100.000
        new UtangDAO().tambahUtang(utang("UTG-A", "B001", 500000, 50000)); // DP 50.000 (bukan 500.000)
        new UtangDAO().tambahUtang(utang("UTG-B", "B002", 40000, 10000));  // DP 10.000
        new UtangDAO().tandaiLunas("UTG-B");                          // pelunasan 30.000

        DashboardSummary s = new DashboardDAO().getSummary();
        assertEquals(190000, s.getPendapatanHariIni());
        assertEquals(4, s.getTotalTransaksiHariIni());
    }

    @Test
    public void utangKemarinTidakDihitungHariIni() throws Exception {
        new UtangDAO().tambahUtang(utang("UTG-A", "B001", 500000, 50000));
        try (java.sql.Connection c = config.koneksi.open(); java.sql.Statement st = c.createStatement()) {
            st.executeUpdate("UPDATE transaksi SET tanggal = DATE_SUB(NOW(), INTERVAL 1 DAY)");
        }
        new UtangDAO().tandaiLunas("UTG-A");                          // pelunasan hari ini 450.000

        assertEquals(450000, new DashboardDAO().getSummary().getPendapatanHariIni());
    }
}
```

- [ ] **Step 2: Jalankan, pastikan gagal**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: `pendapatanAdalahUangMasukTanpaDobel` FAIL (`expected:<190000> but was:<670000>`).

- [ ] **Step 3: Ganti query pendapatan di `DashboardDAO`**

Ganti:
```java
            String sqlTrx = "SELECT SUM(total) as pendapatan, COUNT(kode_transaksi) as total_trx FROM transaksi WHERE DATE(tanggal) = CURDATE()";
```
dengan:
```java
            // Uang masuk: TUNAI (penjualan + pelunasan) dihitung total, KREDIT hanya DP-nya.
            String sqlTrx = "SELECT COALESCE(SUM(CASE WHEN t.jenis_transaksi = 'KREDIT' THEN COALESCE(u.dp, 0) ELSE t.total END), 0) AS pendapatan, "
                    + "COUNT(*) AS total_trx FROM transaksi t LEFT JOIN utang u ON u.kode_transaksi = t.kode_transaksi "
                    + "WHERE DATE(t.tanggal) = CURDATE()";
```

- [ ] **Step 4: Jalankan, pastikan lulus**

Run: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`
Expected: `OK (66 tests)`.

- [ ] **Step 5: Commit**

```powershell
git add src/modern_pos/dao/DashboardDAO.java test/modern_pos/dao/DashboardDAOTest.java
git -c user.name="attia" -c user.email="attiaitem@gmail.com" commit -m "fix(modern_pos): dashboard revenue counts cash in, not full credit twice" -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 7: Verifikasi akhir

- [ ] **Step 1: Suite + tidak ada referensi stack lama**

```powershell
powershell -ExecutionPolicy Bypass -File run-tests.ps1
Select-String -Path src\modern_pos\*\*.java -Pattern '\bgui\.|\bcontroller\.(barang|transaksi|utang|snapshot)controller' | Where-Object { $_.Line -notmatch '^\s*//' }
```
Expected: `OK (66 tests)`; grep tanpa output.

- [ ] **Step 2: Cek manual (user)**

NetBeans → Clean and Build → jalankan `modern_pos.view.LoginView`:
1. Transaksi: jual 2 barang → struk muncul berisi item/total/kembalian → Cetak ke printer thermal (atau "Microsoft Print to PDF") → 58mm, teks tidak terpotong.
2. Log Transaksi: pilih transaksi TUNAI → Cetak Struk → struk sama. Pilih KREDIT → pesan arahkan ke Utang.
3. Utang: pilih utang → Kartu → data + bayar perbulan + 15 baris → Cetak → muat di 15×20 cm.
4. Dashboard: pendapatan hari ini = tunai + DP + pelunasan.
