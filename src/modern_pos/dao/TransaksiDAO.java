package modern_pos.dao;
import config.koneksi;
import modern_pos.model.CartItem;
import java.sql.Connection;
import java.sql.PreparedStatement;
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

    public void simpanTransaksi(List<CartItem> cart, int total, int bayar, int kembali, String pelanggan) throws Exception {
        Connection con = koneksi.getConnection();
        if (con == null) throw new Exception("Koneksi gagal");

        String tglStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String tglFull = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String kodeTrx = "TRX-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        String sqlT = "INSERT INTO transaksi (kode_transaksi, tanggal, nama_pelanggan, total, jenis_transaksi, created_at) VALUES (?,?,?,?,'TUNAI',NOW())";
        String sqlD = "INSERT INTO transaksi_detail (kode_transaksi, kode_barang, harga, qty, subtotal) VALUES (?,?,?,?,?)";
        String sqlL = "INSERT INTO log_transaksi (kode_transaksi, tanggal, nama_pelanggan, total, bayar, kembali, tipe_transaksi, keterangan) VALUES (?,?,?,?,?,?,'TUNAI','Selesai')";
        String sqlUpdateStok = "UPDATE barang SET stok = stok - ? WHERE kode_barang = ?";

        try {
            con.setAutoCommit(false); 

            try (PreparedStatement psT = con.prepareStatement(sqlT)) {
                psT.setString(1, kodeTrx);
                psT.setString(2, tglStr);
                psT.setString(3, pelanggan);
                psT.setInt(4, total);
                psT.executeUpdate();
            }

            try (PreparedStatement psD = con.prepareStatement(sqlD);
                 PreparedStatement psS = con.prepareStatement(sqlUpdateStok)) {
                for (CartItem item : cart) {
                    psD.setString(1, kodeTrx);
                    psD.setString(2, item.getBarang().getKodeBarang());
                    psD.setInt(3, item.getBarang().getHarga());
                    psD.setInt(4, item.getQty());
                    psD.setInt(5, item.getSubtotal());
                    psD.addBatch();

                    psS.setInt(1, item.getQty());
                    psS.setString(2, item.getBarang().getKodeBarang());
                    psS.addBatch();
                }
                psD.executeBatch();
                psS.executeBatch();
            }

            try (PreparedStatement psL = con.prepareStatement(sqlL)) {
                psL.setString(1, kodeTrx);
                psL.setString(2, tglFull);
                psL.setString(3, pelanggan);
                psL.setInt(4, total);
                psL.setInt(5, bayar);
                psL.setInt(6, kembali);
                psL.executeUpdate();
            }

            con.commit(); 
        } catch (Exception e) {
            con.rollback(); 
            throw new Exception("Gagal menyimpan transaksi: " + e.getMessage());
        } finally {
            con.setAutoCommit(true); 
        }
    }
}
