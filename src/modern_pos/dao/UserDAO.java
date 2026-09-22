package modern_pos.dao;
import config.koneksi;
import login.enkripsi;
import modern_pos.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import modern_pos.utils.Akses;

public class UserDAO {
    public User authenticate(String username, String passwordPlain) throws SQLException {
        String passwordHash = enkripsi.sha256(passwordPlain);
        String sql = "SELECT id_user, nama, username, hakakses FROM user WHERE username = ? AND password = ?";
        try (Connection con = koneksi.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Username atau Password salah!");
                User u = new User();
                u.setId(rs.getInt("id_user"));
                u.setNama(rs.getString("nama"));
                u.setUsername(rs.getString("username"));
                u.setHakAkses(rs.getString("hakakses"));
                return u;
            }
        }
    }

    public List<User> listUser() throws SQLException {
        List<User> list = new ArrayList<>();
        try (Connection con = koneksi.open();
             PreparedStatement ps = con.prepareStatement("SELECT id_user, nama, username, hakakses FROM user ORDER BY nama");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id_user"));
                u.setNama(rs.getString("nama"));
                u.setUsername(rs.getString("username"));
                u.setHakAkses(rs.getString("hakakses"));
                list.add(u);
            }
        }
        return list;
    }

    public void tambahUser(String nama, String username, String password, String hakAkses) throws SQLException {
        String n = wajib(nama, "Nama"), u = wajib(username, "Username"), r = role(hakAkses);
        cekPassword(password);
        try (Connection con = koneksi.open()) {
            con.setAutoCommit(false);
            try {
                // Cek eksplisit (pesan jelas) + UNIQUE KEY di DB sebagai jaring pengaman.
                try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM user WHERE username = ? FOR UPDATE")) {
                    ps.setString(1, u);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        if (rs.getInt(1) > 0) throw new SQLException("Username " + u + " sudah dipakai");
                    }
                }
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO user (nama, username, password, hakakses) VALUES (?, ?, ?, ?)")) {
                    ps.setString(1, n);
                    ps.setString(2, u);
                    ps.setString(3, enkripsi.sha256(password));
                    ps.setString(4, r);
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLIntegrityConstraintViolationException e) {
                throw Tx.rollbackQuietly(con, new SQLException("Username " + u + " sudah dipakai", e));
            } catch (SQLException e) {
                throw Tx.rollbackQuietly(con, e);
            }
        }
    }

    public void ubahUser(int id, String nama, String hakAkses) throws SQLException {
        String n = wajib(nama, "Nama"), r = role(hakAkses);
        try (Connection con = koneksi.open()) {
            con.setAutoCommit(false);
            try {
                boolean adminSekarang = kunciDanCekAdmin(con, id);
                if (adminSekarang && !Akses.isAdmin(r) && jumlahAdmin(con) <= 1) {
                    throw new SQLException("Minimal harus ada satu admin");
                }
                try (PreparedStatement ps = con.prepareStatement("UPDATE user SET nama = ?, hakakses = ? WHERE id_user = ?")) {
                    ps.setString(1, n);
                    ps.setString(2, r);
                    ps.setInt(3, id);
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) {
                throw Tx.rollbackQuietly(con, e);
            }
        }
    }

    public void resetPassword(int id, String passwordBaru) throws SQLException {
        cekPassword(passwordBaru);
        try (Connection con = koneksi.open();
             PreparedStatement ps = con.prepareStatement("UPDATE user SET password = ? WHERE id_user = ?")) {
            ps.setString(1, enkripsi.sha256(passwordBaru));
            ps.setInt(2, id);
            if (ps.executeUpdate() == 0) throw new SQLException("User tidak ditemukan");
        }
    }

    public void hapusUser(int id, int idPelaku) throws SQLException {
        if (id == idPelaku) throw new SQLException("Tidak bisa menghapus akun sendiri");
        try (Connection con = koneksi.open()) {
            con.setAutoCommit(false);
            try {
                boolean admin = kunciDanCekAdmin(con, id);
                if (admin && jumlahAdmin(con) <= 1) throw new SQLException("Minimal harus ada satu admin");
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM user WHERE id_user = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) {
                throw Tx.rollbackQuietly(con, e);
            }
        }
    }

    // Kunci semua baris user (tabel kecil) supaya dua admin yang saling menurunkan
    // bersamaan tidak sama-sama lolos cek "minimal satu admin".
    private static boolean kunciDanCekAdmin(Connection con, int id) throws SQLException {
        Boolean admin = null;
        try (PreparedStatement ps = con.prepareStatement("SELECT id_user, hakakses FROM user FOR UPDATE");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                if (rs.getInt("id_user") == id) admin = Akses.isAdmin(rs.getString("hakakses"));
            }
        }
        if (admin == null) throw new SQLException("User tidak ditemukan");
        return admin;
    }

    private static int jumlahAdmin(Connection con) throws SQLException {
        int n = 0;
        try (PreparedStatement ps = con.prepareStatement("SELECT hakakses FROM user");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) if (Akses.isAdmin(rs.getString("hakakses"))) n++;
        }
        return n;
    }

    private static String wajib(String nilai, String label) {
        if (nilai == null || nilai.trim().isEmpty()) throw new IllegalArgumentException(label + " wajib diisi");
        return nilai.trim();
    }

    private static String role(String hakAkses) {
        String r = hakAkses == null ? "" : hakAkses.trim().toLowerCase();
        if (!r.equals(Akses.ADMIN) && !r.equals(Akses.MEMBER)) throw new IllegalArgumentException("Hak akses harus admin atau member");
        return r;
    }

    private static void cekPassword(String password) {
        if (password == null || password.length() < 6) throw new IllegalArgumentException("Password minimal 6 karakter");
    }
}
