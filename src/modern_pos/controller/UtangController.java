package modern_pos.controller;
import java.sql.SQLException;
import java.util.List;
import modern_pos.dao.BarangDAO;
import modern_pos.model.Barang;
import javax.swing.SwingWorker;
import modern_pos.dao.UtangDAO;
import modern_pos.model.KartuAngsuran;
import modern_pos.model.Utang;
import modern_pos.view.UtangView;

public class UtangController {
    private UtangView view;
    private final UtangDAO dao;

    public UtangController() { this.dao = new UtangDAO(); }
    public void setView(UtangView view) {
        this.view = view;
        loadData("");
    }

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

    public void loadData(final String keyword) {
        view.setLoading(true);
        SwingWorker<List<Utang>, Void> worker = new SwingWorker<List<Utang>, Void>() {
            @Override protected List<Utang> doInBackground() throws Exception {
                return dao.getUtangList(keyword);
            }
            @Override protected void done() {
                view.setLoading(false);
                try { view.populateTable(get()); } 
                catch (Exception ex) { view.showError("Gagal memuat utang: " + pesan(ex)); }
            }
        };
        worker.execute();
    }

    // ponytail: dipanggil sinkron di EDT saat form dibuka; tabel barang kecil. Pindah ke SwingWorker bila terasa lambat.
    public List<Barang> daftarBarang() throws SQLException {
        return new BarangDAO().getAllBarang("");
    }

    public void simpanUtang(final Utang u, final boolean isEdit) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                if (isEdit) dao.updateUtang(u); else dao.tambahUtang(u);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    view.showSuccess("Data utang berhasil disimpan!");
                    loadData("");
                } catch (Exception ex) { view.showError("Gagal menyimpan: " + pesan(ex)); }
            }
        };
        worker.execute();
    }

    public void hapusUtang(final String kode) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                dao.hapusUtang(kode); return null;
            }
            @Override protected void done() {
                try {
                    get(); view.showSuccess("Data berhasil dihapus!"); loadData("");
                } catch (Exception ex) { view.showError("Gagal menghapus: " + pesan(ex)); }
            }
        };
        worker.execute();
    }

    public void tandaiLunas(final String kode) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                dao.tandaiLunas(kode); return null;
            }
            @Override protected void done() {
                try {
                    get(); view.showSuccess("Utang berhasil ditandai LUNAS!"); loadData("");
                } catch (Exception ex) { view.showError("Gagal update status: " + pesan(ex)); }
            }
        };
        worker.execute();
    }
}