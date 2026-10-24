package com.minimart.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Chi tiết một dòng sản phẩm trong hóa đơn, lưu đơn giá tại thời điểm bán (BR10).
 */
@Entity
@Table(name = "invoice_detail")
public class InvoiceDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detail_id")
    private Long detailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "product_id", length = 20, nullable = false)
    private String productId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", precision = 14, scale = 0, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "subtotal", precision = 14, scale = 0, nullable = false)
    private BigDecimal subtotal;

    protected InvoiceDetail() {
    }

    public InvoiceDetail(Invoice invoice, String productId, int quantity, BigDecimal unitPrice) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("Mã sản phẩm trong chi tiết hóa đơn không được rỗng");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng bán phải là số nguyên lớn hơn 0");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Đơn giá bán không hợp lệ");
        }
        this.invoice = invoice;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice.setScale(0, RoundingMode.HALF_UP);
        this.subtotal = this.unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(0, RoundingMode.HALF_UP);
    }

    public Long getDetailId() {
        return detailId;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng bán phải là số nguyên lớn hơn 0");
        }
        this.quantity = quantity;
        this.subtotal = this.unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(0, RoundingMode.HALF_UP);
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }
}
