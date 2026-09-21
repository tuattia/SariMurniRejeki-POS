package modern_pos.controller;
import java.util.List;
import javax.swing.SwingWorker;
import modern_pos.dao.BarangDAO;
import modern_pos.model.Barang;
import modern_pos.view.StockView;

public class StockController {
    private StockView view;
    private final BarangDAO dao;

    public StockController() {
        this.dao = new BarangDAO();
    }

    public void setView(StockView view) {
        this.view = view;
        loadData("");
    }

    public void loadData(final String keyword) {
        view.setLoading(true);
        SwingWorker<List<Barang>, Void> worker = new SwingWorker<List<Barang>, Void>() {
            @Override protected List<Barang> doInBackground() throws Exception {
                return dao.getAllBarang(keyword);
            }
            @Override protected void done() {
                view.setLoading(false);
                try {
                    view.populateTable(get());
                } catch (Exception ex) {
                    view.showError("Gagal memuat data stock: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    public void simpanBarang(Barang b, boolean isEdit) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                if (isEdit) dao.updateBarang(b);
                else dao.tambahBarang(b);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    view.showSuccess("Data barang berhasil disimpan!");
                    loadData("");
                } catch (Exception ex) {
                    view.showError("Gagal menyimpan: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    public void hapusBarang(final String kode) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                dao.hapusBarang(kode);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    view.showSuccess("Barang berhasil dihapus!");
                    loadData("");
                } catch (Exception ex) {
                    view.showError("Gagal menghapus: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }
}