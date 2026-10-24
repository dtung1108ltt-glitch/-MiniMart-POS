package com.minimart.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.math.BigDecimal;

/**
 * Lớp sản phẩm đồ gia dụng kế thừa Product.
 * Bổ sung thuộc tính thương hiệu (brand).
 */
@Entity
@DiscriminatorValue("HOUSEHOLD")
public class HouseholdProduct extends Product {

    @Column(name = "brand", length = 100)
    private String brand;

    protected HouseholdProduct() {
        super();
    }

    public HouseholdProduct(String id, String name, BigDecimal price, int quantity, String brand) {
        super(id, name, price, quantity);
        this.brand = brand != null ? brand.trim() : null;
    }

    public HouseholdProduct(String id, String name, BigDecimal price, int quantity) {
        this(id, name, price, quantity, null);
    }

    public HouseholdProduct(String id, String name, double price, int quantity, String brand) {
        super(id, name, price, quantity);
        this.brand = brand != null ? brand.trim() : null;
    }

    public HouseholdProduct(String id, String name, double price, int quantity) {
        super(id, name, price, quantity);
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand != null ? brand.trim() : null;
    }

    @Override
    public String getCategory() {
        return "Đồ gia dụng";
    }

    @Override
    public String getCategoryCode() {
        return "HOUSEHOLD";
    }
}
