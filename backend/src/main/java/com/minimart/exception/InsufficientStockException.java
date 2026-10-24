package com.minimart.exception;

public class InsufficientStockException extends RuntimeException {
    private final String productId;
    private final int requested;
    private final int available;

    public InsufficientStockException(String productId, int requested, int available) {
        super(String.format("Sản phẩm '%s' không đủ tồn kho (yêu cầu: %d, tồn hiện tại: %d)", productId, requested, available));
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }

    public InsufficientStockException(String message) {
        super(message);
        this.productId = null;
        this.requested = 0;
        this.available = 0;
    }

    public String getProductId() {
        return productId;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
