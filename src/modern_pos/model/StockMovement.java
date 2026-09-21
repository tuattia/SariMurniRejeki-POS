package modern_pos.model;

public class StockMovement {
    private String waktu, tipe, kodeTransaksi, keterangan;
    private int qty, stokSebelum, stokSesudah;

    public String getWaktu() { return waktu; }
    public void setWaktu(String waktu) { this.waktu = waktu; }
    public String getTipe() { return tipe; }
    public void setTipe(String tipe) { this.tipe = tipe; }
    public String getKodeTransaksi() { return kodeTransaksi; }
    public void setKodeTransaksi(String kodeTransaksi) { this.kodeTransaksi = kodeTransaksi; }
    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }
    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
    public int getStokSebelum() { return stokSebelum; }
    public void setStokSebelum(int stokSebelum) { this.stokSebelum = stokSebelum; }
    public int getStokSesudah() { return stokSesudah; }
    public void setStokSesudah(int stokSesudah) { this.stokSesudah = stokSesudah; }
}
