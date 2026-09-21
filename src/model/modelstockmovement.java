package model;

import java.time.LocalDateTime;

/**
 * Model untuk tabel stock_movement_log.
 * Mencatat setiap gerakan stok: MASUK, KELUAR, atau KOREKSI.
 *
 * @author attia
 */
public class modelstockmovement {

    private int idLog;
    private String kodeBarang;
    private String kodeTransaksi;
    private String tipeGerakan;
    private int qty;
    private int stokSebelum;
    private int stokSesudah;
    private String keterangan;
    private LocalDateTime waktu;

    public int getIdLog() { return idLog; }
    public void setIdLog(int idLog) { this.idLog = idLog; }

    public String getKodeBarang() { return kodeBarang; }
    public void setKodeBarang(String kodeBarang) { this.kodeBarang = kodeBarang; }

    public String getKodeTransaksi() { return kodeTransaksi; }
    public void setKodeTransaksi(String kodeTransaksi) { this.kodeTransaksi = kodeTransaksi; }

    public String getTipeGerakan() { return tipeGerakan; }
    public void setTipeGerakan(String tipeGerakan) { this.tipeGerakan = tipeGerakan; }

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    public int getStokSebelum() { return stokSebelum; }
    public void setStokSebelum(int stokSebelum) { this.stokSebelum = stokSebelum; }

    public int getStokSesudah() { return stokSesudah; }
    public void setStokSesudah(int stokSesudah) { this.stokSesudah = stokSesudah; }

    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

    public LocalDateTime getWaktu() { return waktu; }
    public void setWaktu(LocalDateTime waktu) { this.waktu = waktu; }

    public int getSelisih() { return stokSesudah - stokSebelum; }
}