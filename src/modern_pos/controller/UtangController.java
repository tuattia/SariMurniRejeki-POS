package modern_pos.controller;
import java.sql.SQLException;
import java.util.List;
import modern_pos.dao.BarangDAO;
import modern_pos.dao.UtangDAO;
import modern_pos.model.Barang;
import modern_pos.model.Utang;
import modern_pos.utils.Async;
import modern_pos.view.UtangView;

public class UtangController {
    private UtangView view;
    private final UtangDAO dao = new UtangDAO();

    public void setView(UtangView view) {
        this.view = view;
        loadData("");
    }

    public void loadData(final String keyword) {
        view.setLoading(true);
        Async.ambil(() -> dao.getUtangList(keyword),
                list -> { view.setLoading(false); view.populateTable(list); },
                msg -> { view.setLoading(false); view.showError("Gagal memuat utang: " + msg); });
    }

    public void tampilKartu(final String kode) {
        Async.ambil(() -> dao.getKartu(kode), view::showKartu,
                msg -> view.showError("Gagal memuat kartu: " + msg));
    }

    // ponytail: dipanggil sinkron di EDT saat form dibuka; tabel barang kecil. Pindah ke Async bila terasa lambat.
    public List<Barang> daftarBarang() throws SQLException {
        return new BarangDAO().getAllBarang("");
    }

    public void simpanUtang(final Utang u, final boolean isEdit) {
        Async.kerjakan(() -> { if (isEdit) dao.updateUtang(u); else dao.tambahUtang(u); },
                () -> { view.showSuccess("Data utang berhasil disimpan!"); loadData(""); },
                msg -> view.showError("Gagal menyimpan: " + msg));
    }

    public void hapusUtang(final String kode) {
        Async.kerjakan(() -> dao.hapusUtang(kode),
                () -> { view.showSuccess("Data berhasil dihapus!"); loadData(""); },
                msg -> view.showError("Gagal menghapus: " + msg));
    }

    public void tandaiLunas(final String kode) {
        Async.kerjakan(() -> dao.tandaiLunas(kode),
                () -> { view.showSuccess("Utang berhasil ditandai LUNAS!"); loadData(""); },
                msg -> view.showError("Gagal update status: " + msg));
    }
}
