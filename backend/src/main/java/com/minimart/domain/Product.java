package com.minimart.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Lớp trừu tượng mô tả thông tin chung của sản phẩm trong siêu thị mini.
 * Thể hiện tính Đóng gói (thuộc tính private, kiểm tra hợp lệ trong constructor/setter)
 * và Trừu tượng hóa (abstract class + abstract method getCategory()).
 */
@Entity
@Table(name = "product")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "category", discriminatorType = DiscriminatorType.STRING, length = 50)
public abstract class Product {

    @Id
    @Column(name = "product_id", length = 20, nullable = false)
    private String productId;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "price", precision = 14, scale = 0, nullable = false)
    private BigDecimal price;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Version
    @Column(name = "version")
    private Long version;

    protected Product() {
        // Phục vụ JPA
    }

    public Product(String productId, String name, BigDecimal price, int quantity) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("Mã sản phẩm không được để trống");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá bán không được âm");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("Số lượng tồn kho không được âm");
        }
        this.productId = productId.trim();
        this.name = name.trim();
        this.price = price.setScale(0, RoundingMode.HALF_UP);
        this.quantity = quantity;
    }

    public Product(String productId, String name, double price, int quantity) {
        this(
            productId,
            name,
            Double.isNaN(price) ? null : BigDecimal.valueOf(price).setScale(0, RoundingMode.HALF_UP),
            quantity
        );
    }

    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống");
        }
        this.name = name.trim();
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá không được âm");
        }
        this.price = price.setScale(0, RoundingMode.HALF_UP);
    }

    public void setPrice(double price) {
        if (Double.isNaN(price) || price < 0) {
            throw new IllegalArgumentException("Giá không được âm");
        }
        this.price = BigDecimal.valueOf(price).setScale(0, RoundingMode.HALF_UP);
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Số lượng không được âm");
        }
        this.quantity = quantity;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * Phương thức trừu tượng trả về tên loại sản phẩm tiếng Việt ("Thực phẩm" / "Đồ gia dụng").
     */
    public abstract String getCategory();

    /**
     * Trả về mã loại sản phẩm phục vụ phân loại API ("FOOD" / "HOUSEHOLD").
     */
    public abstract String getCategoryCode();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product that)) return false;
        return Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }
}
