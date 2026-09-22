package modern_pos.dao;
import config.koneksi;
import modern_pos.model.Barang;
import modern_pos.model.CartItem;
import modern_pos.model.Struk;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TransaksiDAO {
    private static long lastKode;

    // TRX + yyMMddHHmmssSSS + 2 digit urutan = 20 char (kolom kode_transaksi varchar(20)).
    // Nilai selalu naik, jadi unik dalam satu JVM walau > 100 panggilan per milidetik.
    public static synchronized String newKodeTransaksi() {
        lastKode = Math.max(System.currentTimeMillis() * 100, lastKode + 1);
        String waktu = new SimpleDateFormat("yyMMddHHmmssSSS").format(new Date(lastKode / 100));
        return "TRX" + waktu + String.format("%02d", lastKode % 100);
    }

    public String simpanTransaksi(List<CartItem> cart, int total, int bayar, int kembali, String pelanggan) throws SQLException {
        String kodeTrx = newKodeTransaksi();
        String sqlT = "INSERT INTO transaksi (kode_transaksi, tanggal, nama_pelanggan, total, jenis_transaksi, created_at) VALUES (?, CURDATE(), ?, ?, 'TUNAI', NOW())";
        String sqlD = "INSERT INTO transaksi_detail (kode_transaksi, kode_barang, harga, qty, subtotal) VALUES (?,?,?,?,?)";
        String sqlL = "INSERT INTO log_transaksi (kode_transaksi, tanggal, nama_pelanggan, total, bayar, kembali, tipe_transaksi, keterangan) VALUES (?, NOW(), ?, ?, ?, ?, 'TUNAI', 'Selesai')";

        try (Connection con = koneksi.open()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement psT = con.prepareStatement(sqlT)) {
                    psT.setString(1, kodeTrx);
                    psT.setString(2, pelanggan);
                    psT.setInt(3, total);
                    psT.executeUpdate();
                }

                try (PreparedStatement psD = con.prepareStatement(sqlD)) {
                    for (CartItem item : cart) {
                        Barang b = item.getBarang();
                        StokDAO.ubahStok(con, b.getKodeBarang(), -item.getQty(), "KELUAR", kodeTrx, "Penjualan");

                        psD.setString(1, kodeTrx);
                        psD.setString(2, b.getKodeBarang());
                        psD.setInt(3, b.getHarga());
                        psD.setInt(4, item.getQty());
                        psD.setInt(5, item.getSubtotal());
                        psD.executeUpdate();
                    }
                }

                try (PreparedStatement psL = con.prepareStatement(sqlL)) {
                    psL.setString(1, kodeTrx);
                    psL.setString(2, pelanggan);
                    psL.setInt(3, total);
                    psL.setInt(4, bayar);
                    psL.setInt(5, kembali);
                    psL.executeUpdate();
                }

                con.commit();
            } catch (SQLException e) {
                throw Tx.rollbackQuietly(con, e);
            }
        }
        return kodeTrx;
    }

    public Struk getStruk(String kodeTransaksi) throws SQLException {
        Struk s = new Struk();
        try (Connection con = koneksi.open()) {
            String sqlT = "SELECT kode_transaksi, nama_pelanggan, total, jenis_transaksi, created_at, tanggal FROM transaksi WHERE kode_transaksi = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlT)) {
                ps.setString(1, kodeTransaksi);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Transaksi " + kodeTransaksi + " tidak ditemukan");
                    s.setKodeTransaksi(rs.getString("kode_transaksi"));
                    s.setPelanggan(rs.getString("nama_pelanggan"));
                    s.setTotal(rs.getInt("total"));
                    s.setJenis(rs.getString("jenis_transaksi"));
                    Timestamp dibuat = rs.getTimestamp("created_at");
                    s.setWaktu((dibuat != null ? dibuat : rs.getTimestamp("tanggal")).toLocalDateTime());
                    // Default bila log sudah dihapus: dianggap uang pas.
                    s.setBayar(s.getTotal());
                    s.setKembali(0);
                }
            }

            String sqlD = "SELECT COALESCE(b.nama_barang, '-') AS nama, d.qty, d.harga, d.subtotal FROM transaksi_detail d "
                    + "LEFT JOIN barang b ON b.kode_barang = d.kode_barang WHERE d.kode_transaksi = ? ORDER BY d.id_detail";
            try (PreparedStatement ps = con.prepareStatement(sqlD)) {
                ps.setString(1, kodeTransaksi);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        s.getItems().add(new Struk.Item(rs.getString("nama"), rs.getInt("qty"), rs.getInt("harga"), rs.getInt("subtotal")));
                    }
                }
            }

            String sqlL = "SELECT tanggal, bayar, kembali, keterangan FROM log_transaksi WHERE kode_transaksi = ? ORDER BY id_log LIMIT 1";
            try (PreparedStatement ps = con.prepareStatement(sqlL)) {
                ps.setString(1, kodeTransaksi);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        s.setBayar(rs.getInt("bayar"));
                        s.setKembali(rs.getInt("kembali"));
                        s.setKeterangan(rs.getString("keterangan"));
                        Timestamp t = rs.getTimestamp("tanggal");
                        if (t != null) s.setWaktu(t.toLocalDateTime());
                    }
                }
            }
        }
        return s;
    }
}
