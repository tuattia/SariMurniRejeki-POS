package modern_pos.utils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import modern_pos.model.Struk;

// Struk thermal 58mm = 32 karakter monospace per baris.
public final class StrukFormatter {
    private StrukFormatter() {}

    public static final int LEBAR = 32;
    private static final DateTimeFormatter WAKTU = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public static List<String> format(Struk s) {
        List<String> out = new ArrayList<>();
        out.add(tengah(Toko.NAMA));
        for (String b : bungkus(Toko.ALAMAT)) out.add(tengah(b));
        out.add(tengah(Toko.TELEPON));
        out.add(garis());
        out.add(potong("Kode: " + s.getKodeTransaksi()));
        out.add(potong("Waktu: " + s.getWaktu().format(WAKTU)));
        if (s.getPelanggan() != null && !s.getPelanggan().trim().isEmpty()) {
            out.add(potong("Pelanggan: " + s.getPelanggan().trim()));
        }
        out.add(garis());
        for (Struk.Item i : s.getItems()) {
            out.add(potong(i.getNama()));
            out.add(kiriKanan("  " + i.getQty() + " x " + angka(i.getHarga()), angka(i.getSubtotal())));
        }
        if (s.getItems().isEmpty() && s.getKeterangan() != null) out.add(potong(s.getKeterangan()));
        out.add(garis());
        out.add(kiriKanan("Total", rupiah(s.getTotal())));
        out.add(kiriKanan("Bayar", rupiah(s.getBayar())));
        out.add(kiriKanan("Kembali", rupiah(s.getKembali())));
        out.add(garis());
        out.add(tengah("Terima Kasih"));
        out.add(tengah("Selamat Datang Kembali"));
        return out;
    }

    public static String rupiah(int nilai) {
        return "Rp " + angka(nilai);
    }

    private static String angka(int nilai) {
        return String.format(java.util.Locale.US, "%,d", nilai).replace(',', '.');
    }

    private static String potong(String s) {
        return s.length() <= LEBAR ? s : s.substring(0, LEBAR);
    }

    private static String spasi(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(' ');
        return sb.toString();
    }

    private static String garis() {
        return spasi(LEBAR).replace(' ', '-');
    }

    private static String tengah(String s) {
        String p = potong(s);
        return spasi((LEBAR - p.length()) / 2) + p;
    }

    private static String kiriKanan(String kiri, String kanan) {
        return potong(kiri + spasi(Math.max(1, LEBAR - kiri.length() - kanan.length())) + kanan);
    }

    private static List<String> bungkus(String teks) {
        List<String> hasil = new ArrayList<>();
        StringBuilder baris = new StringBuilder();
        for (String kata : teks.split(" ")) {
            if (baris.length() > 0 && baris.length() + 1 + kata.length() > LEBAR) {
                hasil.add(baris.toString());
                baris.setLength(0);
            }
            if (baris.length() > 0) baris.append(' ');
            baris.append(kata);
        }
        if (baris.length() > 0) hasil.add(potong(baris.toString()));
        return hasil;
    }
}
