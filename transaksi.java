/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import config.koneksi;
import controller.logtcontroller;
import controller.transaksicontroller;
import controller.transaksiservice;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import model.modellogt;
import model.modeltd;
import model.modeltransaksi;

/**
 *
 * @author attia
 */
public class transaksi extends javax.swing.JFrame {

    private Connection con = koneksi.getConnection();
    private String nama;
    private DefaultTableModel modelTransaksi;
    private DefaultTableModel modelBarang;
    logtcontroller logc = new logtcontroller();
    /**
     * Creates new form dashboard
     */
public transaksi() {
    initComponents();
    setExtendedState(JFrame.MAXIMIZED_BOTH);
    setLocationRelativeTo(null);
    startClock();
    initTable();
    this.nama = nama;
    tampilnama();
    loadBarang("");
    txtkdtransaksi.setEditable(false);
    txtkembali.setEditable(false);
    txtkembali.setFocusable(false);
    transaksicontroller tc = new transaksicontroller();
    txtkdtransaksi.setText(tc.generateNoTransaksi());
    
    
}

    private void initTable() {

        // TABEL TRANSAKSI
        modelTransaksi = new DefaultTableModel();
        modelTransaksi.addColumn("Kode");
        modelTransaksi.addColumn("Nama");
        modelTransaksi.addColumn("Harga");
        modelTransaksi.addColumn("Qty");
        modelTransaksi.addColumn("Subtotal");
        tbltransaksi.setModel(modelTransaksi);

        // TABEL BARANG
        modelBarang = new DefaultTableModel();
        modelBarang.addColumn("Kode");
        modelBarang.addColumn("Nama Barang");
        modelBarang.addColumn("Harga");
        tblbarang.setModel(modelBarang);
    }
    
    private int hitungTotalNilai() {
    int total = 0;
    for (int i = 0; i < modelTransaksi.getRowCount(); i++) {
        total += Integer.parseInt(modelTransaksi.getValueAt(i, 4).toString());
    }
    return total;
}
    
    private void updateTotal() {
    int total = hitungTotalNilai();
    txttotal.setText(String.valueOf(total));
    }
    
    private List<modeltd> ambilDetailDariTable() {

    List<modeltd> list = new ArrayList<>();

    for (int i = 0; i < modelTransaksi.getRowCount(); i++) {

        modeltd d = new modeltd();
        d.setKodeBarang(modelTransaksi.getValueAt(i, 0).toString());
        d.setHarga(Integer.parseInt(modelTransaksi.getValueAt(i, 2).toString()));
        d.setQty(Integer.parseInt(modelTransaksi.getValueAt(i, 3).toString()));
        d.setSubtotal(Integer.parseInt(modelTransaksi.getValueAt(i, 4).toString()));

        list.add(d);
    }
    return list;
}
    
private void simpanTransaksi() {

    if (modelTransaksi.getRowCount() == 0) {
        JOptionPane.showMessageDialog(this, "Belum ada barang di transaksi");
        return;
    }

    try {
        con.setAutoCommit(false);

        // =========================
        // 1. SIMPAN HEADER
        // =========================
        String sqlTrx = "INSERT INTO transaksi(kode_transaksi, tanggal, nama_pelanggan, total, jenis_transaksi) " +
                        "VALUES(?,?,?,?,?)";

        PreparedStatement psTrx = con.prepareStatement(sqlTrx);

        String kodeTrx = txtkdtransaksi.getText();
        String pelanggan = txtnmpelanggan.getText();
        int total = hitungTotalNilai();

        psTrx.setString(1, kodeTrx);
        psTrx.setDate(2, new java.sql.Date(System.currentTimeMillis()));
        psTrx.setString(3, pelanggan);
        psTrx.setInt(4, total);
        psTrx.setString(5, cmbtransaksi.getSelectedItem().toString());

        psTrx.executeUpdate();

        // =========================
        // 2. SIMPAN DETAIL
        // =========================
        String sqlDetail = "INSERT INTO transaksi_detail " +
                           "(kode_transaksi, kode_barang, harga, qty, subtotal) " +
                           "VALUES(?,?,?,?,?)";

        PreparedStatement psDet = con.prepareStatement(sqlDetail);

        for (int i = 0; i < modelTransaksi.getRowCount(); i++) {

            String kodeBarang = modelTransaksi.getValueAt(i, 0).toString();
            int harga        = Integer.parseInt(modelTransaksi.getValueAt(i, 2).toString());
            int qty          = Integer.parseInt(modelTransaksi.getValueAt(i, 3).toString());
            int subtotal     = Integer.parseInt(modelTransaksi.getValueAt(i, 4).toString());

            psDet.setString(1, kodeTrx);
            psDet.setString(2, kodeBarang);
            psDet.setInt(3, harga);
            psDet.setInt(4, qty);
            psDet.setInt(5, subtotal);

            psDet.addBatch();
        }

        psDet.executeBatch();

        con.commit();

        JOptionPane.showMessageDialog(this, "Transaksi berhasil disimpan");

        modelTransaksi.setRowCount(0);
        txttotal.setText("Rp 0");

    } catch (Exception e) {
        try { con.rollback(); } catch (Exception ex) {}
        JOptionPane.showMessageDialog(this, "Gagal simpan: " + e.getMessage());
        e.printStackTrace();
    } finally {
        try { con.setAutoCommit(true); } catch (Exception e) {}
    }
}

private void hapusBarangDariTransaksi() {

    int row = tbltransaksi.getSelectedRow();

    if (row == -1) {
        JOptionPane.showMessageDialog(this,
                "Pilih barang yang ingin dihapus terlebih dahulu");
        return;
    }

    int konfirmasi = JOptionPane.showConfirmDialog(
            this,
            "Hapus barang ini dari transaksi?",
            "Konfirmasi",
            JOptionPane.YES_NO_OPTION
    );

    if (konfirmasi != JOptionPane.YES_OPTION) return;

    // hapus dari tabel
    modelTransaksi.removeRow(row);

    // update total
    updateTotal();
}

    private void loadBarang(String keyword) {

    modelBarang.setRowCount(0);

    String sql = "SELECT kode_barang, nama_barang, harga_jual FROM barang " +
                 "WHERE nama_barang LIKE ? OR kode_barang LIKE ?";

    try (PreparedStatement ps = con.prepareStatement(sql)) {

        String key = "%" + keyword + "%";
        ps.setString(1, key);
        ps.setString(2, key);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            modelBarang.addRow(new Object[]{
                rs.getString("kode_barang"),
                rs.getString("nama_barang"),
                rs.getInt("harga_jual")
            });
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
}
     

private void hitungKembalian() {
    try {
        int total = Integer.parseInt(txttotal.getText().replaceAll("[^0-9]", ""));
        int bayar = Integer.parseInt(txtbayar.getText());

        int kembalian = bayar - total;

        if (kembalian < 0) {
            txtkembali.setText("0");
        } else {
            txtkembali.setText(String.valueOf(kembalian));
        }

    } catch (Exception e) {
        txtkembali.setText("0");
    }
}

  private void clearForm() {
    txttanggal.setText("");
    txtnmpelanggan.setText("");
    txttotal.setText("");
    txtbayar.setText("");
    DefaultTableModel model = (DefaultTableModel) tbltransaksi.getModel();
    model.setRowCount(0);
}
     

private void tampilnama() {
    txtuser.setText(nama);
}






       public void tampilUser(String user){
           txtuser.setText(user);
       }
        
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        sidepanel = new javax.swing.JPanel();
        dashboardbutton = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        txtuser = new javax.swing.JLabel();
        lbl_date = new javax.swing.JLabel();
        lbl_jam = new javax.swing.JLabel();
        utangbutton = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        transaksibutton = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        stockbutton = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        utangbutton1 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        btntbarang = new javax.swing.JButton();
        btnhps = new javax.swing.JButton();
        btnsimpan = new javax.swing.JButton();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        txtkdtransaksi = new javax.swing.JTextField();
        txttanggal = new javax.swing.JTextField();
        txtnmpelanggan = new javax.swing.JTextField();
        cmbtransaksi = new javax.swing.JComboBox<>();
        jScrollPane1 = new javax.swing.JScrollPane();
        tbltransaksi = new javax.swing.JTable();
        jLabel18 = new javax.swing.JLabel();
        txttotal = new javax.swing.JTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblbarang = new javax.swing.JTable();
        btnbatal = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        txtSearch = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        txtbayar = new javax.swing.JTextField();
        jLabel20 = new javax.swing.JLabel();
        txtkembali = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        dashboardbutton2 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        sidepanel.setBackground(new java.awt.Color(23, 118, 211));
        sidepanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        dashboardbutton.setBackground(new java.awt.Color(242, 246, 250));
        dashboardbutton.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        dashboardbutton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                dashboardbuttonMouseClicked(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Poppins", 0, 24)); // NOI18N
        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Speed_25px.png"))); // NOI18N
        jLabel1.setText("Dashboard");

        javax.swing.GroupLayout dashboardbuttonLayout = new javax.swing.GroupLayout(dashboardbutton);
        dashboardbutton.setLayout(dashboardbuttonLayout);
        dashboardbuttonLayout.setHorizontalGroup(
            dashboardbuttonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dashboardbuttonLayout.createSequentialGroup()
                .addGap(36, 36, 36)
                .addComponent(jLabel1)
                .addContainerGap(58, Short.MAX_VALUE))
        );
        dashboardbuttonLayout.setVerticalGroup(
            dashboardbuttonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, dashboardbuttonLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(29, 29, 29))
        );

        sidepanel.add(dashboardbutton, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 330, 260, 50));

        jPanel6.setBackground(new java.awt.Color(3, 169, 245));

        txtuser.setFont(new java.awt.Font("Poppins", 0, 24)); // NOI18N
        txtuser.setForeground(new java.awt.Color(255, 255, 255));
        txtuser.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        txtuser.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Male_User_25px.png"))); // NOI18N
        txtuser.setText("User");

        lbl_date.setFont(new java.awt.Font("Poppins", 0, 24)); // NOI18N
        lbl_date.setForeground(new java.awt.Color(255, 255, 255));
        lbl_date.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lbl_date.setText("Tanggal");

        lbl_jam.setFont(new java.awt.Font("Poppins", 0, 24)); // NOI18N
        lbl_jam.setForeground(new java.awt.Color(255, 255, 255));
        lbl_jam.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lbl_jam.setText("Jam");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lbl_date, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE)
                    .addComponent(lbl_jam, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(802, 802, 802))
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(txtuser)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(8, 8, 8)
                .addComponent(txtuser)
                .addGap(18, 18, 18)
                .addComponent(lbl_date)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lbl_jam)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        sidepanel.add(jPanel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 168, 260, 140));

        utangbutton.setBackground(new java.awt.Color(242, 246, 250));
        utangbutton.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        utangbutton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                utangbuttonMouseClicked(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Poppins", 0, 24)); // NOI18N
        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Speed_25px.png"))); // NOI18N
        jLabel2.setText("Utang");
        jLabel2.setAlignmentY(0.0F);

        javax.swing.GroupLayout utangbuttonLayout = new javax.swing.GroupLayout(utangbutton);
        utangbutton.setLayout(utangbuttonLayout);
        utangbuttonLayout.setHorizontalGroup(
            utangbuttonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(utangbuttonLayout.createSequentialGroup()
                .addGap(35, 35, 35)
                .addComponent(jLabel2)
                .addContainerGap(120, Short.MAX_VALUE))
        );
        utangbuttonLayout.setVerticalGroup(
            utangbuttonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(utangbuttonLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        sidepanel.add(utangbutton, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 480, 260, 50));

        jPanel1.setBackground(new java.awt.Color(242, 246, 250));
        jPanel1.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));

        jLabel3.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Logout_Rounded_Left_20px.png"))); // NOI18N
        jLabel3.setText("LogOut");
        jLabel3.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jLabel3MousePressed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 103, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addGap(0, 18, Short.MAX_VALUE)
                    .addComponent(jLabel3)
                    .addGap(0, 18, Short.MAX_VALUE)))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 24, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addGap(0, 2, Short.MAX_VALUE)
                    .addComponent(jLabel3)
                    .addGap(0, 2, Short.MAX_VALUE)))
        );

        sidepanel.add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 590, -1, 30));

        jLabel4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/logosmrcropmini.png"))); // NOI18N
        jLabel4.setMaximumSize(new java.awt.Dimension(100, 100));
        jLabel4.setMinimumSize(new java.awt.Dimension(100, 100));
        jLabel4.setPreferredSize(new java.awt.Dimension(100, 100));
        sidepanel.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 0, 195, 152));

        transaksibutton.setBackground(new java.awt.Color(242, 246, 250));
        transaksibutton.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        transaksibutton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                transaksibuttonMouseClicked(evt);
            }
        });

        jLabel7.setFont(new java.awt.Font("Poppins", 0, 24)); // NOI18N
        jLabel7.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Speed_25px.png"))); // NOI18N
        jLabel7.setText("Transaksi");

        javax.swing.GroupLayout transaksibuttonLayout = new javax.swing.GroupLayout(transaksibutton);
        transaksibutton.setLayout(transaksibuttonLayout);
        transaksibuttonLayout.setHorizontalGroup(
            transaksibuttonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(transaksibuttonLayout.createSequentialGroup()
                .addGap(36, 36, 36)
                .addComponent(jLabel7)
                .addContainerGap(78, Short.MAX_VALUE))
        );
        transaksibuttonLayout.setVerticalGroup(
            transaksibuttonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, transaksibuttonLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel7)
                .addGap(29, 29, 29))
        );

        sidepanel.add(transaksibutton, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 380, 260, 50));

        stockbutton.setBackground(new java.awt.Color(242, 246, 250));
        stockbutton.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        stockbutton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                stockbuttonMouseClicked(evt);
            }
        });

        jLabel9.setFont(new java.awt.Font("Poppins", 0, 24)); // NOI18N
        jLabel9.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Speed_25px.png"))); // NOI18N
        jLabel9.setText("Stock");

        javax.swing.GroupLayout stockbuttonLayout = new javax.swing.GroupLayout(stockbutton);
        stockbutton.setLayout(stockbuttonLayout);
        stockbuttonLayout.setHorizontalGroup(
            stockbuttonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(stockbuttonLayout.createSequentialGroup()
                .addGap(36, 36, 36)
                .addComponent(jLabel9)
                .addContainerGap(126, Short.MAX_VALUE))
        );
        stockbuttonLayout.setVerticalGroup(
            stockbuttonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, stockbuttonLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel9)
                .addGap(29, 29, 29))
        );

        sidepanel.add(stockbutton, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 430, 260, 50));

        utangbutton1.setBackground(new java.awt.Color(242, 246, 250));
        utangbutton1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        utangbutton1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                utangbutton1MouseClicked(evt);
            }
        });

        jLabel6.setFont(new java.awt.Font("Poppins", 0, 24)); // NOI18N
        jLabel6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Speed_25px.png"))); // NOI18N
        jLabel6.setText("Log Transaksi");
        jLabel6.setAlignmentY(0.0F);

        javax.swing.GroupLayout utangbutton1Layout = new javax.swing.GroupLayout(utangbutton1);
        utangbutton1.setLayout(utangbutton1Layout);
        utangbutton1Layout.setHorizontalGroup(
            utangbutton1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(utangbutton1Layout.createSequentialGroup()
                .addGap(35, 35, 35)
                .addComponent(jLabel6)
                .addContainerGap(31, Short.MAX_VALUE))
        );
        utangbutton1Layout.setVerticalGroup(
            utangbutton1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(utangbutton1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel6)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        sidepanel.add(utangbutton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 530, 260, -1));

        getContentPane().add(sidepanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 260, 650));

        jPanel3.setBackground(new java.awt.Color(242, 246, 250));
        jPanel3.setEnabled(false);

        btntbarang.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
        btntbarang.setText("Tambah Barang");
        btntbarang.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btntbarangActionPerformed(evt);
            }
        });

        btnhps.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
        btnhps.setText("Hapus");
        btnhps.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnhpsActionPerformed(evt);
            }
        });

        btnsimpan.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
        btnsimpan.setText("Simpan");
        btnsimpan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnsimpanActionPerformed(evt);
            }
        });

        jLabel14.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
        jLabel14.setText("Kode Transaksi");

        jLabel15.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
        jLabel15.setText("Tanggal");

        jLabel16.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
        jLabel16.setText("Jenis Transaksi ");

        jLabel17.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
        jLabel17.setText("Nama Pelanggan");

        txtkdtransaksi.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
        txtkdtransaksi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtkdtransaksiActionPerformed(evt);
            }
        });

        txttanggal.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N

        txtnmpelanggan.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N

        cmbtransaksi.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
        cmbtransaksi.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "TUNAI", "KREDIT" }));
        cmbtransaksi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbtransaksiActionPerformed(evt);
            }
        });

        tbltransaksi.setBackground(new java.awt.Color(242, 246, 250));
        tbltransaksi.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        tbltransaksi.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane1.setViewportView(tbltransaksi);

        jLabel18.setFont(new java.awt.Font("Poppins", 0, 18)); // NOI18N
        jLabel18.setText("Total :");

        txttotal.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
        txttotal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txttotalActionPerformed(evt);
            }
        });

        tblbarang.setBackground(new java.awt.Color(242, 246, 250));
        tblbarang.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        tblbarang.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane2.setViewportView(tblbarang);

        btnbatal.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
        btnbatal.setText("Batal");

        jLabel5.setText("Search :");

        txtSearch.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                txtSearchMouseReleased(evt);
            }
        });
        txtSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtSearchActionPerformed(evt);
            }
        });
        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtSearchKeyReleased(evt);
            }
        });

        jLabel19.setFont(new java.awt.Font("Poppins", 0, 18)); // NOI18N
        jLabel19.setText("Bayar :");

        txtbayar.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
        txtbayar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtbayarActionPerformed(evt);
            }
        });
        txtbayar.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtbayarKeyReleased(evt);
            }
        });

        jLabel20.setFont(new java.awt.Font("Poppins", 0, 18)); // NOI18N
        jLabel20.setText("Kembali :");

        txtkembali.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
        txtkembali.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtkembaliActionPerformed(evt);
            }
        });
        txtkembali.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtkembaliKeyReleased(evt);
            }
        });

        jLabel11.setText("Rp.");

        jLabel12.setText("Rp.");

        jLabel13.setFont(new java.awt.Font("Poppins", 1, 24)); // NOI18N
        jLabel13.setText("TRANSAKSI");

        jLabel21.setText("Rp.");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(41, 41, 41)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel14)
                                    .addComponent(jLabel15))
                                .addGap(29, 29, 29)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(txtkdtransaksi, javax.swing.GroupLayout.PREFERRED_SIZE, 316, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txttanggal, javax.swing.GroupLayout.PREFERRED_SIZE, 316, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel17)
                                    .addComponent(jLabel16))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(txtnmpelanggan, javax.swing.GroupLayout.PREFERRED_SIZE, 315, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(cmbtransaksi, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addComponent(btnsimpan)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnbatal))
                            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 444, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel3Layout.createSequentialGroup()
                                    .addGap(14, 14, 14)
                                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel3Layout.createSequentialGroup()
                                            .addComponent(jLabel19)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(jLabel12))
                                        .addGroup(jPanel3Layout.createSequentialGroup()
                                            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jLabel18)
                                                .addComponent(jLabel20, javax.swing.GroupLayout.Alignment.TRAILING))
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 16, Short.MAX_VALUE)
                                            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jLabel21, javax.swing.GroupLayout.Alignment.TRAILING)
                                                .addComponent(jLabel11, javax.swing.GroupLayout.Alignment.TRAILING))))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(txtbayar, javax.swing.GroupLayout.PREFERRED_SIZE, 318, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(txttotal, javax.swing.GroupLayout.PREFERRED_SIZE, 318, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(txtkembali, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 318, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGap(10, 10, 10))))
                        .addGap(63, 63, 63)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 383, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 243, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(btntbarang)
                                .addGap(30, 30, 30)
                                .addComponent(btnhps))))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(467, 467, 467)
                        .addComponent(jLabel13)))
                .addContainerGap(709, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addComponent(jLabel13)
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(txtkdtransaksi, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel5)
                        .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel14))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel15)
                            .addComponent(txttanggal, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel17)
                            .addComponent(txtnmpelanggan, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(cmbtransaksi, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel16))
                        .addGap(12, 12, 12)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 251, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel18)
                            .addComponent(txttotal, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel11))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel19)
                            .addComponent(txtbayar, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel12))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel20)
                            .addComponent(txtkembali, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel21)))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 470, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btntbarang, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnhps, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(12, 12, 12))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnsimpan, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnbatal, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(304, 304, 304))
        );

        getContentPane().add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 0, 1660, 650));

        dashboardbutton2.setBackground(new java.awt.Color(204, 204, 204));
        dashboardbutton2.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        dashboardbutton2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                dashboardbutton2MouseClicked(evt);
            }
        });

        jLabel8.setFont(new java.awt.Font("Tahoma", 0, 24)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(255, 255, 255));
        jLabel8.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Speed_25px.png"))); // NOI18N
        jLabel8.setText("Dashboard");

        javax.swing.GroupLayout dashboardbutton2Layout = new javax.swing.GroupLayout(dashboardbutton2);
        dashboardbutton2.setLayout(dashboardbutton2Layout);
        dashboardbutton2Layout.setHorizontalGroup(
            dashboardbutton2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dashboardbutton2Layout.createSequentialGroup()
                .addGap(36, 36, 36)
                .addComponent(jLabel8)
                .addContainerGap(77, Short.MAX_VALUE))
        );
        dashboardbutton2Layout.setVerticalGroup(
            dashboardbutton2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, dashboardbutton2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel8)
                .addGap(29, 29, 29))
        );

        getContentPane().add(dashboardbutton2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 430, 260, 50));

        jLabel10.setFont(new java.awt.Font("Poppins", 1, 36)); // NOI18N
        jLabel10.setText("TRANSAKSI");
        getContentPane().add(jLabel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(710, 0, -1, -1));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void dashboardbuttonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_dashboardbuttonMouseClicked
    dashboard dboard = new dashboard();
    dboard.setVisible(true);
    this.dispose();
    }//GEN-LAST:event_dashboardbuttonMouseClicked

    private void jLabel3MousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel3MousePressed
    int confirm = JOptionPane.showConfirmDialog(
        this,
        "Apakah Anda yakin ingin logout?",
        "Konfirmasi Logout",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.QUESTION_MESSAGE
    );

    if (confirm == JOptionPane.YES_OPTION) {
        this.dispose();              // Tutup dashboard
        login lg = new login();      // Kembali ke form login
        lg.setVisible(true);
    }
    }//GEN-LAST:event_jLabel3MousePressed

    private void utangbuttonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_utangbuttonMouseClicked
    crudUtang cu = new crudUtang();
    cu.setVisible(true);
    }//GEN-LAST:event_utangbuttonMouseClicked

    private void transaksibuttonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_transaksibuttonMouseClicked
    transaksi tr = new transaksi();
    tr.setVisible(true);
    this.dispose();
    }//GEN-LAST:event_transaksibuttonMouseClicked

    private void dashboardbutton2MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_dashboardbutton2MouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_dashboardbutton2MouseClicked

    private void stockbuttonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_stockbuttonMouseClicked
    stock stok = new stock();
    stok.setVisible(true);
    this.dispose();
    }//GEN-LAST:event_stockbuttonMouseClicked

    private void btntbarangActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btntbarangActionPerformed
    int row = tblbarang.getSelectedRow();

    if (row == -1) {
        JOptionPane.showMessageDialog(this, "Pilih barang terlebih dahulu");
        return;
    }

    // Ambil data dari table barang
    String kode_barang = tblbarang.getValueAt(row, 0).toString();
    String nama_barang = tblbarang.getValueAt(row, 1).toString();
    int harga_jual = Integer.parseInt(tblbarang.getValueAt(row, 2).toString());

    // default qty = 1
    int qty = 1;
    int subtotal = harga_jual * qty;

    // cek apakah barang sudah ada di table transaksi
    boolean ditemukan = false;

    for (int i = 0; i < modelTransaksi.getRowCount(); i++) {
        String kodeDiTabel = modelTransaksi.getValueAt(i, 0).toString();

        if (kodeDiTabel.equals(kode_barang)) {
            int qtyLama = Integer.parseInt(modelTransaksi.getValueAt(i, 3).toString());
            int qtyBaru = qtyLama + 1;

            modelTransaksi.setValueAt(qtyBaru, i, 3);
            modelTransaksi.setValueAt(qtyBaru * harga_jual, i, 4);

            ditemukan = true;
            break;
        }
    }

    // jika belum ada → tambah baris baru
    if (!ditemukan) {
        modelTransaksi.addRow(new Object[]{
            kode_barang, nama_barang, harga_jual, qty, subtotal
        });
    }

    updateTotal();
    }//GEN-LAST:event_btntbarangActionPerformed

    private void txttotalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txttotalActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txttotalActionPerformed

    private void txtSearchMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_txtSearchMouseReleased

    }//GEN-LAST:event_txtSearchMouseReleased

    private void txtSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtSearchActionPerformed

    }//GEN-LAST:event_txtSearchActionPerformed

    private void txtSearchKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtSearchKeyReleased
    
        loadBarang(txtSearch.getText());
    
    }//GEN-LAST:event_txtSearchKeyReleased

    private void btnsimpanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnsimpanActionPerformed
    try {
        
        // ================= TRANSAKSI =================
        modeltransaksi t = new modeltransaksi();
        t.setKodeTransaksi(txtkdtransaksi.getText());
        t.setTanggal(LocalDateTime.now());
        t.setNamaPelanggan(txtnmpelanggan.getText());
        t.setTotal(Integer.parseInt(txttotal.getText()));
        t.setJenisTransaksi(cmbtransaksi.getSelectedItem().toString());

        int bayar = Integer.parseInt(txtbayar.getText());
        int kembali = bayar - t.getTotal();

        // ================= DETAIL =================
        List<modeltd> detailList = ambilDetailDariTable();

        // ================= LOG =================
        modellogt log = new modellogt();
        log.setKodeTransaksi(t.getKodeTransaksi());
        log.setTanggal(LocalDateTime.now());
        log.setNamaPelanggan(t.getNamaPelanggan());
        log.setTotal(t.getTotal());
        log.setBayar(bayar);
        log.setKembali(kembali);
        log.setTipeTransaksi(t.getJenisTransaksi());
        log.setKeterangan("OK");

        // ================= SIMPAN SEMUA =================
        transaksiservice service = new transaksiservice();
        boolean sukses = service.simpanSemua(t, detailList, log);

        if (sukses) {
            JOptionPane.showMessageDialog(this, "Transaksi berhasil disimpan");
            clearForm();
        } else {
            JOptionPane.showMessageDialog(this, "Transaksi gagal disimpan");
        }

    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Terjadi kesalahan: " + e.getMessage());
        e.printStackTrace();
    }
    }//GEN-LAST:event_btnsimpanActionPerformed

    private void txtbayarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtbayarActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtbayarActionPerformed

    private void txtkembaliActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtkembaliActionPerformed

    }//GEN-LAST:event_txtkembaliActionPerformed

    private void txtkdtransaksiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtkdtransaksiActionPerformed

    }//GEN-LAST:event_txtkdtransaksiActionPerformed

    private void utangbutton1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_utangbutton1MouseClicked
    logtransaksi lt = new logtransaksi();
    lt.setVisible(true);
    }//GEN-LAST:event_utangbutton1MouseClicked

    private void btnhpsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnhpsActionPerformed
  hapusBarangDariTransaksi();
    }//GEN-LAST:event_btnhpsActionPerformed

    private void txtkembaliKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtkembaliKeyReleased
        hitungKembalian();
    }//GEN-LAST:event_txtkembaliKeyReleased

    private void txtbayarKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtbayarKeyReleased
    hitungKembalian();
    }//GEN-LAST:event_txtbayarKeyReleased

    private void cmbtransaksiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbtransaksiActionPerformed
    String jenis = cmbtransaksi.getSelectedItem().toString();

    if (jenis.equals("KREDIT")) {
        txtbayar.setText("0");
        txtbayar.setEnabled(false);

        txtkembali.setText("0");
        txtkembali.setEnabled(false);

    } else { // TUNAI
        txtbayar.setEnabled(true);
        txtkembali.setEnabled(false); // tetap readonly
    }
    }//GEN-LAST:event_cmbtransaksiActionPerformed

private void startClock() {
    Timer timer = new Timer(1000, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            Date now = new Date();

            SimpleDateFormat jamFormat = new SimpleDateFormat("HH:mm:ss");
            SimpleDateFormat tanggalFormat = new SimpleDateFormat("EEEE, dd MMM yyyy");

            lbl_jam.setText(jamFormat.format(now));
            lbl_date.setText(tanggalFormat.format(now));
        }
    });
    timer.start();
}
   
    
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
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(transaksi.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(transaksi.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(transaksi.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(transaksi.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new transaksi().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnbatal;
    private javax.swing.JButton btnhps;
    private javax.swing.JButton btnsimpan;
    private javax.swing.JButton btntbarang;
    private javax.swing.JComboBox<String> cmbtransaksi;
    private javax.swing.JPanel dashboardbutton;
    private javax.swing.JPanel dashboardbutton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lbl_date;
    private javax.swing.JLabel lbl_jam;
    private javax.swing.JPanel sidepanel;
    private javax.swing.JPanel stockbutton;
    private javax.swing.JTable tblbarang;
    private javax.swing.JTable tbltransaksi;
    private javax.swing.JPanel transaksibutton;
    private javax.swing.JTextField txtSearch;
    private javax.swing.JTextField txtbayar;
    private javax.swing.JTextField txtkdtransaksi;
    private javax.swing.JTextField txtkembali;
    private javax.swing.JTextField txtnmpelanggan;
    private javax.swing.JTextField txttanggal;
    private javax.swing.JTextField txttotal;
    private javax.swing.JLabel txtuser;
    private javax.swing.JPanel utangbutton;
    private javax.swing.JPanel utangbutton1;
    // End of variables declaration//GEN-END:variables

//    private void SetLocationRelativeTo(Object object) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }
}
