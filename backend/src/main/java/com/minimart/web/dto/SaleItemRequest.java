package com.minimart.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SaleItemRequest(
    @NotBlank(message = "Mã sản phẩm không được để trống")
    String productId,

    @NotNull(message = "Số lượng bán không được để trống")
    @Min(value = 1, message = "Số lượng bán phải là số nguyên lớn hơn 0")
    Integer quantity
) {}
