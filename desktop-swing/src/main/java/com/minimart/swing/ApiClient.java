package com.minimart.swing;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Lớp ApiClient sử dụng java.net.http.HttpClient để gọi REST API của Backend Spring Boot.
 * Cấu hình địa chỉ từ tệp app.properties hoặc biến môi trường BACKEND_URL.
 */
public class ApiClient {

    public record ProductItem(
        String productId,
        String name,
        long price,
        int quantity,
        String category,
        String categoryName,
        String expiryDate,
        String brand,
        boolean expired,
        boolean lowStock
    ) {}

    public record CartLine(String productId, String name, long unitPrice, int quantity) {
        public long subtotal() {
            return unitPrice * quantity;
        }
    }

    public record PreviewResult(long subtotal, long discount, long totalAmount) {}

    public record DailyPoint(String date, long revenue, long invoiceCount) {}

    public record StatsOverview(
        long totalRevenue,
        long completedInvoices,
        long cancelledInvoices,
        long averageOrderValue,
        long totalDiscount,
        List<DailyPoint> dailyPoints
    ) {}

    private final HttpClient httpClient;
    private final String baseUrl;
    private final Gson gson = new Gson();

    public ApiClient() {
        this(loadConfiguredBaseUrl(), HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }

    public ApiClient(String baseUrl, HttpClient httpClient) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.httpClient = httpClient;
    }

    private static String loadConfiguredBaseUrl() {
        String envUrl = System.getenv("BACKEND_URL");
        if (envUrl != null && !envUrl.isBlank()) {
            return envUrl.trim();
        }
        Properties props = new Properties();
        try (InputStream in = ApiClient.class.getResourceAsStream("/app.properties")) {
            if (in != null) {
                props.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException ignored) {
        }
        return props.getProperty("backend.url", "http://localhost:8080").trim();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public List<ProductItem> fetchProducts(String query) throws IOException, InterruptedException {
        String url = baseUrl + "/api/products?size=200";
        if (query != null && !query.isBlank()) {
            url += "&q=" + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
        }
        String body = sendRequest("GET", url, null);
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        JsonArray content = root.getAsJsonArray("content");
        List<ProductItem> list = new ArrayList<>();
        for (JsonElement el : content) {
            JsonObject obj = el.getAsJsonObject();
            list.add(parseProduct(obj));
        }
        return list;
    }

    public ProductItem createProduct(
            String id,
            String name,
            long price,
            int quantity,
            String category,
            String extra) throws IOException, InterruptedException {
        JsonObject payload = new JsonObject();
        payload.addProperty("productId", id);
        payload.addProperty("name", name);
        payload.addProperty("price", price);
        payload.addProperty("quantity", quantity);
        payload.addProperty("category", category);
        if ("FOOD".equalsIgnoreCase(category) && extra != null && !extra.isBlank()) {
            payload.addProperty("expiryDate", extra.trim());
        } else if ("HOUSEHOLD".equalsIgnoreCase(category) && extra != null && !extra.isBlank()) {
            payload.addProperty("brand", extra.trim());
        }
        String res = sendRequest("POST", baseUrl + "/api/products", gson.toJson(payload));
        return parseProduct(JsonParser.parseString(res).getAsJsonObject());
    }

    public ProductItem updateProduct(
            String id,
            String name,
            long price,
            int quantity,
            String category,
            String extra) throws IOException, InterruptedException {
        JsonObject payload = new JsonObject();
        payload.addProperty("name", name);
        payload.addProperty("price", price);
        payload.addProperty("quantity", quantity);
        payload.addProperty("category", category);
        if ("FOOD".equalsIgnoreCase(category) && extra != null && !extra.isBlank()) {
            payload.addProperty("expiryDate", extra.trim());
        } else if ("HOUSEHOLD".equalsIgnoreCase(category) && extra != null && !extra.isBlank()) {
            payload.addProperty("brand", extra.trim());
        }
        String res = sendRequest(
            "PUT",
            baseUrl + "/api/products/" + URLEncoder.encode(id, StandardCharsets.UTF_8),
            gson.toJson(payload)
        );
        return parseProduct(JsonParser.parseString(res).getAsJsonObject());
    }

    public void deleteProduct(String id) throws IOException, InterruptedException {
        sendRequest("DELETE", baseUrl + "/api/products/" + URLEncoder.encode(id, StandardCharsets.UTF_8), null);
    }

    public PreviewResult previewSale(List<CartLine> cart, String discountPolicy) throws IOException, InterruptedException {
        JsonObject payload = buildSalePayload(cart, discountPolicy, null);
        String res = sendRequest("POST", baseUrl + "/api/sales/preview", gson.toJson(payload));
        JsonObject obj = JsonParser.parseString(res).getAsJsonObject();
        return new PreviewResult(
            obj.get("subtotal").getAsLong(),
            obj.get("discount").getAsLong(),
            obj.get("totalAmount").getAsLong()
        );
    }

    public String checkout(List<CartLine> cart, String discountPolicy, Long amountPaid) throws IOException, InterruptedException {
        JsonObject payload = buildSalePayload(cart, discountPolicy, amountPaid);
        String res = sendRequest("POST", baseUrl + "/api/sales/checkout", gson.toJson(payload));
        JsonObject obj = JsonParser.parseString(res).getAsJsonObject();
        return obj.get("invoiceId").getAsString();
    }

    public StatsOverview fetchStatsOverview() throws IOException, InterruptedException {
        String sumBody = sendRequest("GET", baseUrl + "/api/stats/summary", null);
        String dailyBody = sendRequest("GET", baseUrl + "/api/stats/daily", null);

        JsonObject sumObj = JsonParser.parseString(sumBody).getAsJsonObject();
        JsonArray dailyArr = JsonParser.parseString(dailyBody).getAsJsonArray();
        List<DailyPoint> points = new ArrayList<>();
        for (JsonElement el : dailyArr) {
            JsonObject d = el.getAsJsonObject();
            points.add(new DailyPoint(
                d.get("date").getAsString(),
                d.get("revenue").getAsLong(),
                d.get("invoiceCount").getAsLong()
            ));
        }
        return new StatsOverview(
            sumObj.get("totalRevenue").getAsLong(),
            sumObj.get("completedInvoices").getAsLong(),
            sumObj.get("cancelledInvoices").getAsLong(),
            sumObj.get("averageOrderValue").getAsLong(),
            sumObj.get("totalDiscount").getAsLong(),
            points
        );
    }

    public String fetchExternalSampleSummary() throws IOException, InterruptedException {
        String body = sendRequest("GET", baseUrl + "/api/external/products?limit=5", null);
        JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
        StringBuilder sb = new StringBuilder("Kết quả truy vấn API ngoài (dummyjson.com qua Backend):\n\n");
        for (JsonElement el : arr) {
            JsonObject o = el.getAsJsonObject();
            sb.append("• ")
                .append(o.get("suggestedProductId").getAsString())
                .append(" — ")
                .append(o.get("title").getAsString())
                .append(" (")
                .append(String.format("%,d đ", o.get("priceVnd").getAsLong()))
                .append(")\n");
        }
        return sb.toString();
    }

    private JsonObject buildSalePayload(List<CartLine> cart, String discountPolicy, Long amountPaid) {
        JsonObject payload = new JsonObject();
        JsonArray items = new JsonArray();
        for (CartLine line : cart) {
            JsonObject item = new JsonObject();
            item.addProperty("productId", line.productId());
            item.addProperty("quantity", line.quantity());
            items.add(item);
        }
        payload.add("items", items);
        payload.addProperty("discountPolicy", discountPolicy);
        if (amountPaid != null) {
            payload.addProperty("amountPaid", amountPaid);
        }
        return payload;
    }

    private ProductItem parseProduct(JsonObject obj) {
        return new ProductItem(
            obj.get("productId").getAsString(),
            obj.get("name").getAsString(),
            obj.get("price").getAsLong(),
            obj.get("quantity").getAsInt(),
            obj.get("category").getAsString(),
            obj.get("categoryName").getAsString(),
            obj.has("expiryDate") && !obj.get("expiryDate").isJsonNull() ? obj.get("expiryDate").getAsString() : "",
            obj.has("brand") && !obj.get("brand").isJsonNull() ? obj.get("brand").getAsString() : "",
            obj.has("expired") && obj.get("expired").getAsBoolean(),
            obj.has("lowStock") && obj.get("lowStock").getAsBoolean()
        );
    }

    private String sendRequest(String method, String url, String jsonBody) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json");

        switch (method) {
            case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody != null ? jsonBody : "{}"));
            case "PUT" -> builder.PUT(HttpRequest.BodyPublishers.ofString(jsonBody != null ? jsonBody : "{}"));
            case "DELETE" -> builder.DELETE();
            default -> builder.GET();
        }

        try {
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String errMsg = "Mã lỗi HTTP " + response.statusCode();
                try {
                    JsonObject errJson = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (errJson.has("detail")) {
                        errMsg = errJson.get("detail").getAsString();
                    }
                } catch (Exception ignored) {
                }
                throw new IOException(errMsg);
            }
            return response.body();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw ex;
        }
    }
}
