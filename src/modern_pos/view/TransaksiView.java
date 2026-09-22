package modern_pos.view;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import modern_pos.controller.TransaksiController;
import modern_pos.model.Barang;
import modern_pos.model.CartItem;
import modern_pos.utils.SwingHelper;
import modern_pos.utils.UITheme;

public class TransaksiView extends JFrame {
    private final TransaksiController controller;
    private JTextField txtSearch, txtPelanggan, txtBayar;
    private JLabel lblTotalBesar, lblKembali;
    private JTable tblBarang, tblCart;
    private DefaultTableModel modelBarang, modelCart;
    private JButton btnBayar;
    private List<Barang> currentBarangList;
    private int currentTotal = 0;

    public TransaksiView(TransaksiController controller) {
        this.controller = controller;
        initUI();
        controller.setView(this);
    }

    private void initUI() {
        setTitle("Sari Murni Rejeki - Point of Sales");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- SIDEBAR (Mirip Dashboard) ---
        add(Sidebar.buat(this, Sidebar.TRANSAKSI, Sidebar.keLogin(this)), BorderLayout.WEST);
        
        // --- EVENT NAVIGASI ---

        // --- MAIN SPLIT CONTENT ---
        JPanel mainContent = new JPanel(new BorderLayout(15, 15));
        mainContent.setBackground(UITheme.COLOR_BG_APP);
        mainContent.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // --- BAGIAN KIRI: KATALOG BARANG ---
        JPanel pnlKatalog = new JPanel(new BorderLayout(10,10));
        pnlKatalog.setBackground(Color.WHITE);
        pnlKatalog.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230,230,230)), BorderFactory.createEmptyBorder(10,10,10,10)
        ));
        
        JPanel pnlSearch = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlSearch.setBackground(Color.WHITE);
        txtSearch = SwingHelper.createMaterialTextField();
        pnlSearch.add(SwingHelper.createLabel("Cari Barang: ", UITheme.FONT_BODY, UITheme.COLOR_TEXT_PRIMARY));
        pnlSearch.add(txtSearch);
        pnlKatalog.add(pnlSearch, BorderLayout.NORTH);

        modelBarang = new DefaultTableModel(new String[]{"Kode", "Nama", "Harga", "Stok"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tblBarang = new JTable(modelBarang);
        SwingHelper.styleTable(tblBarang);
        pnlKatalog.add(new JScrollPane(tblBarang), BorderLayout.CENTER);
        
        mainContent.add(pnlKatalog, BorderLayout.CENTER);

        // --- BAGIAN KANAN: CART & PEMBAYARAN ---
        JPanel pnlKanan = new JPanel(new BorderLayout(10,10));
        pnlKanan.setPreferredSize(new Dimension(450, 0)); // Lebar tetap untuk keranjang
        pnlKanan.setOpaque(false);

        // 1. Keranjang
        JPanel pnlCart = new JPanel(new BorderLayout());
        pnlCart.setBackground(Color.WHITE);
        pnlCart.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230,230,230)), BorderFactory.createEmptyBorder(10,10,10,10)
        ));
        pnlCart.add(SwingHelper.createLabel("Keranjang Belanja", UITheme.FONT_HEADING, UITheme.COLOR_PRIMARY), BorderLayout.NORTH);
        
        modelCart = new DefaultTableModel(new String[]{"Barang", "Qty", "Subtotal"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tblCart = new JTable(modelCart);
        SwingHelper.styleTable(tblCart);
        pnlCart.add(new JScrollPane(tblCart), BorderLayout.CENTER);

        // Tombol Hapus item keranjang (Klik Kanan / Double Click)
        JLabel lblHint = SwingHelper.createLabel("* Double-click baris keranjang untuk menghapus", UITheme.FONT_SMALL, UITheme.COLOR_DANGER);
        pnlCart.add(lblHint, BorderLayout.SOUTH);

        // 2. Pembayaran (Bawah Kanan)
        JPanel pnlBayar = new JPanel(new BorderLayout(10, 10));
        pnlBayar.setBackground(Color.WHITE);
        pnlBayar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(3, 0, 0, 0, UITheme.COLOR_PRIMARY), BorderFactory.createEmptyBorder(15,15,15,15)
        ));
        
        lblTotalBesar = new JLabel("Rp 0");
        lblTotalBesar.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 36));
        lblTotalBesar.setForeground(UITheme.COLOR_PRIMARY);
        lblTotalBesar.setHorizontalAlignment(JLabel.RIGHT);
        pnlBayar.add(lblTotalBesar, BorderLayout.NORTH);

        JPanel pnlForm = new JPanel(new GridLayout(3, 2, 5, 10));
        pnlForm.setBackground(Color.WHITE);
        
        txtPelanggan = SwingHelper.createMaterialTextField();
        txtPelanggan.setText("Umum"); // Default
        txtBayar = SwingHelper.createMaterialTextField();
        lblKembali = SwingHelper.createLabel("Rp 0", UITheme.FONT_HEADING, UITheme.COLOR_SUCCESS);
        
        pnlForm.add(SwingHelper.createLabel("Pelanggan:", UITheme.FONT_BODY, UITheme.COLOR_TEXT_PRIMARY));
        pnlForm.add(txtPelanggan);
        pnlForm.add(SwingHelper.createLabel("Bayar (Rp):", UITheme.FONT_BODY, UITheme.COLOR_TEXT_PRIMARY));
        pnlForm.add(txtBayar);
        pnlForm.add(SwingHelper.createLabel("Kembali:", UITheme.FONT_BODY, UITheme.COLOR_TEXT_PRIMARY));
        pnlForm.add(lblKembali);
        pnlBayar.add(pnlForm, BorderLayout.CENTER);

        btnBayar = SwingHelper.createFlatButton("SIMPAN & BAYAR", UITheme.COLOR_SUCCESS, new Color(56, 142, 60));
        btnBayar.setPreferredSize(new Dimension(0, 50));
        btnBayar.setFont(UITheme.FONT_TITLE);
        pnlBayar.add(btnBayar, BorderLayout.SOUTH);

        pnlKanan.add(pnlCart, BorderLayout.CENTER);
        pnlKanan.add(pnlBayar, BorderLayout.SOUTH);

        mainContent.add(pnlKanan, BorderLayout.EAST);
        add(mainContent, BorderLayout.CENTER);

        // --- EVENT LISTENERS ---
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { controller.loadBarang(txtSearch.getText()); }
        });

        // Pilih Barang -> Masuk Cart
        tblBarang.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tblBarang.getSelectedRow() != -1) {
                    int row = tblBarang.getSelectedRow();
                    Barang b = currentBarangList.get(row);
                    String input = JOptionPane.showInputDialog(TransaksiView.this, "Masukkan Qty untuk " + b.getNamaBarang() + ":", "1");
                    try {
                        int qty = Integer.parseInt(input);
                        controller.addToCart(b, qty);
                    } catch (Exception ex) {} // cancel or invalid
                }
            }
        });

        // Pilih Cart -> Hapus
        tblCart.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tblCart.getSelectedRow() != -1) {
                    controller.removeFromCart(tblCart.getSelectedRow());
                }
            }
        });

        // Hitung Kembalian Otomatis
        txtBayar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                try {
                    int bayar = Integer.parseInt(txtBayar.getText());
                    int kembali = bayar - currentTotal;
                    lblKembali.setText("Rp " + Math.max(0, kembali));
                } catch (Exception ex) {
                    lblKembali.setText("Rp 0");
                }
            }
        });

        btnBayar.addActionListener(e -> {
            try {
                int bayar = Integer.parseInt(txtBayar.getText());
                controller.processPayment(bayar, txtPelanggan.getText());
            } catch (Exception ex) {
                showError("Masukkan jumlah bayar yang valid!");
            }
        });
    }

    public void populateTableBarang(List<Barang> list) {
        this.currentBarangList = list;
        modelBarang.setRowCount(0);
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("id", "ID"));
        for (Barang b : list) {
            modelBarang.addRow(new Object[]{ b.getKodeBarang(), b.getNamaBarang(), "Rp " + nf.format(b.getHarga()), b.getStok() });
        }
    }

    public void populateTableCart(List<CartItem> cart) {
        modelCart.setRowCount(0);
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("id", "ID"));
        for (CartItem item : cart) {
            modelCart.addRow(new Object[]{ item.getBarang().getNamaBarang(), item.getQty(), "Rp " + nf.format(item.getSubtotal()) });
        }
    }

    public void updateTotal(int total) {
        this.currentTotal = total;
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("id", "ID"));
        lblTotalBesar.setText("Rp " + nf.format(total));
        // Reset bayar & kembali
        txtBayar.setText("");
        lblKembali.setText("Rp 0");
    }

    public void resetForm() {
        txtPelanggan.setText("Umum");
        txtBayar.setText("");
        lblKembali.setText("Rp 0");
        lblTotalBesar.setText("Rp 0");
    }

    public void setLoading(boolean isLoading) { btnBayar.setEnabled(!isLoading); }
    public void showStruk(modern_pos.model.Struk struk) { new StrukDialog(this, struk).setVisible(true); }
    public void showError(String msg) { JOptionPane.showMessageDialog(this, "<html><body style='width: 350px; font-family: Segoe UI, sans-serif;'>" + msg.replace("\n", "<br>") + "</body></html>", "Error", JOptionPane.ERROR_MESSAGE); }
    public void showSuccess(String msg) { JOptionPane.showMessageDialog(this, msg, "Sukses", JOptionPane.INFORMATION_MESSAGE); }
}