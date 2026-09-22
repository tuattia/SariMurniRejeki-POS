package modern_pos.controller;
import java.util.ArrayList;
import java.util.List;
import modern_pos.dao.TransaksiDAO;
import modern_pos.utils.Async;
import modern_pos.dao.BarangDAO;
import modern_pos.model.Struk;
import java.sql.SQLException;
import modern_pos.model.Barang;
import modern_pos.model.CartItem;
import modern_pos.view.TransaksiView;

public class TransaksiController {
    private TransaksiView view;
    private final TransaksiDAO dao;
    private final BarangDAO barangDAO = new BarangDAO();
    private final List<CartItem> cart;

    public TransaksiController() {
        this.dao = new TransaksiDAO();
        this.cart = new ArrayList<>();
    }

    public void setView(TransaksiView view) {
        this.view = view;
        loadBarang("");
    }

    public void loadBarang(String keyword) {
        Async.ambil(() -> barangDAO.getAllBarang(keyword), view::populateTableBarang,
                msg -> view.showError("Gagal memuat barang: " + msg));
    }

    public void addToCart(Barang barang, int qty) {
        if (qty <= 0) return;
        if (barang.getStok() < qty) {
            view.showError("Stok tidak mencukupi!");
            return;
        }

        boolean found = false;
        for (CartItem item : cart) {
            if (item.getBarang().getKodeBarang().equals(barang.getKodeBarang())) {
                item.setQty(item.getQty() + qty);
                found = true;
                break;
            }
        }
        if (!found) {
            cart.add(new CartItem(barang, qty));
        }
        updateCartView();
    }
    
    public void removeFromCart(int index) {
        if (index >= 0 && index < cart.size()) {
            cart.remove(index);
            updateCartView();
        }
    }

    private void updateCartView() {
        view.populateTableCart(cart);
        int total = 0;
        for (CartItem item : cart) {
            total += item.getSubtotal();
        }
        view.updateTotal(total);
    }

    public void processPayment(final int bayar, final String pelanggan) {
        if (cart.isEmpty()) {
            view.showError("Keranjang masih kosong!");
            return;
        }
        
        // Hitung total di variable temporary dahulu
        int tempTotal = 0;
        for (CartItem item : cart) tempTotal += item.getSubtotal();
        
        final int finalTotal = tempTotal;
        
        if (bayar < finalTotal) {
            view.showError("Uang bayar kurang!");
            return;
        }

        final int kembali = bayar - finalTotal;
        
        view.setLoading(true);
        final String[] strukGagal = new String[1];
        Async.ambil(() -> {
            String kode = dao.simpanTransaksi(cart, finalTotal, bayar, kembali, pelanggan);
            try {
                return dao.getStruk(kode);
            } catch (SQLException e) {
                strukGagal[0] = e.getMessage(); // transaksi sudah tersimpan; hanya struk yang gagal
                return null;
            }
        }, (Struk struk) -> {
            view.setLoading(false);
            view.showSuccess("Transaksi Berhasil! Kembalian: Rp " + kembali);
            cart.clear();
            updateCartView();
            loadBarang("");
            view.resetForm();
            if (struk != null) view.showStruk(struk);
            else view.showError("Transaksi tersimpan, tapi struk gagal dimuat: " + strukGagal[0]);
        }, msg -> {
            view.setLoading(false);
            view.showError("Gagal menyimpan: " + msg);
            loadBarang(""); // stok di layar mungkin basi
        });
    }
}