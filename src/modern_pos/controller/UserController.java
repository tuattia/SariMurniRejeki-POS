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
