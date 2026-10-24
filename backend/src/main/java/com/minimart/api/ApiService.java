package com.minimart.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Lớp kết nối API bên ngoài bằng java.net.http.HttpClient (khớp mục 3.7 trong báo cáo docx).
 * Cho phép tiêm HttpClient và baseUrl qua constructor để phục vụ kiểm thử đơn vị (Mock).
 */
@Service
public class ApiService {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);

    private final HttpClient client;
    private final String baseUrl;
    private final BigDecimal usdVndRate;
    private final ObjectMapper objectMapper;

    @Autowired
    public ApiService(
            @Value("${app.external-api-base-url:https://dummyjson.com}") String baseUrl,
            @Value("${app.usd-vnd-rate:25000}") BigDecimal usdVndRate,
            ObjectMapper objectMapper) {
        this(
            HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build(),
            baseUrl,
            usdVndRate,
            objectMapper
        );
    }

    public ApiService(HttpClient client, String baseUrl, BigDecimal usdVndRate, ObjectMapper objectMapper) {
        this.client = client != null ? client : HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        this.baseUrl = (baseUrl != null && !baseUrl.isBlank())
            ? baseUrl.replaceAll("/+$", "")
            : "https://dummyjson.com";
        this.usdVndRate = (usdVndRate != null && usdVndRate.compareTo(BigDecimal.ZERO) > 0)
            ? usdVndRate
            : new BigDecimal("25000");
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Phương thức gốc minh họa trong mục 3.7 của báo cáo docx: GET /products/1.
     */
    public String fetchData() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/products/1"))
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("API trả về mã lỗi: " + response.statusCode());
            }
            return response.body();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw ex;
        }
    }

    /**
     * Truy vấn danh sách sản phẩm tham khảo từ dummyjson.com và chuẩn hóa sang DTO tiếng Việt / VND.
     */
    public List<ExternalProductDto> fetchExternalProducts(int limit, String query)
            throws IOException, InterruptedException {
        int safeLimit = (limit <= 0 || limit > 100) ? 20 : limit;
        String endpoint;
        if (query != null && !query.isBlank()) {
            String encodedQ = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
            endpoint = baseUrl + "/products/search?q=" + encodedQ + "&limit=" + safeLimit;
        } else {
            endpoint = baseUrl + "/products?limit=" + safeLimit;
        }

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .timeout(REQUEST_TIMEOUT)
            .header("Accept", "application/json")
            .GET()
            .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw ex;
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("API trả về mã lỗi: " + response.statusCode());
        }

        return parseProductsJson(response.body());
    }

    public List<ExternalProductDto> parseProductsJson(String jsonBody) throws IOException {
        if (jsonBody == null || jsonBody.isBlank()) {
            throw new IOException("Phản hồi từ API ngoài rỗng");
        }
        try {
            JsonNode root = objectMapper.readTree(jsonBody);
            JsonNode productsNode = root.has("products") && root.get("products").isArray()
                ? root.get("products")
                : (root.isObject() && root.has("id") ? objectMapper.createArrayNode().add(root) : null);

            if (productsNode == null) {
                throw new IOException("Cấu trúc JSON từ API ngoài không hợp lệ (thiếu trường products)");
            }

            List<ExternalProductDto> result = new ArrayList<>();
            for (JsonNode item : productsNode) {
                String extId = item.path("id").asText("");
                String title = item.path("title").asText("Sản phẩm tham khảo");
                String rawCategory = item.path("category").asText("groceries");
                String brand = item.path("brand").asText("Nhập khẩu");
                if (brand == null || brand.isBlank() || "null".equalsIgnoreCase(brand)) {
                    brand = "OEM";
                }
                double priceUsd = item.path("price").asDouble(1.0);
                if (priceUsd < 0) {
                    priceUsd = 0;
                }
                int stock = Math.max(0, item.path("stock").asInt(10));
                String thumbnail = item.path("thumbnail").asText("");

                BigDecimal priceVnd = BigDecimal.valueOf(priceUsd)
                    .multiply(usdVndRate)
                    .setScale(0, RoundingMode.HALF_UP);

                boolean isFood = rawCategory.toLowerCase(Locale.ROOT).contains("grocer")
                    || rawCategory.toLowerCase(Locale.ROOT).contains("food")
                    || rawCategory.toLowerCase(Locale.ROOT).contains("drink");

                String mappedCode = isFood ? "FOOD" : "HOUSEHOLD";
                String mappedName = isFood ? "Thực phẩm" : "Đồ gia dụng";
                String suggestedId = "API" + String.format("%03d", item.path("id").asInt(1));

                result.add(new ExternalProductDto(
                    extId,
                    suggestedId,
                    title,
                    rawCategory,
                    mappedCode,
                    mappedName,
                    brand,
                    priceUsd,
                    priceVnd,
                    stock,
                    thumbnail
                ));
            }
            return result;
        } catch (IOException ex) {
            throw new IOException("Dữ liệu JSON từ API ngoài không hợp lệ: " + ex.getMessage(), ex);
        }
    }

    public BigDecimal getUsdVndRate() {
        return usdVndRate;
    }
}
