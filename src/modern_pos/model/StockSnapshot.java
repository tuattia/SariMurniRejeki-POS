package modern_pos.model;

import java.time.LocalDate;

public class StockSnapshot {
    private LocalDate periode;
    private String kodeBarang, namaBarang, satuan;
    private int hargaJual, stokSnapshot;

    public LocalDate getPeriode() { return periode; }
    public void setPeriode(LocalDate periode) { this.periode = periode; }
    public String getKodeBarang() { return kodeBarang; }
    public void setKodeBarang(String kodeBarang) { this.kodeBarang = kodeBarang; }
    public String getNamaBarang() { return namaBarang; }
    public void setNamaBarang(String namaBarang) { this.namaBarang = namaBarang; }
    public String getSatuan() { return satuan; }
    public void setSatuan(String satuan) { this.satuan = satuan; }
    public int getHargaJual() { return hargaJual; }
    public void setHargaJual(int hargaJual) { this.hargaJual = hargaJual; }
    public int getStokSnapshot() { return stokSnapshot; }
    public void setStokSnapshot(int stokSnapshot) { this.stokSnapshot = stokSnapshot; }
}
