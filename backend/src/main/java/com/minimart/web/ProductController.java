package com.minimart.web;

import com.minimart.domain.Product;
import com.minimart.service.ProductManager;
import com.minimart.web.dto.ProductRequest;
import com.minimart.web.dto.ProductResponse;
import com.minimart.web.dto.ProductUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Quản lý danh sách sản phẩm và tồn kho")
public class ProductController {

    private final ProductManager productManager;

    public ProductController(ProductManager productManager) {
        this.productManager = productManager;
    }

    @GetMapping
    @Operation(summary = "Danh sách sản phẩm có phân trang, tìm kiếm và lọc")
    public Page<ProductResponse> listProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean lowStock,
            @RequestParam(required = false) Boolean expiringSoon,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "productId,asc") String sort) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), parseSort(sort));
        return productManager.findProducts(q, category, lowStock, expiringSoon, pageable)
            .map(ProductResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết sản phẩm theo mã")
    public ProductResponse getProduct(@PathVariable String id) {
        return ProductResponse.from(productManager.getProductById(id));
    }

    @PostMapping
    @Operation(summary = "Thêm sản phẩm mới")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        Product product = ProductManager.createProductFromFields(
            request.productId(),
            request.name(),
            request.price(),
            request.quantity(),
            request.category(),
            request.expiryDate(),
            request.brand()
        );
        Product saved = productManager.addProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin sản phẩm")
    public ProductResponse updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductUpdateRequest request) {
        Product updated = productManager.updateProduct(
            id,
            request.name(),
            request.price(),
            request.quantity(),
            request.category(),
            request.expiryDate(),
            request.brand()
        );
        return ProductResponse.from(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa sản phẩm")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        productManager.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    private Sort parseSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Direction.ASC, "productId");
        }
        String[] parts = sortParam.split(",");
        String prop = parts[0].trim();
        Sort.Direction dir = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim()))
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;
        return Sort.by(dir, prop);
    }
}
