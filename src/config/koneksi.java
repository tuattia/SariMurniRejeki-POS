package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author attia
 */
public class koneksi {
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/sarimurnirejeki";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASS = "";

    // Koneksi baru tiap panggilan; pemanggil wajib menutupnya (try-with-resources).
    // Bisa di-override lewat -Ddb.url / -Ddb.user / -Ddb.pass (dipakai test).
    public static Connection open() throws SQLException {
        return DriverManager.getConnection(
                System.getProperty("db.url", DEFAULT_URL),
                System.getProperty("db.user", DEFAULT_USER),
                System.getProperty("db.pass", DEFAULT_PASS));
    }

    // Dipakai stack lama (gui/, controller/): return null bila gagal. Hapus di sub-proyek 4.
    public static Connection getConnection() {
        try {
            return open();
        } catch (SQLException e) {
            System.out.println("Koneksi gagal: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
