package com.minimart.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Kiểm thử các kỹ thuật OOP trong gói domain (Đóng gói, Kế thừa, Đa hình, Abstract class, Interface, Lambda).
 */
class DomainOopTest {

    @Test
    @DisplayName("Đóng gói: Tạo sản phẩm với mã rỗng, giá âm hoặc số lượng âm phải ném IllegalArgumentException")
    void testEncapsulationValidation() {
        IllegalArgumentException exBlankId = assertThrows(
            IllegalArgumentException.class,
            () -> new FoodProduct("  ", "Sữa tươi", 30000, 10)
        );
        assertTrue(exBlankId.getMessage().contains("Mã sản phẩm"));

        IllegalArgumentException exNegPrice = assertThrows(
            IllegalArgumentException.class,
            () -> new FoodProduct("SP01", "Sữa tươi", -1000, 10)
        );
        assertTrue(exNegPrice.getMessage().contains("Giá"));

        IllegalArgumentException exNegQty = assertThrows(
            IllegalArgumentException.class,
            () -> new HouseholdProduct("SP02", "Chổi lau nhà", 50000, -5)
        );
        assertTrue(exNegQty.getMessage().contains("Số lượng"));

        Product p = new FoodProduct("SP01", "Sữa tươi", 30000, 20);
        assertThrows(IllegalArgumentException.class, () -> p.setPrice(-1));
        assertThrows(IllegalArgumentException.class, () -> p.setQuantity(-1));
    }

    @Test
    @DisplayName("Kế thừa & Đa hình: List<Product> hỗn hợp gọi đúng getCategory() của từng lớp con (docx 3.3)")
    void testInheritanceAndPolymorphism() {
        Product p1 = new FoodProduct("SP01", "Sữa tươi", 30000, 20, LocalDate.now().plusDays(10));
        Product p2 = new HouseholdProduct("SP02", "Nước rửa chén", 45000, 15, "Sunlight");

        List<Product> products = List.of(p1, p2);
        assertEquals("Thực phẩm", products.get(0).getCategory());
        assertEquals("Đồ gia dụng", products.get(1).getCategory());
        assertEquals("FOOD", products.get(0).getCategoryCode());
        assertEquals("HOUSEHOLD", products.get(1).getCategoryCode());
    }

    @Test
    @DisplayName("Ca 8 (docx 4.4): Tính tiền 1 sản phẩm giá 30.000, số lượng 2 -> Thành tiền 60.000 và giữ nguyên đơn giá lịch sử (BR10)")
    void testCartItemSubtotalAndPriceSnapshot() {
        Product p = new FoodProduct("SP01", "Sữa tươi", new BigDecimal("30000"), 20);
        CartItem item = new CartItem(p, 2);
        assertEquals(new BigDecimal("60000"), item.getSubtotal());

        // Thay đổi giá sản phẩm sau khi đã đưa vào giỏ/hóa đơn không làm đổi đơn giá đã chụp (BR10)
        p.setPrice(new BigDecimal("50000"));
        assertEquals(new BigDecimal("30000"), item.getUnitPrice());
        assertEquals(new BigDecimal("60000"), item.getSubtotal());
    }

    @Test
    @DisplayName("Ca 9 (docx 4.4): Hóa đơn 600.000, giảm 5% bằng Lambda -> Tiền giảm 30.000, Tổng thanh toán 570.000")
    void testDiscountPolicyLambda() {
        DiscountPolicy policy5Percent = DiscountPolicies.resolve(DiscountPolicies.PERCENT5_OVER_500K);
        BigDecimal total = new BigDecimal("600000");
        BigDecimal discount = policy5Percent.calculateDiscount(total);
        assertEquals(new BigDecimal("30000"), discount);

        Product p = new FoodProduct("SP01", "Thùng sữa", new BigDecimal("300000"), 10);
        Invoice invoice = new Invoice("HD001", LocalDateTime.now());
        invoice.addItem(p, 2); // 600.000
        BigDecimal finalAmount = invoice.calculateTotal(policy5Percent);

        assertEquals(new BigDecimal("600000"), invoice.calculateSubtotal());
        assertEquals(new BigDecimal("30000"), invoice.getDiscount());
        assertEquals(new BigDecimal("570000"), finalAmount);
    }

    @Test
    @DisplayName("FoodProduct kiểm tra hạn sử dụng isExpired() chính xác")
    void testFoodProductExpiry() {
        FoodProduct expired = new FoodProduct("SP_EXP", "Sữa chua", 10000, 5, LocalDate.now().minusDays(1));
        FoodProduct valid = new FoodProduct("SP_OK", "Sữa chua", 10000, 5, LocalDate.now().plusDays(5));

        assertTrue(expired.isExpired());
        assertFalse(valid.isExpired());
        assertTrue(valid.isExpiringSoon(7));
    }
}
