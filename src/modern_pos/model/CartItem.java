package modern_pos.model;
public class CartItem {
    private Barang barang;
    private int qty;
    
    public CartItem(Barang barang, int qty) {
        this.barang = barang;
        this.qty = qty;
    }
    public Barang getBarang() { return barang; }
    public void setBarang(Barang barang) { this.barang = barang; }
    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
    public int getSubtotal() { return barang.getHarga() * qty; }
}