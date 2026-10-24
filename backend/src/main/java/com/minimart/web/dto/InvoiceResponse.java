package com.minimart.web.dto;

import com.minimart.domain.Invoice;
import com.minimart.domain.InvoiceDetail;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record InvoiceResponse(
    String invoiceId,
    LocalDateTime invoiceDate,
    String status,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal totalAmount,
    BigDecimal amountPaid,
    BigDecimal changeAmount,
    List<InvoiceItemResponse> items
) {
    public record InvoiceItemResponse(
        Long detailId,
        String productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
    ) {}

    public static InvoiceResponse from(Invoice invoice, Map<String, String> productNames, BigDecimal amountPaid) {
        List<InvoiceItemResponse> itemDtos = invoice.getDetails().stream()
            .map(d -> new InvoiceItemResponse(
                d.getDetailId(),
                d.getProductId(),
                productNames != null ? productNames.getOrDefault(d.getProductId(), d.getProductId()) : d.getProductId(),
                d.getQuantity(),
                d.getUnitPrice(),
                d.getSubtotal()
            ))
            .toList();

        BigDecimal subtotal = invoice.calculateSubtotal();
        BigDecimal total = invoice.getTotalAmount();
        BigDecimal effectivePaid = amountPaid != null ? amountPaid : total;
        BigDecimal change = effectivePaid.subtract(total);
        if (change.compareTo(BigDecimal.ZERO) < 0) {
            change = BigDecimal.ZERO;
        }

        return new InvoiceResponse(
            invoice.getInvoiceId(),
            invoice.getInvoiceDate(),
            invoice.getStatus().name(),
            subtotal,
            invoice.getDiscount(),
            total,
            effectivePaid,
            change,
            itemDtos
        );
    }
}
