/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controller;

/**
 *
 * @author attia
 */import config.koneksi;
import model.modeltransaksi;
import model.modeltd;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import model.modellogt;
import model.modelutang;

public class transaksicontroller {

//    Connection con = koneksi.getConnection();
    barangcontroller bc = new barangcontroller();

    // ============================
    // SIMPAN TRANSAKSI + DETAIL
    // ============================
public boolean simpanTransaksi(Connection con, modeltransaksi t,
        List<modeltd> details) {

    String sqlTransaksi =
      "INSERT INTO transaksi(kode_transaksi, tanggal, nama_pelanggan, total, jenis_transaksi) " +
      "VALUES(?,?,?,?,?)";

    String sqlDetail =
      "INSERT INTO transaksi_detail(kode_transaksi, kode_barang, harga, qty, subtotal) " +
      "VALUES(?,?,?,?,?)";

    try {
        con.setAutoCommit(false);

        // 1. SIMPAN HEADER
        PreparedStatement psT = con.prepareStatement(sqlTransaksi);
        psT.setString(1, t.getKodeTransaksi());
        psT.setTimestamp(2, Timestamp.valueOf(t.getTanggal()));
        psT.setString(3, t.getNamaPelanggan());
        psT.setInt(4, t.getTotal());
        psT.setString(5, t.getJenisTransaksi());
        psT.executeUpdate();

        // 2. SIMPAN DETAIL + UPDATE STOK
        for (modeltd d : details) {

            boolean stokOK =
                bc.kurangiStok(d.getKodeBarang(), d.getQty());

            if (!stokOK) {
                con.rollback();
                throw new Exception("Stok tidak cukup: " + d.getKodeBarang());
            }

            PreparedStatement psD = con.prepareStatement(sqlDetail);
            psD.setString(1, t.getKodeTransaksi());
            psD.setString(2, d.getKodeBarang());
            psD.setInt(3, d.getHarga());
            psD.setInt(4, d.getQty());
            psD.setInt(5, d.getSubtotal());
            psD.executeUpdate();
        }

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

public void simpanUtang(Connection con, modelutang u) throws Exception {

    String sql = "INSERT INTO utang "
            + "(kode_utang, nama, alamat, telepon, harga_brng, dp, jumlah_cicilan, jatuh_tempo, status, kode_transaksi) "
            + "VALUES (?,?,?,?,?,?,?,?,?,?)";

    try (PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setString(1, u.getKdutang());
        ps.setString(2, u.getNama());
        ps.setString(3, u.getAlamat());
        ps.setString(4, u.getTelepon());
        ps.setInt(5, u.getHargabrng());
        ps.setInt(6, u.getDp());
        ps.setInt(7, u.getJumlahcicilan());
        ps.setDate(8, java.sql.Date.valueOf(u.getJatuhTempo()));
        ps.setString(9, u.getStatus());
        ps.setString(10, u.getKodeTransaksi());

        ps.executeUpdate();
    }
}

public boolean simpanSemua(
        modeltransaksi t,
        List<modeltd> details,
        modellogt log,
        modelutang utang) {

    Connection con = null;
    
    try {
        // Buat connection BARU
        con = koneksi.getConnection();
        
        if (con == null) {
            System.out.println("✗ ERROR: Connection NULL!");
            JOptionPane.showMessageDialog(null, "ERROR:  Koneksi database gagal!");
            return false;
        }
        
        System.out.println("✓ Connection OK");
        
        // Cek autocommit status
        System.out.println("Current autocommit: " + con. getAutoCommit());
        
        // Set autocommit FALSE
        try {
            con.setAutoCommit(false);
            System. out.println("✓ Autocommit set to FALSE");
        } catch (SQLException e) {
            System. out.println("⚠ Warning: Could not set autocommit to false");
            e.printStackTrace();
        }

        System.out.println("=== SIMPAN TRANSAKSI ===");
        System.out.println("Transaksi:  " + t.getKodeTransaksi());
        System.out.println("Detail items: " + details.size());

        // Step 1: Simpan transaksi + detail
        boolean okTransaksi = simpanTransaksiTanpaCommit(con, t, details);
        if (! okTransaksi) {
            throw new Exception("Gagal simpan transaksi header / detail");
        }
        System.out.println("✓ Transaksi saved");

        // Step 2: Simpan log HANYA jika tidak null
        if (log != null) {
            boolean okLog = simpanLogTanpaCommit(con, log);
            if (!okLog) {
                throw new Exception("Gagal simpan log transaksi");
            }
            System.out.println("✓ Log saved");
        } else {
            System.out. println("⚠ Log is null, skipping.. .");
        }

        // Step 3: Simpan utang HANYA jika tidak null
        if (utang != null) {
            try {
                simpanUtangTanpaCommit(con, utang);
                System.out.println("✓ Utang saved");
            } catch (Exception e) {
                throw new Exception("Gagal simpan utang:  " + e.getMessage());
            }
        }

        // COMMIT
        try {
            con.commit();
            System.out.println("✓ COMMIT BERHASIL - Semua data saved!");
        } catch (SQLException e) {
            System.out.println("⚠ Commit warning: " + e.getMessage());
            // Jika commit gagal, cek apakah data sudah tersimpan
        }
        
        return true;

    } catch (Exception e) {
        // Rollback jika ada error
        if (con != null) {
            try {
                con.rollback();
                System.out.println("⚠ ROLLBACK executed");
            } catch (SQLException ex) {
                System.out. println("✗ Rollback failed: " + ex.getMessage());
            }
        }
        
        System.out.println("✗ Error:  " + e.getMessage());
        e.printStackTrace();
        
        JOptionPane.showMessageDialog(null,
            "ERROR DATABASE:\n" + e.getMessage());
        return false;
        
    } finally {
        // PENTING: Close connection setelah selesai
        if (con != null) {
            try {
                con.close();
                System.out.println("✓ Connection closed");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}

// ← BUAT METHOD BARU:  Simpan transaksi TANPA commit
private boolean simpanTransaksiTanpaCommit(Connection con, modeltransaksi t,
        List<modeltd> details) {

    String sqlTransaksi =
      "INSERT INTO transaksi(kode_transaksi, tanggal, nama_pelanggan, total, jenis_transaksi) " +
      "VALUES(?,?,?,?,?)";

    String sqlDetail =
      "INSERT INTO transaksi_detail(kode_transaksi, kode_barang, harga, qty, subtotal) " +
      "VALUES(?,?,?,?,?)";

    try {
        // 1. SIMPAN HEADER
        try (PreparedStatement psT = con.prepareStatement(sqlTransaksi)) {
            psT.setString(1, t.getKodeTransaksi());
            psT.setTimestamp(2, Timestamp.valueOf(t.getTanggal()));
            psT.setString(3, t.getNamaPelanggan());
            psT.setInt(4, t.getTotal());
            psT.setString(5, t.getJenisTransaksi());
            psT.executeUpdate();
            System. out.println("✓ Header inserted");
        }

        // 2. SIMPAN DETAIL + UPDATE STOK
        for (modeltd d : details) {

            boolean stokOK = bc.kurangiStok(d.getKodeBarang(), d.getQty());

            if (!stokOK) {
                throw new Exception("Stok tidak cukup:  " + d.getKodeBarang());
            }

            try (PreparedStatement psD = con.prepareStatement(sqlDetail)) {
                psD.setString(1, t.getKodeTransaksi());
                psD.setString(2, d.getKodeBarang());
                psD.setInt(3, d.getHarga());
                psD.setInt(4, d.getQty());
                psD.setInt(5, d.getSubtotal());
                psD.executeUpdate();
                System.out. println("✓ Detail inserted:  " + d.getKodeBarang());
            }
        }

        return true;

    } catch (Exception e) {
        e.printStackTrace();
        System.out.println("✗ Error di simpanTransaksiTanpaCommit: " + e.getMessage());
        return false;
    }
}

// ← BUAT METHOD BARU: Simpan log TANPA commit
private boolean simpanLogTanpaCommit(Connection con, modellogt lt) {

    String sql = "INSERT INTO log_transaksi " +
                 "(kode_transaksi, tanggal, nama_pelanggan, total, bayar, kembali, tipe_transaksi, keterangan) " +
                 "VALUES (?,?,?,?,?,?,?,?)";

    try (PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, lt.getKodeTransaksi());
        ps.setTimestamp(2, Timestamp.valueOf(lt.getTanggal()));
        ps.setString(3, lt.getNamaPelanggan());
        ps.setInt(4, lt.getTotal());
        ps.setInt(5, lt.getBayar());
        ps.setInt(6, lt.getKembali());
        ps.setString(7, lt.getTipeTransaksi());
        ps.setString(8, lt.getKeterangan());

        int result = ps.executeUpdate();
        return result > 0;
        
    } catch (Exception e) {
        e.printStackTrace();
        System.out.println("✗ Error di simpanLogTanpaCommit: " + e.getMessage());
        return false;
    }
}

// ← BUAT METHOD BARU: Simpan utang TANPA commit
private void simpanUtangTanpaCommit(Connection con, modelutang u) throws Exception {

    String sql = "INSERT INTO utang "
            + "(kode_utang, nama, alamat, telepon, harga_brng, dp, jumlah_cicilan, jatuh_tempo, status, kode_transaksi) "
            + "VALUES (?,?,?,?,?,?,?,?,?,?)";

    try (PreparedStatement ps = con. prepareStatement(sql)) {
        ps.setString(1, u.getKdutang());
        ps.setString(2, u.getNama());
        ps.setString(3, u.getAlamat());
        ps.setString(4, u.getTelepon());
        ps.setInt(5, u.getHargabrng());
        ps.setInt(6, u.getDp());
        ps.setInt(7, u.getJumlahcicilan());
        ps.setDate(8, java.sql.Date.valueOf(u.getJatuhTempo()));
        ps.setString(9, u.getStatus());
        ps.setString(10, u.getKodeTransaksi());
        
        ps.executeUpdate();
        System.out.println("✓ Utang inserted");
    }
}

public String generateKodeTransaksi() {
    return "TRX-" + System.currentTimeMillis();
} 

    // ============================
    // GET TRANSAKSI BY KODE
    // ============================
public modeltransaksi getByKode(String kode) {
    Connection con = koneksi.getConnection();
    modeltransaksi t = null;
    String sql = "SELECT * FROM transaksi WHERE kode_transaksi=?";

    try (PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, kode);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            t = new modeltransaksi();
            t.setIdTransaksi(rs.getInt("id_transaksi"));
            t.setKodeTransaksi(rs.getString("kode_transaksi"));
            t.setTanggal(rs.getTimestamp("tanggal").toLocalDateTime());
            t.setNamaPelanggan(rs.getString("nama_pelanggan"));
            t.setTotal(rs.getInt("total"));
            t.setJenisTransaksi(rs.getString("jenis_transaksi"));
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
    return t;
}
    
public List<modeltd> getDetailByTransaksi(int idTransaksi) {
    Connection con = koneksi.getConnection();
    List<modeltd> list = new ArrayList<>();

    String sql =
      "SELECT d.*, b.nama_barang " +
      "FROM detail_transaksi d " +
      "JOIN barang b ON d.kode_barang = b.kode_barang " +
      "WHERE d.id_transaksi=?";

    try (PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setInt(1, idTransaksi);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            modeltd d = new modeltd();
            d.setIdDetail(rs.getInt("id_detail"));
            d.setKodeTransaksi(rs.getString("kodeTransaksi"));
            d.setKodeBarang(rs.getString("kode_barang"));
            d.setHarga(rs.getInt("harga"));
            d.setQty(rs.getInt("qty"));
            d.setSubtotal(rs.getInt("subtotal"));

            // nama_barang dipakai langsung di GUI
            String namaBarang = rs.getString("nama_barang");

            list.add(d);
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
    return list;
}
}
