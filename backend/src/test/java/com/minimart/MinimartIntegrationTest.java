package com.minimart;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimart.api.ApiService;
import com.minimart.api.ExternalProductDto;
import com.minimart.domain.DiscountPolicies;
import com.minimart.domain.FoodProduct;
import com.minimart.domain.Product;
import com.minimart.repository.InvoiceRepository;
import com.minimart.repository.ProductRepository;
import com.minimart.service.SalesManager;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Kiểm thử tích hợp toàn diện cho 12 ca kiểm thử trong docx (mục 4.4) và các quy tắc BR1–BR11.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.seed-enabled=false",
    "app.demo-readonly=false"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MinimartIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private SalesManager salesManager;

    @SpyBean
    private ApiService apiService;

    @BeforeEach
    void cleanDatabase() {
        invoiceRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("Ca 1 (docx 4.4) & BR1: Thêm sản phẩm hợp lệ SP01, giá 30000, SL 20 -> 201 Created; trùng mã -> 409")
    void testCase01_AddProductSuccessAndDuplicateCheck() throws Exception {
        String payload = """
            {
              "productId": "SP01",
              "name": "Sữa tươi Vinamilk",
              "price": 30000,
              "quantity": 20,
              "category": "FOOD",
              "expiryDate": "%s"
            }
            """.formatted(LocalDate.now().plusDays(30));

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.productId", is("SP01")))
            .andExpect(jsonPath("$.price", is(30000)))
            .andExpect(jsonPath("$.quantity", is(20)))
            .andExpect(jsonPath("$.categoryName", is("Thực phẩm")));

        // Thử thêm trùng mã SP01 -> 409 Conflict (BR1)
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail", containsString("SP01")));
    }

    @Test
    @Order(2)
    @DisplayName("Ca 2 (docx 4.4) & BR1: Thêm sản phẩm với mã bị trống -> 400 Bad Request + thông báo tiếng Việt")
    void testCase02_AddProductEmptyId() throws Exception {
        String payload = """
            {
              "productId": "   ",
              "name": "Sữa tươi",
              "price": 30000,
              "quantity": 20,
              "category": "FOOD"
            }
            """;

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail", containsString("Mã sản phẩm không được để trống")));
    }

    @Test
    @Order(3)
    @DisplayName("Ca 3 (docx 4.4): Thêm sản phẩm với giá nhập bằng chữ -> 400 Bad Request (không bị 500)")
    void testCase03_AddProductNonNumericPrice() throws Exception {
        String invalidJson = """
            {
              "productId": "SP01",
              "name": "Sữa tươi",
              "price": "ba_muoi_nghin",
              "quantity": 20,
              "category": "FOOD"
            }
            """;

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @Order(4)
    @DisplayName("Ca 4 (docx 4.4) & BR2: Thêm sản phẩm với giá âm -> 400 Bad Request")
    void testCase04_AddProductNegativePrice() throws Exception {
        String payload = """
            {
              "productId": "SP01",
              "name": "Sữa tươi",
              "price": -5000,
              "quantity": 20,
              "category": "FOOD"
            }
            """;

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail", containsString("không được âm")));
    }

    @Test
    @Order(5)
    @DisplayName("Ca 5 (docx 4.4): Tìm kiếm sản phẩm theo từ khóa trùng tên (không phân biệt hoa thường)")
    void testCase05_SearchProductsByName() throws Exception {
        productRepository.save(new FoodProduct("SP01", "Sữa tươi Vinamilk", new BigDecimal("30000"), 20, LocalDate.now().plusDays(15)));
        productRepository.save(new FoodProduct("SP02", "Bánh quy bơ", new BigDecimal("45000"), 15, LocalDate.now().plusDays(60)));

        mockMvc.perform(get("/api/products").param("q", "vinamilk"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].productId", is("SP01")));
    }

    @Test
    @Order(6)
    @DisplayName("Ca 6, 8, 9 (docx 4.4) & BR6, BR10: Bán hàng SL < tồn, tính tiền 30000x2=60000, giảm 5% đơn 600000=30000")
    void testCase06_08_09_PreviewAndCheckoutSuccess() throws Exception {
        productRepository.save(new FoodProduct("SP01", "Sữa tươi", new BigDecimal("30000"), 25, LocalDate.now().plusDays(30)));

        // Ca 8: Preview 30000 x 2 = 60000
        String preview2Items = """
            {
              "items": [{ "productId": "SP01", "quantity": 2 }],
              "discountPolicy": "NONE"
            }
            """;
        mockMvc.perform(post("/api/sales/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(preview2Items))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subtotal", is(60000)))
            .andExpect(jsonPath("$.discount", is(0)))
            .andExpect(jsonPath("$.totalAmount", is(60000)));

        // Ca 9 & Ca 6: Checkout 20 hộp x 30000 = 600000, giảm 5% = 30000, còn 570000, tồn kho giảm từ 25 -> 5
        String checkoutPayload = """
            {
              "items": [{ "productId": "SP01", "quantity": 20 }],
              "discountPolicy": "PERCENT5_OVER_500K",
              "amountPaid": 600000
            }
            """;
        mockMvc.perform(post("/api/sales/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkoutPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.subtotal", is(600000)))
            .andExpect(jsonPath("$.discount", is(30000)))
            .andExpect(jsonPath("$.totalAmount", is(570000)))
            .andExpect(jsonPath("$.changeAmount", is(30000)))
            .andExpect(jsonPath("$.status", is("COMPLETED")));

        Product updated = productRepository.findById("SP01").orElseThrow();
        assertEquals(5, updated.getQuantity());
    }

    @Test
    @Order(7)
    @DisplayName("Ca 7 (docx 4.4) & BR3: Bán hàng SL > tồn kho -> 409 Conflict và tồn kho KHÔNG đổi")
    void testCase07_CheckoutInsufficientStock() throws Exception {
        productRepository.save(new FoodProduct("SP01", "Sữa tươi", new BigDecimal("30000"), 10, LocalDate.now().plusDays(30)));

        String checkoutPayload = """
            {
              "items": [{ "productId": "SP01", "quantity": 15 }],
              "discountPolicy": "NONE"
            }
            """;

        mockMvc.perform(post("/api/sales/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkoutPayload))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail", containsString("không đủ tồn kho")));

        Product afterFailedSale = productRepository.findById("SP01").orElseThrow();
        assertEquals(10, afterFailedSale.getQuantity());
    }

    @Test
    @Order(8)
    @DisplayName("Ca 10 & Ca 11 (docx 4.4): Endpoint /api/external/products khi thành công (200) và khi mất mạng (502)")
    void testCase10_11_ExternalApiEndpoint() throws Exception {
        List<ExternalProductDto> mockList = List.of(
            new ExternalProductDto("1", "API001", "Sản phẩm mẫu API", "groceries",
                "FOOD", "Thực phẩm", "BrandX", 2.0, new BigDecimal("50000"), 12, "")
        );
        when(apiService.fetchExternalProducts(anyInt(), anyString())).thenReturn(mockList);

        mockMvc.perform(get("/api/external/products").param("limit", "5").param("q", "apple"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].suggestedProductId", is("API001")));

        // Giả lập mất mạng -> 502 Bad Gateway
        when(apiService.fetchExternalProducts(anyInt(), anyString()))
            .thenThrow(new IOException("Không có kết nối mạng tới dummyjson.com"));

        mockMvc.perform(get("/api/external/products").param("limit", "5").param("q", "error"))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.detail", containsString("Không có kết nối mạng")));
    }

    @Test
    @Order(9)
    @DisplayName("Ca 12 (docx 4.4) & BR7, BR8: Hủy hóa đơn hoàn kho, hủy lần 2 trả 409, thống kê doanh thu loại trừ hóa đơn hủy")
    void testCase12_CancelInvoiceAndRevenueExcludesCancelled() throws Exception {
        productRepository.save(new FoodProduct("SP01", "Sữa tươi", new BigDecimal("30000"), 20, LocalDate.now().plusDays(30)));

        // Tạo hóa đơn 1 (2 hộp = 60.000) -> giữ COMPLETED
        String req1 = """
            { "items": [{ "productId": "SP01", "quantity": 2 }], "discountPolicy": "NONE" }
            """;
        mockMvc.perform(post("/api/sales/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req1))
            .andExpect(status().isCreated());

        // Tạo hóa đơn 2 (3 hộp = 90.000) -> sau đó hủy
        String req2 = """
            { "items": [{ "productId": "SP01", "quantity": 3 }], "discountPolicy": "NONE" }
            """;
        MvcResult res2 = mockMvc.perform(post("/api/sales/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req2))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode inv2Node = objectMapper.readTree(res2.getResponse().getContentAsString());
        String invoice2Id = inv2Node.get("invoiceId").asText();

        // Trước khi hủy: tồn kho = 20 - 2 - 3 = 15
        assertEquals(15, productRepository.findById("SP01").orElseThrow().getQuantity());

        // Hủy hóa đơn 2 -> tồn kho hoàn lại 15 + 3 = 18
        mockMvc.perform(post("/api/invoices/" + invoice2Id + "/cancel"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("CANCELLED")));

        assertEquals(18, productRepository.findById("SP01").orElseThrow().getQuantity());

        // BR8: Hủy lần hai trả 409 Conflict
        mockMvc.perform(post("/api/invoices/" + invoice2Id + "/cancel"))
            .andExpect(status().isConflict());

        // Ca 12 & BR7: Thống kê doanh thu chỉ tính hóa đơn 1 (60.000), không tính hóa đơn 2 đã hủy
        mockMvc.perform(get("/api/stats/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRevenue", is(60000)))
            .andExpect(jsonPath("$.completedInvoices", is(1)))
            .andExpect(jsonPath("$.cancelledInvoices", is(1)));
    }

    @Test
    @Order(10)
    @DisplayName("BR9: Thực phẩm đã hết hạn sử dụng bị chặn không cho bán (409 Conflict)")
    void testExpiredFoodProductBlockedFromSale() throws Exception {
        productRepository.save(new FoodProduct("SP_EXP", "Sữa chua hết hạn", new BigDecimal("25000"), 10, LocalDate.now().minusDays(1)));

        String payload = """
            { "items": [{ "productId": "SP_EXP", "quantity": 1 }], "discountPolicy": "NONE" }
            """;

        mockMvc.perform(post("/api/sales/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail", containsString("hết hạn")));
    }

    @Test
    @Order(11)
    @DisplayName("BR11: Hai yêu cầu checkout đồng thời cùng mua 4 sản phẩm khi tồn kho chỉ có 5 -> Chỉ 1 thành công, tồn không âm")
    void testConcurrentCheckoutDoesNotOversell() throws Exception {
        productRepository.saveAndFlush(
            new FoodProduct("SP_CONC", "Gạo ST25", new BigDecimal("180000"), 5, LocalDate.now().plusDays(90))
        );

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                readyLatch.countDown();
                startLatch.await();
                try {
                    salesManager.checkout(
                        List.of(new SalesManager.SaleItemCommand("SP_CONC", 4)),
                        DiscountPolicies.NONE,
                        null
                    );
                    successCount.incrementAndGet();
                } catch (Exception ex) {
                    conflictCount.incrementAndGet();
                }
                return null;
            });
        }

        List<Future<Void>> futures = new ArrayList<>();
        for (Callable<Void> task : tasks) {
            futures.add(executor.submit(task));
        }
        readyLatch.await();
        startLatch.countDown();
        for (Future<Void> f : futures) {
            f.get();
        }
        executor.shutdown();

        assertEquals(1, successCount.get(), "Chỉ đúng 1 giao dịch được phép thành công");
        assertEquals(1, conflictCount.get(), "Giao dịch đồng thời thứ 2 phải bị từ chối");
        Product remaining = productRepository.findById("SP_CONC").orElseThrow();
        assertEquals(1, remaining.getQuantity(), "Tồn kho còn lại phải đúng bằng 1 (không bán vượt tồn)");
    }

    @Test
    @Order(12)
    @DisplayName("Kiểm tra sửa, xóa sản phẩm và Actuator Health endpoint (/actuator/health = UP)")
    void testUpdateDeleteProductAndHealthEndpoint() throws Exception {
        productRepository.save(new FoodProduct("SP99", "Trà xanh", new BigDecimal("10000"), 10, LocalDate.now().plusDays(20)));

        String updatePayload = """
            {
              "name": "Trà xanh Không Độ",
              "price": 12000,
              "quantity": 30,
              "category": "FOOD",
              "expiryDate": "%s"
            }
            """.formatted(LocalDate.now().plusDays(60));

        mockMvc.perform(put("/api/products/SP99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name", is("Trà xanh Không Độ")))
            .andExpect(jsonPath("$.price", is(12000)))
            .andExpect(jsonPath("$.quantity", is(30)));

        mockMvc.perform(delete("/api/products/SP99"))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/SP99"))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("UP")));

        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string("Minimart POS API is running"));

        mockMvc.perform(get("/duong-dan-khong-ton-tai"))
            .andExpect(status().isNotFound());

        mockMvc.perform(options("/api/products")
                .header("Origin", "https://mini-mart-pos-orpin.vercel.app")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "https://mini-mart-pos-orpin.vercel.app"));
    }
}
