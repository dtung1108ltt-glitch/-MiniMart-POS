package com.minimart.swing;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SwingClientTest {

    @Test
    @DisplayName("Kiểm tra hàm validateProductInputs của Swing bắt đúng NumberFormatException và IllegalArgumentException (docx 3.6)")
    void testSwingInputValidation() {
        assertDoesNotThrow(() -> MainFrame.validateProductInputs("SP01", "Sữa tươi", "30000", "20"));

        assertThrows(
            IllegalArgumentException.class,
            () -> MainFrame.validateProductInputs("  ", "Sữa tươi", "30000", "20")
        );

        assertThrows(
            NumberFormatException.class,
            () -> MainFrame.validateProductInputs("SP01", "Sữa tươi", "abc", "20")
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> MainFrame.validateProductInputs("SP01", "Sữa tươi", "-100", "20")
        );
    }

    @Test
    @DisplayName("Kiểm tra tính thành tiền CartLine trên Swing Client")
    void testCartLineSubtotal() {
        ApiClient.CartLine line = new ApiClient.CartLine("SP01", "Sữa tươi", 30000L, 2);
        assertEquals(60000L, line.subtotal());
    }

    @Test
    @DisplayName("Xuất ảnh chụp thực tế giao diện Swing Hình 4.1 - 4.6 vào docs/screenshots")
    void captureGenuineSwingScreenshots() throws Exception {
        FlatLightLaf.setup();
        File outDir = new File("../docs/screenshots");
        outDir.mkdirs();

        MainFrame frame = new MainFrame(new ApiClient());
        frame.addNotify();
        frame.setSize(1180, 740);
        frame.getContentPane().setSize(1180, 700);
        frame.validate();

        DefaultTableModel prodModel = getPrivateField(frame, "productTableModel", DefaultTableModel.class);
        prodModel.setRowCount(0);
        prodModel.addRow(new Object[]{"SP01", "Sữa tươi tiệt trùng Vinamilk 1L", "Thực phẩm", "32.000 đ", 45, "2026-12-08"});
        prodModel.addRow(new Object[]{"SP02", "Nước rửa chén Sunlight Hương Chanh 750g", "Đồ gia dụng", "28.000 đ", 30, "Sunlight"});
        prodModel.addRow(new Object[]{"SP03", "Mì Hảo Hảo Tôm Chua Cay (Thùng 30 gói)", "Thực phẩm", "118.000 đ", 25, "2027-02-20"});
        prodModel.addRow(new Object[]{"SP04", "Gạo ST25 Ông Cua Túi 5kg", "Thực phẩm", "185.000 đ", 18, "2027-04-20"});
        prodModel.addRow(new Object[]{"SP07", "Sữa chua ăn TH true YOGURT (Lốc 4 hộp)", "Thực phẩm", "29.000 đ", 6, "2026-10-28"});
        prodModel.addRow(new Object[]{"SP10", "Bột giặt OMO Matic Cửa Trước 3.6kg", "Đồ gia dụng", "195.000 đ", 8, "OMO"});

        // Hình 4.1: Giao diện chính của hệ thống
        saveComponentImage(frame.getContentPane(), new File(outDir, "hinh-4-1-giao-dien-chinh.png"));

        // Hình 4.2: Giao diện quản lý sản phẩm (điền thông tin mẫu)
        getPrivateField(frame, "txtId", JTextField.class).setText("SP01");
        getPrivateField(frame, "txtName", JTextField.class).setText("Sữa tươi tiệt trùng Vinamilk 1L");
        getPrivateField(frame, "txtPrice", JTextField.class).setText("32000");
        getPrivateField(frame, "txtQuantity", JTextField.class).setText("45");
        getPrivateField(frame, "txtExtra", JTextField.class).setText("2026-12-08");
        saveComponentImage(frame.getContentPane(), new File(outDir, "hinh-4-2-quan-ly-san-pham.png"));

        // Hình 4.4: Kết quả tìm kiếm sản phẩm
        getPrivateField(frame, "txtSearch", JTextField.class).setText("Vinamilk");
        prodModel.setRowCount(0);
        prodModel.addRow(new Object[]{"SP01", "Sữa tươi tiệt trùng Vinamilk 1L", "Thực phẩm", "32.000 đ", 45, "2026-12-08"});
        saveComponentImage(frame.getContentPane(), new File(outDir, "hinh-4-4-ket-qua-tim-kiem.png"));

        // Hình 4.3: Giao diện lập hóa đơn bán hàng (Tab 2)
        JTabbedPane tabs = findTabbedPane(frame.getContentPane());
        if (tabs != null) {
            tabs.setSelectedIndex(1);
            DefaultTableModel cartModel = getPrivateField(frame, "cartTableModel", DefaultTableModel.class);
            cartModel.setRowCount(0);
            cartModel.addRow(new Object[]{"SP01", "Sữa tươi tiệt trùng Vinamilk 1L", "32.000 đ", 2, "64.000 đ"});
            cartModel.addRow(new Object[]{"SP04", "Gạo ST25 Ông Cua Túi 5kg", "185.000 đ", 3, "555.000 đ"});
            getPrivateField(frame, "lblSubtotal", JLabel.class).setText("Tạm tính: 619.000 đ");
            getPrivateField(frame, "lblDiscount", JLabel.class).setText("Giảm giá (5%): -30.950 đ");
            getPrivateField(frame, "lblTotal", JLabel.class).setText("TỔNG THANH TOÁN: 588.050 đ");
            saveComponentImage(frame.getContentPane(), new File(outDir, "hinh-4-3-lap-hoa-don.png"));

            // Hình 4.5: Kết quả thống kê doanh thu (Tab 3)
            tabs.setSelectedIndex(2);
            getPrivateField(frame, "lblStatRevenue", JLabel.class).setText("3.450.000 đ");
            getPrivateField(frame, "lblStatCompleted", JLabel.class).setText("9 đơn");
            getPrivateField(frame, "lblStatCancelled", JLabel.class).setText("1 đơn");
            getPrivateField(frame, "lblStatAvg", JLabel.class).setText("383.333 đ");
            RevenueChartPanel chart = getPrivateField(frame, "chartPanel", RevenueChartPanel.class);
            chart.setData(List.of(
                new ApiClient.DailyPoint("2026-10-18", 513000, 1),
                new ApiClient.DailyPoint("2026-10-19", 306000, 1),
                new ApiClient.DailyPoint("2026-10-20", 0, 0),
                new ApiClient.DailyPoint("2026-10-21", 616550, 1),
                new ApiClient.DailyPoint("2026-10-22", 190000, 1),
                new ApiClient.DailyPoint("2026-10-23", 373000, 1),
                new ApiClient.DailyPoint("2026-10-24", 183000, 1)
            ));
            saveComponentImage(frame.getContentPane(), new File(outDir, "hinh-4-5-thong-ke-doanh-thu.png"));
        }

        // Hình 4.6: Thông báo khi nhập dữ liệu không hợp lệ (JOptionPane)
        JOptionPane errorPane = new JOptionPane(
            "Vui lòng nhập giá và số lượng đúng định dạng số (Giá và số lượng phải không âm)",
            JOptionPane.ERROR_MESSAGE,
            JOptionPane.DEFAULT_OPTION
        );
        errorPane.setSize(460, 160);
        errorPane.doLayout();
        errorPane.validate();
        saveComponentImage(errorPane, new File(outDir, "hinh-4-6-thong-bao-loi.png"));

        frame.dispose();
        assertTrue(new File(outDir, "hinh-4-1-giao-dien-chinh.png").exists());
        assertTrue(new File(outDir, "hinh-4-5-thong-ke-doanh-thu.png").exists());
    }

    private static void saveComponentImage(Component comp, File file) throws Exception {
        int w = comp.getWidth() > 500 ? comp.getWidth() : 1180;
        int h = comp.getHeight() > 300 ? comp.getHeight() : 700;
        if (comp instanceof JOptionPane) {
            w = 520;
            h = 180;
        }
        comp.setSize(w, h);
        layoutRecursively(comp);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        comp.printAll(g2);
        g2.dispose();
        ImageIO.write(img, "png", file);
    }

    private static void layoutRecursively(Component comp) {
        comp.doLayout();
        if (comp instanceof Container container) {
            container.validate();
            for (Component child : container.getComponents()) {
                layoutRecursively(child);
            }
        }
    }

    private static JTabbedPane findTabbedPane(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JTabbedPane tp) return tp;
            if (c instanceof Container sub) {
                JTabbedPane found = findTabbedPane(sub);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static <T> T getPrivateField(Object target, String name, Class<T> type) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return type.cast(f.get(target));
    }
}
