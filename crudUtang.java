/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package gui;

import config.koneksi;
import controller.transaksicontroller;
import java.awt.Color;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import model.modelutang;
import controller.utangcontroller;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.swing.JFrame;
import model.modeltd;
import model.modeltransaksi;

/**
 *
 * @author attia
 */
public class crudUtang extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(crudUtang.class.getName());

    int x = 0; 
    private DefaultTableModel model; 
    DefaultTableModel modelBarang;      // kanan atas
    DefaultTableModel modelCartKredit;  // kiri atas
    utangcontroller uc = new utangcontroller(); 
    modelutang mu = new modelutang(); 
    List<modelutang> ListUtang = new ArrayList<modelutang>();
    /**
     * Creates new form crudUtang
     */
    public crudUtang() {
        initComponents();
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        initTable();
        generateKodeTransaksi();
        clearForm();
        loadBarang();
        buatTable();
        showTable();
        setPlaceholderTanggal();
        
    }
    
    private boolean isFormFilled() {
    return !txtKd.getText().isEmpty()
        || !txtNm.getText().isEmpty()
        || !txtAlt.getText().isEmpty()
        || !txtTlp.getText().isEmpty()
        || !txtHb.getText().isEmpty()
        || !txtDp.getText().isEmpty()
        || !txtJt.getText().isEmpty();
}
    
    private void initTable() {

    modelBarang = new DefaultTableModel(
        new Object[]{"Kode", "Nama", "Harga"}, 0
    );
    tblbarang.setModel(modelBarang);

    modelCartKredit = new DefaultTableModel(
        new Object[]{"Kode", "Nama", "Harga", "Qty", "Subtotal"}, 0
    );
    tblkredit.setModel(modelCartKredit);

}
    
private String generateKodeTransaksi() {
    return "TRX-" + System.currentTimeMillis();
}    
    
    
private void loadBarang() {
    modelBarang.setRowCount(0);

    try {
        Connection con = koneksi.getConnection();
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery("SELECT kode_barang, nama_barang, harga_jual FROM barang");

        while (rs.next()) {
            modelBarang.addRow(new Object[]{
                rs.getString("kode_barang"),
                rs.getString("nama_barang"),
                rs.getInt("harga_jual")
            });
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, e.getMessage());
    }
}

private int hitungTotal() {
    int total = 0;
    for (int i = 0; i < modelCartKredit.getRowCount(); i++) {
        total += Integer.parseInt(
            modelCartKredit.getValueAt(i, 4).toString()
        );
    }
    txtHb.setText(String.valueOf(total));
    return total;
}
    
private List<modeltd> ambilDetail(String kodeTransaksi) {

    List<modeltd> list = new ArrayList<>();

    for (int i = 0; i < tblkredit.getRowCount(); i++) {
        modeltd d = new modeltd();
        d.setKodeTransaksi(kodeTransaksi);
        d.setKodeBarang(tblkredit.getValueAt(i, 0).toString());
//        d.setNamaBarang(tblkredit.getValueAt(i, 1).toString());
        d.setHarga(Integer.parseInt(
                tblkredit.getValueAt(i, 2).toString()
        ));
        d.setQty(Integer.parseInt(
                tblkredit.getValueAt(i, 3).toString()
        ));
        d.setSubtotal(Integer.parseInt(
                tblkredit.getValueAt(i, 4).toString()
        ));
        list.add(d);
    }
    return list;
}

    private void buatTable() {
        model = new DefaultTableModel();
        model.addColumn("Kode Transaksi");
        model.addColumn("Nama");
        model.addColumn("Alamat");
        model.addColumn("Telepon");
        model.addColumn("Harga Barang");
        model.addColumn("DP");
        model.addColumn("Jumlah Cicilan");
        model.addColumn("Jatuh Tempo");
        Tableutang.setModel(model);
    }
    
    private void showTable() {
    model.setRowCount(0);
    ListUtang = uc.tampil();

    for (modelutang mu : ListUtang) {
        Object[] data = new Object[8];
        data[0] = mu.getKdutang();
        data[1] = mu.getNama();
        data[2] = mu.getAlamat();
        data[3] = mu.getTelepon();
        data[4] = mu.getHargabrng();
        data[5] = mu.getDp();
        data[6] = mu.getJumlahcicilan();
        data[7] = mu.getJatuhTempo().toString(); // LocalDate → String

        model.addRow(data);
    }   
}
    
    private void clearForm() {
    txtKd.setText(generateKodeTransaksi());
    txtNm.setText("");
    txtAlt.setText("");
    txtTlp.setText("");
    txtHb.setText("");
    txtDp.setText("");
    txtJt.setText("");
    cmbJc.setSelectedIndex(0);
    
    txtJt.setText(PLACEHOLDER_TGL);
    txtJt.setForeground(Color.GRAY);
    DefaultTableModel model = (DefaultTableModel) tblkredit.getModel();
    model.setRowCount(0);
}
    
    private final String PLACEHOLDER_TGL = "Contoh : 2026-01-31";
    
    private void setPlaceholderTanggal() {
    txtJt.setText(PLACEHOLDER_TGL);
    txtJt.setForeground(Color.GRAY);

    txtJt.addFocusListener(new FocusAdapter() {

        @Override
        public void focusGained(FocusEvent e) {
            if (txtJt.getText().equals(PLACEHOLDER_TGL)) {
                txtJt.setText("");
                txtJt.setForeground(Color.BLACK);
            }
        }

        @Override
        public void focusLost(FocusEvent e) {
            if (txtJt.getText().isEmpty()) {
                txtJt.setText(PLACEHOLDER_TGL);
                txtJt.setForeground(Color.GRAY);
            }
        }
    });
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
        utangbutton1 = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        utangbutton = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        dashboardbutton1 = new javax.swing.JPanel();
        jLabel14 = new javax.swing.JLabel();
        dashboardbutton3 = new javax.swing.JPanel();
        jLabel15 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        Tableutang = new javax.swing.JTable();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        txtNm = new javax.swing.JTextField();
        txtAlt = new javax.swing.JTextField();
        txtTlp = new javax.swing.JTextField();
        txtHb = new javax.swing.JTextField();
        txtDp = new javax.swing.JTextField();
        cmbJc = new javax.swing.JComboBox<>();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        btnDetail = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jLabel16 = new javax.swing.JLabel();
        txtJt = new javax.swing.JTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblkredit = new javax.swing.JTable();
        jScrollPane3 = new javax.swing.JScrollPane();
        tblbarang = new javax.swing.JTable();
        jLabel17 = new javax.swing.JLabel();
        txtKd = new javax.swing.JTextField();
        btntmbahbrng = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

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
                .addContainerGap(68, Short.MAX_VALUE))
        );
        dashboardbuttonLayout.setVerticalGroup(
            dashboardbuttonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, dashboardbuttonLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(29, 29, 29))
        );

        sidepanel.add(dashboardbutton, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 320, 270, 50));

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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lbl_jam)
                .addContainerGap())
        );

        sidepanel.add(jPanel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 168, 330, 140));

        utangbutton1.setBackground(new java.awt.Color(242, 246, 250));
        utangbutton1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        utangbutton1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                utangbutton1MouseClicked(evt);
            }
        });

        jLabel10.setFont(new java.awt.Font("Poppins", 0, 24)); // NOI18N
        jLabel10.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Speed_25px.png"))); // NOI18N
        jLabel10.setText("Log Transaksi");
        jLabel10.setAlignmentY(0.0F);

        javax.swing.GroupLayout utangbutton1Layout = new javax.swing.GroupLayout(utangbutton1);
        utangbutton1.setLayout(utangbutton1Layout);
        utangbutton1Layout.setHorizontalGroup(
            utangbutton1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(utangbutton1Layout.createSequentialGroup()
                .addGap(35, 35, 35)
                .addComponent(jLabel10)
                .addContainerGap(41, Short.MAX_VALUE))
        );
        utangbutton1Layout.setVerticalGroup(
            utangbutton1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(utangbutton1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel10)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        sidepanel.add(utangbutton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 470, 270, -1));

        utangbutton.setBackground(new java.awt.Color(242, 246, 250));
        utangbutton.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        utangbutton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                utangbuttonMouseClicked(evt);
            }
        });

        jLabel11.setFont(new java.awt.Font("Poppins", 0, 24)); // NOI18N
        jLabel11.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Speed_25px.png"))); // NOI18N
        jLabel11.setText("Utang");
        jLabel11.setAlignmentY(0.0F);

        javax.swing.GroupLayout utangbuttonLayout = new javax.swing.GroupLayout(utangbutton);
        utangbutton.setLayout(utangbuttonLayout);
        utangbuttonLayout.setHorizontalGroup(
            utangbuttonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(utangbuttonLayout.createSequentialGroup()
                .addGap(35, 35, 35)
                .addComponent(jLabel11)
                .addContainerGap(130, Short.MAX_VALUE))
        );
        utangbuttonLayout.setVerticalGroup(
            utangbuttonLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(utangbuttonLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel11)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        sidepanel.add(utangbutton, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 520, 270, 50));

        jPanel1.setBackground(new java.awt.Color(242, 246, 250));
        jPanel1.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));

        jLabel12.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Logout_Rounded_Left_20px.png"))); // NOI18N
        jLabel12.setText("LogOut");
        jLabel12.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jLabel12MousePressed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addGap(0, 18, Short.MAX_VALUE)
                    .addComponent(jLabel12)
                    .addGap(0, 18, Short.MAX_VALUE)))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 24, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addGap(0, 2, Short.MAX_VALUE)
                    .addComponent(jLabel12)
                    .addGap(0, 2, Short.MAX_VALUE)))
        );

        sidepanel.add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 600, -1, 30));

        jLabel13.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/logosmrcropmini.png"))); // NOI18N
        jLabel13.setMaximumSize(new java.awt.Dimension(100, 100));
        jLabel13.setMinimumSize(new java.awt.Dimension(100, 100));
        jLabel13.setPreferredSize(new java.awt.Dimension(100, 100));
        sidepanel.add(jLabel13, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 0, 195, 152));

        dashboardbutton1.setBackground(new java.awt.Color(242, 246, 250));
        dashboardbutton1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        dashboardbutton1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                dashboardbutton1MouseClicked(evt);
            }
        });

        jLabel14.setFont(new java.awt.Font("Poppins", 0, 24)); // NOI18N
        jLabel14.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Speed_25px.png"))); // NOI18N
        jLabel14.setText("Transaksi");

        javax.swing.GroupLayout dashboardbutton1Layout = new javax.swing.GroupLayout(dashboardbutton1);
        dashboardbutton1.setLayout(dashboardbutton1Layout);
        dashboardbutton1Layout.setHorizontalGroup(
            dashboardbutton1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dashboardbutton1Layout.createSequentialGroup()
                .addGap(36, 36, 36)
                .addComponent(jLabel14)
                .addContainerGap(88, Short.MAX_VALUE))
        );
        dashboardbutton1Layout.setVerticalGroup(
            dashboardbutton1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, dashboardbutton1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel14)
                .addGap(29, 29, 29))
        );

        sidepanel.add(dashboardbutton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 370, 270, 50));

        dashboardbutton3.setBackground(new java.awt.Color(242, 246, 250));
        dashboardbutton3.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        dashboardbutton3.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                dashboardbutton3MouseClicked(evt);
            }
        });

        jLabel15.setFont(new java.awt.Font("Poppins", 0, 24)); // NOI18N
        jLabel15.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/icons8_Speed_25px.png"))); // NOI18N
        jLabel15.setText("Stock");

        javax.swing.GroupLayout dashboardbutton3Layout = new javax.swing.GroupLayout(dashboardbutton3);
        dashboardbutton3.setLayout(dashboardbutton3Layout);
        dashboardbutton3Layout.setHorizontalGroup(
            dashboardbutton3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dashboardbutton3Layout.createSequentialGroup()
                .addGap(36, 36, 36)
                .addComponent(jLabel15)
                .addContainerGap(136, Short.MAX_VALUE))
        );
        dashboardbutton3Layout.setVerticalGroup(
            dashboardbutton3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, dashboardbutton3Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel15)
                .addGap(29, 29, 29))
        );

        sidepanel.add(dashboardbutton3, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 420, 270, 50));

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        Tableutang.setModel(new javax.swing.table.DefaultTableModel(
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
        Tableutang.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                TableutangMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(Tableutang);

        jLabel3.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        jLabel3.setText("Nama");

        jLabel4.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        jLabel4.setText("Alamat");

        jLabel5.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        jLabel5.setText("Telepon");

        jLabel6.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        jLabel6.setText("Total");

        jLabel7.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        jLabel7.setText("Dp");

        jLabel8.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        jLabel8.setText("Jumlah Cicilan");

        jLabel9.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        jLabel9.setText("Jatuh Tempo");

        txtNm.setBackground(new java.awt.Color(242, 246, 250));
        txtNm.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        txtNm.addActionListener(this::txtNmActionPerformed);

        txtAlt.setBackground(new java.awt.Color(242, 246, 250));
        txtAlt.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N

        txtTlp.setBackground(new java.awt.Color(242, 246, 250));
        txtTlp.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N

        txtHb.setBackground(new java.awt.Color(242, 246, 250));
        txtHb.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N

        txtDp.setBackground(new java.awt.Color(242, 246, 250));
        txtDp.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N

        cmbJc.setBackground(new java.awt.Color(242, 246, 250));
        cmbJc.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1", "2", "3", "4" }));

        jButton1.setText("Add");
        jButton1.addActionListener(this::jButton1ActionPerformed);

        jButton2.setText("Edit");
        jButton2.addActionListener(this::jButton2ActionPerformed);

        jButton3.setText("Delete");
        jButton3.addActionListener(this::jButton3ActionPerformed);

        btnDetail.setText("Detail");
        btnDetail.addActionListener(this::btnDetailActionPerformed);

        jButton5.setText("Save");
        jButton5.addActionListener(this::jButton5ActionPerformed);

        jLabel16.setFont(new java.awt.Font("Poppins", 1, 24)); // NOI18N
        jLabel16.setText("KREDIT");

        txtJt.setBackground(new java.awt.Color(242, 246, 250));
        txtJt.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        txtJt.setText("Contoh : 2026-01-31");

        tblkredit.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane2.setViewportView(tblkredit);

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
        tblbarang.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                tblbarangKeyPressed(evt);
            }
        });
        jScrollPane3.setViewportView(tblbarang);

        jLabel17.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        jLabel17.setText("Kode Transaksi");

        txtKd.setBackground(new java.awt.Color(242, 246, 250));
        txtKd.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
        txtKd.addActionListener(this::txtKdActionPerformed);

        btntmbahbrng.setText("Tambah");
        btntmbahbrng.addActionListener(this::btntmbahbrngActionPerformed);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(35, 35, 35)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jButton5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnDetail))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addGap(125, 125, 125)
                                        .addComponent(txtDp, javax.swing.GroupLayout.PREFERRED_SIZE, 193, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(jLabel7)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jLabel8)
                                            .addComponent(jLabel6)
                                            .addComponent(jLabel5)
                                            .addComponent(jLabel4)
                                            .addComponent(jLabel3)
                                            .addComponent(jLabel9)
                                            .addComponent(jLabel17))
                                        .addGap(35, 35, 35)
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(txtNm, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 193, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(txtAlt, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 193, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(txtTlp, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 193, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(txtHb, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 193, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(cmbJc, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(txtJt, javax.swing.GroupLayout.PREFERRED_SIZE, 193, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(txtKd, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 193, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 359, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 391, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(btntmbahbrng))
                                .addGap(27, 27, 27))))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(453, 453, 453)
                        .addComponent(jLabel16))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 960, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(24, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel16)
                        .addGap(1, 1, 1)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 392, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btntmbahbrng)
                        .addGap(23, 23, 23))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 171, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtKd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel17))
                        .addGap(5, 5, 5)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtNm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtAlt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel4))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtTlp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtHb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel6))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtDp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel7))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel8)
                            .addComponent(cmbJc, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel9)
                            .addComponent(txtJt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton5)
                            .addComponent(jButton1)
                            .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButton3)
                            .addComponent(btnDetail))))
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(sidepanel, javax.swing.GroupLayout.PREFERRED_SIZE, 270, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(sidepanel, javax.swing.GroupLayout.DEFAULT_SIZE, 642, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtNmActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtNmActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtNmActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
    if (isFormFilled()) {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Form masih berisi data.\nApakah ingin membersihkan dan menambah data baru?",
            "Konfirmasi",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return; // batal clear
        }
    }
    clearForm();
    txtKd.setEditable(true);
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
    Connection con = null;
    try{
        // VALIDASI 1: Cart tidak boleh kosong
        if (tblkredit.getRowCount() == 0) {
            JOptionPane. showMessageDialog(this, "Keranjang kredit masih kosong!  Tambah barang terlebih dahulu.");
            return;
        }
        
        // VALIDASI 2: Input form
        if (txtNm.getText().isEmpty() || txtDp.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nama dan DP harus diisi!");
            return;
        }
        
        String kodeTransaksi = txtKd.getText();
        int total = hitungTotal();

        System.out.println("DEBUG: Starting save process. .");
        System.out.println("Kode Transaksi: " + kodeTransaksi);
        System.out.println("Total:  " + total);
        System.out.println("Cart items: " + tblkredit. getRowCount());

        // 1. transaksi
        modeltransaksi t = new modeltransaksi();
        t.setKodeTransaksi(kodeTransaksi);
        t.setNamaPelanggan(txtNm.getText());
        t.setTotal(total);
        t.setJenisTransaksi("KREDIT");
        t.setTanggal(java.time.LocalDateTime.now());
        
        System.out.println("DEBUG:  Transaksi model created");

        // 2. detail
        List<modeltd> detail = ambilDetail(kodeTransaksi);
        System.out.println("DEBUG: Detail items: " + detail.size());
        
        if (detail.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Detail transaksi kosong!");
            return;
        }

        // 3. utang
        modelutang u = new modelutang();
        u.setKdutang("UT-" + kodeTransaksi);
        u.setNama(txtNm.getText());
        u.setAlamat(txtAlt.getText());
        u.setTelepon(txtTlp.getText());
        u.setHargabrng(total);
        u.setDp(Integer.parseInt(txtDp.getText()));
        u.setJumlahcicilan(Integer.parseInt(cmbJc.getSelectedItem().toString()));
        u.setJatuhTempo(LocalDate.parse(txtJt.getText().trim()));
        u.setStatus("belum");
        u.setKodeTransaksi(kodeTransaksi);
        
        System.out.println("DEBUG: Utang model created");

        // 4. simpan semua
        transaksicontroller ctrl = new transaksicontroller();
        
        System.out.println("DEBUG: About to call simpanSemua()");
        boolean sukses = ctrl.simpanSemua(t, detail, null, u);
        System.out.println("DEBUG: simpanSemua() returned: " + sukses);

        if (sukses) {
            JOptionPane.showMessageDialog(this, "Transaksi Kredit Berhasil Disimpan!");
            clearForm();
            showTable();
        } else {
            JOptionPane.showMessageDialog(this, "Gagal menyimpan transaksi.  Cek console untuk error detail.");
        }
    }
    catch (NumberFormatException e) {
        System.out.println("ERROR:  Format number salah");
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Format input salah! (DP harus angka)");
    }
    catch (java.time.format.DateTimeParseException e) {
        System.out.println("ERROR: Format tanggal salah");
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Format tanggal salah!  Gunakan format:  YYYY-MM-DD");
    }
    catch (Exception e) {
        System.out. println("ERROR Exception di jButton5ActionPerformed:");
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Error:  " + e.getMessage());
    }
    

    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed

        String tgl = txtJt.getText();

    if (tgl.equals(PLACEHOLDER_TGL)) {
        JOptionPane.showMessageDialog(this,
            "Tanggal jatuh tempo wajib diisi");
        txtJt.requestFocus();
        return;
    }
        
    modelutang mu = new modelutang();
    mu.setKdutang(txtKd.getText());
    mu.setNama(txtNm.getText());
    mu.setAlamat(txtAlt.getText());
    mu.setTelepon(txtTlp.getText());
    mu.setHargabrng(Integer.parseInt(txtHb.getText()));
    mu.setDp(Integer.parseInt(txtDp.getText()));
    mu.setJumlahcicilan(Integer.parseInt(cmbJc.getSelectedItem().toString()));
    mu.setJatuhTempo(LocalDate.parse(txtJt.getText()));

    uc.updateUtang(mu);
    showTable();
    clearForm();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        int row = Tableutang.getSelectedRow();
    if (row == -1) {
        JOptionPane.showMessageDialog(this, "Pilih data terlebih dahulu");
        return;
    }

    String kodeUtang = Tableutang.getValueAt(row, 0).toString();

    int konfirmasi = JOptionPane.showConfirmDialog(
            this,
            "Yakin ingin menghapus data?",
            "Konfirmasi",
            JOptionPane.YES_NO_OPTION
    );

    if (konfirmasi == JOptionPane.YES_OPTION) {
        uc.deleteUtang(kodeUtang);
        showTable();
        clearForm();
    }
    }//GEN-LAST:event_jButton3ActionPerformed

    private void TableutangMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_TableutangMouseClicked
            int row = Tableutang.getSelectedRow();

    txtKd.setText(Tableutang.getValueAt(row, 0).toString());
    txtNm.setText(Tableutang.getValueAt(row, 1).toString());
    txtAlt.setText(Tableutang.getValueAt(row, 2).toString());
    txtTlp.setText(Tableutang.getValueAt(row, 3).toString());
    txtHb.setText(Tableutang.getValueAt(row, 4).toString());
    txtDp.setText(Tableutang.getValueAt(row, 5).toString());
    cmbJc.setSelectedItem(
        Tableutang.getValueAt(row, 6).toString()
    );
        txtJt.setText(Tableutang.getValueAt(row, 7).toString());
    txtJt.setForeground(Color.BLACK);

    // Kode utang TIDAK boleh diubah saat edit
    txtKd.setEditable(false);
    }//GEN-LAST:event_TableutangMouseClicked

    private void btnDetailActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDetailActionPerformed
    int row = Tableutang.getSelectedRow();

    System.out.println("Row terpilih: " + row);
    if (row == -1) {
        JOptionPane.showMessageDialog(this, "Pilih satu data terlebih dahulu");
        return;
    }

    String kode_utang = Tableutang.getValueAt(row, 0).toString();
        System.out.println("Kode transaksi: " + kode_utang);
    modelutang mu = uc.getByKode(kode_utang);

    if (mu == null) {
        JOptionPane.showMessageDialog(this, "Data tidak ditemukan");
        return;
    }

    detailutang detail = new detailutang(mu);
    detail.setVisible(true);
    }//GEN-LAST:event_btnDetailActionPerformed

    private void dashboardbuttonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_dashboardbuttonMouseClicked

        // TODO add your handling code here:
    }//GEN-LAST:event_dashboardbuttonMouseClicked

    private void utangbutton1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_utangbutton1MouseClicked
        logtransaksi lt = new logtransaksi();
        lt.setVisible(true);

    }//GEN-LAST:event_utangbutton1MouseClicked

    private void utangbuttonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_utangbuttonMouseClicked
        crudUtang cu = new crudUtang();
        cu.setVisible(true);
        this.dispose(); 
    }//GEN-LAST:event_utangbuttonMouseClicked

    private void jLabel12MousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel12MousePressed
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
    }//GEN-LAST:event_jLabel12MousePressed

    private void dashboardbutton1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_dashboardbutton1MouseClicked
        transaksi tr = new transaksi();
        tr.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_dashboardbutton1MouseClicked

    private void dashboardbutton3MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_dashboardbutton3MouseClicked
        stock st = new stock();
        st.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_dashboardbutton3MouseClicked

    private void tblbarangKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tblbarangKeyPressed

    int row = tblbarang.getSelectedRow();

    String kode = modelBarang.getValueAt(row, 0).toString();
    String nama = modelBarang.getValueAt(row, 1).toString();
    int harga = Integer.parseInt(modelBarang.getValueAt(row, 2).toString());

    int qty = Integer.parseInt(
        JOptionPane.showInputDialog("Qty:")
    );

    int subtotal = harga * qty;

    modelCartKredit.addRow(new Object[]{
        kode, nama, harga, qty, subtotal
    });

    hitungTotal();
    }//GEN-LAST:event_tblbarangKeyPressed

    private void txtKdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtKdActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtKdActionPerformed

    private void btntmbahbrngActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btntmbahbrngActionPerformed
    int row = tblbarang.getSelectedRow();

    if (row == -1) {
        JOptionPane.showMessageDialog(this, "Pilih barang terlebih dahulu");
        return;
    }

    String kodeBarang = tblbarang.getValueAt(row, 0).toString();
    String namaBarang = tblbarang.getValueAt(row, 1).toString();
    int harga = Integer.parseInt(
            tblbarang.getValueAt(row, 2).toString()
    );

    // Cek apakah barang sudah ada di cart
    for (int i = 0; i < modelCartKredit.getRowCount(); i++) {
        if (modelCartKredit.getValueAt(i, 0).toString().equals(kodeBarang)) {

            int qty = Integer.parseInt(modelCartKredit.getValueAt(i, 3).toString());
            qty++;

            modelCartKredit.setValueAt(qty, i, 3);
            modelCartKredit.setValueAt(qty * harga, i, 4);

            hitungTotal();
            return;
        }
    }

    // Jika belum ada → tambah baris baru
    modelCartKredit.addRow(new Object[]{
        kodeBarang,
        namaBarang,
        harga,
        1,
        harga
    });

    hitungTotal();
    }//GEN-LAST:event_btntmbahbrngActionPerformed

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
        java.awt.EventQueue.invokeLater(() -> new crudUtang().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable Tableutang;
    private javax.swing.JButton btnDetail;
    private javax.swing.JButton btntmbahbrng;
    private javax.swing.JComboBox<String> cmbJc;
    private javax.swing.JPanel dashboardbutton;
    private javax.swing.JPanel dashboardbutton1;
    private javax.swing.JPanel dashboardbutton3;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton5;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JLabel lbl_date;
    private javax.swing.JLabel lbl_jam;
    private javax.swing.JPanel sidepanel;
    private javax.swing.JTable tblbarang;
    private javax.swing.JTable tblkredit;
    private javax.swing.JTextField txtAlt;
    private javax.swing.JTextField txtDp;
    private javax.swing.JTextField txtHb;
    private javax.swing.JTextField txtJt;
    private javax.swing.JTextField txtKd;
    private javax.swing.JTextField txtNm;
    private javax.swing.JTextField txtTlp;
    private javax.swing.JLabel txtuser;
    private javax.swing.JPanel utangbutton;
    private javax.swing.JPanel utangbutton1;
    // End of variables declaration//GEN-END:variables
}
