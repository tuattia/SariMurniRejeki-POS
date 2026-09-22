# Sub-proyek 2c: Akun (kelola user, hak akses)

Tanggal: 2026-09-22
Status: menunggu review
Sebelumnya: 2b Cetak (sudah di `main`)

## Konteks

- Stack lama: `gui.register` dibuka dari layar login tanpa autentikasi; siapa
  pun bisa membuat akun `admin`. Role: `admin`, `member`. Login lama hanya
  meneruskan `admin`; `member` tidak masuk ke mana pun.
- `user.username` tidak UNIQUE. Data saat ini: `admin` dan `diva`, keduanya admin.
- Password SHA-256 tanpa salt (`login.enkripsi.sha256`) — keputusan user:
  **tetap** di sub-proyek ini.
- `modern_pos` belum membatasi apa pun per role.

Keputusan user: pembuatan akun hanya oleh admin (menu Kelola User);
`member` = operasional saja.

## Tujuan & kriteria sukses

- Admin bisa tambah user, ubah nama/role, reset password, hapus user dari
  `modern_pos`.
- Tidak mungkin terkunci: admin terakhir tidak bisa dihapus/diturunkan,
  admin tidak bisa menghapus dirinya sendiri.
- `member` tidak melihat tombol yang bukan haknya.
- Username unik (DAO + constraint DB).

## A. `modern_pos.utils.Akses`

```java
public static boolean admin()
```
`true` bila `Session.currentUser != null` dan `hakAkses` = `"admin"`
(abaikan besar/kecil, trim). Selain itu `false`.

Konstanta role: `Akses.ADMIN = "admin"`, `Akses.MEMBER = "member"`.

## B. `UserDAO`

Model `modern_pos.model.User` dipakai apa adanya (`id`, `nama`, `username`, `hakAkses`).

- `List<User> listUser()` — urut `nama`.
- `void tambahUser(String nama, String username, String password, String hakAkses)`.
- `void ubahUser(int id, String nama, String hakAkses)`.
- `void resetPassword(int id, String passwordBaru)`.
- `void hapusUser(int id, int idPelaku)`.

Validasi (`IllegalArgumentException`, dicek sebelum ke DB):
- nama dan username tidak kosong (setelah trim; username disimpan ter-trim);
- `hakAkses` ∈ {`admin`, `member`} (disimpan huruf kecil);
- password (tambah/reset) minimal 6 karakter.

Aturan DB (`SQLException`, dalam transaksi):
- username sudah dipakai → `"Username <u> sudah dipakai"`;
- id tidak ada → `"User tidak ditemukan"`;
- `hapusUser` dengan `id == idPelaku` → `"Tidak bisa menghapus akun sendiri"`;
- menghapus atau mengubah ke `member` user yang merupakan satu-satunya admin
  → `"Minimal harus ada satu admin"`. Hitung admin memakai
  `SELECT ... FOR UPDATE` supaya dua admin yang saling menurunkan bersamaan
  tidak sama-sama lolos.

Password disimpan `enkripsi.sha256(password)` (sama dengan login saat ini).
`authenticate` tidak berubah.

## C. `UserView` (Kelola User)

- Sidebar standar + tabel Nama, Username, Hak Akses.
- Tombol: Tambah (nama, username, password, role), Edit (nama, role;
  username tidak bisa diubah), Reset Password (password baru + konfirmasi
  harus sama), Hapus (konfirmasi).
- `UserController` dengan `SwingWorker`, pesan error dari `getCause()`.
- Dibuka dari tombol sidebar "Kelola User" di `DashboardView`, hanya terlihat
  bila `Akses.admin()`. `idPelaku` = `Session.currentUser.getId()`.

## D. Pembatasan per role (UI)

Bila `!Akses.admin()`, tombol berikut `setVisible(false)`:
- `StockView`: Tambah, Edit, Hapus, Stock Snapshot (Restock, Riwayat tetap).
- `UtangView`: Edit, Hapus (Tambah, Tandai Lunas, Kartu tetap).
- `LogTransaksiView`: Hapus Riwayat (Cetak Struk tetap).
- `DashboardView`: Kelola User.

Aplikasi desktop terhubung langsung ke DB, jadi pembatasan di UI adalah
kontrol yang realistis; stack lama tidak diubah.

## E. Migrasi

`migrations/2026-09-22-user-username-unique.sql`:
```sql
ALTER TABLE user ADD UNIQUE KEY uq_user_username (username);
```
Diterapkan ke DB test di awal; ke DB utama hanya setelah izin eksplisit user
(data saat ini tidak punya username dobel). `schema.sql` di-dump ulang.

## Test

- `AksesTest`: session null → false; member → false; `" Admin "` → true.
- `UserDAOTest`: tambah user lalu `authenticate` berhasil; username dobel
  ditolak; role tidak valid ditolak; password < 6 ditolak; nama kosong ditolak;
  reset password lalu login dengan password baru berhasil dan lama gagal;
  ubah nama/role; admin terakhir tidak bisa diturunkan; admin terakhir tidak
  bisa dihapus; hapus diri sendiri ditolak; hapus member berhasil;
  `listUser` urut nama.
- Visibilitas tombol per role: cek manual.

## Di luar cakupan

- Ganti algoritma hash password (keputusan user: nanti).
- Mengubah register/login stack lama.
- Entry point tetap `gui.login` (sub-proyek 3).
