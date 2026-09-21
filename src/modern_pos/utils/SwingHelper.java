package modern_pos.utils;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class SwingHelper {

    // Komponen Tombol Kustom dengan Sudut Melengkung (Rounded Button) & Anti-Aliasing
    public static class RoundedButton extends JButton {
        private int cornerRadius = 12;
        private Color currentBg;
        private Color normalBg;
        private Color hoverBg;

        public RoundedButton(String text, Color bg, Color bgHover, int radius) {
            super(text);
            this.normalBg = bg;
            this.hoverBg = bgHover;
            this.currentBg = bg;
            this.cornerRadius = radius;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setForeground(Color.WHITE);
            setFont(UITheme.FONT_HEADING);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override 
                public void mouseEntered(MouseEvent e) { 
                    currentBg = hoverBg; 
                    repaint(); 
                }
                @Override 
                public void mouseExited(MouseEvent e) { 
                    currentBg = normalBg; 
                    repaint(); 
                }
                @Override 
                public void mousePressed(MouseEvent e) { 
                    currentBg = hoverBg.darker(); 
                    repaint(); 
                }
                @Override 
                public void mouseReleased(MouseEvent e) { 
                    currentBg = hoverBg; 
                    repaint(); 
                }
            });
        }

        @Override
        public void setBackground(Color bg) {
            super.setBackground(bg);
            this.normalBg = bg;
            this.currentBg = bg;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (!isEnabled()) {
                g2.setColor(new Color(200, 200, 200));
            } else {
                g2.setColor(currentBg);
            }
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static JButton createFlatButton(String text, Color bg, Color bgHover) {
        return createFlatButton(text, bg, bgHover, 12);
    }

    public static JButton createFlatButton(String text, Color bg, Color bgHover, int radius) {
        JButton btn = new RoundedButton(text, bg, bgHover, radius);
        btn.setPreferredSize(new Dimension(130, 35));
        return btn;
    }
    
    public static JButton createSidebarButton(String text) {
        JButton btn = new JButton(text); // Teks aslinya saja
        btn.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
        btn.setBackground(UITheme.COLOR_PRIMARY_DARK);
        btn.setForeground(new Color(200, 200, 200)); // Off-white untuk yang tidak aktif
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        // Menambahkan padding yang elegan (Atas: 12, Kiri: 25, Bawah: 12, Kanan: 10)
        btn.setBorder(BorderFactory.createEmptyBorder(12, 25, 12, 10));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { 
                btn.setBackground(UITheme.COLOR_PRIMARY); 
                btn.setForeground(Color.WHITE);
            }
            @Override public void mouseExited(MouseEvent e) { 
                Boolean isActive = (Boolean) btn.getClientProperty("active_menu");
                if (isActive == null || !isActive) {
                    btn.setBackground(UITheme.COLOR_PRIMARY_DARK); 
                    btn.setForeground(new Color(200, 200, 200));
                }
            }
        });
        return btn;
    }

    public static JTextField createMaterialTextField() {
        JTextField txt = new JTextField();
        txt.setFont(UITheme.FONT_BODY);
        txt.setForeground(UITheme.COLOR_TEXT_PRIMARY);
        txt.setBackground(UITheme.COLOR_BG_CARD);
        txt.setCaretColor(UITheme.COLOR_PRIMARY);
        txt.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, UITheme.COLOR_PRIMARY),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        txt.setPreferredSize(new Dimension(200, 30));
        return txt;
    }

    public static JPasswordField createMaterialPasswordField() {
        JPasswordField txt = new JPasswordField();
        txt.setFont(UITheme.FONT_BODY);
        txt.setForeground(UITheme.COLOR_TEXT_PRIMARY);
        txt.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, UITheme.COLOR_PRIMARY),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        txt.setPreferredSize(new Dimension(200, 30));
        return txt;
    }

    public static JLabel createLabel(String text, java.awt.Font font, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(font);
        lbl.setForeground(color);
        return lbl;
    }
    
    public static void styleTable(JTable table) {
        table.getTableHeader().setFont(UITheme.FONT_HEADING);
        table.getTableHeader().setBackground(UITheme.COLOR_PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setPreferredSize(new Dimension(0, 35));
        table.setRowHeight(30);
        table.setFont(UITheme.FONT_BODY);
        table.setSelectionBackground(new Color(225, 240, 255));
        table.setSelectionForeground(UITheme.COLOR_TEXT_PRIMARY);
        table.setGridColor(new Color(230, 230, 230));
        table.setShowVerticalLines(false); 
    }

    // Membuat KPI Card bergaya Modern
    public static JPanel createSummaryCard(String title, String valueId, Color accentColor) {
        JPanel card = new JPanel(new GridLayout(2, 1, 0, 5));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 6, 1, 1, accentColor), // Garis tebal di kiri
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(UITheme.FONT_BODY);
        lblTitle.setForeground(UITheme.COLOR_TEXT_SECONDARY);
        
        JLabel lblValue = new JLabel("0");
        lblValue.setName(valueId); // Nama komponen untuk dicari nanti oleh Controller
        lblValue.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 22));
        lblValue.setForeground(UITheme.COLOR_TEXT_PRIMARY);
        
        card.add(lblTitle);
        card.add(lblValue);
        return card;
    }
}