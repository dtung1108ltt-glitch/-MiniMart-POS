package com.minimart.web;

import com.minimart.api.ApiService;
import com.minimart.api.ExternalProductDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/external")
@Tag(name = "External API", description = "Tích hợp API ngoài dummyjson.com qua ApiService (mục 3.7 docx)")
public class ExternalController {

    private final ApiService apiService;

    public ExternalController(ApiService apiService) {
        this.apiService = apiService;
    }

    @GetMapping("/products")
    @Operation(summary = "Lấy danh sách sản phẩm tham khảo từ dummyjson.com đã chuẩn hóa sang VND")
    public List<ExternalProductDto> getExternalProducts(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String q) throws IOException, InterruptedException {
        return apiService.fetchExternalProducts(limit, q);
    }
}
