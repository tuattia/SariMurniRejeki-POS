package modern_pos.utils;

public final class Akses {
    private Akses() {}

    public static final String ADMIN = "admin";
    public static final String MEMBER = "member";

    public static boolean isAdmin(String hakAkses) {
        return hakAkses != null && ADMIN.equals(hakAkses.trim().toLowerCase());
    }

    // Pembatasan di UI: aplikasi desktop terhubung langsung ke DB.
    public static boolean admin() {
        return Session.currentUser != null && isAdmin(Session.currentUser.getHakAkses());
    }
}
