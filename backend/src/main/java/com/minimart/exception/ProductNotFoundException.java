package com.minimart.exception;

public class ProductNotFoundException extends RuntimeException {
    private final String productId;

    public ProductNotFoundException(String productId) {
        super("Không tìm thấy sản phẩm với mã '" + productId + "'");
        this.productId = productId;
    }

    public String getProductId() {
        return productId;
    }
}
