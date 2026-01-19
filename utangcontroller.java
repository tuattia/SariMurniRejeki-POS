/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controller;

import model.modelutang;
import config.koneksi;
import java.sql.*;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;


/**
 *
 * @author attia
 */
public class utangcontroller {
    private Connection con = koneksi.getConnection();

    // =========================
    // TAMPIL DATA KE TABEL
    // =========================
    public List<modelutang> tampil() {

        List<modelutang> listUtang = new ArrayList<>();

        String sql = "SELECT kode_utang, nama, alamat, telepon, harga_brng, dp, jumlah_cicilan, jatuh_tempo " +
                     "FROM utang ORDER BY id_utang ASC";

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                modelutang mu = new modelutang();
                mu.setKdutang(rs.getString("kode_utang"));
                mu.setNama(rs.getString("nama"));
                mu.setAlamat(rs.getString("alamat"));
                mu.setTelepon(rs.getString("telepon"));
                mu.setHargabrng(rs.getInt("harga_brng"));
                mu.setDp(rs.getInt("dp"));
                mu.setJumlahcicilan(rs.getInt("jumlah_cicilan"));

                // SQL DATE -> LocalDate
                mu.setJatuhTempo(
                    rs.getDate("jatuh_tempo").toLocalDate()
                );

                listUtang.add(mu);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                "Gagal mengambil data utang\n" + e.getMessage());
        }

        return listUtang;
    }
    

    
public int tambahUtang(modelutang e) {

    int hasil = 0;

    String sql = "INSERT INTO utang "
            + "(kode_utang, nama, alamat, telepon, harga_brng, dp, jumlah_cicilan, jatuh_tempo, status, kode_transaksi) "
            + "VALUES (?,?,?,?,?,?,?,?,?,?)";

    try (PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, e.getKdutang());
        ps.setString(2, e.getNama());
        ps.setString(3, e.getAlamat());
        ps.setString(4, e.getTelepon());
        ps.setInt(5, e.getHargabrng());
        ps.setInt(6, e.getDp());
        ps.setInt(7, e.getJumlahcicilan());
        ps.setString(9, e.getStatus());
        ps.setString(10, e.getKodeTransaksi());

        // LocalDate -> SQL DATE
        ps.setDate(8, java.sql.Date.valueOf(e.getJatuhTempo()));

        hasil = ps.executeUpdate();

    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(null, ex.getMessage());
    }

    return hasil;
}

public int updateUtang(modelutang e) {

    int hasil = 0;

    String sql = "UPDATE utang SET "
            + "nama=?, alamat=?, telepon=?, harga_brng=?, dp=?, "
            + "jumlah_cicilan=?, jatuh_tempo=? "
            + "WHERE kode_utang=?";

    try (PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, e.getNama());
        ps.setString(2, e.getAlamat());
        ps.setString(3, e.getTelepon());
        ps.setInt(4, e.getHargabrng());
        ps.setInt(5, e.getDp());
        ps.setInt(6, e.getJumlahcicilan());
        ps.setDate(7, java.sql.Date.valueOf(e.getJatuhTempo()));
        ps.setString(8, e.getKdutang());

        hasil = ps.executeUpdate();

    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(null,
                "Gagal update data utang\n" + ex.getMessage());
    }

    return hasil;
}

public int deleteUtang(String kodeUtang) {

    int hasil = 0;
    String sql = "DELETE FROM utang WHERE kode_utang=?";

    try (PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, kodeUtang);
        hasil = ps.executeUpdate();

    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(null,
                "Gagal menghapus data utang\n" + ex.getMessage());
    }

    return hasil;
}



public modelutang getByKode(String kodeUtang) {

    modelutang mu = null;
    String sql = "SELECT * FROM utang WHERE kode_utang = ?";

    try (PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, kodeUtang);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            mu = new modelutang();
            mu.setKdutang(rs.getString("kode_utang"));
            mu.setNama(rs.getString("nama"));
            mu.setAlamat(rs.getString("alamat"));
            mu.setTelepon(rs.getString("telepon"));
            mu.setHargabrng(rs.getInt("harga_brng"));
            mu.setDp(rs.getInt("dp"));
            mu.setJumlahcicilan(rs.getInt("jumlah_cicilan"));
            mu.setJatuhTempo(rs.getDate("jatuh_tempo").toLocalDate());
        }

    } catch (Exception e) {
        JOptionPane.showMessageDialog(null, "Gagal ambil detail: " + e);
    }

    return mu;
}

}

