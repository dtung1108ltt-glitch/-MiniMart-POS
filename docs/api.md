# Đặc tả REST API — MiniMart POS Backend

- **Base URL (Local):** `http://localhost:8080`
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`
- **Định dạng lỗi thống nhất:** Chuẩn RFC 7807 `ProblemDetail` (`application/problem+json`) với thông báo tiếng Việt, không lộ stack trace.

---

## 1. Quản lý Sản phẩm (`/api/products`)

### 1.1. Danh sách & Tìm kiếm sản phẩm
`GET /api/products?q=&category=&lowStock=false&expiringSoon=false&page=0&size=20&sort=productId,asc`

**Response `200 OK`:**
```json
{
  "content": [
    {
      "productId": "SP01",
      "name": "Sữa tươi tiệt trùng Vinamilk 1L",
      "price": 32000,
      "quantity": 45,
      "category": "FOOD",
      "categoryName": "Thực phẩm",
      "expiryDate": "2026-12-08",
      "brand": null,
      "expired": false,
      "expiringSoon": false,
      "lowStock": false,
      "version": 0
    }
  ],
  "totalElements": 14,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

### 1.2. Thêm sản phẩm mới
`POST /api/products`

**Request Body:**
```json
{
  "productId": "SP15",
  "name": "Nước ngọt Coca-Cola 1.5L",
  "price": 22000,
  "quantity": 30,
  "category": "FOOD",
  "expiryDate": "2027-05-01",
  "brand": null
}
```

**Response `201 Created`:** Trả về `ProductResponse`.
**Response `400 Bad Request` (Mã rỗng / Giá âm / Sai định dạng):**
```json
{
  "type": "about:blank",
  "title": "Dữ liệu không hợp lệ",
  "status": 400,
  "detail": "Mã sản phẩm không được để trống",
  "errors": [
    {
      "field": "productId",
      "message": "Mã sản phẩm không được để trống"
    }
  ]
}
```
**Response `409 Conflict` (Trùng mã sản phẩm):**
```json
{
  "type": "about:blank",
  "title": "Xung đột dữ liệu nghiệp vụ",
  "status": 409,
  "detail": "Mã sản phẩm 'SP01' đã tồn tại trong hệ thống"
}
```

### 1.3. Sửa sản phẩm
`PUT /api/products/{id}`

### 1.4. Xóa sản phẩm
`DELETE /api/products/{id}` → `204 No Content`.

---

## 2. Bán hàng POS & Chính sách Giảm giá (`/api/sales`, `/api/discount-policies`)

### 2.1. Danh sách chính sách giảm giá (Lambda)
`GET /api/discount-policies`

**Response `200 OK`:**
```json
[
  {
    "code": "NONE",
    "name": "Không giảm giá",
    "description": "Áp dụng nguyên giá niêm yết"
  },
  {
    "code": "PERCENT5_OVER_500K",
    "name": "Giảm 5% (Đơn ≥ 500.000đ)",
    "description": "Giảm 5% tổng hóa đơn khi tạm tính từ 500.000đ trở lên"
  },
  {
    "code": "PERCENT10_OVER_1M",
    "name": "Giảm 10% (Đơn ≥ 1.000.000đ)",
    "description": "Giảm 10% tổng hóa đơn khi tạm tính từ 1.000.000đ trở lên"
  },
  {
    "code": "FIXED_20K",
    "name": "Giảm trực tiếp 20.000đ",
    "description": "Giảm cố định 20.000đ trên hóa đơn (tối đa bằng tổng tiền hàng)"
  }
]
```

### 2.2. Tính thử hóa đơn (Preview)
`POST /api/sales/preview`

**Request Body:**
```json
{
  "items": [
    { "productId": "SP01", "quantity": 2 }
  ],
  "discountPolicy": "NONE"
}
```

**Response `200 OK`:**
```json
{
  "items": [
    {
      "productId": "SP01",
      "productName": "Sữa tươi tiệt trùng Vinamilk 1L",
      "category": "Thực phẩm",
      "quantity": 2,
      "unitPrice": 32000,
      "subtotal": 64000
    }
  ],
  "subtotal": 64000,
  "discountPolicy": "NONE",
  "discount": 0,
  "totalAmount": 64000
}
```

### 2.3. Thanh toán & Lập hóa đơn nguyên tử (Checkout)
`POST /api/sales/checkout`

**Request Body:**
```json
{
  "items": [
    { "productId": "SP01", "quantity": 2 }
  ],
  "discountPolicy": "NONE",
  "amountPaid": 100000
}
```

**Response `201 Created`:**
```json
{
  "invoiceId": "HD261024093000101",
  "invoiceDate": "2026-10-24T09:30:00",
  "status": "COMPLETED",
  "subtotal": 64000,
  "discount": 0,
  "totalAmount": 64000,
  "amountPaid": 100000,
  "changeAmount": 36000,
  "items": [
    {
      "detailId": 25,
      "productId": "SP01",
      "productName": "Sữa tươi tiệt trùng Vinamilk 1L",
      "quantity": 2,
      "unitPrice": 32000,
      "subtotal": 64000
    }
  ]
}
```

---

## 3. Hóa đơn (`/api/invoices`)

- `GET /api/invoices?from=2026-10-01&to=2026-10-24&status=COMPLETED&page=0&size=20`
- `GET /api/invoices/{id}`
- `POST /api/invoices/{id}/cancel` — Hủy hóa đơn và **hoàn trả tồn kho** (nếu đã hủy trước đó sẽ trả về `409 Conflict`).

---

## 4. Thống kê Doanh thu (`/api/stats`)

> **Quy tắc BR7 (docx mục 2.3.4):** Chỉ tổng hợp doanh thu từ các hóa đơn có trạng thái `COMPLETED`, loại trừ hoàn toàn các hóa đơn `CANCELLED`.

- `GET /api/stats/summary?from=&to=`
- `GET /api/stats/daily?from=&to=`
- `GET /api/stats/by-category?from=&to=`
- `GET /api/stats/top-products?from=&to=&limit=5`

---

## 5. Tích hợp API ngoài (`/api/external/products`)

- `GET /api/external/products?limit=20&q=`
- Gọi `https://dummyjson.com/products` thông qua `ApiService` (`java.net.http.HttpClient`), chuẩn hóa danh mục và quy đổi USD sang VND theo tỷ giá `APP_USD_VND_RATE` (mặc định `25000`).
- Nếu mất kết nối mạng hoặc dịch vụ trả về mã ngoài `200..299`, trả về `502 Bad Gateway` / `504 Gateway Timeout` chuẩn `ProblemDetail`.
