package com.minimart.service;

import com.minimart.domain.CartItem;
import com.minimart.domain.DiscountPolicies;
import com.minimart.domain.DiscountPolicy;
import com.minimart.domain.FoodProduct;
import com.minimart.domain.Invoice;
import com.minimart.domain.InvoiceDetail;
import com.minimart.domain.InvoiceStatus;
import com.minimart.domain.Product;
import com.minimart.exception.DemoReadonlyException;
import com.minimart.exception.InsufficientStockException;
import com.minimart.exception.InvoiceNotFoundException;
import com.minimart.exception.ProductNotFoundException;
import com.minimart.repository.InvoiceRepository;
import com.minimart.repository.ProductRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lớp nghiệp vụ xử lý bán hàng, tính tiền tạm tính, lập hóa đơn nguyên tử và hủy hóa đơn hoàn kho.
 */
@Service
public class SalesManager {

    public record SaleItemCommand(String productId, int quantity) {}

    public record PreviewLineItem(
        String productId,
        String productName,
        String category,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
    ) {}

    public record SalePreviewResult(
        List<PreviewLineItem> items,
        BigDecimal subtotal,
        String discountPolicy,
        BigDecimal discount,
        BigDecimal totalAmount
    ) {}

    private static final DateTimeFormatter ID_DATE_FMT = DateTimeFormatter.ofPattern("yyMMddHHmmss");
    private final AtomicInteger invoiceSeq = new AtomicInteger(100);

    private final ProductRepository productRepository;
    private final InvoiceRepository invoiceRepository;
    private final boolean demoReadonly;

    public SalesManager(
            ProductRepository productRepository,
            InvoiceRepository invoiceRepository,
            @Value("${app.demo-readonly:false}") boolean demoReadonly) {
        this.productRepository = productRepository;
        this.invoiceRepository = invoiceRepository;
        this.demoReadonly = demoReadonly;
    }

    @Transactional(readOnly = true)
    public SalePreviewResult previewSale(List<SaleItemCommand> rawItems, String discountPolicyCode) {
        Map<String, Integer> mergedItems = validateAndMergeItems(rawItems);
        String policyCode = DiscountPolicies.normalizeCode(discountPolicyCode);
        DiscountPolicy policy = DiscountPolicies.resolve(policyCode);

        List<PreviewLineItem> previewLines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (Map.Entry<String, Integer> entry : mergedItems.entrySet()) {
            String productId = entry.getKey();
            int qty = entry.getValue();

            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

            validateProductSellable(product, qty);

            CartItem cartItem = new CartItem(product, qty);
            BigDecimal lineSubtotal = cartItem.getSubtotal();
            subtotal = subtotal.add(lineSubtotal);

            previewLines.add(new PreviewLineItem(
                product.getProductId(),
                product.getName(),
                product.getCategory(),
                qty,
                cartItem.getUnitPrice(),
                lineSubtotal
            ));
        }

        BigDecimal discount = policy.calculateDiscount(subtotal);
        if (discount == null || discount.compareTo(BigDecimal.ZERO) < 0) {
            discount = BigDecimal.ZERO;
        }
        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }
        discount = discount.setScale(0, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.subtract(discount).setScale(0, RoundingMode.HALF_UP);

        return new SalePreviewResult(previewLines, subtotal, policyCode, discount, totalAmount);
    }

    @Transactional
    public Invoice checkout(List<SaleItemCommand> rawItems, String discountPolicyCode, BigDecimal amountPaid) {
        Map<String, Integer> mergedItems = validateAndMergeItems(rawItems);
        String policyCode = DiscountPolicies.normalizeCode(discountPolicyCode);
        DiscountPolicy policy = DiscountPolicies.resolve(policyCode);

        String invoiceId = generateInvoiceId();
        Invoice invoice = new Invoice(invoiceId, LocalDateTime.now());

        // Sắp xếp theo productId để tránh deadlock khi khóa nhiều sản phẩm đồng thời
        List<String> sortedIds = mergedItems.keySet().stream().sorted().toList();

        for (String productId : sortedIds) {
            int qty = mergedItems.get(productId);
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

            validateProductSellable(product, qty);

            // Trừ tồn kho và lưu ngay để kích hoạt kiểm tra @Version chống bán vượt đồng thời (BR11)
            product.setQuantity(product.getQuantity() - qty);
            productRepository.saveAndFlush(product);

            invoice.addItem(new CartItem(product, qty));
        }

        BigDecimal totalAmount = invoice.calculateTotal(policy);
        if (amountPaid != null && amountPaid.compareTo(totalAmount) < 0) {
            throw new IllegalArgumentException(
                String.format("Số tiền khách đưa (%s đ) nhỏ hơn tổng cần thanh toán (%s đ)",
                    amountPaid.toPlainString(), totalAmount.toPlainString())
            );
        }

        invoice.setStatus(InvoiceStatus.COMPLETED);
        return invoiceRepository.saveAndFlush(invoice);
    }

    @Transactional
    public Invoice cancelInvoice(String invoiceId) {
        if (demoReadonly) {
            throw new DemoReadonlyException("Chế độ demo chỉ đọc (DEMO_READONLY) đang bật, không cho phép hủy hóa đơn");
        }
        if (invoiceId == null || invoiceId.isBlank()) {
            throw new IllegalArgumentException("Mã hóa đơn không được để trống");
        }
        Invoice invoice = invoiceRepository.findById(invoiceId.trim())
            .orElseThrow(() -> new InvoiceNotFoundException(invoiceId.trim()));

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Hóa đơn '" + invoiceId + "' đã ở trạng thái đã hủy (CANCELLED)");
        }

        // Hoàn tồn kho cho từng mặt hàng trong hóa đơn (BR8)
        for (InvoiceDetail detail : invoice.getDetails()) {
            productRepository.findById(detail.getProductId()).ifPresent(product -> {
                product.setQuantity(product.getQuantity() + detail.getQuantity());
                productRepository.saveAndFlush(product);
            });
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        return invoiceRepository.saveAndFlush(invoice);
    }

    @Transactional(readOnly = true)
    public Invoice getInvoiceById(String invoiceId) {
        if (invoiceId == null || invoiceId.isBlank()) {
            throw new IllegalArgumentException("Mã hóa đơn không được để trống");
        }
        return invoiceRepository.findById(invoiceId.trim())
            .orElseThrow(() -> new InvoiceNotFoundException(invoiceId.trim()));
    }

    @Transactional(readOnly = true)
    public Page<Invoice> findInvoices(LocalDate from, LocalDate to, String status, Pageable pageable) {
        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt = to != null ? to.atTime(LocalTime.MAX) : null;
        InvoiceStatus statusEnum = parseStatus(status);

        List<Invoice> filtered = invoiceRepository.findAll().stream()
            .filter(inv -> fromDt == null || !inv.getInvoiceDate().isBefore(fromDt))
            .filter(inv -> toDt == null || !inv.getInvoiceDate().isAfter(toDt))
            .filter(inv -> statusEnum == null || inv.getStatus() == statusEnum)
            .sorted(Comparator.comparing(Invoice::getInvoiceDate).reversed())
            .toList();

        if (pageable == null || pageable.isUnpaged()) {
            return new PageImpl<>(filtered);
        }
        int start = (int) pageable.getOffset();
        if (start >= filtered.size()) {
            return new PageImpl<>(List.of(), pageable, filtered.size());
        }
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }

    private Map<String, Integer> validateAndMergeItems(List<SaleItemCommand> rawItems) {
        if (rawItems == null || rawItems.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng trống, không thể thực hiện thanh toán");
        }
        Map<String, Integer> merged = new LinkedHashMap<>();
        for (SaleItemCommand item : rawItems) {
            if (item == null || item.productId() == null || item.productId().isBlank()) {
                throw new IllegalArgumentException("Mã sản phẩm trong giỏ hàng không được để trống");
            }
            if (item.quantity() <= 0) {
                throw new IllegalArgumentException("Số lượng bán phải là số nguyên lớn hơn 0");
            }
            String id = item.productId().trim();
            merged.merge(id, item.quantity(), Integer::sum);
        }
        return merged;
    }

    private void validateProductSellable(Product product, int requestedQty) {
        if (product instanceof FoodProduct foodProduct && foodProduct.isExpired()) {
            throw new IllegalStateException(
                String.format("Sản phẩm thực phẩm '%s' (%s) đã hết hạn sử dụng (%s), không được phép bán",
                    product.getName(), product.getProductId(), foodProduct.getExpiryDate())
            );
        }
        if (requestedQty > product.getQuantity()) {
            throw new InsufficientStockException(product.getProductId(), requestedQty, product.getQuantity());
        }
    }

    private InvoiceStatus parseStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status.trim())) {
            return null;
        }
        try {
            return InvoiceStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Trạng thái hóa đơn không hợp lệ: " + status);
        }
    }

    private String generateInvoiceId() {
        int seq = invoiceSeq.incrementAndGet() % 1000;
        String candidate = "HD" + LocalDateTime.now().format(ID_DATE_FMT) + String.format("%03d", seq);
        if (candidate.length() > 20) {
            candidate = candidate.substring(0, 20);
        }
        return candidate;
    }
}
