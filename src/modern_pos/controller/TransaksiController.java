package modern_pos.controller;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingWorker;
import modern_pos.dao.TransaksiDAO;
import modern_pos.dao.BarangDAO;
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
        SwingWorker<List<Barang>, Void> worker = new SwingWorker<List<Barang>, Void>() {
            @Override protected List<Barang> doInBackground() throws Exception {
                return barangDAO.getAllBarang(keyword);
            }
            @Override protected void done() {
                try {
                    view.populateTableBarang(get());
                } catch (Exception ex) {
                    view.showError("Gagal memuat barang: " + ex.getMessage());
                }
            }
        };
        worker.execute();
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

    // Menggunakan kata kunci final agar lolos validasi Java 8 Background Thread
    public void processPayment(final int bayar, final String pelanggan) {
        if (cart.isEmpty()) {
            view.showError("Keranjang masih kosong!");
            return;
        }
        
        // Hitung total di variable temporary dahulu
        int tempTotal = 0;
        for (CartItem item : cart) tempTotal += item.getSubtotal();
        
        final int finalTotal = tempTotal; // Variable ini tidak berubah, aman untuk SwingWorker
        
        if (bayar < finalTotal) {
            view.showError("Uang bayar kurang!");
            return;
        }

        final int kembali = bayar - finalTotal; // Aman untuk SwingWorker
        
        view.setLoading(true);
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                dao.simpanTransaksi(cart, finalTotal, bayar, kembali, pelanggan);
                return null;
            }
            @Override protected void done() {
                view.setLoading(false);
                try {
                    get(); 
                    view.showSuccess("Transaksi Berhasil! Kembalian: Rp " + kembali);
                    
                    // --- BUKA JENDELA CETAK STRUK OTOMATIS ---
                    try {
                        gui.detailtransaksi dt = new gui.detailtransaksi();
                        dt.setVisible(true);
                    } catch (Exception ex) {
                        System.out.println("Gagal membuka modul cetak struk: " + ex.getMessage());
                    }
                    
                    cart.clear();
                    updateCartView();
                    loadBarang("");
                    view.resetForm();
                } catch (Exception ex) {
                    view.showError("Gagal menyimpan: " + ex.getMessage());
                    loadBarang(""); // stok di layar mungkin basi
                }
            }
        };
        worker.execute();
    }
}