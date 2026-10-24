package com.minimart.exception;

public class InvoiceNotFoundException extends RuntimeException {
    private final String invoiceId;

    public InvoiceNotFoundException(String invoiceId) {
        super("Không tìm thấy hóa đơn với mã '" + invoiceId + "'");
        this.invoiceId = invoiceId;
    }

    public String getInvoiceId() {
        return invoiceId;
    }
}
