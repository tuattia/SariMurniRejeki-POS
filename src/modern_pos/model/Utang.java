package modern_pos.model;
import java.time.LocalDate;
public class Utang {
    private String kodeUtang, nama, alamat, telepon, kodeBarang, status;
    private int hargaBarang, dp, jumlahCicilan;
    private LocalDate jatuhTempo;

    public String getKodeUtang() { return kodeUtang; }
    public void setKodeUtang(String kodeUtang) { this.kodeUtang = kodeUtang; }
    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }
    public String getAlamat() { return alamat; }
    public void setAlamat(String alamat) { this.alamat = alamat; }
    public String getTelepon() { return telepon; }
    public void setTelepon(String telepon) { this.telepon = telepon; }
    public int getHargaBarang() { return hargaBarang; }
    public void setHargaBarang(int hargaBarang) { this.hargaBarang = hargaBarang; }
    public int getDp() { return dp; }
    public void setDp(int dp) { this.dp = dp; }
    public int getJumlahCicilan() { return jumlahCicilan; }
    public void setJumlahCicilan(int jumlahCicilan) { this.jumlahCicilan = jumlahCicilan; }
    public LocalDate getJatuhTempo() { return jatuhTempo; }
    public void setJatuhTempo(LocalDate jatuhTempo) { this.jatuhTempo = jatuhTempo; }
    public String getKodeBarang() { return kodeBarang; }
    public void setKodeBarang(String kodeBarang) { this.kodeBarang = kodeBarang; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}