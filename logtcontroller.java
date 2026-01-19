/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controller;

import config.koneksi;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import model.modellogt;

/**
 *
 * @author attia
 */
public class logtcontroller {
    private Connection con = koneksi.getConnection();

    // ================= SIMPAN LOG =================


    // ================= TAMPIL SEMUA LOG =================
public List<modellogt> getAll() {

    List<modellogt> list = new ArrayList<>();
    String sql = "SELECT * FROM log_transaksi ORDER BY tanggal DESC";

    try (Statement st = con.createStatement();
         ResultSet rs = st.executeQuery(sql)) {

        while (rs.next()) {
            modellogt lt = new modellogt();

            lt.setIdLog(rs.getInt("id_log"));
            lt.setKodeTransaksi(rs.getString("kode_transaksi"));
            lt.setTanggal(rs.getTimestamp("tanggal").toLocalDateTime());
            lt.setNamaPelanggan(rs.getString("nama_pelanggan"));
            lt.setTotal(rs.getInt("total"));
            lt.setBayar(rs.getInt("bayar"));
            lt.setKembali(rs.getInt("kembali"));
            lt.setTipeTransaksi(rs.getString("tipe_transaksi"));
            lt.setKeterangan(rs.getString("keterangan"));

            list.add(lt);
        }

    } catch (Exception e) {
        e.printStackTrace();
    }

    return list;
}

    // ================= CARI LOG BERDASARKAN NO TRANSAKSI =================
    public List<modellogt> cariByNoTransaksi(String keyword) {

        List<modellogt> list = new ArrayList<>();
        String sql = "SELECT * FROM log_transaksi " +
                     "WHERE kode_transaksi LIKE ? OR nama_pelanggan LIKE ? " +
                     "ORDER BY tanggal DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            String key = "%" + keyword + "%";
            ps.setString(1, key);
            ps.setString(2, key);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                modellogt lt = new modellogt();
                lt.setIdLog(rs.getInt("id_log"));
                lt.setKodeTransaksi(rs.getString("kode_transaksi"));
                lt.setTanggal(rs.getTimestamp("tanggal").toLocalDateTime());
                lt.setNamaPelanggan(rs.getString("nama_pelanggan"));
                lt.setTotal(rs.getInt("total"));
                lt.setBayar(rs.getInt("bayar"));
                lt.setKembali(rs.getInt("kembali"));
                lt.setTipeTransaksi(rs.getString("tipe_transaksi"));
                lt.setKeterangan(rs.getString("keterangan"));

                list.add(lt);
            }

        } catch (Exception e) {
            System.err.println("Gagal mencari log transaksi");
            e.printStackTrace();
        }

        return list;
    }

    // ================= HAPUS LOG =================
    public void hapusLog(int idLog) {

        String sql = "DELETE FROM log_transaksi WHERE id_log = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idLog);
            ps.executeUpdate();

            System.out.println("Log transaksi berhasil dihapus.");

        } catch (Exception e) {
            System.err.println("Gagal hapus log transaksi");
            e.printStackTrace();
        }
    }
}

