package com.minimart.web;

import com.minimart.domain.Invoice;
import com.minimart.domain.Product;
import com.minimart.repository.ProductRepository;
import com.minimart.service.SalesManager;
import com.minimart.web.dto.InvoiceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Invoices", description = "Tra cứu và hủy hóa đơn bán hàng")
public class InvoiceController {

    private final SalesManager salesManager;
    private final ProductRepository productRepository;

    public InvoiceController(SalesManager salesManager, ProductRepository productRepository) {
        this.salesManager = salesManager;
        this.productRepository = productRepository;
    }

    @GetMapping
    @Operation(summary = "Danh sách hóa đơn lọc theo khoảng ngày và trạng thái")
    public Page<InvoiceResponse> listInvoices(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Map<String, String> productNames = getProductNameMap();
        return salesManager.findInvoices(from, to, status, pageable)
            .map(inv -> InvoiceResponse.from(inv, productNames, null));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết hóa đơn theo mã")
    public InvoiceResponse getInvoice(@PathVariable String id) {
        Invoice invoice = salesManager.getInvoiceById(id);
        return InvoiceResponse.from(invoice, getProductNameMap(), null);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Hủy hóa đơn và hoàn trả tồn kho")
    public InvoiceResponse cancelInvoice(@PathVariable String id) {
        Invoice cancelled = salesManager.cancelInvoice(id);
        return InvoiceResponse.from(cancelled, getProductNameMap(), null);
    }

    private Map<String, String> getProductNameMap() {
        return productRepository.findAll().stream()
            .collect(Collectors.toMap(Product::getProductId, Product::getName, (a, b) -> a));
    }
}
