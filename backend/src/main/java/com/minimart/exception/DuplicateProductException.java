package com.minimart.exception;

public class DuplicateProductException extends RuntimeException {
    private final String productId;

    public DuplicateProductException(String productId) {
        super("Mã sản phẩm '" + productId + "' đã tồn tại trong hệ thống");
        this.productId = productId;
    }

    public String getProductId() {
        return productId;
    }
}
