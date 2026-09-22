package modern_pos.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import modern_pos.model.Struk;
import modern_pos.utils.Cetak;
import modern_pos.utils.StrukFormatter;

public class StrukDialog extends JDialog {
    public StrukDialog(Frame parent, Struk struk) {
        super(parent, "Struk " + struk.getKodeTransaksi(), true);
        final List<String> baris = StrukFormatter.format(struk);

        JTextArea area = new JTextArea(String.join("\n", baris));
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setMargin(new Insets(10, 10, 10, 10));
        // Batasi tinggi supaya tombol Cetak tetap terlihat di layar 768px; sisanya discroll.
        area.setRows(Math.min(baris.size(), 30));
        area.setColumns(StrukFormatter.LEBAR + 2);

        JButton btnCetak = new JButton("Cetak");
        JButton btnTutup = new JButton("Tutup");
        btnCetak.addActionListener(e -> {
            try {
                Cetak.cetakBaris(baris, "Struk " + struk.getKodeTransaksi());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Gagal mencetak: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        btnTutup.addActionListener(e -> dispose());

        JPanel tombol = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        tombol.add(btnCetak);
        tombol.add(btnTutup);

        setLayout(new BorderLayout());
        add(new JScrollPane(area), BorderLayout.CENTER);
        add(tombol, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(parent);
    }
}
