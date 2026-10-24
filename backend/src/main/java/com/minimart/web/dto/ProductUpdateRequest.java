package com.minimart.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductUpdateRequest(
    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 100, message = "Tên sản phẩm không được vượt quá 100 ký tự")
    String name,

    @NotNull(message = "Đơn giá không được để trống")
    @DecimalMin(value = "0", message = "Đơn giá không được âm")
    BigDecimal price,

    @NotNull(message = "Số lượng tồn kho không được để trống")
    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    Integer quantity,

    String category,

    LocalDate expiryDate,

    @Size(max = 100, message = "Thương hiệu không được vượt quá 100 ký tự")
    String brand
) {}
