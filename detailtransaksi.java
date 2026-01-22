/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package gui;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import model.modelbarang;
import model.modellogt;
import model.modeltd;
import model.modeltransaksi;

/**
 *
 * @author attia
 */
public class detailtransaksi extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(detailtransaksi.class.getName());

    /**
     * Creates new form detailtransaksi
     */
    
    public detailtransaksi() {
    initComponents();
    jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
    jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    jScrollPane1.setBorder(null);
    tblDetail.setTableHeader(null);
    setUkuranThermal58mm();
    setLocationRelativeTo(null);
    }
    
    
    public detailtransaksi(modeltransaksi mt, List<modeltd> list,  Map<String, modelbarang> mapBarang, modellogt mlog) {
    this();
    loadHeader(mlog);
    loadTableDetail(list, mapBarang);
    setPembayaran(mt, mlog);
    }

private String formatTanggal(LocalDateTime tanggal) {
    DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    return tanggal.format(formatter);
}

private String formatRupiah(int nilai) {
    return String.format("%,d", nilai).replace(',', '.');
}

private void setUkuranThermal58mm() {
    // Gunakan lebar antara 300 - 350
    int width = 320; 
    jPanel1.setPreferredSize(new Dimension(width, jPanel1.getPreferredSize().height));
    jPanel1.setMinimumSize(new Dimension(width, 100));
    jPanel1.setMaximumSize(new Dimension(width, 5000)); // Tinggi biarkan fleksibel
}

    private void loadHeader(modellogt mlog) {
    lblTransaksi.setText(mlog.getKodeTransaksi());
    lblWaktu.setText(formatTanggal(mlog.getTanggal()));
    }
    
private void loadTableDetail(
        List<modeltd> list,
        Map<String, modelbarang> mapBarang) {

    DefaultTableModel model = new DefaultTableModel();
    model.addColumn("Nama Barang");
    model.addColumn("Qty");
    model.addColumn("Harga");
    model.addColumn("Total");

    for (modeltd dt : list) {

        modelbarang barang = mapBarang.get(dt.getKodeBarang());

        String namaBarang = (barang != null)
                ? barang.getNamaBarang()
                : "-";

        int totalPerItem = dt.getHarga() * dt.getQty();

        model.addRow(new Object[]{
            namaBarang,
            dt.getQty(),
            formatRupiah(dt.getHarga()),
            formatRupiah(totalPerItem)
        });
    }
    
    tblDetail.setModel(model);
    javax.swing.table.TableColumnModel columnModel = tblDetail.getColumnModel();

// Asumsi total lebar tabel sekitar 300-320 pixel untuk 58mm
// Index 0: Nama Barang, Index 1: Qty, Index 2: Harga, Index 3: Total

// 1. Perkecil Qty (Index 1)
    columnModel.getColumn(1).setPreferredWidth(30); 
    columnModel.getColumn(1).setMaxWidth(40); // Kunci agar tidak melebar

    // 2. Atur Harga dan Total (Index 2 & 3) agar cukup untuk angka
    columnModel.getColumn(2).setPreferredWidth(70);
    columnModel.getColumn(3).setPreferredWidth(70);

    // 3. Perpanjang Nama Barang (Index 0) - Berikan sisa ruangnya
    columnModel.getColumn(0).setPreferredWidth(150);
    // --- KODE AGAR TABEL FIT ---
    int rowHeight = tblDetail.getRowHeight();
    int rowCount = tblDetail.getRowCount();
    // Hitung total tinggi: (jumlah baris * tinggi baris) + tinggi header (biasanya 20-25)
    tblDetail.setBackground(java.awt.Color.WHITE); // Mengubah background tabel
    tblDetail.setGridColor(java.awt.Color.WHITE); // Menyembunyikan garis grid (opsional agar bersih)
    jScrollPane1.getViewport().setBackground(java.awt.Color.WHITE); // Mengubah background area scroll
    jScrollPane1.setBackground(java.awt.Color.WHITE);
    int headerHeight = 0;
    if (tblDetail.getTableHeader() != null) {
        headerHeight = tblDetail.getTableHeader().getPreferredSize().height;
    }

    // Gunakan headerHeight yang sudah dicek (0 jika null)
    int totalHeight = (rowCount * rowHeight) + headerHeight;

    tblDetail.setPreferredScrollableViewportSize(new Dimension(tblDetail.getPreferredSize().width, totalHeight));
    
    jScrollPane1.setViewportView(tblDetail);
    jPanel1.revalidate();

    tblDetail.setModel(model);
}
    
    private int hitungTotal() {
    int total = 0;

    for (int i = 0; i < tblDetail.getRowCount(); i++) {
        String nilai = tblDetail.getValueAt(i, 3).toString()
                .replace(".", "");

        total += Integer.parseInt(nilai);
    }

    lbl1.setText("Rp. " + formatRupiah(total));
    return total;
}
    
    private void setPembayaran(modeltransaksi mt, modellogt mlog) {
    int subtotal = hitungTotal();
    int bayar = mlog.getBayar();
    int kembalian = bayar - subtotal;

    lbl2.setText("Rp. " + formatRupiah(bayar));
    txt3.setText("Rp. " + formatRupiah(kembalian));
}
    
private void printThermal58mm() {
    PrinterJob job = PrinterJob.getPrinterJob();
    job.setJobName("Struk Transaksi 58mm");

    PageFormat pf = job.defaultPage();
    Paper paper = new Paper();

    // === 58mm thermal (Ukuran dalam Point) ===
    // 1 mm = 2.83 points. Jadi 58mm = ~164.4 points.
    double paperWidth  = 165;  
    double paperHeight = 1000; // Tinggi fleksibel mengikuti isi

    paper.setSize(paperWidth, paperHeight);

    // Margin sangat kecil karena kertas 58mm sangat sempit
    double margin = 2; 
    paper.setImageableArea(margin, margin, 
            paperWidth - (margin * 2), 
            paperHeight - (margin * 2));

    pf.setPaper(paper);
    pf.setOrientation(PageFormat.PORTRAIT);

    job.setPrintable((graphics, pageFormat, pageIndex) -> {
        if (pageIndex > 0) return Printable.NO_SUCH_PAGE;

        Graphics2D g2 = (Graphics2D) graphics;

        double panelWidth = jPanel1.getWidth();
        double printableWidth = pageFormat.getImageableWidth();

        // Skala otomatis agar isi panel masuk ke lebar kertas 165pt
        double scale = printableWidth / panelWidth;

        g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        g2.scale(scale, scale);

        jPanel1.printAll(g2);

        return Printable.PAGE_EXISTS;
    });

try {
    // Baris ini akan memunculkan jendela pilihan printer seperti di gambarmu
    if (job.printDialog()) { 
        job.print();
    }
} catch (PrinterException e) {
    JOptionPane.showMessageDialog(this, "Gagal Print: " + e.getMessage());
}
}


    
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        lblWaktu = new javax.swing.JLabel();
        lblKasir = new javax.swing.JLabel();
        lblTransaksi = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblDetail = new javax.swing.JTable();
        jSeparator3 = new javax.swing.JSeparator();
        lbl1 = new javax.swing.JLabel();
        jSeparator4 = new javax.swing.JSeparator();
        lblSub = new javax.swing.JLabel();
        lbl2 = new javax.swing.JLabel();
        lblBayar = new javax.swing.JLabel();
        txt3 = new javax.swing.JLabel();
        lblKembali = new javax.swing.JLabel();
        jSeparator5 = new javax.swing.JSeparator();
        jLabel13 = new javax.swing.JLabel();
        btntutup = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jLabel15 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(255, 255, 255));

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setMaximumSize(new java.awt.Dimension(320, 32767));
        jPanel1.setMinimumSize(new java.awt.Dimension(320, 100));
        jPanel1.setPreferredSize(new java.awt.Dimension(320, 600));

        jLabel1.setFont(new java.awt.Font("Monospaced", 0, 10)); // NOI18N
        jLabel1.setText("SARI MURNI REJEKI");

        jLabel2.setFont(new java.awt.Font("Monospaced", 0, 10)); // NOI18N
        jLabel2.setText("Jln. Raya Katung Payangan Kintamani, Bangli ");

        jLabel3.setFont(new java.awt.Font("Monospaced", 0, 10)); // NOI18N
        jLabel3.setText("Telp. 083851003084");

        lblWaktu.setFont(new java.awt.Font("Monospaced", 0, 10)); // NOI18N
        lblWaktu.setText("Waktu");

        lblKasir.setFont(new java.awt.Font("Monospaced", 0, 10)); // NOI18N
        lblKasir.setText("Admin");

        lblTransaksi.setFont(new java.awt.Font("Monospaced", 0, 10)); // NOI18N
        lblTransaksi.setText("Kode Transaksi");

        tblDetail.setFont(new java.awt.Font("Monospaced", 0, 9)); // NOI18N
        tblDetail.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(tblDetail);

        lbl1.setFont(new java.awt.Font("Monospaced", 0, 10)); // NOI18N
        lbl1.setText("Rp.0");

        lblSub.setFont(new java.awt.Font("Monospaced", 0, 10)); // NOI18N
        lblSub.setText("Sub Total");

        lbl2.setFont(new java.awt.Font("Monospaced", 0, 10)); // NOI18N
        lbl2.setText("Rp.0");

        lblBayar.setFont(new java.awt.Font("Monospaced", 0, 10)); // NOI18N
        lblBayar.setText("Bayar");

        txt3.setFont(new java.awt.Font("Monospaced", 0, 10)); // NOI18N
        txt3.setText("Rp.0");

        lblKembali.setFont(new java.awt.Font("Monospaced", 0, 10)); // NOI18N
        lblKembali.setText("Kembalian");

        jLabel13.setFont(new java.awt.Font("Monospaced", 0, 9)); // NOI18N
        jLabel13.setText("Terima Kasih");

        btntutup.setText("Tutup");
        btntutup.addActionListener(this::btntutupActionPerformed);

        jButton2.setText("Print");
        jButton2.addActionListener(this::jButton2ActionPerformed);

        jLabel15.setFont(new java.awt.Font("Monospaced", 0, 9)); // NOI18N
        jLabel15.setText("Selamat Datang Kembali");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator1)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblTransaksi)
                    .addComponent(lblWaktu))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblKasir)
                .addGap(48, 48, 48))
            .addComponent(jSeparator2)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(jSeparator3)
            .addComponent(jSeparator4)
            .addComponent(jSeparator5)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(btntutup)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton2))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(115, 115, 115)
                        .addComponent(jLabel13))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(86, 86, 86)
                        .addComponent(jLabel15))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(107, 107, 107)
                        .addComponent(jLabel1))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(27, 27, 27)
                        .addComponent(jLabel2))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(16, 16, 16)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblBayar)
                            .addComponent(lblKembali))
                        .addGap(160, 160, 160)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbl2)
                            .addComponent(txt3)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(104, 104, 104)
                        .addComponent(jLabel3)))
                .addContainerGap(29, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addComponent(lblSub)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lbl1)
                .addGap(66, 66, 66))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblWaktu)
                    .addComponent(lblKasir))
                .addGap(27, 27, 27)
                .addComponent(lblTransaksi)
                .addGap(18, 18, 18)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 202, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblSub)
                    .addComponent(lbl1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblBayar)
                    .addComponent(lbl2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txt3)
                    .addComponent(lblKembali))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator5, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel15)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 66, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btntutup)
                    .addComponent(jButton2))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 626, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btntutupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btntutupActionPerformed
    this.dispose();         // TODO add your handling code here:
    }//GEN-LAST:event_btntutupActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
    printThermal58mm();        // TODO add your handling code here:
    }//GEN-LAST:event_jButton2ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new detailtransaksi().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btntutup;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JLabel lbl1;
    private javax.swing.JLabel lbl2;
    private javax.swing.JLabel lblBayar;
    private javax.swing.JLabel lblKasir;
    private javax.swing.JLabel lblKembali;
    private javax.swing.JLabel lblSub;
    private javax.swing.JLabel lblTransaksi;
    private javax.swing.JLabel lblWaktu;
    private javax.swing.JTable tblDetail;
    private javax.swing.JLabel txt3;
    // End of variables declaration//GEN-END:variables
}
