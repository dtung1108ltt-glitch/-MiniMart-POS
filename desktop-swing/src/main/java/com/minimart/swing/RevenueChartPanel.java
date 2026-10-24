package com.minimart.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import javax.swing.JPanel;

/**
 * Biểu đồ cột doanh thu 7 ngày tự vẽ bằng Graphics2D (đáp ứng yêu cầu mục 4 của prompt).
 */
public class RevenueChartPanel extends JPanel {

    private static final Color PRIMARY_BLUE = new Color(0x25, 0x63, 0xEB);
    private static final Color SURFACE_WHITE = Color.WHITE;
    private static final Color TEXT_MAIN = new Color(0x0F, 0x17, 0x2A);
    private static final Color TEXT_MUTED = new Color(0x64, 0x74, 0x8B);
    private static final Color GRID_LINE = new Color(0xE2, 0xE8, 0xF0);

    private List<ApiClient.DailyPoint> data = List.of();
    private final NumberFormat vndFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

    public RevenueChartPanel() {
        setBackground(SURFACE_WHITE);
        setPreferredSize(new Dimension(700, 300));
    }

    public void setData(List<ApiClient.DailyPoint> points) {
        this.data = points != null ? points : List.of();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int padLeft = 70;
        int padRight = 30;
        int padTop = 45;
        int padBottom = 45;

        g2.setColor(TEXT_MAIN);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2.drawString("Biểu đồ doanh thu 7 ngày gần nhất (Vẽ bằng Java Graphics2D)", 20, 24);

        if (data.isEmpty()) {
            g2.setColor(TEXT_MUTED);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            g2.drawString("Chưa có dữ liệu doanh thu để hiển thị biểu đồ.", padLeft, h / 2);
            g2.dispose();
            return;
        }

        long maxRev = 1;
        for (ApiClient.DailyPoint pt : data) {
            if (pt.revenue() > maxRev) {
                maxRev = pt.revenue();
            }
        }

        int chartW = Math.max(100, w - padLeft - padRight);
        int chartH = Math.max(80, h - padTop - padBottom);

        // Vẽ lưới ngang
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        for (int i = 0; i <= 4; i++) {
            int y = padTop + chartH - (i * chartH / 4);
            g2.setColor(GRID_LINE);
            g2.drawLine(padLeft, y, padLeft + chartW, y);
        }

        int count = data.size();
        int slotW = chartW / count;
        int barW = Math.min(48, Math.max(18, slotW - 24));

        for (int i = 0; i < count; i++) {
            ApiClient.DailyPoint pt = data.get(i);
            int barH = (int) Math.round((pt.revenue() * 1.0 / maxRev) * (chartH - 15));
            if (pt.revenue() > 0 && barH < 6) {
                barH = 6;
            }
            int x = padLeft + i * slotW + (slotW - barW) / 2;
            int y = padTop + chartH - barH;

            g2.setColor(PRIMARY_BLUE);
            g2.fillRoundRect(x, y, barW, barH, 8, 8);

            // Giá trị trên cột
            String valStr = vndFormat.format(pt.revenue()) + " đ";
            FontMetrics fm = g2.getFontMetrics();
            int valX = x + (barW - fm.stringWidth(valStr)) / 2;
            g2.setColor(TEXT_MAIN);
            g2.drawString(valStr, valX, Math.max(padTop + 10, y - 6));

            // Nhãn ngày dưới trục X
            String dateLabel = pt.date().length() >= 10 ? pt.date().substring(5) : pt.date();
            int lblX = x + (barW - fm.stringWidth(dateLabel)) / 2;
            g2.setColor(TEXT_MUTED);
            g2.drawString(dateLabel, lblX, padTop + chartH + 20);
        }

        g2.dispose();
    }
}
