package com.minimart.config;

import com.minimart.domain.DiscountPolicies;
import com.minimart.domain.FoodProduct;
import com.minimart.domain.HouseholdProduct;
import com.minimart.domain.Invoice;
import com.minimart.domain.InvoiceStatus;
import com.minimart.domain.Product;
import com.minimart.repository.InvoiceRepository;
import com.minimart.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khởi tạo dữ liệu mẫu Việt Nam khi cơ sở dữ liệu trống (≥ 14 sản phẩm & ≥ 10 hóa đơn rải 14 ngày).
 */
@Component
public class SeedData implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final InvoiceRepository invoiceRepository;
    private final boolean seedEnabled;

    public SeedData(
            ProductRepository productRepository,
            InvoiceRepository invoiceRepository,
            @Value("${app.seed-enabled:true}") boolean seedEnabled) {
        this.productRepository = productRepository;
        this.invoiceRepository = invoiceRepository;
        this.seedEnabled = seedEnabled;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled || productRepository.count() > 0) {
            return;
        }

        LocalDate today = LocalDate.now();

        Product sp01 = new FoodProduct("SP01", "Sữa tươi tiệt trùng Vinamilk 1L", new BigDecimal("32000"), 45, today.plusDays(45));
        Product sp02 = new HouseholdProduct("SP02", "Nước rửa chén Sunlight Hương Chanh 750g", new BigDecimal("28000"), 30, "Sunlight");
        Product sp03 = new FoodProduct("SP03", "Mì Hảo Hảo Tôm Chua Cay (Thùng 30 gói)", new BigDecimal("118000"), 25, today.plusDays(120));
        Product sp04 = new FoodProduct("SP04", "Gạo ST25 Ông Cua Túi 5kg", new BigDecimal("185000"), 18, today.plusDays(180));
        Product sp05 = new FoodProduct("SP05", "Dầu ăn thực vật Neptune Light 2L", new BigDecimal("112000"), 22, today.plusDays(240));
        Product sp06 = new FoodProduct("SP06", "Nước mắm Nam Ngư Phú Quốc 750ml", new BigDecimal("42000"), 35, today.plusDays(300));
        Product sp07 = new FoodProduct("SP07", "Sữa chua ăn TH true YOGURT (Lốc 4 hộp)", new BigDecimal("29000"), 6, today.plusDays(4)); // Tồn thấp + sắp hết hạn
        Product sp08 = new FoodProduct("SP08", "Bánh mì tươi Kinh Đô nhân bơ sữa", new BigDecimal("15000"), 5, today.plusDays(2)); // Tồn thấp + sắp hết hạn
        Product sp09 = new FoodProduct("SP09", "Cà phê hòa tan G7 3in1 (Hộp 21 gói)", new BigDecimal("65000"), 40, today.plusDays(365));
        Product sp10 = new HouseholdProduct("SP10", "Bột giặt OMO Matic Cửa Trước 3.6kg", new BigDecimal("195000"), 8, "OMO"); // Tồn thấp
        Product sp11 = new HouseholdProduct("SP11", "Khăn giấy lụa Pulppy 2 lớp (Lốc 3 hộp)", new BigDecimal("54000"), 28, "Pulppy");
        Product sp12 = new HouseholdProduct("SP12", "Nước lau sàn Gift Hương Hoa Ly 1kg", new BigDecimal("34000"), 7, "Gift"); // Tồn thấp
        Product sp13 = new HouseholdProduct("SP13", "Màng bọc thực phẩm Ringo 30cm x 50m", new BigDecimal("45000"), 19, "Ringo");
        Product sp14 = new FoodProduct("SP14", "Xúc xích tiệt trùng Vissan (Gói 5 cây)", new BigDecimal("26000"), 4, today.minusDays(2)); // Đã hết hạn để kiểm thử BR9

        productRepository.saveAll(List.of(
            sp01, sp02, sp03, sp04, sp05, sp06, sp07,
            sp08, sp09, sp10, sp11, sp12, sp13, sp14
        ));

        LocalDateTime now = LocalDateTime.now();

        createSampleInvoice("HD261011001", now.minusDays(12).withHour(9).withMinute(15),
            InvoiceStatus.COMPLETED, DiscountPolicies.NONE,
            new ItemSeed(sp01, 2), new ItemSeed(sp02, 1), new ItemSeed(sp06, 2));

        createSampleInvoice("HD261013002", now.minusDays(10).withHour(14).withMinute(30),
            InvoiceStatus.COMPLETED, DiscountPolicies.PERCENT5_OVER_500K,
            new ItemSeed(sp04, 2), new ItemSeed(sp03, 2), new ItemSeed(sp09, 1));

        createSampleInvoice("HD261015003", now.minusDays(8).withHour(17).withMinute(45),
            InvoiceStatus.COMPLETED, DiscountPolicies.NONE,
            new ItemSeed(sp10, 1), new ItemSeed(sp11, 2));

        createSampleInvoice("HD261018004", now.minusDays(6).withHour(10).withMinute(20),
            InvoiceStatus.COMPLETED, DiscountPolicies.FIXED_20K,
            new ItemSeed(sp04, 1), new ItemSeed(sp05, 1), new ItemSeed(sp01, 2));

        createSampleInvoice("HD261019005", now.minusDays(5).withHour(16).withMinute(10),
            InvoiceStatus.COMPLETED, DiscountPolicies.NONE,
            new ItemSeed(sp03, 1), new ItemSeed(sp09, 2), new ItemSeed(sp07, 2));

        createSampleInvoice("HD261020006", now.minusDays(4).withHour(11).withMinute(5),
            InvoiceStatus.CANCELLED, DiscountPolicies.NONE,
            new ItemSeed(sp10, 2), new ItemSeed(sp04, 1)); // Hóa đơn đã hủy để minh họa BR7

        createSampleInvoice("HD261021007", now.minusDays(3).withHour(18).withMinute(40),
            InvoiceStatus.COMPLETED, DiscountPolicies.PERCENT5_OVER_500K,
            new ItemSeed(sp04, 2), new ItemSeed(sp10, 1), new ItemSeed(sp06, 2));

        createSampleInvoice("HD261022008", now.minusDays(2).withHour(9).withMinute(50),
            InvoiceStatus.COMPLETED, DiscountPolicies.NONE,
            new ItemSeed(sp01, 3), new ItemSeed(sp08, 4), new ItemSeed(sp12, 1));

        createSampleInvoice("HD261023009", now.minusDays(1).withHour(15).withMinute(25),
            InvoiceStatus.COMPLETED, DiscountPolicies.FIXED_20K,
            new ItemSeed(sp03, 2), new ItemSeed(sp05, 1), new ItemSeed(sp13, 1));

        createSampleInvoice("HD261024010", now.withHour(8).withMinute(30),
            InvoiceStatus.COMPLETED, DiscountPolicies.NONE,
            new ItemSeed(sp01, 2), new ItemSeed(sp09, 1), new ItemSeed(sp11, 1));
    }

    private record ItemSeed(Product product, int qty) {}

    private void createSampleInvoice(
            String id,
            LocalDateTime date,
            InvoiceStatus status,
            String policyCode,
            ItemSeed... items) {
        Invoice inv = new Invoice(id, date);
        for (ItemSeed seed : items) {
            inv.addItem(seed.product(), seed.qty());
        }
        inv.calculateTotal(DiscountPolicies.resolve(policyCode));
        inv.setStatus(status);
        invoiceRepository.save(inv);
    }
}
