package modern_pos.model;
public class LogTransaksi {
    private String kodeTransaksi, tanggal, namaPelanggan, tipeTransaksi, keterangan;
    private int total, bayar, kembali;

    public String getKodeTransaksi() { return kodeTransaksi; }
    public void setKodeTransaksi(String kodeTransaksi) { this.kodeTransaksi = kodeTransaksi; }
    public String getTanggal() { return tanggal; }
    public void setTanggal(String tanggal) { this.tanggal = tanggal; }
    public String getNamaPelanggan() { return namaPelanggan; }
    public void setNamaPelanggan(String namaPelanggan) { this.namaPelanggan = namaPelanggan; }
    public String getTipeTransaksi() { return tipeTransaksi; }
    public void setTipeTransaksi(String tipeTransaksi) { this.tipeTransaksi = tipeTransaksi; }
    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public int getBayar() { return bayar; }
    public void setBayar(int bayar) { this.bayar = bayar; }
    public int getKembali() { return kembali; }
    public void setKembali(int kembali) { this.kembali = kembali; }
}