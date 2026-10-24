package com.minimart.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lớp Invoice lưu thông tin hóa đơn bán hàng, danh sách chi tiết mặt hàng và tổng tiền.
 * Hỗ trợ các phương thức addItem(), calculateTotal(), getInvoiceDate() theo thiết kế docx mục 2.4.1.
 */
@Entity
@Table(name = "invoice")
public class Invoice {

    @Id
    @Column(name = "invoice_id", length = 20, nullable = false)
    private String invoiceId;

    @Column(name = "invoice_date", nullable = false)
    private LocalDateTime invoiceDate;

    @Column(name = "discount", precision = 14, scale = 0, nullable = false)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 14, scale = 0, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private InvoiceStatus status = InvoiceStatus.COMPLETED;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<InvoiceDetail> details = new ArrayList<>();

    protected Invoice() {
    }

    public Invoice(String invoiceId, LocalDateTime invoiceDate) {
        if (invoiceId == null || invoiceId.isBlank()) {
            throw new IllegalArgumentException("Mã hóa đơn không được để trống");
        }
        this.invoiceId = invoiceId.trim();
        this.invoiceDate = invoiceDate != null ? invoiceDate : LocalDateTime.now();
        this.status = InvoiceStatus.COMPLETED;
    }

    public void addItem(CartItem cartItem) {
        if (cartItem == null) {
            throw new IllegalArgumentException("Mặt hàng thêm vào hóa đơn không hợp lệ");
        }
        addItem(cartItem.getProduct(), cartItem.getQuantity());
    }

    public void addItem(Product product, int quantity) {
        if (product == null) {
            throw new IllegalArgumentException("Sản phẩm không được để trống");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng bán phải là số nguyên lớn hơn 0");
        }
        for (InvoiceDetail existing : details) {
            if (existing.getProductId().equals(product.getProductId())) {
                existing.setQuantity(existing.getQuantity() + quantity);
                recalculateTotals(null);
                return;
            }
        }
        InvoiceDetail detail = new InvoiceDetail(this, product.getProductId(), quantity, product.getPrice());
        details.add(detail);
        recalculateTotals(null);
    }

    public void addDetail(InvoiceDetail detail) {
        detail.setInvoice(this);
        this.details.add(detail);
    }

    public BigDecimal calculateSubtotal() {
        return details.stream()
            .map(InvoiceDetail::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(0, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotal() {
        return calculateTotal(DiscountPolicies.resolve(DiscountPolicies.NONE));
    }

    public BigDecimal calculateTotal(DiscountPolicy policy) {
        recalculateTotals(policy);
        return this.totalAmount;
    }

    private void recalculateTotals(DiscountPolicy policy) {
        BigDecimal subtotal = calculateSubtotal();
        if (policy != null) {
            BigDecimal rawDiscount = policy.calculateDiscount(subtotal);
            if (rawDiscount == null || rawDiscount.compareTo(BigDecimal.ZERO) < 0) {
                rawDiscount = BigDecimal.ZERO;
            }
            if (rawDiscount.compareTo(subtotal) > 0) {
                rawDiscount = subtotal;
            }
            this.discount = rawDiscount.setScale(0, RoundingMode.HALF_UP);
        }
        BigDecimal finalTotal = subtotal.subtract(this.discount != null ? this.discount : BigDecimal.ZERO);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }
        this.totalAmount = finalTotal.setScale(0, RoundingMode.HALF_UP);
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public LocalDateTime getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDateTime invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount != null ? discount.setScale(0, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount != null ? totalAmount.setScale(0, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public List<InvoiceDetail> getDetails() {
        return Collections.unmodifiableList(details);
    }
}
