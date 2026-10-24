package com.minimart.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.List;

public record CheckoutRequest(
    @NotEmpty(message = "Giỏ hàng không được để trống")
    @Valid
    List<SaleItemRequest> items,

    String discountPolicy,

    BigDecimal amountPaid
) {}
