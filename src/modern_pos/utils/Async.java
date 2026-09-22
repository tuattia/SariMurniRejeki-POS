package modern_pos.utils;

import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.SwingWorker;

// Kerja DB di background, hasil/pesan error kembali di EDT. Satu tempat untuk semua controller.
public final class Async {
    private Async() {}

    public interface Tugas<T> { T jalan() throws Exception; }
    public interface Aksi { void jalan() throws Exception; }

    public static <T> void ambil(Tugas<T> tugas, Consumer<T> sukses, Consumer<String> gagal) {
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return tugas.jalan(); }
            @Override protected void done() {
                T hasil;
                try {
                    hasil = get();
                } catch (Exception ex) {
                    gagal.accept(pesan(ex));
                    return;
                }
                sukses.accept(hasil); // di luar try: error di callback bukan "gagal" tugas
            }
        }.execute();
    }

    public static void kerjakan(Aksi aksi, Runnable sukses, Consumer<String> gagal) {
        ambil(() -> { aksi.jalan(); return null; }, x -> sukses.run(), gagal);
    }

    public static String pesan(Throwable ex) {
        Throwable c = (ex instanceof ExecutionException && ex.getCause() != null) ? ex.getCause() : ex;
        return c.getMessage() != null ? c.getMessage() : c.toString();
    }
}
