package com.minimart.web;

import com.minimart.exception.DemoReadonlyException;
import com.minimart.exception.DuplicateProductException;
import com.minimart.exception.InsufficientStockException;
import com.minimart.exception.InvalidDiscountException;
import com.minimart.exception.InvoiceNotFoundException;
import com.minimart.exception.ProductNotFoundException;
import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Map;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Bộ xử lý ngoại lệ tập trung chuẩn RFC 7807 ProblemDetail với thông báo tiếng Việt, không lộ stack trace.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    public record FieldValidationError(String field, String message) {}

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Dữ liệu đầu vào không hợp lệ, vui lòng kiểm tra lại các trường thông tin"
        );
        pd.setTitle("Dữ liệu không hợp lệ");
        List<FieldValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> new FieldValidationError(fe.getField(), fe.getDefaultMessage()))
            .toList();
        pd.setProperty("errors", errors);
        if (!errors.isEmpty()) {
            pd.setDetail(errors.get(0).message());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler({
        IllegalArgumentException.class,
        NumberFormatException.class,
        InvalidDiscountException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ProblemDetail> handleBadRequest(RuntimeException ex) {
        String detail = ex instanceof NumberFormatException
            ? "Định dạng số không hợp lệ, vui lòng nhập giá và số lượng bằng số"
            : ex.getMessage();
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setTitle("Yêu cầu không hợp lệ");
        pd.setProperty("errors", List.of());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableJson(HttpMessageNotReadableException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Dữ liệu gửi lên không đúng định dạng (ví dụ: nhập chữ vào ô giá hoặc số lượng)"
        );
        pd.setTitle("Định dạng dữ liệu không hợp lệ");
        pd.setProperty("errors", List.of(
            Map.of("field", "body", "message", "Giá bán và số lượng phải là kiểu số hợp lệ")
        ));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler({ProductNotFoundException.class, InvoiceNotFoundException.class})
    public ResponseEntity<ProblemDetail> handleNotFound(RuntimeException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Không tìm thấy dữ liệu");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler({
        DuplicateProductException.class,
        InsufficientStockException.class,
        IllegalStateException.class
    })
    public ResponseEntity<ProblemDetail> handleConflict(RuntimeException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Xung đột dữ liệu nghiệp vụ");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(OptimisticLockingFailureException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "Dữ liệu sản phẩm vừa được giao dịch khác cập nhật đồng thời. Vui lòng thử lại."
        );
        pd.setTitle("Xung đột giao dịch đồng thời");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(DemoReadonlyException.class)
    public ResponseEntity<ProblemDetail> handleDemoReadonly(DemoReadonlyException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setTitle("Thao tác bị khóa ở chế độ Demo");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
    }

    @ExceptionHandler(HttpTimeoutException.class)
    public ResponseEntity<ProblemDetail> handleExternalTimeout(HttpTimeoutException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.GATEWAY_TIMEOUT,
            "Quá thời gian chờ phản hồi từ dịch vụ API bên ngoài"
        );
        pd.setTitle("Lỗi kết nối API ngoài (Timeout)");
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(pd);
    }

    @ExceptionHandler({IOException.class, InterruptedException.class})
    public ResponseEntity<ProblemDetail> handleExternalApiError(Exception ex) {
        if (ex instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_GATEWAY,
            "Không thể kết nối hoặc nhận dữ liệu hợp lệ từ API bên ngoài: " + ex.getMessage()
        );
        pd.setTitle("Lỗi dịch vụ API bên ngoài");
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(pd);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Đã xảy ra lỗi hệ thống ngoài dự kiến. Vui lòng thử lại sau."
        );
        pd.setTitle("Lỗi máy chủ nội bộ");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
    }
}
