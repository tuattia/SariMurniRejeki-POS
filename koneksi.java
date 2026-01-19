/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author attia
 */
public class koneksi {
        private static Connection con;

    public static Connection getConnection() {
        try {
            if (con == null) {
                String url = "jdbc:mysql://localhost:3306/debit_app";
                String user = "root";
                String pass = "";

                con = DriverManager.getConnection(url, user, pass);
            }
        } catch (SQLException e) {
            System.out.println("Koneksi gagal: " + e.getMessage());
        }
        return con;
    }
}
