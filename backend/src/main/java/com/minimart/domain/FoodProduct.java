package com.minimart.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lớp sản phẩm thực phẩm kế thừa Product.
 * Bổ sung thuộc tính hạn sử dụng (expiryDate) và kiểm tra hết hạn (isExpired).
 */
@Entity
@DiscriminatorValue("FOOD")
public class FoodProduct extends Product {

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    protected FoodProduct() {
        super();
    }

    public FoodProduct(String id, String name, BigDecimal price, int quantity, LocalDate expiryDate) {
        super(id, name, price, quantity);
        this.expiryDate = expiryDate;
    }

    public FoodProduct(String id, String name, BigDecimal price, int quantity) {
        this(id, name, price, quantity, null);
    }

    public FoodProduct(String id, String name, double price, int quantity, LocalDate expiryDate) {
        super(id, name, price, quantity);
        this.expiryDate = expiryDate;
    }

    public FoodProduct(String id, String name, double price, int quantity) {
        super(id, name, price, quantity);
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    public boolean isExpiringSoon(int days) {
        if (expiryDate == null || isExpired()) {
            return false;
        }
        return !expiryDate.isAfter(LocalDate.now().plusDays(days));
    }

    @Override
    public String getCategory() {
        return "Thực phẩm";
    }

    @Override
    public String getCategoryCode() {
        return "FOOD";
    }
}
