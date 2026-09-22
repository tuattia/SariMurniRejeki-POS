package modern_pos.utils;

import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class AsyncTest {

    @Test
    public void suksesMenerimaHasilDiEDT() throws Exception {
        CountDownLatch selesai = new CountDownLatch(1);
        AtomicReference<Object> hasil = new AtomicReference<>();
        AtomicReference<Boolean> diEdt = new AtomicReference<>();
        Async.ambil(() -> 42, v -> { hasil.set(v); diEdt.set(SwingUtilities.isEventDispatchThread()); selesai.countDown(); },
                msg -> selesai.countDown());
        assertTrue(selesai.await(5, TimeUnit.SECONDS));
        assertEquals(42, hasil.get());
        assertTrue(diEdt.get());
    }

    @Test
    public void gagalMenerimaPesanAsliBukanExecutionException() throws Exception {
        CountDownLatch selesai = new CountDownLatch(1);
        AtomicReference<String> pesan = new AtomicReference<>();
        Async.kerjakan(() -> { throw new SQLException("Stok Gula 1kg tidak cukup"); },
                selesai::countDown, msg -> { pesan.set(msg); selesai.countDown(); });
        assertTrue(selesai.await(5, TimeUnit.SECONDS));
        assertEquals("Stok Gula 1kg tidak cukup", pesan.get());
    }

    @Test
    public void pesanKosongMemakaiNamaException() {
        assertEquals("java.lang.NullPointerException", Async.pesan(new NullPointerException()));
    }
}
