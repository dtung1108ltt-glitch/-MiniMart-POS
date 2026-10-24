package com.minimart.web.dto;

import com.minimart.domain.FoodProduct;
import com.minimart.domain.HouseholdProduct;
import com.minimart.domain.Product;
import com.minimart.service.ProductManager;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductResponse(
    String productId,
    String name,
    BigDecimal price,
    int quantity,
    String category,
    String categoryName,
    LocalDate expiryDate,
    String brand,
    boolean expired,
    boolean expiringSoon,
    boolean lowStock,
    Long version
) {
    public static ProductResponse from(Product product) {
        LocalDate expiryDate = null;
        String brand = null;
        boolean expired = false;
        boolean expiringSoon = false;

        if (product instanceof FoodProduct fp) {
            expiryDate = fp.getExpiryDate();
            expired = fp.isExpired();
            expiringSoon = fp.isExpiringSoon(ProductManager.EXPIRING_SOON_DAYS);
        } else if (product instanceof HouseholdProduct hp) {
            brand = hp.getBrand();
        }

        boolean lowStock = product.getQuantity() <= ProductManager.LOW_STOCK_THRESHOLD;

        return new ProductResponse(
            product.getProductId(),
            product.getName(),
            product.getPrice(),
            product.getQuantity(),
            product.getCategoryCode(),
            product.getCategory(),
            expiryDate,
            brand,
            expired,
            expiringSoon,
            lowStock,
            product.getVersion()
        );
    }
}
