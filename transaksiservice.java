/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controller;

import config.koneksi;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;
import model.modellogt;
import model.modeltransaksi;
import model.modeltd;

/**
 *
 * @author attia
 */
public class transaksiservice {
    private Connection con = koneksi.getConnection();

    public boolean simpanSemua(
            modeltransaksi t,
            List<modeltd> detailList,
            modellogt log) {

        try {
            con.setAutoCommit(false); // ===== START DB TRANSACTION =====

            // ================= 1. SIMPAN TRANSAKSI =================
            String sqlT = "INSERT INTO transaksi " +
                    "(kode_transaksi, tanggal, nama_pelanggan, total, jenis_transaksi, created_at) " +
                    "VALUES (?,?,?,?,?,NOW())";

            try (PreparedStatement ps = con.prepareStatement(sqlT)) {
                ps.setString(1, t.getKodeTransaksi());
                ps.setTimestamp(2, Timestamp.valueOf(t.getTanggal()));
                ps.setString(3, t.getNamaPelanggan());
                ps.setInt(4, t.getTotal());
                ps.setString(5, t.getJenisTransaksi());
                ps.executeUpdate();
            }

            // ================= 2. SIMPAN DETAIL =================
            String sqlD = "INSERT INTO transaksi_detail " +
                    "(kode_transaksi, kode_barang, harga, qty, subtotal) " +
                    "VALUES (?,?,?,?,?)";

            try (PreparedStatement ps = con.prepareStatement(sqlD)) {
                for (modeltd d : detailList) {
                    ps.setString(1, t.getKodeTransaksi());
                    ps.setString(2, d.getKodeBarang());
                    ps.setInt(3, d.getHarga());
                    ps.setInt(4, d.getQty());
                    ps.setInt(5, d.getSubtotal());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // ================= 3. SIMPAN LOG =================
            String sqlL = "INSERT INTO log_transaksi " +
                    "(kode_transaksi, tanggal, nama_pelanggan, total, bayar, kembali, tipe_transaksi, keterangan) " +
                    "VALUES (?,?,?,?,?,?,?,?)";

            try (PreparedStatement ps = con.prepareStatement(sqlL)) {
                ps.setString(1, log.getKodeTransaksi());
                ps.setTimestamp(2, Timestamp.valueOf(log.getTanggal()));
                ps.setString(3, log.getNamaPelanggan());
                ps.setInt(4, log.getTotal());
                ps.setInt(5, log.getBayar());
                ps.setInt(6, log.getKembali());
                ps.setString(7, log.getTipeTransaksi());
                ps.setString(8, log.getKeterangan());
                ps.executeUpdate();
            }

            con.commit(); // ===== COMMIT =====
            return true;

        } catch (Exception e) {
            try { con.rollback(); } catch (Exception ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return false;

        } finally {
            try { con.setAutoCommit(true); } catch (Exception e) { e.printStackTrace(); }
        }
    }
}
