package modern_pos.utils;

public final class Angka {
    private Angka() {}

    // Qty dari input user: hanya bilangan bulat > 0. Tidak menebak ("-5" bukan 5, "1.5" bukan 15).
    public static int parseQty(String input) {
        String s = input == null ? "" : input.trim();
        if (s.matches("\\d{1,9}")) {
            int n = Integer.parseInt(s);
            if (n > 0) return n;
        }
        throw new IllegalArgumentException("Qty harus bilangan bulat lebih dari 0");
    }
}
