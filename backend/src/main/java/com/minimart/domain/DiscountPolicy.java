package com.minimart.domain;

import java.math.BigDecimal;

/**
 * Functional Interface quy định hành vi tính số tiền được giảm giá.
 * Có thể triển khai ngắn gọn bằng biểu thức Lambda.
 */
@FunctionalInterface
public interface DiscountPolicy {
    BigDecimal calculateDiscount(BigDecimal total);
}
