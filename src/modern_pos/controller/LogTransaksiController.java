package modern_pos.controller;
import java.util.List;
import javax.swing.SwingWorker;
import modern_pos.dao.LogTransaksiDAO;
import modern_pos.dao.TransaksiDAO;
import modern_pos.model.Struk;
import modern_pos.model.LogTransaksi;
import modern_pos.view.LogTransaksiView;

public class LogTransaksiController {
    private LogTransaksiView view;
    private final LogTransaksiDAO dao;

    private final TransaksiDAO transaksiDAO = new TransaksiDAO();

    public LogTransaksiController() { this.dao = new LogTransaksiDAO(); }

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
    public void setView(LogTransaksiView view) {
        this.view = view;
        loadData("");
    }

    public void loadData(final String keyword) {
        view.setLoading(true);
        SwingWorker<List<LogTransaksi>, Void> worker = new SwingWorker<List<LogTransaksi>, Void>() {
            @Override protected List<LogTransaksi> doInBackground() throws Exception {
                return dao.getLogList(keyword);
            }
            @Override protected void done() {
                view.setLoading(false);
                try { view.populateTable(get()); } 
                catch (Exception ex) { view.showError("Gagal memuat log transaksi: " + ex.getMessage()); }
            }
        };
        worker.execute();
    }

    public void hapusLog(final String kode) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                dao.hapusLog(kode); return null;
            }
            @Override protected void done() {
                try {
                    get(); view.showSuccess("Data riwayat berhasil dihapus!"); loadData("");
                } catch (Exception ex) { view.showError("Gagal menghapus log: " + ex.getMessage()); }
            }
        };
        worker.execute();
    }
}