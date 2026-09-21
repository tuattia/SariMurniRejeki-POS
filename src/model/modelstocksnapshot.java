package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Model untuk tabel stock_snapshot.
 * Field `periode` selalu berisi hari ke-1 dari bulan snapshot,
 * contoh: September 2026 -> 2026-09-01
 *
 * @author attia
 */
public class modelstocksnapshot {

    private int idSnapshot;
    private LocalDate periode;
    private String kodeBarang;
    private String namaBarang;
    private int hargaJual;
    private String satuan;
    private int stokSnapshot;
    private LocalDateTime dibuatPada;

    public int getIdSnapshot() { return idSnapshot; }
    public void setIdSnapshot(int idSnapshot) { this.idSnapshot = idSnapshot; }

    public LocalDate getPeriode() { return periode; }
    public void setPeriode(LocalDate periode) { this.periode = periode; }

    public String getKodeBarang() { return kodeBarang; }
    public void setKodeBarang(String kodeBarang) { this.kodeBarang = kodeBarang; }

    public String getNamaBarang() { return namaBarang; }
    public void setNamaBarang(String namaBarang) { this.namaBarang = namaBarang; }

    public int getHargaJual() { return hargaJual; }
    public void setHargaJual(int hargaJual) { this.hargaJual = hargaJual; }

    public String getSatuan() { return satuan; }
    public void setSatuan(String satuan) { this.satuan = satuan; }

    public int getStokSnapshot() { return stokSnapshot; }
    public void setStokSnapshot(int stokSnapshot) { this.stokSnapshot = stokSnapshot; }

    public LocalDateTime getDibuatPada() { return dibuatPada; }
    public void setDibuatPada(LocalDateTime dibuatPada) { this.dibuatPada = dibuatPada; }

    public YearMonth getYearMonth() {
        return periode != null ? YearMonth.from(periode) : null;
    }

    public String getPeriodeLabel() {
        if (periode == null) return "-";
        return periode.format(DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("id", "ID")));
    }
}