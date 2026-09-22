package modern_pos.controller;
import modern_pos.dao.BarangDAO;
import modern_pos.dao.StokDAO;
import modern_pos.model.Barang;
import modern_pos.utils.Async;
import modern_pos.view.StockView;

public class StockController {
    private StockView view;
    private final BarangDAO dao = new BarangDAO();
    private final StokDAO stokDAO = new StokDAO();

    public void setView(StockView view) {
        this.view = view;
        loadData("");
    }

    public void loadData(final String keyword) {
        view.setLoading(true);
        Async.ambil(() -> dao.getAllBarang(keyword),
                list -> { view.setLoading(false); view.populateTable(list); },
                msg -> { view.setLoading(false); view.showError("Gagal memuat data stock: " + msg); });
    }

    public void restock(final String kode, final int qty, final String ket) {
        Async.kerjakan(() -> stokDAO.restock(kode, qty, ket),
                () -> { view.showSuccess("Restock berhasil!"); loadData(""); },
                msg -> { view.showError("Gagal restock: " + msg); loadData(""); });
    }

    public void tampilRiwayat(final Barang b) {
        Async.ambil(() -> stokDAO.riwayat(b.getKodeBarang()), list -> view.showRiwayat(b, list),
                msg -> view.showError("Gagal memuat riwayat: " + msg));
    }

    // lama == null berarti tambah barang baru.
    public void simpanBarang(final Barang baru, final Barang lama) {
        Async.kerjakan(() -> {
            if (lama != null) dao.updateBarang(baru, lama.getStok());
            else dao.tambahBarang(baru);
        }, () -> { view.showSuccess("Data barang berhasil disimpan!"); loadData(""); },
           msg -> { view.showError("Gagal menyimpan: " + msg); loadData(""); });
    }

    public void hapusBarang(final String kode) {
        Async.kerjakan(() -> dao.hapusBarang(kode),
                () -> { view.showSuccess("Barang berhasil dihapus!"); loadData(""); },
                msg -> view.showError("Gagal menghapus: " + msg));
    }
}
