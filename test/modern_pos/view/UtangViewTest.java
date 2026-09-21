package modern_pos.view;

import java.awt.event.KeyEvent;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import modern_pos.model.Barang;
import org.junit.Test;
import static org.junit.Assert.*;

public class UtangViewTest {

    private static Barang barang(String kode, int harga) {
        Barang b = new Barang();
        b.setKodeBarang(kode);
        b.setNamaBarang(kode);
        b.setHarga(harga);
        return b;
    }

    @Test
    public void totalManualTidakTertimpaKecualiQtyAtauBarangBerubah() {
        Barang beras = barang("B001", 65000);
        Barang gula = barang("B002", 15000);
        JComboBox<Barang> cmb = new JComboBox<>();
        cmb.addItem(beras);
        cmb.addItem(gula);
        JTextField txtQty = new JTextField("2");
        JTextField txtHarga = new JTextField("100000"); // harga kredit diketik manual

        UtangView.pasangTotalOtomatis(cmb, txtQty, txtHarga, false);

        // Tab lewat field qty tidak boleh menimpa total manual
        txtQty.dispatchEvent(new KeyEvent(txtQty, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0,
                KeyEvent.VK_TAB, KeyEvent.CHAR_UNDEFINED));
        assertEquals("100000", txtHarga.getText());

        // Memilih ulang barang yang sama juga tidak
        cmb.setSelectedItem(beras);
        assertEquals("100000", txtHarga.getText());

        // Qty benar-benar berubah -> dihitung ulang
        txtQty.setText("3");
        assertEquals("195000", txtHarga.getText());

        // Barang berubah -> dihitung ulang
        cmb.setSelectedItem(gula);
        assertEquals("45000", txtHarga.getText());

        // Qty tidak valid -> total tidak disentuh
        txtQty.setText("1.5");
        assertEquals("45000", txtHarga.getText());
    }
}
