package modern_pos.dao;
import config.koneksi;
import login.enkripsi;
import modern_pos.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {
    public User authenticate(String username, String passwordPlain) throws Exception {
        Connection con = koneksi.getConnection();
        if (con == null) throw new SQLException("Tidak ada koneksi database");
        String passwordHash = enkripsi.sha256(passwordPlain);
        String sql = "SELECT id_user, nama, username, hakakses FROM user WHERE username = ? AND password = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = new User();
                    u.setId(rs.getInt("id_user"));
                    u.setNama(rs.getString("nama"));
                    u.setUsername(rs.getString("username"));
                    u.setHakAkses(rs.getString("hakakses"));
                    return u; 
                } else {
                    throw new Exception("Username atau Password salah!");
                }
            }
        }
    }
}