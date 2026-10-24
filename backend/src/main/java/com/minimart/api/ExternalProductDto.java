package com.minimart.api;

import java.math.BigDecimal;

/**
 * DTO chuẩn hóa dữ liệu sản phẩm lấy từ API ngoài (dummyjson.com) để hiển thị tham khảo/nhập mẫu.
 */
public record ExternalProductDto(
    String externalId,
    String suggestedProductId,
    String title,
    String category,
    String mappedCategoryCode,
    String mappedCategoryName,
    String brand,
    double priceUsd,
    BigDecimal priceVnd,
    int stock,
    String thumbnail
) {}
