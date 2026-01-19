/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

import java.time.LocalDate;

/**
 *
 * @author attia
 */
public class modelutang {
    public String kode_utang, nama, alamat,telepon, Status;
    public Integer dp, harga_brng, jumlah_cicilan;
    public LocalDate jatuh_tempo;
    private String kodeTransaksi;

public String getKdutang(){
    return kode_utang;
}
public String getNama(){
    return nama;
}
public String getAlamat(){
    return alamat;
}

public LocalDate getJatuhTempo() {
    return jatuh_tempo;
}

public String getKodeTransaksi(){
    return kodeTransaksi;
}

public String getStatus(){
    return Status;
}

public void setStatus(String Status){
    this.Status = Status;
}

public void setKodeTransaksi(String kodeTransaksi){
    this.kodeTransaksi = kodeTransaksi;
}

public void setJatuhTempo(LocalDate jatuh_tempo){
    this.jatuh_tempo = jatuh_tempo;
}

public void setKdutang(String kode_utang){
    this.kode_utang = kode_utang;
}
public void setNama(String nama){
    this.nama = nama;
}
public void setAlamat(String alamat){
    this.alamat = alamat;
}


public String getTelepon(){
    return telepon;
}
public int getHargabrng(){
    return harga_brng;
}
public int getDp(){
    return dp;
}
public int getJumlahcicilan(){
    return jumlah_cicilan;
}

public void setTelepon(String telepon){
    this.telepon = telepon;
}
public void setHargabrng(int harga_brng){
    this.harga_brng = harga_brng;
}
public void setDp(int dp){
    this.dp = dp;
}
public void setJumlahcicilan(int jumlah_cicilan){
    this.jumlah_cicilan = jumlah_cicilan;
}

}
