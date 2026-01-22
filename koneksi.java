package config;

import java.sql. Connection;
import java.sql. DriverManager;
import java. sql.SQLException;

/**
 * @author attia
 */

public class koneksi {
    private static final String URL = "jdbc:mysql://localhost:3306/sarimurnirejeki";
    private static final String USER = "root";
    private static final String PASS = "";
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    // Static initializer untuk load driver sekali saja
    static {
        try {
            Class.forName(DRIVER);
            System.out.println("✓ JDBC Driver loaded");
        } catch (ClassNotFoundException e) {
            System.out.println("✗ JDBC Driver not found: " + e. getMessage());
            e.printStackTrace();
        }
    }

    // Method untuk buat connection BARU setiap kali dipanggil
    public static Connection getConnection() {
        try {
            Connection con = DriverManager.getConnection(URL, USER, PASS);
            System.out.println("✓ New connection created");
            return con;
        } catch (SQLException e) {
            System.out.println("✗ Connection failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Method untuk test connection
    public static boolean testConnection() {
        Connection con = null;
        try {
            con = getConnection();
            if (con != null && ! con.isClosed()) {
                System.out.println("✓ Connection test OK");
                con.close();
                return true;
            }
        } catch (SQLException e) {
            System.out.println("✗ Connection test FAILED: " + e.getMessage());
        }
        return false;
    }
}