package modern_pos.utils;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.List;
import javax.swing.JComponent;

public final class Cetak {
    private Cetak() {}

    private static final double PT_PER_MM = 72 / 25.4;

    // Struk thermal 58mm: baris teks monospace, tinggi kertas mengikuti jumlah baris.
    // Mengembalikan false bila user membatalkan dialog printer.
    public static boolean cetakBaris(final List<String> baris, String judul) throws PrinterException {
        // 6.5pt: 32 kolom = ~125pt, muat di driver 58mm yang hanya mencetak 48mm (~136pt).
        final Font font = new Font(Font.MONOSPACED, Font.PLAIN, 1).deriveFont(6.5f);
        final double tinggiBaris = 8.5, margin = 4;
        double lebar = 58 * PT_PER_MM;
        double tinggi = baris.size() * tinggiBaris + 2 * margin;

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName(judul);
        job.setPrintable((g, format, halaman) -> {
            if (halaman > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) g;
            g2.translate(format.getImageableX(), format.getImageableY());
            g2.setFont(font);
            for (int i = 0; i < baris.size(); i++) g2.drawString(baris.get(i), 0, (float) (tinggiBaris * (i + 1)));
            return Printable.PAGE_EXISTS;
        }, halaman(lebar, tinggi, margin, job));
        if (!job.printDialog()) return false;
        job.print();
        return true;
    }

    // Komponen Swing diskalakan agar muat di kertas lebarMm x tinggiMm (misal kartu angsuran 150x200).
    public static boolean cetakKomponen(final JComponent komponen, double lebarMm, double tinggiMm, String judul) throws PrinterException {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName(judul);
        job.setPrintable((g, format, halaman) -> {
            if (halaman > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) g;
            g2.translate(format.getImageableX(), format.getImageableY());
            double skala = Math.min(format.getImageableWidth() / komponen.getWidth(),
                    format.getImageableHeight() / komponen.getHeight());
            g2.scale(skala, skala);
            komponen.printAll(g2);
            return Printable.PAGE_EXISTS;
        }, halaman(lebarMm * PT_PER_MM, tinggiMm * PT_PER_MM, 10, job));
        if (!job.printDialog()) return false;
        job.print();
        return true;
    }

    private static PageFormat halaman(double lebar, double tinggi, double margin, PrinterJob job) {
        PageFormat pf = job.defaultPage();
        Paper p = new Paper();
        p.setSize(lebar, tinggi);
        p.setImageableArea(margin, margin, lebar - 2 * margin, tinggi - 2 * margin);
        pf.setPaper(p);
        pf.setOrientation(PageFormat.PORTRAIT);
        return pf;
    }
}
