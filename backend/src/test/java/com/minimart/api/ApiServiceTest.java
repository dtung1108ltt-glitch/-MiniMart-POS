package com.minimart.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Kiểm thử đơn vị cho ApiService với HttpClient giả (Mock), không phụ thuộc kết nối mạng thật.
 * Đáp ứng Ca 10 & Ca 11 trong bảng kiểm thử docx mục 4.4.
 */
class ApiServiceTest {

    private HttpClient mockHttpClient;
    private HttpResponse<String> mockResponse;
    private ApiService apiService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        mockHttpClient = mock(HttpClient.class);
        mockResponse = (HttpResponse<String>) mock(HttpResponse.class);
        apiService = new ApiService(
            mockHttpClient,
            "https://dummyjson.com",
            new BigDecimal("25000"),
            new ObjectMapper()
        );
    }

    @Test
    @DisplayName("Ca 10 (docx 4.4): API phản hồi 200 OK -> Nhận và chuẩn hóa dữ liệu sản phẩm thành công")
    void testFetchExternalProductsSuccess200() throws Exception {
        String sampleJson = """
            {
              "products": [
                {
                  "id": 1,
                  "title": "Essence Mascara",
                  "category": "beauty",
                  "brand": "Essence",
                  "price": 10.0,
                  "stock": 15,
                  "thumbnail": "https://cdn.dummyjson.com/1.png"
                },
                {
                  "id": 2,
                  "title": "Organic Apple Juice",
                  "category": "groceries",
                  "brand": "FreshFarm",
                  "price": 2.0,
                  "stock": 40,
                  "thumbnail": "https://cdn.dummyjson.com/2.png"
                }
              ]
            }
            """;

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(sampleJson);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(mockResponse);

        List<ExternalProductDto> products = apiService.fetchExternalProducts(10, null);
        assertEquals(2, products.size());
        assertEquals("API001", products.get(0).suggestedProductId());
        assertEquals(new BigDecimal("250000"), products.get(0).priceVnd());
        assertEquals("HOUSEHOLD", products.get(0).mappedCategoryCode());
        assertEquals("FOOD", products.get(1).mappedCategoryCode());
        assertEquals(new BigDecimal("50000"), products.get(1).priceVnd());
    }

    @Test
    @DisplayName("API trả về mã lỗi HTTP 500 -> Ném IOException với thông báo mã lỗi rõ ràng")
    void testFetchExternalProductsHttpError() throws Exception {
        when(mockResponse.statusCode()).thenReturn(500);
        when(mockResponse.body()).thenReturn("Internal Error");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(mockResponse);

        IOException ex = assertThrows(IOException.class, () -> apiService.fetchExternalProducts(5, null));
        assertTrue(ex.getMessage().contains("API trả về mã lỗi: 500"));
    }

    @Test
    @DisplayName("Ca 11 (docx 4.4): Không có kết nối mạng / Timeout -> Ném IOException / HttpTimeoutException")
    void testFetchExternalProductsNetworkDisconnect() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenThrow(new HttpTimeoutException("Connection timed out"));

        assertThrows(HttpTimeoutException.class, () -> apiService.fetchExternalProducts(5, null));
    }

    @Test
    @DisplayName("API trả về JSON sai định dạng -> Ném IOException thông báo lỗi JSON")
    void testFetchExternalProductsMalformedJson() throws Exception {
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{not-valid-json");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(mockResponse);

        IOException ex = assertThrows(IOException.class, () -> apiService.fetchExternalProducts(5, null));
        assertTrue(ex.getMessage().contains("JSON"));
    }
}
