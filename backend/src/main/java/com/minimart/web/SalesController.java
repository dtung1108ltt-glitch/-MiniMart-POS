package com.minimart.web;

import com.minimart.domain.DiscountPolicies;
import com.minimart.domain.Invoice;
import com.minimart.domain.Product;
import com.minimart.repository.ProductRepository;
import com.minimart.service.SalesManager;
import com.minimart.web.dto.CheckoutRequest;
import com.minimart.web.dto.DiscountPolicyResponse;
import com.minimart.web.dto.InvoiceResponse;
import com.minimart.web.dto.SalePreviewRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Sales & Discounts", description = "Bán hàng POS, tính tiền tạm tính và chính sách giảm giá")
public class SalesController {

    private final SalesManager salesManager;
    private final ProductRepository productRepository;

    public SalesController(SalesManager salesManager, ProductRepository productRepository) {
        this.salesManager = salesManager;
        this.productRepository = productRepository;
    }

    @GetMapping("/discount-policies")
    @Operation(summary = "Danh sách các chính sách giảm giá dựng sẵn bằng Lambda")
    public List<DiscountPolicyResponse> listDiscountPolicies() {
        return DiscountPolicies.getAllDescriptors().stream()
            .map(DiscountPolicyResponse::from)
            .toList();
    }

    @PostMapping("/sales/preview")
    @Operation(summary = "Tính thử giỏ hàng (tạm tính, giảm giá, tổng thanh toán)")
    public SalesManager.SalePreviewResult previewSale(@Valid @RequestBody SalePreviewRequest request) {
        List<SalesManager.SaleItemCommand> commands = request.items().stream()
            .map(i -> new SalesManager.SaleItemCommand(i.productId(), i.quantity()))
            .toList();
        return salesManager.previewSale(commands, request.discountPolicy());
    }

    @PostMapping("/sales/checkout")
    @Operation(summary = "Thanh toán và tạo hóa đơn nguyên tử (trừ tồn kho)")
    public ResponseEntity<InvoiceResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        List<SalesManager.SaleItemCommand> commands = request.items().stream()
            .map(i -> new SalesManager.SaleItemCommand(i.productId(), i.quantity()))
            .toList();
        Invoice invoice = salesManager.checkout(commands, request.discountPolicy(), request.amountPaid());
        Map<String, String> productNames = productRepository.findAll().stream()
            .collect(Collectors.toMap(Product::getProductId, Product::getName, (a, b) -> a));
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(InvoiceResponse.from(invoice, productNames, request.amountPaid()));
    }
}
