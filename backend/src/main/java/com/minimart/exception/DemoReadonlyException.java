package com.minimart.exception;

public class DemoReadonlyException extends RuntimeException {
    public DemoReadonlyException(String message) {
        super(message);
    }
}
