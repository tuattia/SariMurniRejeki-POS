package modern_pos.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import modern_pos.controller.UserController;
import modern_pos.model.User;
import modern_pos.utils.Akses;
import modern_pos.utils.Session;
import modern_pos.utils.SwingHelper;

public class UserDialog extends JDialog {
    private final UserController controller;
    private final DefaultTableModel model;
    private final JTable tabel;
    private List<User> daftar;

    public UserDialog(Frame parent, UserController controller) {
        super(parent, "Kelola User", true);
        this.controller = controller;

        model = new DefaultTableModel(new String[]{"Nama", "Username", "Hak Akses"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tabel = new JTable(model);
        SwingHelper.styleTable(tabel);

        JButton btnTambah = new JButton("Tambah");
        JButton btnEdit = new JButton("Edit");
        JButton btnReset = new JButton("Reset Password");
        JButton btnHapus = new JButton("Hapus");
        JButton btnTutup = new JButton("Tutup");
        btnTambah.addActionListener(e -> formTambah());
        btnEdit.addActionListener(e -> { User u = dipilih(); if (u != null) formEdit(u); });
        btnReset.addActionListener(e -> { User u = dipilih(); if (u != null) formReset(u); });
        btnHapus.addActionListener(e -> {
            User u = dipilih();
            if (u == null) return;
            int ok = JOptionPane.showConfirmDialog(this, "Hapus user " + u.getNama() + "?", "Hapus", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) controller.hapus(u.getId(), idPelaku());
        });
        btnTutup.addActionListener(e -> dispose());

        JPanel tombol = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        tombol.add(btnTambah);
        tombol.add(btnEdit);
        tombol.add(btnReset);
        tombol.add(btnHapus);
        tombol.add(btnTutup);

        setLayout(new BorderLayout());
        add(new JScrollPane(tabel), BorderLayout.CENTER);
        add(tombol, BorderLayout.SOUTH);
        setSize(640, 420);
        setLocationRelativeTo(parent);
        controller.setView(this);
    }

    private static int idPelaku() {
        return Session.currentUser != null ? Session.currentUser.getId() : -1;
    }

    private User dipilih() {
        int row = tabel.getSelectedRow();
        if (row == -1) { showError("Pilih user terlebih dahulu!"); return null; }
        return daftar.get(row);
    }

    private static JComboBox<String> comboRole(String pilih) {
        JComboBox<String> c = new JComboBox<>(new String[]{Akses.MEMBER, Akses.ADMIN});
        if (pilih != null) c.setSelectedItem(pilih.trim().toLowerCase());
        return c;
    }

    private void formTambah() {
        JTextField nama = new JTextField();
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        JComboBox<String> role = comboRole(null);
        JPanel p = new JPanel(new GridLayout(4, 2, 8, 8));
        p.add(new JLabel("Nama:")); p.add(nama);
        p.add(new JLabel("Username:")); p.add(username);
        p.add(new JLabel("Password (min 6):")); p.add(password);
        p.add(new JLabel("Hak Akses:")); p.add(role);
        if (JOptionPane.showConfirmDialog(this, p, "Tambah User", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            controller.tambah(nama.getText(), username.getText(), new String(password.getPassword()), (String) role.getSelectedItem());
        }
    }

    private void formEdit(User u) {
        JTextField nama = new JTextField(u.getNama());
        JComboBox<String> role = comboRole(u.getHakAkses());
        JPanel p = new JPanel(new GridLayout(3, 2, 8, 8));
        p.add(new JLabel("Username:")); p.add(new JLabel(u.getUsername()));
        p.add(new JLabel("Nama:")); p.add(nama);
        p.add(new JLabel("Hak Akses:")); p.add(role);
        if (JOptionPane.showConfirmDialog(this, p, "Edit User", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            controller.ubah(u.getId(), nama.getText(), (String) role.getSelectedItem(), idPelaku());
        }
    }

    private void formReset(User u) {
        JPasswordField baru = new JPasswordField();
        JPasswordField ulang = new JPasswordField();
        JPanel p = new JPanel(new GridLayout(2, 2, 8, 8));
        p.add(new JLabel("Password baru (min 6):")); p.add(baru);
        p.add(new JLabel("Ulangi password:")); p.add(ulang);
        if (JOptionPane.showConfirmDialog(this, p, "Reset Password " + u.getUsername(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        String a = new String(baru.getPassword()), b = new String(ulang.getPassword());
        if (!a.equals(b)) { showError("Password dan ulangan tidak sama!"); return; }
        controller.resetPassword(u.getId(), a);
    }

    public void tampilkan(List<User> list) {
        this.daftar = list;
        model.setRowCount(0);
        for (User u : list) model.addRow(new Object[]{u.getNama(), u.getUsername(), u.getHakAkses()});
    }

    public void showError(String msg) { JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE); }
    public void showSuccess(String msg) { JOptionPane.showMessageDialog(this, msg, "Sukses", JOptionPane.INFORMATION_MESSAGE); }
}
