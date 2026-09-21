package modern_pos.controller;
import java.util.List;
import javax.swing.SwingWorker;
import modern_pos.dao.UtangDAO;
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

    public void loadData(final String keyword) {
        view.setLoading(true);
        SwingWorker<List<Utang>, Void> worker = new SwingWorker<List<Utang>, Void>() {
            @Override protected List<Utang> doInBackground() throws Exception {
                return dao.getUtangList(keyword);
            }
            @Override protected void done() {
                view.setLoading(false);
                try { view.populateTable(get()); } 
                catch (Exception ex) { view.showError("Gagal memuat utang: " + ex.getMessage()); }
            }
        };
        worker.execute();
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
                } catch (Exception ex) { view.showError("Gagal menyimpan: " + ex.getMessage()); }
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
                } catch (Exception ex) { view.showError("Gagal menghapus: " + ex.getMessage()); }
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
                } catch (Exception ex) { view.showError("Gagal update status: " + ex.getMessage()); }
            }
        };
        worker.execute();
    }
}