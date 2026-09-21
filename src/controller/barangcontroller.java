/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controller;

/**
 *
 * @author attia
 */
import config.koneksi;
import model.modelbarang;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class barangcontroller {

    Connection con = koneksi.getConnection();

    // =========================
    // TAMBAH BARANG
    // =========================
    public int tambahBarang(modelbarang b) {
        int hasil = 0;
        String sql = "INSERT INTO barang(kode_barang, nama_barang, harga_jual, stok, satuan) VALUES(?,?,?,?,?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, b.getKodeBarang());
            ps.setString(2, b.getNamaBarang());
            ps.setInt(3, b.getHargaJual());
            ps.setInt(4, b.getStok());
            ps.setString(5, b.getSatuan());

            hasil = ps.executeUpdate();

            // Catat stok awal sebagai gerakan MASUK
            if (hasil > 0 && b.getStok() > 0) {
                catatMovement(con, b.getKodeBarang(), null,
                        "MASUK", b.getStok(), 0, b.getStok(),
                        "Stok awal barang baru");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hasil;
    }

    // =========================
    // UPDATE BARANG
    // =========================
    public int updateBarang(modelbarang b) {
        int hasil = 0;
        String sql = "UPDATE barang SET nama_barang=?, harga_jual=?, stok=?, satuan=? WHERE kode_barang=?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, b.getNamaBarang());
            ps.setInt(2, b.getHargaJual());
            ps.setInt(3, b.getStok());
            ps.setString(4, b.getSatuan());
            ps.setString(5, b.getKodeBarang());

            hasil = ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hasil;
    }

    // =========================
    // TAMBAH STOK (RESTOCK)
    // Menambah jumlah stok barang yang sudah ada dan mencatat sebagai MASUK.
    // =========================
    public boolean tambahStok(String kodeBarang, int jumlahTambah, String keterangan) {
        if (jumlahTambah <= 0) return false;

        String cekSql = "SELECT stok FROM barang WHERE kode_barang=?";
        String updateSql = "UPDATE barang SET stok = stok + ? WHERE kode_barang=?";

        try {
            con.setAutoCommit(false);

            // 1. Ambil stok sebelumnya
            int stokSebelum = 0;
            try (PreparedStatement cek = con.prepareStatement(cekSql)) {
                cek.setString(1, kodeBarang);
                ResultSet rs = cek.executeQuery();
                if (!rs.next()) {
                    con.rollback();
                    return false;
                }
                stokSebelum = rs.getInt("stok");
            }

            // 2. Tambah stok
            try (PreparedStatement upd = con.prepareStatement(updateSql)) {
                upd.setInt(1, jumlahTambah);
                upd.setString(2, kodeBarang);
                upd.executeUpdate();
            }

            int stokSesudah = stokSebelum + jumlahTambah;

            // 3. Catat movement log MASUK
            catatMovement(con, kodeBarang, null,
                    "MASUK", jumlahTambah, stokSebelum, stokSesudah,
                    keterangan != null ? keterangan : "Restock barang");

            con.commit();
            return true;

        } catch (Exception e) {
            try { con.rollback(); } catch (Exception ex) {}
            e.printStackTrace();
            return false;
        } finally {
            try { con.setAutoCommit(true); } catch (Exception e) {}
        }
    }

    // =========================
    // DELETE BARANG
    // =========================
    public int deleteBarang(String kodeBarang) {
        int hasil = 0;
        String sql = "DELETE FROM barang WHERE kode_barang=?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, kodeBarang);
            hasil = ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hasil;
    }

    // =========================
    // GET BARANG BY KODE
    // =========================
    public modelbarang getByKode(String kode) {
        modelbarang b = null;
        String sql = "SELECT * FROM barang WHERE kode_barang=?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, kode);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                b = new modelbarang();
                b.setIdBarang(rs.getInt("id_barang"));
                b.setKodeBarang(rs.getString("kode_barang"));
                b.setNamaBarang(rs.getString("nama_barang"));
                b.setHargaJual(rs.getInt("harga_jual"));
                b.setStok(rs.getInt("stok"));
                b.setSatuan(rs.getString("satuan"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return b;
    }

    // =========================
    // TAMPIL SEMUA BARANG
    // =========================
    public List<modelbarang> tampil() {
        List<modelbarang> list = new ArrayList<>();
        String sql = "SELECT * FROM barang";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                modelbarang b = new modelbarang();
                b.setIdBarang(rs.getInt("id_barang"));
                b.setKodeBarang(rs.getString("kode_barang"));
                b.setNamaBarang(rs.getString("nama_barang"));
                b.setHargaJual(rs.getInt("harga_jual"));
                b.setStok(rs.getInt("stok"));
                b.setSatuan(rs.getString("satuan"));
                list.add(b);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // =========================
    // KURANGI STOK (FITUR INTI)
    // =========================
    public boolean kurangiStok(String kodeBarang, int qty) {
        boolean sukses = false;

        String cekSql = "SELECT stok FROM barang WHERE kode_barang=?";
        String updateSql = "UPDATE barang SET stok = stok - ? WHERE kode_barang=?";

        try {
            con.setAutoCommit(false);

            // 1. cek stok dulu
            PreparedStatement cek = con.prepareStatement(cekSql);
            cek.setString(1, kodeBarang);
            ResultSet rs = cek.executeQuery();

            if (!rs.next()) return false;

            int stokSekarang = rs.getInt("stok");
            if (stokSekarang < qty) return false;

            // 2. kurangi stok
            PreparedStatement upd = con.prepareStatement(updateSql);
            upd.setInt(1, qty);
            upd.setString(2, kodeBarang);
            upd.executeUpdate();

            int stokSesudah = stokSekarang - qty;

            // 3. Catat movement log KELUAR
            catatMovement(con, kodeBarang, null,
                    "KELUAR", qty, stokSekarang, stokSesudah,
                    "Transaksi penjualan");

            con.commit();
            sukses = true;

        } catch (Exception e) {
            try { con.rollback(); } catch (Exception ex) {}
            e.printStackTrace();
        } finally {
            try { con.setAutoCommit(true); } catch (Exception e) {}
        }
        return sukses;
    }

    // =========================
    // PRIVATE HELPER: CATAT MOVEMENT LOG
    // Menyisipkan baris ke tabel stock_movement_log.
    // kodeTransaksi boleh null jika bukan dari transaksi penjualan.
    // =========================
    private void catatMovement(Connection c, String kodeBarang, String kodeTransaksi,
            String tipe, int qty, int stokSebelum, int stokSesudah, String keterangan) {

        String sql = "INSERT INTO stock_movement_log "
                + "(kode_barang, kode_transaksi, tipe_gerakan, qty, stok_sebelum, stok_sesudah, keterangan) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, kodeBarang);
            if (kodeTransaksi != null) {
                ps.setString(2, kodeTransaksi);
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            ps.setString(3, tipe);
            ps.setInt(4, qty);
            ps.setInt(5, stokSebelum);
            ps.setInt(6, stokSesudah);
            ps.setString(7, keterangan);
            ps.executeUpdate();
        } catch (Exception e) {
            // Log error tapi jangan gagalkan transaksi utama
            System.err.println("Warning: gagal catat movement log - " + e.getMessage());
        }
    }
}