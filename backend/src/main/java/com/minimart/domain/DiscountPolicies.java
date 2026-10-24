package com.minimart.domain;

import com.minimart.exception.InvalidDiscountException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tập hợp các chính sách giảm giá dựng sẵn được cài đặt bằng biểu thức Lambda.
 */
public final class DiscountPolicies {

    public record PolicyDescriptor(String code, String name, String description, DiscountPolicy policy) {}

    public static final String NONE = "NONE";
    public static final String PERCENT5_OVER_500K = "PERCENT5_OVER_500K";
    public static final String PERCENT10_OVER_1M = "PERCENT10_OVER_1M";
    public static final String FIXED_20K = "FIXED_20K";

    private static final BigDecimal FIVE_HUNDRED_K = new BigDecimal("500000");
    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");
    private static final BigDecimal TWENTY_K = new BigDecimal("20000");
    private static final BigDecimal RATE_5_PERCENT = new BigDecimal("0.05");
    private static final BigDecimal RATE_10_PERCENT = new BigDecimal("0.10");

    private static final Map<String, PolicyDescriptor> REGISTRY;

    static {
        Map<String, PolicyDescriptor> map = new LinkedHashMap<>();

        map.put(NONE, new PolicyDescriptor(
            NONE,
            "Không giảm giá",
            "Áp dụng nguyên giá niêm yết",
            total -> BigDecimal.ZERO
        ));

        map.put(PERCENT5_OVER_500K, new PolicyDescriptor(
            PERCENT5_OVER_500K,
            "Giảm 5% (Đơn ≥ 500.000đ)",
            "Giảm 5% tổng hóa đơn khi tạm tính từ 500.000đ trở lên",
            total -> total != null && total.compareTo(FIVE_HUNDRED_K) >= 0
                ? total.multiply(RATE_5_PERCENT).setScale(0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO
        ));

        map.put(PERCENT10_OVER_1M, new PolicyDescriptor(
            PERCENT10_OVER_1M,
            "Giảm 10% (Đơn ≥ 1.000.000đ)",
            "Giảm 10% tổng hóa đơn khi tạm tính từ 1.000.000đ trở lên",
            total -> total != null && total.compareTo(ONE_MILLION) >= 0
                ? total.multiply(RATE_10_PERCENT).setScale(0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO
        ));

        map.put(FIXED_20K, new PolicyDescriptor(
            FIXED_20K,
            "Giảm trực tiếp 20.000đ",
            "Giảm cố định 20.000đ trên hóa đơn (tối đa bằng tổng tiền hàng)",
            total -> {
                if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
                    return BigDecimal.ZERO;
                }
                return total.compareTo(TWENTY_K) >= 0 ? TWENTY_K : total;
            }
        ));

        REGISTRY = Collections.unmodifiableMap(map);
    }

    private DiscountPolicies() {
    }

    public static DiscountPolicy resolve(String code) {
        if (code == null || code.isBlank()) {
            return REGISTRY.get(NONE).policy();
        }
        PolicyDescriptor descriptor = REGISTRY.get(code.trim().toUpperCase());
        if (descriptor == null) {
            throw new InvalidDiscountException("Chính sách giảm giá không hợp lệ: " + code);
        }
        return descriptor.policy();
    }

    public static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            return NONE;
        }
        String normalized = code.trim().toUpperCase();
        if (!REGISTRY.containsKey(normalized)) {
            throw new InvalidDiscountException("Chính sách giảm giá không hợp lệ: " + code);
        }
        return normalized;
    }

    public static List<PolicyDescriptor> getAllDescriptors() {
        return List.copyOf(REGISTRY.values());
    }
}
