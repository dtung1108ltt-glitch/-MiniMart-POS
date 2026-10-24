package com.minimart.swing;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

/**
 * Lớp giao diện chính MainFrame xây dựng bằng Java Swing (khớp mục 2.4.1, 2.6, 4.2, 4.3 của docx).
 * Tất cả lời gọi mạng đều chạy qua SwingWorker để không chặn luồng giao diện (EDT).
 * Tất cả sự kiện nút bấm đều dùng biểu thức Lambda (addActionListener(e -> ...)).
 */
public class MainFrame extends JFrame {

    private static final Color PRIMARY_COLOR = new Color(0x25, 0x63, 0xEB);
    private static final Color BG_COLOR = new Color(0xF4, 0xF6, 0xFA);
    private static final Color SURFACE_COLOR = Color.WHITE;

    private final ApiClient apiClient;
    private final NumberFormat vndFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

    // Thành phần Tab 1: Quản lý sản phẩm
    private final JTextField txtId = new JTextField(10);
    private final JTextField txtName = new JTextField(18);
    private final JTextField txtPrice = new JTextField(10);
    private final JTextField txtQuantity = new JTextField(8);
    private final JComboBox<String> cbCategory = new JComboBox<>(new String[]{"FOOD - Thực phẩm", "HOUSEHOLD - Đồ gia dụng"});
    private final JTextField txtExtra = new JTextField(12);
    private final JTextField txtSearch = new JTextField(16);
    private final DefaultTableModel productTableModel;
    private final JTable productTable;

    // Thành phần Tab 2: Bán hàng (POS)
    private final JTextField txtPosProductId = new JTextField(10);
    private final JTextField txtPosQty = new JTextField("1", 5);
    private final JComboBox<String> cbDiscountPolicy = new JComboBox<>(new String[]{
        "NONE",
        "PERCENT5_OVER_500K",
        "PERCENT10_OVER_1M",
        "FIXED_20K"
    });
    private final DefaultTableModel cartTableModel;
    private final JLabel lblSubtotal = new JLabel("Tạm tính: 0 đ");
    private final JLabel lblDiscount = new JLabel("Giảm giá: 0 đ");
    private final JLabel lblTotal = new JLabel("TỔNG THANH TOÁN: 0 đ");
    private final List<ApiClient.CartLine> cartLines = new ArrayList<>();
    private List<ApiClient.ProductItem> cachedProducts = new ArrayList<>();

    // Thành phần Tab 3: Thống kê doanh thu
    private final JLabel lblStatRevenue = new JLabel("0 đ");
    private final JLabel lblStatCompleted = new JLabel("0 đơn");
    private final JLabel lblStatCancelled = new JLabel("0 đơn");
    private final JLabel lblStatAvg = new JLabel("0 đ");
    private final RevenueChartPanel chartPanel = new RevenueChartPanel();

    public MainFrame(ApiClient apiClient) {
        this.apiClient = apiClient;
        setTitle("MiniMart POS — Hệ thống Quản lý Bán hàng & Siêu thị Mini (Java Swing)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 740);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_COLOR);

        // Khởi tạo model bảng sản phẩm
        productTableModel = new DefaultTableModel(
            new Object[]{"Mã SP", "Tên sản phẩm", "Loại", "Đơn giá (VND)", "Tồn kho", "Hạn dùng / Hãng"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        productTable = new JTable(productTableModel);
        styleTable(productTable);

        // Khởi tạo model bảng giỏ hàng
        cartTableModel = new DefaultTableModel(
            new Object[]{"Mã SP", "Tên sản phẩm", "Đơn giá", "Số lượng", "Thành tiền"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        initLayout();
    }

    private void initLayout() {
        setLayout(new BorderLayout(0, 0));

        // Thanh tiêu đề trên cùng
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0x0F, 0x17, 0x2A));
        header.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        JLabel title = new JLabel("MINIMART POS — QUẢN LÝ BÁN HÀNG & SIÊU THỊ MINI");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        JLabel subtitle = new JLabel("Kết nối REST API: " + apiClient.getBaseUrl());
        subtitle.setForeground(new Color(0xCB, 0xD5, 0xE1));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        header.add(title, BorderLayout.WEST);
        header.add(subtitle, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // JTabbedPane 3 màn hình chính
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.addTab("1. Quản lý Sản phẩm", buildProductsTab());
        tabs.addTab("2. Bán hàng (POS)", buildPosTab());
        tabs.addTab("3. Thống kê Doanh thu", buildStatsTab());

        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 2) {
                loadStatistics();
            }
        });

        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildProductsTab() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(BG_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        // Form nhập sản phẩm
        JPanel formCard = new JPanel(new GridLayout(2, 6, 10, 8));
        formCard.setBackground(SURFACE_COLOR);
        formCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xE2, 0xE8, 0xF0)),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        formCard.add(createFieldPanel("Mã sản phẩm *", txtId));
        formCard.add(createFieldPanel("Tên sản phẩm *", txtName));
        formCard.add(createFieldPanel("Đơn giá (VND) *", txtPrice));
        formCard.add(createFieldPanel("Số lượng tồn *", txtQuantity));
        formCard.add(createFieldPanel("Loại sản phẩm *", cbCategory));
        formCard.add(createFieldPanel("HSD (yyyy-MM-dd) / Hãng", txtExtra));

        // Hàng nút chức năng
        JButton btnAdd = createPrimaryButton("Thêm sản phẩm");
        JButton btnUpdate = createSecondaryButton("Sửa sản phẩm");
        JButton btnDelete = createDangerButton("Xóa sản phẩm");
        JButton btnRefresh = createSecondaryButton("Làm mới");
        JButton btnExternalApi = createSecondaryButton("Tham khảo API ngoài");

        formCard.add(btnAdd);
        formCard.add(btnUpdate);
        formCard.add(btnDelete);
        formCard.add(btnRefresh);
        formCard.add(createFieldPanel("Tìm kiếm mã/tên", txtSearch));
        formCard.add(btnExternalApi);

        panel.add(formCard, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(productTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(0xE2, 0xE8, 0xF0)));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Chọn dòng trên bảng -> đổ dữ liệu lên ô nhập
        productTable.getSelectionModel().addListSelectionListener(e -> {
            int row = productTable.getSelectedRow();
            if (!e.getValueIsAdjusting() && row >= 0 && row < cachedProducts.size()) {
                ApiClient.ProductItem item = cachedProducts.get(row);
                txtId.setText(item.productId());
                txtName.setText(item.name());
                txtPrice.setText(String.valueOf(item.price()));
                txtQuantity.setText(String.valueOf(item.quantity()));
                cbCategory.setSelectedIndex("FOOD".equalsIgnoreCase(item.category()) ? 0 : 1);
                txtExtra.setText("FOOD".equalsIgnoreCase(item.category()) ? item.expiryDate() : item.brand());
            }
        });

        // Gắn sự kiện bằng Lambda Expression + kiểm tra ngoại lệ đúng mục 3.6 & 4.3 của docx
        btnAdd.addActionListener(e -> handleAddOrUpdateProduct(true));
        btnUpdate.addActionListener(e -> handleAddOrUpdateProduct(false));
        btnDelete.addActionListener(e -> handleDeleteSelectedProduct());
        btnRefresh.addActionListener(e -> loadProducts(txtSearch.getText()));
        txtSearch.addActionListener(e -> loadProducts(txtSearch.getText()));
        btnExternalApi.addActionListener(e -> handleCallExternalApi());

        return panel;
    }

    private JPanel buildPosTab() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(BG_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        topBar.setBackground(SURFACE_COLOR);
        topBar.setBorder(BorderFactory.createLineBorder(new Color(0xE2, 0xE8, 0xF0)));

        JButton btnAddToCart = createPrimaryButton("Thêm vào giỏ");
        JButton btnClearCart = createSecondaryButton("Xóa giỏ hàng");

        topBar.add(new JLabel("Mã sản phẩm:"));
        topBar.add(txtPosProductId);
        topBar.add(new JLabel("Số lượng:"));
        topBar.add(txtPosQty);
        topBar.add(btnAddToCart);
        topBar.add(btnClearCart);
        topBar.add(new JLabel("Chính sách giảm giá:"));
        topBar.add(cbDiscountPolicy);

        panel.add(topBar, BorderLayout.NORTH);

        JTable cartTable = new JTable(cartTableModel);
        styleTable(cartTable);
        panel.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new BorderLayout(16, 8));
        bottomBar.setBackground(SURFACE_COLOR);
        bottomBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xE2, 0xE8, 0xF0)),
            BorderFactory.createEmptyBorder(14, 18, 14, 18)
        ));

        JPanel totalsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 0));
        totalsPanel.setOpaque(false);
        lblSubtotal.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblDiscount.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblDiscount.setForeground(new Color(0x16, 0xA3, 0x4A));
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTotal.setForeground(PRIMARY_COLOR);
        totalsPanel.add(lblSubtotal);
        totalsPanel.add(lblDiscount);
        totalsPanel.add(lblTotal);

        JButton btnCheckout = createPrimaryButton("THANH TOÁN & LẬP HÓA ĐƠN");
        btnCheckout.setPreferredSize(new Dimension(240, 44));

        bottomBar.add(totalsPanel, BorderLayout.WEST);
        bottomBar.add(btnCheckout, BorderLayout.EAST);
        panel.add(bottomBar, BorderLayout.SOUTH);

        // Lambda listeners cho POS
        btnAddToCart.addActionListener(e -> handleAddCartItem());
        cbDiscountPolicy.addActionListener(e -> refreshCartPreview());
        btnClearCart.addActionListener(e -> {
            cartLines.clear();
            refreshCartTableAndPreview();
        });
        btnCheckout.addActionListener(e -> handleCheckout());

        return panel;
    }

    private JPanel buildStatsTab() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(BG_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 12));
        cards.setOpaque(false);
        cards.add(createMetricCard("Tổng doanh thu (COMPLETED)", lblStatRevenue));
        cards.add(createMetricCard("Số hóa đơn hoàn tất", lblStatCompleted));
        cards.add(createMetricCard("Hóa đơn đã hủy (Loại trừ)", lblStatCancelled));
        cards.add(createMetricCard("Trung bình / hóa đơn", lblStatAvg));

        panel.add(cards, BorderLayout.NORTH);

        chartPanel.setBorder(BorderFactory.createLineBorder(new Color(0xE2, 0xE8, 0xF0)));
        panel.add(chartPanel, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setOpaque(false);
        JButton btnReloadStats = createPrimaryButton("Làm mới thống kê");
        btnReloadStats.addActionListener(e -> loadStatistics());
        footer.add(btnReloadStats);
        panel.add(footer, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Kiểm tra dữ liệu nhập từ giao diện bằng try/catch NumberFormatException và IllegalArgumentException
     * đúng theo mẫu mã nguồn mục 3.6 và 4.3 trong báo cáo docx.
     */
    public static void validateProductInputs(String id, String name, String priceText, String qtyText) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã sản phẩm không được để trống");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống");
        }
        double price = Double.parseDouble(priceText.trim());
        int quantity = Integer.parseInt(qtyText.trim());
        if (price < 0 || quantity < 0) {
            throw new IllegalArgumentException("Giá và số lượng phải không âm");
        }
    }

    private void handleAddOrUpdateProduct(boolean isCreate) {
        try {
            String id = txtId.getText().trim();
            String name = txtName.getText().trim();
            validateProductInputs(id, name, txtPrice.getText(), txtQuantity.getText());

            long price = Math.round(Double.parseDouble(txtPrice.getText().trim()));
            int quantity = Integer.parseInt(txtQuantity.getText().trim());
            String category = cbCategory.getSelectedIndex() == 0 ? "FOOD" : "HOUSEHOLD";
            String extra = txtExtra.getText().trim();

            new SwingWorker<ApiClient.ProductItem, Void>() {
                @Override
                protected ApiClient.ProductItem doInBackground() throws Exception {
                    return isCreate
                        ? apiClient.createProduct(id, name, price, quantity, category, extra)
                        : apiClient.updateProduct(id, name, price, quantity, category, extra);
                }

                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(
                            MainFrame.this,
                            isCreate ? "Thêm sản phẩm thành công" : "Cập nhật sản phẩm thành công",
                            "Thành công",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                        loadProducts(txtSearch.getText());
                    } catch (Exception ex) {
                        showErrorDialog(ex);
                    }
                }
            }.execute();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Vui lòng nhập giá và số lượng đúng định dạng số",
                "Dữ liệu không hợp lệ",
                JOptionPane.ERROR_MESSAGE
            );
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(
                this,
                ex.getMessage(),
                "Dữ liệu không hợp lệ",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void handleDeleteSelectedProduct() {
        String id = txtId.getText().trim();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một sản phẩm để xóa");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Bạn có chắc muốn xóa sản phẩm '" + id + "'?",
            "Xác nhận xóa",
            JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                apiClient.deleteProduct(id);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(MainFrame.this, "Đã xóa sản phẩm " + id);
                    loadProducts(txtSearch.getText());
                } catch (Exception ex) {
                    showErrorDialog(ex);
                }
            }
        }.execute();
    }

    public void loadProducts(String query) {
        new SwingWorker<List<ApiClient.ProductItem>, Void>() {
            @Override
            protected List<ApiClient.ProductItem> doInBackground() throws Exception {
                return apiClient.fetchProducts(query);
            }

            @Override
            protected void done() {
                try {
                    cachedProducts = get();
                    productTableModel.setRowCount(0);
                    for (ApiClient.ProductItem p : cachedProducts) {
                        String extra = "FOOD".equalsIgnoreCase(p.category()) ? p.expiryDate() : p.brand();
                        productTableModel.addRow(new Object[]{
                            p.productId(),
                            p.name(),
                            p.categoryName(),
                            vndFormat.format(p.price()) + " đ",
                            p.quantity(),
                            extra
                        });
                    }
                } catch (Exception ex) {
                    showErrorDialog(ex);
                }
            }
        }.execute();
    }

    private void handleAddCartItem() {
        try {
            String pid = txtPosProductId.getText().trim();
            int qty = Integer.parseInt(txtPosQty.getText().trim());
            if (pid.isEmpty() || qty <= 0) {
                throw new IllegalArgumentException("Mã sản phẩm không được trống và số lượng phải > 0");
            }
            ApiClient.ProductItem found = cachedProducts.stream()
                .filter(p -> p.productId().equalsIgnoreCase(pid))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã sản phẩm: " + pid));

            cartLines.add(new ApiClient.CartLine(found.productId(), found.name(), found.price(), qty));
            refreshCartTableAndPreview();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Số lượng bán phải là số nguyên hợp lệ");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }

    private void refreshCartTableAndPreview() {
        cartTableModel.setRowCount(0);
        for (ApiClient.CartLine line : cartLines) {
            cartTableModel.addRow(new Object[]{
                line.productId(),
                line.name(),
                vndFormat.format(line.unitPrice()) + " đ",
                line.quantity(),
                vndFormat.format(line.subtotal()) + " đ"
            });
        }
        refreshCartPreview();
    }

    private void refreshCartPreview() {
        if (cartLines.isEmpty()) {
            lblSubtotal.setText("Tạm tính: 0 đ");
            lblDiscount.setText("Giảm giá: 0 đ");
            lblTotal.setText("TỔNG THANH TOÁN: 0 đ");
            return;
        }
        String policy = (String) cbDiscountPolicy.getSelectedItem();
        new SwingWorker<ApiClient.PreviewResult, Void>() {
            @Override
            protected ApiClient.PreviewResult doInBackground() throws Exception {
                return apiClient.previewSale(cartLines, policy);
            }

            @Override
            protected void done() {
                try {
                    ApiClient.PreviewResult res = get();
                    lblSubtotal.setText("Tạm tính: " + vndFormat.format(res.subtotal()) + " đ");
                    lblDiscount.setText("Giảm giá: -" + vndFormat.format(res.discount()) + " đ");
                    lblTotal.setText("TỔNG THANH TOÁN: " + vndFormat.format(res.totalAmount()) + " đ");
                } catch (Exception ex) {
                    showErrorDialog(ex);
                }
            }
        }.execute();
    }

    private void handleCheckout() {
        if (cartLines.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Giỏ hàng trống, không thể thanh toán");
            return;
        }
        String policy = (String) cbDiscountPolicy.getSelectedItem();
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return apiClient.checkout(cartLines, policy, null);
            }

            @Override
            protected void done() {
                try {
                    String invoiceId = get();
                    JOptionPane.showMessageDialog(
                        MainFrame.this,
                        "Lập hóa đơn thành công! Mã hóa đơn: " + invoiceId,
                        "Thanh toán hoàn tất",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                    cartLines.clear();
                    refreshCartTableAndPreview();
                    loadProducts("");
                } catch (Exception ex) {
                    showErrorDialog(ex);
                }
            }
        }.execute();
    }

    private void loadStatistics() {
        new SwingWorker<ApiClient.StatsOverview, Void>() {
            @Override
            protected ApiClient.StatsOverview doInBackground() throws Exception {
                return apiClient.fetchStatsOverview();
            }

            @Override
            protected void done() {
                try {
                    ApiClient.StatsOverview stats = get();
                    lblStatRevenue.setText(vndFormat.format(stats.totalRevenue()) + " đ");
                    lblStatCompleted.setText(stats.completedInvoices() + " đơn");
                    lblStatCancelled.setText(stats.cancelledInvoices() + " đơn");
                    lblStatAvg.setText(vndFormat.format(stats.averageOrderValue()) + " đ");
                    chartPanel.setData(stats.dailyPoints());
                } catch (Exception ex) {
                    showErrorDialog(ex);
                }
            }
        }.execute();
    }

    private void handleCallExternalApi() {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return apiClient.fetchExternalSampleSummary();
            }

            @Override
            protected void done() {
                try {
                    String summary = get();
                    JOptionPane.showMessageDialog(
                        MainFrame.this,
                        summary,
                        "Dữ liệu sản phẩm từ API ngoài",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                } catch (Exception ex) {
                    showErrorDialog(ex);
                }
            }
        }.execute();
    }

    private void showErrorDialog(Exception ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        JOptionPane.showMessageDialog(
            this,
            cause.getMessage(),
            "Thông báo lỗi",
            JOptionPane.ERROR_MESSAGE
        );
    }

    private void styleTable(JTable table) {
        table.setRowHeight(40);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setPreferredSize(new Dimension(100, 38));
    }

    private JPanel createFieldPanel(String labelText, javax.swing.JComponent field) {
        JPanel p = new JPanel(new BorderLayout(4, 3));
        p.setOpaque(false);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        p.add(lbl, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JPanel createMetricCard(String title, JLabel valueLabel) {
        JPanel card = new JPanel(new BorderLayout(6, 6));
        card.setBackground(SURFACE_COLOR);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xE2, 0xE8, 0xF0)),
            BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(new Color(0x64, 0x74, 0x8B));
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valueLabel.setForeground(PRIMARY_COLOR);
        card.add(lblTitle, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JButton createPrimaryButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(PRIMARY_COLOR);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return b;
    }

    private JButton createSecondaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return b;
    }

    private JButton createDangerButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(new Color(0xDC, 0x26, 0x26));
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return b;
    }

    public static void main(String[] args) {
        try {
            FlatLightLaf.setup();
        } catch (Exception ex) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
        }
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(new ApiClient());
            frame.setVisible(true);
            frame.loadProducts("");
        });
    }
}
