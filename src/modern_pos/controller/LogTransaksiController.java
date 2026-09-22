package modern_pos.controller;
import modern_pos.dao.LogTransaksiDAO;
import modern_pos.dao.TransaksiDAO;
import modern_pos.utils.Async;
import modern_pos.view.LogTransaksiView;

public class LogTransaksiController {
    private LogTransaksiView view;
    private final LogTransaksiDAO dao = new LogTransaksiDAO();
    private final TransaksiDAO transaksiDAO = new TransaksiDAO();

    public void setView(LogTransaksiView view) {
        this.view = view;
        loadData("");
    }

    public void loadData(final String keyword) {
        view.setLoading(true);
        Async.ambil(() -> dao.getLogList(keyword),
                list -> { view.setLoading(false); view.populateTable(list); },
                msg -> { view.setLoading(false); view.showError("Gagal memuat log transaksi: " + msg); });
    }

    public void tampilStruk(final String kode) {
        Async.ambil(() -> transaksiDAO.getStruk(kode), view::showStruk,
                msg -> view.showError("Gagal memuat struk: " + msg));
    }

    public void hapusLog(final String kode) {
        Async.kerjakan(() -> dao.hapusLog(kode),
                () -> { view.showSuccess("Data riwayat berhasil dihapus!"); loadData(""); },
                msg -> view.showError("Gagal menghapus log: " + msg));
    }
}
