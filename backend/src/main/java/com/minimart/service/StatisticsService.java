package com.minimart.service;

import com.minimart.domain.FoodProduct;
import com.minimart.domain.Invoice;
import com.minimart.domain.InvoiceDetail;
import com.minimart.domain.InvoiceStatus;
import com.minimart.domain.Product;
import com.minimart.repository.InvoiceRepository;
import com.minimart.repository.ProductRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dịch vụ thống kê doanh thu.
 * Tuân thủ nghiêm ngặt quy tắc BR7 (docx mục 2.3.4): chỉ tính các hóa đơn COMPLETED, loại trừ CANCELLED.
 * Vận dụng biểu thức Lambda và Stream API (filter, groupingBy, sorted).
 */
@Service
public class StatisticsService {

    public record StatsSummary(
        BigDecimal totalRevenue,
        long completedInvoices,
        long cancelledInvoices,
        BigDecimal averageOrderValue,
        BigDecimal totalDiscount,
        long lowStockProductsCount,
        long expiringSoonProductsCount
    ) {}

    public record DailyRevenuePoint(
        LocalDate date,
        BigDecimal revenue,
        long invoiceCount
    ) {}

    public record CategoryRevenuePoint(
        String categoryCode,
        String categoryName,
        BigDecimal revenue,
        long quantitySold,
        double percentage
    ) {}

    public record TopProductPoint(
        String productId,
        String productName,
        String categoryName,
        long quantitySold,
        BigDecimal revenue
    ) {}

    private final InvoiceRepository invoiceRepository;
    private final ProductRepository productRepository;

    public StatisticsService(InvoiceRepository invoiceRepository, ProductRepository productRepository) {
        this.invoiceRepository = invoiceRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public StatsSummary getSummary(LocalDate from, LocalDate to) {
        List<Invoice> rangeInvoices = findInvoicesInRange(from, to);

        List<Invoice> completed = rangeInvoices.stream()
            .filter(i -> i.getStatus() == InvoiceStatus.COMPLETED)
            .toList();

        long cancelledCount = rangeInvoices.stream()
            .filter(i -> i.getStatus() == InvoiceStatus.CANCELLED)
            .count();

        BigDecimal totalRevenue = completed.stream()
            .map(Invoice::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(0, RoundingMode.HALF_UP);

        BigDecimal totalDiscount = completed.stream()
            .map(Invoice::getDiscount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(0, RoundingMode.HALF_UP);

        BigDecimal avgOrder = completed.isEmpty()
            ? BigDecimal.ZERO
            : totalRevenue.divide(BigDecimal.valueOf(completed.size()), 0, RoundingMode.HALF_UP);

        List<Product> allProducts = productRepository.findAll();
        long lowStockCount = allProducts.stream()
            .filter(p -> p.getQuantity() <= ProductManager.LOW_STOCK_THRESHOLD)
            .count();

        long expiringCount = allProducts.stream()
            .filter(p -> p instanceof FoodProduct fp && (fp.isExpired() || fp.isExpiringSoon(ProductManager.EXPIRING_SOON_DAYS)))
            .count();

        return new StatsSummary(
            totalRevenue,
            completed.size(),
            cancelledCount,
            avgOrder,
            totalDiscount,
            lowStockCount,
            expiringCount
        );
    }

    @Transactional(readOnly = true)
    public List<DailyRevenuePoint> getDailyRevenue(LocalDate from, LocalDate to) {
        LocalDate effectiveTo = to != null ? to : LocalDate.now();
        LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(6);
        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được lớn hơn ngày kết thúc");
        }

        List<Invoice> completed = findCompletedInvoicesInRange(effectiveFrom, effectiveTo);

        Map<LocalDate, List<Invoice>> groupedByDate = completed.stream()
            .collect(Collectors.groupingBy(inv -> inv.getInvoiceDate().toLocalDate()));

        List<DailyRevenuePoint> result = new ArrayList<>();
        LocalDate cursor = effectiveFrom;
        while (!cursor.isAfter(effectiveTo)) {
            List<Invoice> dayInvoices = groupedByDate.getOrDefault(cursor, List.of());
            BigDecimal dayRevenue = dayInvoices.stream()
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, RoundingMode.HALF_UP);
            result.add(new DailyRevenuePoint(cursor, dayRevenue, dayInvoices.size()));
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<CategoryRevenuePoint> getRevenueByCategory(LocalDate from, LocalDate to) {
        List<Invoice> completed = findCompletedInvoicesInRange(from, to);
        Map<String, Product> productMap = productRepository.findAll().stream()
            .collect(Collectors.toMap(Product::getProductId, Function.identity(), (a, b) -> a));

        Map<String, BigDecimal> revenueByCat = new LinkedHashMap<>();
        Map<String, Long> qtyByCat = new LinkedHashMap<>();
        Map<String, String> nameByCat = new LinkedHashMap<>();

        revenueByCat.put("FOOD", BigDecimal.ZERO);
        qtyByCat.put("FOOD", 0L);
        nameByCat.put("FOOD", "Thực phẩm");

        revenueByCat.put("HOUSEHOLD", BigDecimal.ZERO);
        qtyByCat.put("HOUSEHOLD", 0L);
        nameByCat.put("HOUSEHOLD", "Đồ gia dụng");

        for (Invoice inv : completed) {
            for (InvoiceDetail detail : inv.getDetails()) {
                Product p = productMap.get(detail.getProductId());
                String catCode = p != null ? p.getCategoryCode() : "FOOD";
                String catName = p != null ? p.getCategory() : "Thực phẩm";
                nameByCat.put(catCode, catName);
                revenueByCat.put(catCode, revenueByCat.getOrDefault(catCode, BigDecimal.ZERO).add(detail.getSubtotal()));
                qtyByCat.put(catCode, qtyByCat.getOrDefault(catCode, 0L) + detail.getQuantity());
            }
        }

        BigDecimal grandTotal = revenueByCat.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategoryRevenuePoint> points = new ArrayList<>();
        for (String code : revenueByCat.keySet()) {
            BigDecimal rev = revenueByCat.get(code).setScale(0, RoundingMode.HALF_UP);
            long qty = qtyByCat.getOrDefault(code, 0L);
            double pct = grandTotal.compareTo(BigDecimal.ZERO) > 0
                ? rev.multiply(BigDecimal.valueOf(10000))
                    .divide(grandTotal, 0, RoundingMode.HALF_UP).doubleValue() / 100.0
                : 0.0;
            points.add(new CategoryRevenuePoint(code, nameByCat.get(code), rev, qty, pct));
        }
        return points;
    }

    @Transactional(readOnly = true)
    public List<TopProductPoint> getTopProducts(LocalDate from, LocalDate to, int limit) {
        int effectiveLimit = limit > 0 ? limit : 5;
        List<Invoice> completed = findCompletedInvoicesInRange(from, to);
        Map<String, Product> productMap = productRepository.findAll().stream()
            .collect(Collectors.toMap(Product::getProductId, Function.identity(), (a, b) -> a));

        Map<String, Long> qtyMap = new LinkedHashMap<>();
        Map<String, BigDecimal> revMap = new LinkedHashMap<>();

        for (Invoice inv : completed) {
            for (InvoiceDetail detail : inv.getDetails()) {
                String pid = detail.getProductId();
                qtyMap.merge(pid, (long) detail.getQuantity(), Long::sum);
                revMap.merge(pid, detail.getSubtotal(), BigDecimal::add);
            }
        }

        return qtyMap.keySet().stream()
            .map(pid -> {
                Product p = productMap.get(pid);
                String name = p != null ? p.getName() : pid;
                String cat = p != null ? p.getCategory() : "Thực phẩm";
                return new TopProductPoint(
                    pid,
                    name,
                    cat,
                    qtyMap.get(pid),
                    revMap.getOrDefault(pid, BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP)
                );
            })
            .sorted(Comparator.comparingLong(TopProductPoint::quantitySold).reversed()
                .thenComparing(Comparator.comparing(TopProductPoint::revenue).reversed()))
            .limit(effectiveLimit)
            .toList();
    }

    private List<Invoice> findCompletedInvoicesInRange(LocalDate from, LocalDate to) {
        return findInvoicesInRange(from, to).stream()
            .filter(i -> i.getStatus() == InvoiceStatus.COMPLETED)
            .toList();
    }

    private List<Invoice> findInvoicesInRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được lớn hơn ngày kết thúc");
        }
        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt = to != null ? to.atTime(LocalTime.MAX) : null;

        return invoiceRepository.findAll().stream()
            .filter(inv -> fromDt == null || !inv.getInvoiceDate().isBefore(fromDt))
            .filter(inv -> toDt == null || !inv.getInvoiceDate().isAfter(toDt))
            .toList();
    }
}
