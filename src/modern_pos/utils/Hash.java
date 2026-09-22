package modern_pos.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Hash {
    private Hash() {}

    // ponytail: SHA-256 tanpa salt (format lama, keputusan user). Ganti ke PBKDF2 + upgrade saat login bila perlu.
    public static String sha256(String teks) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(teks.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : h) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e); // SHA-256 wajib ada di setiap JVM
        }
    }
}
