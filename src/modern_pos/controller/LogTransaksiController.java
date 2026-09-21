package modern_pos.controller;
import java.util.List;
import javax.swing.SwingWorker;
import modern_pos.dao.LogTransaksiDAO;
import modern_pos.model.LogTransaksi;
import modern_pos.view.LogTransaksiView;

public class LogTransaksiController {
    private LogTransaksiView view;
    private final LogTransaksiDAO dao;

    public LogTransaksiController() { this.dao = new LogTransaksiDAO(); }
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