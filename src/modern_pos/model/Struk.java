package modern_pos.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Struk {
    public static class Item {
        private final String nama;
        private final int qty, harga, subtotal;

        public Item(String nama, int qty, int harga, int subtotal) {
            this.nama = nama;
            this.qty = qty;
            this.harga = harga;
            this.subtotal = subtotal;
        }
        public String getNama() { return nama; }
        public int getQty() { return qty; }
        public int getHarga() { return harga; }
        public int getSubtotal() { return subtotal; }
    }

    private String kodeTransaksi, pelanggan, jenis, keterangan;
    private LocalDateTime waktu;
    private int total, bayar, kembali;
    private final List<Item> items = new ArrayList<>();

    public String getKodeTransaksi() { return kodeTransaksi; }
    public void setKodeTransaksi(String kodeTransaksi) { this.kodeTransaksi = kodeTransaksi; }
    public String getPelanggan() { return pelanggan; }
    public void setPelanggan(String pelanggan) { this.pelanggan = pelanggan; }
    public String getJenis() { return jenis; }
    public void setJenis(String jenis) { this.jenis = jenis; }
    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }
    public LocalDateTime getWaktu() { return waktu; }
    public void setWaktu(LocalDateTime waktu) { this.waktu = waktu; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getBayar() { return bayar; }
    public void setBayar(int bayar) { this.bayar = bayar; }
    public int getKembali() { return kembali; }
    public void setKembali(int kembali) { this.kembali = kembali; }
    public List<Item> getItems() { return items; }
}
