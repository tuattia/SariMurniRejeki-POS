/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package login;

import config.koneksi;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.JOptionPane;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author attia
 */
public class usercontroller {
    public usercontroller() {
        
    }

    public List<user> cariLogin(String username, String passwordPlain) {
        
        Connection con = koneksi.getConnection();
        List<user> logLogin = new ArrayList<>();

        // HASH password input menggunakan SHA-256
        String passwordHash = enkripsi.sha256(passwordPlain);

        String sql = "SELECT username, password, hakakses "
                   + "FROM user "
                   + "WHERE username = ? AND password = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, passwordHash);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                user u = new user();
                u.setusername(rs.getString("username"));
                u.setpassword(rs.getString("password"));
                u.sethakakses(rs.getString("hakakses"));
                logLogin.add(u);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                null,
                "Terjadi Kesalahan Login:\n" + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }

        return logLogin;
    }
    

    
public int tambah(user e) {
    Connection con = koneksi.getConnection();

    if (con == null) {
        JOptionPane.showMessageDialog(
            null,
            "Koneksi database belum tersedia",
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
        return 0; // ⬅ WAJIB int
    }

    int hasil =0;

    try {
        String sql = "INSERT INTO user (nama, username, password, hakakses) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = con.prepareStatement(sql);

        ps.setString(1, e.getnama());
        ps.setString(2, e.getusername());
        ps.setString(3, e.getpassword());
        ps.setString(4, e.gethakakses());

        hasil = ps.executeUpdate();

    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(
            null,
            "Gagal menyimpan user:\n" + ex.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
        return 0; // ⬅ WAJIB
    }

    return hasil; // ⬅ WAJIB
}
    
    
}
