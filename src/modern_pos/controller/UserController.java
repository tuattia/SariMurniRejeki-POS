package modern_pos.controller;

import modern_pos.dao.UserDAO;
import modern_pos.utils.Async;
import modern_pos.view.UserDialog;

public class UserController {
    private final UserDAO dao = new UserDAO();
    private UserDialog view;

    public void setView(UserDialog view) {
        this.view = view;
        muat();
    }

    public void muat() {
        Async.ambil(dao::listUser, view::tampilkan, msg -> view.showError("Gagal memuat user: " + msg));
    }

    private void jalankan(Async.Aksi aksi, String sukses) {
        Async.kerjakan(aksi, () -> { view.showSuccess(sukses); muat(); }, msg -> { view.showError(msg); muat(); });
    }

    public void tambah(String nama, String username, String password, String role) {
        jalankan(() -> dao.tambahUser(nama, username, password, role), "User berhasil ditambahkan!");
    }

    public void ubah(int id, String nama, String role, int idPelaku) {
        jalankan(() -> dao.ubahUser(id, nama, role, idPelaku), "User berhasil diubah!");
    }

    public void resetPassword(int id, String password) {
        jalankan(() -> dao.resetPassword(id, password), "Password berhasil direset!");
    }

    public void hapus(int id, int idPelaku) {
        jalankan(() -> dao.hapusUser(id, idPelaku), "User berhasil dihapus!");
    }
}
