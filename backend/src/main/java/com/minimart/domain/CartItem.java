package com.minimart.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Lớp CartItem lưu thông tin một mặt hàng trong giỏ hàng/hóa đơn kèm đơn giá chụp tại thời điểm bán.
 */
public class CartItem {

    private final Product product;
    private int quantity;
    private final BigDecimal unitPrice;

    public CartItem(Product product, int quantity) {
        if (product == null) {
            throw new IllegalArgumentException("Sản phẩm không được để trống");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng bán phải là số nguyên lớn hơn 0");
        }
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = product.getPrice();
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng bán phải là số nguyên lớn hơn 0");
        }
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(0, RoundingMode.HALF_UP);
    }
}
