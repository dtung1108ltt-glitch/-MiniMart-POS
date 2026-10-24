# PROMPT GIAO VIỆC CHO AGENT (agy) — PHIÊN BẢN 2

## Hệ thống quản lý bán hàng & siêu thị mini: Backend Java (Spring Boot, Docker) + Frontend riêng trên Vercel

> **Thay đổi so với bản 1:** thay vì ứng dụng desktop Swing duy nhất, hệ thống tách thành **Backend Java REST API** (deploy Render/Railway/Fly.io/Cloud Run) và **Frontend web** (deploy Vercel, **ngôn ngữ/framework tùy chọn**). Phần Swing được giữ lại dưới dạng **module tùy chọn** để vẫn đáp ứng đề bài gốc (xem mục 0.2).
>
> **Nguồn yêu cầu:** file `tiểu_luận_lập_trình_hướng_đối_tượng.docx` (agy phải đọc kỹ trước khi code).
> **Nguồn cảm hứng giao diện:** https://keko.vn/p/phan-mem-quan-ly-ban-hang-keko-shop-new.html

---

## 0. THAM SỐ DỰ ÁN & NGUYÊN TẮC

### 0.1. Tham số (người dùng có thể đổi trước khi giao việc)

| Tham số | Giá trị mặc định | Ý nghĩa |
|---|---|---|
| `FE_STACK` | `React 18 + Vite + TypeScript + Tailwind CSS` | Ngôn ngữ/framework frontend. Có thể đổi: `Next.js`, `Vue 3 + Vite`, `SvelteKit`, `Angular`, hoặc `HTML/CSS/JS thuần`. **Nếu người dùng đã ghi giá trị khác ở đây thì dùng giá trị đó** |
| `BE_HOST` | `Render` (Web Service dùng Docker) | Nơi chạy backend. Có thể: `Railway`, `Fly.io`, `Google Cloud Run` |
| `DB` | `PostgreSQL` (Render Postgres hoặc Neon) | CSDL production. Local/test dùng H2 |
| `INCLUDE_SWING_CLIENT` | `true` | Có làm module desktop Swing gọi chung REST API hay không |
| `JAVA_VERSION` | `17` | Java LTS |

### 0.2. Vì sao có module Swing (đọc kỹ)
Đề bài gốc (docx) yêu cầu **"lập trình giao diện đồ họa Swing/JavaFX và bắt sự kiện"**, và mục 2.4, 3.x, 4.x của báo cáo mô tả `MainFrame`, `JTable`, `JOptionPane`, `addActionListener`. Giao diện web **không** thay thế được yêu cầu này. Do đó:
- Nếu `INCLUDE_SWING_CLIENT=true` → làm đủ cả web và Swing (Swing nhỏ gọn, gọi cùng REST API).
- Nếu `false` → ghi rõ trong README: *"Dự án chọn giao diện web; chưa đáp ứng tiêu chí Swing/JavaFX của đề bài gốc"*.

### 0.3. Quy tắc bắt buộc
1. **Đọc trước, code sau.** Đọc toàn bộ docx và trang Keko (nếu có công cụ duyệt web). Tóm tắt hiểu biết trong 10–15 dòng rồi mới làm.
2. **Không bịa kết quả.** Chỉ báo "đạt/đã deploy" khi đã **thực sự chạy/kiểm chứng**. Agent thường **không có tài khoản Render/Vercel** → chỉ tạo cấu hình và hướng dẫn deploy, **không tuyên bố đã deploy**. Ghi rõ điều gì đã chạy local, điều gì chưa.
3. **Bám phạm vi docx** (không thêm đa chi nhánh, thanh toán ngân hàng thật, kế toán, phân quyền chi tiết, thiết bị quét mã vạch chuyên dụng).
4. **Làm theo giai đoạn** (mục 11); mỗi giai đoạn phải build/chạy được.
5. Tên lớp/biến/endpoint bằng **tiếng Anh**; mọi chữ hiển thị cho người dùng và thông báo lỗi bằng **tiếng Việt có dấu**; mã hóa **UTF-8**.
6. Không commit bí mật (mật khẩu DB, token). Dùng biến môi trường; có `.env.example`.
7. Thứ tự ưu tiên khi mâu thuẫn: **docx > tài liệu này > cảm hứng Keko**.

---

## 1. KIẾN TRÚC TỔNG THỂ

```
 ┌──────────────────────────┐      HTTPS / JSON       ┌─────────────────────────────┐
 │  Frontend (Vercel)       │ ──────────────────────► │  Backend Spring Boot        │
 │  FE_STACK tùy chọn       │   /api/**  (rewrite     │  Docker container (Render)  │
 │  (SPA tĩnh hoặc SSR)     │    hoặc CORS)           │  Controller→Service→Repo    │
 └──────────────────────────┘                         └──────────┬──────────────────┘
 ┌──────────────────────────┐   cùng REST API                    │ JDBC
 │  Swing client (tùy chọn) │ ──────────────────────────────────►│
 └──────────────────────────┘                          ┌─────────▼─────────┐
                                                        │ PostgreSQL        │
                                         gọi API ngoài  └───────────────────┘
                     Backend ───────────────► dummyjson.com  (ApiService, đúng docx 3.7)
```

### Cấu trúc monorepo

```
minimart/
├── README.md                 # tổng quan, ảnh chụp, bảng đối chiếu docx → mã nguồn
├── DEPLOY.md                 # hướng dẫn deploy từng bước (Render + Vercel)
├── TESTING.md                # bảng kiểm thử, cột kết quả thực tế
├── docker-compose.yml        # chạy local: backend + postgres (+ frontend dev)
├── render.yaml               # Blueprint cho Render (backend + database)
├── docs/                     # class-diagram.puml, er-diagram, api.md, screenshots/
├── backend/
│   ├── pom.xml
│   ├── Dockerfile
│   ├── .env.example
│   └── src/{main,test}/...
├── frontend/                 # theo FE_STACK
│   ├── vercel.json
│   ├── .env.example
│   └── ...
└── desktop-swing/            # chỉ khi INCLUDE_SWING_CLIENT=true
    ├── pom.xml
    └── src/...
```

---

## 2. BACKEND (JAVA / SPRING BOOT)

### 2.1. Công nghệ

| Hạng mục | Quy định |
|---|---|
| Java / Build | Java 17, **Maven** (`mvnw` kèm theo) |
| Framework | **Spring Boot 3.x**: Web, Validation, Data JPA, Actuator |
| CSDL | PostgreSQL (prod) · H2 (local/test) · **Flyway** quản lý migration |
| Tài liệu API | `springdoc-openapi` (Swagger UI tại `/swagger-ui.html`) |
| Test | JUnit 5, Mockito, `MockMvc`/`@SpringBootTest` với H2 |
| Tiện ích | Không dùng Lombok bắt buộc (viết getter/setter tay để giữ rõ **đóng gói** cho báo cáo); có thể dùng `record` cho DTO |

### 2.2. Mô hình miền (giữ đúng tên docx mục 2.4)

Gói `domain` **không phụ thuộc Spring Web**; entity JPA chỉ thêm annotation lưu trữ.

| Lớp | Nội dung |
|---|---|
| `abstract class Product` | `@Entity`, `@Inheritance(SINGLE_TABLE)`, `@DiscriminatorColumn(name="category")`. Thuộc tính **private**: `productId`, `name`, `price`, `quantity`, `@Version version`. Constructor + `setPrice()`/`setQuantity()` **kiểm tra hợp lệ** → ném `IllegalArgumentException` thông điệp tiếng Việt. `abstract String getCategory()` |
| `FoodProduct extends Product` | `getCategory()` = `"Thực phẩm"`, thêm `expiryDate`, `isExpired()` |
| `HouseholdProduct extends Product` | `getCategory()` = `"Đồ gia dụng"`, thêm `brand` |
| `CartItem` | sản phẩm + số lượng + **đơn giá chụp tại thời điểm bán**, `getSubtotal()` |
| `Invoice` + `InvoiceDetail` | `invoiceId`, `invoiceDate`, `status` (`COMPLETED`/`CANCELLED`), `discount`, `totalAmount`; `addItem()`, `calculateTotal()`, `getInvoiceDate()` |
| `@FunctionalInterface DiscountPolicy` | `BigDecimal calculateDiscount(BigDecimal total)` |
| `DiscountPolicies` | Các chính sách dựng sẵn **bằng lambda** (mã → lambda): `NONE`, `PERCENT5_OVER_500K`, `PERCENT10_OVER_1M`, `FIXED_20K` |

**Tiền tệ:** dùng `BigDecimal` (scale 0, VND) trong backend để tránh sai số; JSON trả về dạng số. Ghi chú trong README rằng docx minh họa bằng `double` còn bản cài đặt dùng `BigDecimal` cho chính xác. Test vẫn phải khớp: 30.000 × 2 = **60.000**; hóa đơn 600.000 giảm 5% = **30.000**.

### 2.3. Phân tầng

```
com.minimart
 ├─ domain/       Product, FoodProduct, HouseholdProduct, CartItem, Invoice, InvoiceDetail, DiscountPolicy, DiscountPolicies
 ├─ repository/   ProductRepository, InvoiceRepository   (Spring Data JPA)
 ├─ service/      ProductManager, SalesManager, StatisticsService      (@Service, logic nghiệp vụ)
 ├─ api/          ApiService, ExternalProductDto                       (gọi API ngoài – docx 3.7)
 ├─ web/          ProductController, SalesController, InvoiceController, StatsController, ExternalController, dto/*, GlobalExceptionHandler
 ├─ exception/    InsufficientStockException, DuplicateProductException, ProductNotFoundException, InvalidDiscountException
 └─ config/       CorsConfig, JacksonConfig, OpenApiConfig, SeedData
```

- Controller **mỏng**, không chứa nghiệp vụ. DTO tách khỏi entity (không trả entity trực tiếp).
- `GlobalExceptionHandler` (`@RestControllerAdvice`) ánh xạ ngoại lệ → JSON lỗi thống nhất (RFC 7807 `ProblemDetail`) với `status`, `title`, `detail` tiếng Việt, và `errors[]` theo từng trường khi validate:

| Ngoại lệ | HTTP |
|---|---|
| `IllegalArgumentException`, `MethodArgumentNotValidException`, `NumberFormatException` | 400 |
| `ProductNotFoundException` | 404 |
| `DuplicateProductException`, `InsufficientStockException`, `ObjectOptimisticLockingFailureException` | 409 |
| Lỗi API ngoài (`IOException`, timeout, mã HTTP ≠ 2xx) | 502 / 504 |
| Còn lại | 500 (không lộ stack trace) |

### 2.4. Đặc tả REST API

Tiền tố `/api`. Phân trang dạng `?page=0&size=20&sort=name,asc`.

| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/products` | Danh sách; lọc `q` (mã/tên, không phân biệt hoa thường), `category`, `lowStock=true`, `expiringSoon=true` |
| GET | `/api/products/{id}` | Chi tiết |
| POST | `/api/products` | Thêm (body có `category`: `FOOD`/`HOUSEHOLD` + trường riêng) |
| PUT | `/api/products/{id}` | Sửa |
| DELETE | `/api/products/{id}` | Xóa |
| GET | `/api/discount-policies` | Danh sách chính sách (mã, mô tả) |
| POST | `/api/sales/preview` | Tính thử: nhận `items[]`, `discountPolicy` → trả tạm tính, giảm giá, tổng |
| POST | `/api/sales/checkout` | Tạo hóa đơn (nguyên tử); nhận `items[]`, `discountPolicy`, `amountPaid?` |
| GET | `/api/invoices` | Lọc `from`, `to`, `status` |
| GET | `/api/invoices/{id}` | Chi tiết |
| POST | `/api/invoices/{id}/cancel` | Hủy + **hoàn tồn kho** |
| GET | `/api/stats/summary?from=&to=` | Doanh thu, số hóa đơn, TB/hóa đơn, tổng giảm giá |
| GET | `/api/stats/daily?from=&to=` | Doanh thu theo ngày |
| GET | `/api/stats/by-category?from=&to=` | Theo loại sản phẩm |
| GET | `/api/stats/top-products?from=&to=&limit=5` | Bán chạy |
| GET | `/api/external/products?limit=20&q=` | **Proxy** gọi `dummyjson.com` qua `ApiService`, trả danh sách đã chuẩn hóa |
| GET | `/actuator/health` | Health check cho nền tảng deploy |

Viết `docs/api.md` (hoặc dựa vào OpenAPI) mô tả request/response mẫu.

### 2.5. Quy tắc nghiệp vụ (bắt buộc, có test)

| Mã | Quy tắc |
|---|---|
| BR1 | Mã sản phẩm không rỗng, **không trùng** |
| BR2 | Giá ≥ 0; số lượng tồn ≥ 0 |
| BR3 | Bán vượt tồn → `InsufficientStockException` (409), **không trừ kho** |
| BR4 | Số lượng bán là số nguyên > 0 |
| BR5 | Giỏ trống → từ chối thanh toán |
| BR6 | Hoàn tất → tồn giảm đúng, hóa đơn `COMPLETED` |
| BR7 | **Doanh thu chỉ tính `COMPLETED`**; loại `CANCELLED` (docx 2.3.4) |
| BR8 | Hủy hóa đơn → `CANCELLED` + hoàn kho (idempotent: hủy lần hai trả 409) |
| BR9 | Thực phẩm hết hạn: **không cho bán** |
| BR10 | Hóa đơn lưu **đơn giá tại thời điểm bán** |
| BR11 | `checkout` chạy trong **một transaction**; dùng `@Version`/khóa để **chống bán vượt khi 2 yêu cầu đồng thời** |

### 2.6. Tích hợp API ngoài (docx 3.7)
- `ApiService` dùng `java.net.http.HttpClient`, **cho phép tiêm** `HttpClient` và base URL qua constructor (để test, không gọi mạng thật).
- `GET https://dummyjson.com/products/1` (ví dụ gốc của docx) và `/products?limit=…`, `/products/search?q=…`.
- Timeout kết nối 5s, yêu cầu 8s. Mã ngoài 200–299 → ném `IOException("API trả về mã lỗi: xxx")`. Xử lý `InterruptedException` (**`Thread.currentThread().interrupt()`**), JSON sai.
- Tỷ giá USD→VND cấu hình qua `app.usd-vnd-rate` (mặc định 25000).
- Ghi chú trong tài liệu: dữ liệu API chỉ để tham khảo/nhập mẫu; hóa đơn, tồn kho, doanh thu dựa trên dữ liệu hệ thống.

### 2.7. Cấu hình & dữ liệu
- `application.yml` + profile `local` (H2), `prod` (PostgreSQL).
- `server.port: ${PORT:8080}` (Render/Cloud Run cấp `PORT`), `server.shutdown: graceful`.
- Biến môi trường: `SPRING_PROFILES_ACTIVE`, `DATABASE_URL` **hoặc** `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`, `ALLOWED_ORIGINS` (danh sách, phân tách dấu phẩy), `APP_USD_VND_RATE`, `TZ=Asia/Ho_Chi_Minh`.
- **Lưu ý Render:** biến `DATABASE_URL` của Render có dạng `postgres://user:pass@host:5432/db` (không phải `jdbc:`). Viết một lớp cấu hình nhỏ chuyển chuỗi này sang `jdbc:postgresql://…` + username/password, hoặc hướng dẫn rõ cách khai báo 3 biến `SPRING_DATASOURCE_*` trong `render.yaml`.
- `SeedData` (Flyway `R__seed` hoặc `CommandLineRunner` chỉ chạy khi bảng rỗng): ≥ 12 sản phẩm Việt Nam (có hàng tồn thấp, hàng sắp hết hạn) và ≥ 8 hóa đơn mẫu rải 14 ngày để biểu đồ có dữ liệu.
- Schema khớp docx 2.5: bảng `product(product_id, name, price, quantity, category, expiry_date, brand, version)`, `invoice(invoice_id, invoice_date, total_amount, discount, status)`, `invoice_detail(detail_id, invoice_id, product_id, quantity, unit_price, subtotal)`, có khóa ngoại.

### 2.8. CORS & bảo mật tối thiểu
- CORS chỉ cho phép các origin trong `ALLOWED_ORIGINS` (khuyến nghị dùng **rewrite của Vercel** để FE gọi `/api` cùng domain, xem mục 3.5).
- Bật `Validation` cho mọi DTO; dùng JPA/Spring Data (tham số hóa) để tránh SQL injection.
- Không có đăng nhập (ngoài phạm vi docx). **Ghi rõ trong README:** bản demo công khai không có xác thực, chỉ chứa dữ liệu mẫu. Có thể thêm cờ `DEMO_READONLY=true` để chặn `DELETE`/`cancel` trên môi trường công khai.
- Không log dữ liệu nhạy cảm; không trả stack trace.

### 2.9. Dockerfile (backend) — yêu cầu

```dockerfile
# Multi-stage: build bằng Maven, chạy bằng JRE nhỏ
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd -r -u 1001 appuser
COPY --from=build /app/target/*.jar app.jar
USER appuser
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Xss512k -Duser.timezone=Asia/Ho_Chi_Minh"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://localhost:${PORT:-8080}/actuator/health || exit 1
ENTRYPOINT ["java","-jar","app.jar"]
```

(Agent có thể tinh chỉnh, miễn: multi-stage, chạy non-root, tôn trọng biến `PORT`, dùng ít RAM vì gói miễn phí thường chỉ ~512 MB.) Thêm `.dockerignore`.

---

## 3. FRONTEND (`FE_STACK`, deploy trên VERCEL)

### 3.1. Nguyên tắc
- Dùng đúng `FE_STACK` đã chọn. Mặc định: **React + Vite + TypeScript + Tailwind** (build ra file tĩnh, đơn giản và ổn định trên Vercel). Nếu chọn framework khác, giữ nguyên **thiết kế và chức năng** ở mục 3.3–3.4.
- FE **chỉ gọi REST API** của backend; không chứa nghiệp vụ tính tiền (chỉ hiển thị kết quả từ `/sales/preview` để tránh lệch số liệu).
- Cấu hình URL API bằng biến môi trường: `VITE_API_BASE_URL` (hoặc `NEXT_PUBLIC_API_BASE_URL`…). Nếu để trống → dùng đường dẫn tương đối `/api` (đi qua rewrite).
- Có lớp `apiClient` tập trung: timeout, parse lỗi `ProblemDetail`, **retry có backoff** cho lần gọi đầu.

### 3.2. Xử lý "máy chủ ngủ" (rất quan trọng)
Gói miễn phí của Render/Fly/Railway thường **tắt container khi rảnh** và mất vài chục giây để khởi động lại (JVM càng chậm). FE phải:
- Gọi `/actuator/health` ngay khi mở trang (`warm-up`), hiện banner **"Đang đánh thức máy chủ, vui lòng chờ…"** kèm thanh tiến trình;
- Retry tự động (tối đa ~60s) trước khi báo lỗi;
- Có skeleton/loading ở mọi bảng và nút "Thử lại".

### 3.3. Màn hình (bám docx 2.6, 2.2.1)

| Màn hình | Nội dung |
|---|---|
| **Tổng quan** | 4 thẻ số liệu (doanh thu hôm nay, số hóa đơn, sắp hết hàng, sắp hết hạn); biểu đồ cột doanh thu 7 ngày; donut doanh thu theo loại; danh sách "Cần chú ý"; hóa đơn gần đây |
| **Bán hàng (POS)** | Trái: ô tìm/nhập mã (Enter thêm ngay – phục vụ máy quét USB), chip lọc loại, lưới thẻ sản phẩm (badge *Hết hàng/Sắp hết/Sắp hết hạn*, thẻ hết hàng/hết hạn mờ và không bấm được). Phải: giỏ hàng (−/+, ✕), chọn chính sách giảm giá, tạm tính/giảm giá/**tổng** (lấy từ `/sales/preview`), tiền khách đưa → tiền thừa, nút **THANH TOÁN (F9)**. Xong → xem trước hóa đơn khổ 80mm + nút in/tải `.txt` |
| **Sản phẩm** | Bảng (Mã, Tên, Loại-badge, Giá, Tồn kèm cảnh báo màu, Hạn dùng), tìm khi gõ (debounce 250ms), lọc, sắp xếp cột, thêm/sửa trong **modal form** (trường đổi theo loại; báo lỗi dưới từng ô), xóa có xác nhận, xuất CSV (UTF-8 BOM) |
| **Hóa đơn** | Bảng + lọc khoảng ngày/trạng thái, chi tiết hóa đơn, **Hủy hóa đơn** (xác nhận, hoàn kho) |
| **Báo cáo** | Chọn Từ–Đến + nút nhanh (Hôm nay/7 ngày/30 ngày/Tháng này); thẻ tổng hợp; biểu đồ theo ngày; top bán chạy; theo loại. Khoảng rỗng → trạng thái trống đẹp |
| **Sản phẩm từ API** | Gọi `/api/external/products`, bảng kết quả, nút "Thêm vào cửa hàng" (quy đổi USD→VND), trạng thái đang tải/lỗi/trống + nút thử lại |

Có **trang 404** và **trạng thái lỗi toàn cục** thân thiện.

### 3.4. Hệ thống thiết kế (nâng cấp so với phần mềm POS cổ điển kiểu Keko)

Phong cách: **phẳng, bo góc, khoảng trắng rộng, màu có chủ đích, phản hồi mượt, sáng/tối**. Không dùng logo/ảnh/tên Keko; đặt thương hiệu riêng (**"MiniMart POS"**).

> Nếu **không xem được** ảnh giao diện trên trang Keko, đừng suy đoán chi tiết hình ảnh; nêu rõ trong README rằng giao diện "lấy cảm hứng về bố cục POS và nhóm tính năng".

**Màu (Sáng):** `primary #2563EB`, `primaryHover #1D4ED8`, `primarySoft #DBEAFE`, `success #16A34A`, `warning #F59E0B`, `danger #DC2626`, `info #0891B2`, `bg #F4F6FA`, `surface #FFFFFF`, `border #E2E8F0`, `text #0F172A`, `textMuted #64748B`, `sidebar #0F172A`, `sidebarText #CBD5E1`.
**Màu (Tối):** `bg #0B1220`, `surface #111B2E`, `border #22304A`, `text #E5E7EB`, `textMuted #94A3B8`, `primary #3B82F6`. Tương phản chữ **≥ 4.5:1**. Khai báo thành **design token** (CSS variables / Tailwind theme), không rải mã màu trong component.

**Chữ:** `Inter` (nạp từ Google Fonts hoặc tự host) → fallback `Segoe UI, Roboto, system-ui, sans-serif`. Tiêu đề trang 22/700, tiêu đề thẻ 14/600, nội dung 14, chú thích 12; **tổng thanh toán 32/700**.
**Hình khối:** thẻ bo 12px, nút/ô nhập 8px, badge 999px; bóng đổ nhẹ; lưới 8px; chiều cao ô nhập/nút 40px, nút Thanh toán 52px.

**Bố cục khung:** sidebar tối 240px (thu gọn còn 72px), top bar (tiêu đề trang, tìm nhanh `Ctrl+K`, đổi Sáng/Tối, đồng hồ), vùng nội dung, thanh trạng thái nhỏ (kết nối backend, thời gian phản hồi). Mục "Khách hàng/Nhân viên/Công nợ…" **không tạo** (ngoài phạm vi docx); nếu cần cho đầy đặn chỉ thêm tối đa 1–2 mục `disabled` có huy hiệu **"Sắp có"** và ghi rõ là chưa cài đặt.

**Tương tác:** phím tắt `F2` bán hàng mới · `F3` tìm/nhập mã · `F9` thanh toán · `Ctrl+N` thêm sản phẩm · `Ctrl+K` tìm nhanh · `Ctrl+D` đổi giao diện · `Esc` đóng modal; **toast** (thành công/cảnh báo/lỗi, tự ẩn ~2,5s); hover/pressed/disabled/focus-visible rõ ràng; hiệu ứng chuyển trang ~150ms; biểu đồ có tooltip và hiệu ứng mọc; ảnh sản phẩm = **avatar chữ cái đầu** nền màu sinh từ tên (không cần file ảnh); tôn trọng `prefers-reduced-motion`, `prefers-color-scheme`.

**Chất lượng:** responsive từ **1024px trở lên là chính**, dùng được trên tablet (POS) và không vỡ bố cục trên điện thoại (tối thiểu bảng cuộn ngang). Đạt a11y cơ bản (nhãn form, `aria-*` cho modal/toast, điều hướng bàn phím). Định dạng tiền `1.234.000 đ` bằng `Intl.NumberFormat('vi-VN')`. Danh sách lớn dùng phân trang phía server.

### 3.5. Cấu hình Vercel (`frontend/vercel.json`)

Ưu tiên **rewrite** để trình duyệt gọi cùng domain (tránh CORS, ẩn URL backend):

```json
{
  "rewrites": [
    { "source": "/api/:path*", "destination": "https://TEN-BACKEND.onrender.com/api/:path*" },
    { "source": "/((?!api/).*)", "destination": "/index.html" }
  ]
}
```

(Dòng thứ hai chỉ cần với SPA; với Next.js/SvelteKit… bỏ nếu framework tự xử lý. Agent phải thay `TEN-BACKEND` bằng placeholder rõ ràng và ghi trong `DEPLOY.md` chỗ cần sửa.) Phương án B: gọi thẳng `VITE_API_BASE_URL` và cấu hình `ALLOWED_ORIGINS` ở backend.

---

## 4. MODULE SWING TÙY CHỌN (`INCLUDE_SWING_CLIENT=true`)

Mục đích: đáp ứng "Lập trình giao diện Swing + bắt sự kiện" của docx, **tái dùng backend**.

- Maven riêng `desktop-swing/`, Java 17, **FlatLaf** làm Look & Feel (có fallback nếu thiếu), Gson.
- `ApiClient` dùng `HttpClient` gọi REST của backend (URL cấu hình trong `app.properties`), **chạy bằng `SwingWorker`**, không chặn EDT; xử lý `IOException`/`InterruptedException`/mã lỗi HTTP, thông báo bằng `JOptionPane`.
- `MainFrame` (`JFrame`) với `JTabbedPane`: **Sản phẩm** (JTable, thêm/sửa/xóa/tìm), **Bán hàng** (giỏ hàng JTable, chọn chính sách giảm giá, thanh toán, xem hóa đơn), **Thống kê** (bảng + biểu đồ cột tự vẽ `Graphics2D`).
- Mọi listener dùng **lambda** (ví dụ đúng docx 4.3). Kiểm tra nhập bằng `try/catch` `NumberFormatException`/`IllegalArgumentException` (docx 3.6).
- Dùng lại bảng màu mục 3.4 (bo góc, sidebar/tab hiện đại, bảng cao 40px, font tiếng Việt đúng dấu).
- Có thể đóng gói `jar` chạy bằng `java -jar`. Không deploy lên Vercel (ghi rõ trong README).

---

## 5. ÁNH XẠ YÊU CẦU DOCX → NƠI THỂ HIỆN

| Nội dung chấm | Backend | Frontend / Swing |
|---|---|---|
| Đóng gói | `Product` private + setter kiểm tra | — |
| Kế thừa | `FoodProduct`, `HouseholdProduct` | Hiển thị `category` |
| Đa hình | `getCategory()` override; `List<Product>` hỗn hợp | Badge loại theo dữ liệu |
| Abstract class | `Product` | — |
| Interface | `DiscountPolicy` | Chọn chính sách trên UI |
| Lambda | Chính sách giảm giá, `stream().filter/sorted/groupingBy` | Swing: `addActionListener(e -> …)`; Web: handler sự kiện |
| Ngoại lệ | `GlobalExceptionHandler`, exception tự định nghĩa | Hiển thị lỗi theo từng ô / toast / `JOptionPane` |
| API | `ApiService` + `/api/external/**` | Màn "Sản phẩm từ API" |
| GUI + sự kiện | — | Swing (nếu bật) + Web |

---

## 6. KIỂM THỬ

### 6.1. Unit/Integration test bắt buộc (khớp 12 ca docx mục 4.4)

| STT | Chức năng | Dữ liệu | Kỳ vọng |
|---|---|---|---|
| 1 | Thêm sản phẩm | `SP01`, 30000, SL 20 | 201 Created |
| 2 | Thêm sản phẩm | Mã trống | 400 + thông báo tiếng Việt |
| 3 | Thêm sản phẩm | Giá là chữ | 400 (JSON/parse lỗi được bắt, không 500) |
| 4 | Thêm sản phẩm | Giá âm | 400 |
| 5 | Tìm kiếm | Từ khóa trùng tên | Trả đúng sản phẩm |
| 6 | Bán hàng | SL < tồn | Hóa đơn thành công, tồn giảm |
| 7 | Bán hàng | SL > tồn | 409, **tồn không đổi** |
| 8 | Tính tiền | 30000 × 2 | Thành tiền 60000 |
| 9 | Giảm giá | 600000, giảm 5% | Tiền giảm 30000 |
| 10 | API ngoài | Phản hồi 200 | Nhận dữ liệu (HttpClient giả) |
| 11 | API ngoài | Mất mạng/timeout | 502/504 + thông báo phù hợp |
| 12 | Thống kê | Có hóa đơn hoàn tất + đã hủy | Doanh thu **loại** hóa đơn hủy |

Thêm: trùng mã (409), sửa/xóa, hủy hóa đơn hoàn kho và hủy lần hai (409), thực phẩm hết hạn bị chặn, **hai `checkout` đồng thời không bán vượt tồn**, đa hình `getCategory()`, validate DTO, health endpoint.

### 6.2. Frontend
Test đơn vị cho `apiClient` (lỗi/timeout/retry) và định dạng tiền; test thành phần cho giỏ hàng (+/−, vượt tồn). Nếu làm được, 1 kịch bản E2E (Playwright) "bán một đơn hàng". Nếu không làm được, ghi **"Chưa kiểm thử"**.

### 6.3. `TESTING.md`
Bảng: `STT · Chức năng · Dữ liệu · Kỳ vọng · Kết quả thực tế · Đạt/Chưa đạt · Ghi chú`. **Điền từ lần chạy thật** (dán tóm tắt `mvn test`). Ca chưa kiểm được: ghi "Chưa kiểm thử" và lý do.

---

## 7. HƯỚNG DẪN DEPLOY (`DEPLOY.md` — agent phải viết)

### 7.1. Backend lên Render (mặc định)
1. Đẩy repo lên GitHub.
2. Tạo **PostgreSQL** (Render Postgres hoặc Neon) → lấy chuỗi kết nối.
3. Tạo **Web Service** → *Runtime: Docker*, *Root Directory:* `backend`, *Health Check Path:* `/actuator/health`.
4. Khai báo biến môi trường: `SPRING_PROFILES_ACTIVE=prod`, thông tin DB (xem mục 2.7), `ALLOWED_ORIGINS`, `TZ=Asia/Ho_Chi_Minh`.
5. Dùng `render.yaml` (Blueprint) để tự động hóa các bước trên.
6. Kiểm tra: `curl https://<backend>/actuator/health` → `{"status":"UP"}`; mở `/swagger-ui.html`.
7. Ghi chú: gói miễn phí **tắt khi rảnh** → request đầu chậm; DB miễn phí có thể giới hạn thời hạn/dung lượng (kiểm tra chính sách hiện hành trên trang của nhà cung cấp trước khi dùng cho bản nộp).

### 7.2. Phương án thay thế
Mô tả ngắn **Railway**, **Fly.io** (`fly launch`, `fly.toml`, cổng nội bộ 8080), **Google Cloud Run** (`gcloud run deploy --source`, tôn trọng `PORT`) — chỉ liệt kê các lệnh/điểm khác biệt chính.

### 7.3. Frontend lên Vercel
1. *Add New → Project*, chọn repo, **Root Directory = `frontend`**, nhận diện framework tự động (Vite/Next…).
2. Khai báo biến môi trường (nếu dùng phương án B) và sửa `destination` trong `vercel.json` (phương án A).
3. Deploy, mở URL, chạy kịch bản kiểm tra (mục 8).
4. Gắn domain tùy chọn.

### 7.4. Chạy local
`docker compose up` (backend + Postgres), `npm run dev` cho frontend; liệt kê địa chỉ và biến môi trường mẫu.

### 7.5. Kiểm tra sau deploy (smoke test)
Danh sách lệnh `curl` và thao tác tay: health → liệt kê sản phẩm → thêm sản phẩm → bán 1 đơn → xem hóa đơn → thống kê → gọi API ngoài → hủy hóa đơn.

---

## 8. TIÊU CHÍ NGHIỆM THU

**Backend**
- [ ] `./mvnw verify` thành công; số test đạt được báo **thật**.
- [ ] `docker build` và `docker run -e PORT=8080 …` chạy được, `/actuator/health` = UP.
- [ ] Đủ endpoint ở mục 2.4, lỗi trả JSON thống nhất tiếng Việt, không lộ stack trace.
- [ ] BR1–BR11 có test; `checkout` đồng thời không bán vượt tồn.
- [ ] `ApiService` xử lý được mã lỗi, timeout, mất mạng, JSON sai.
- [ ] Migration Flyway chạy được trên cả H2 và PostgreSQL (nếu không kiểm được PostgreSQL thì ghi rõ).

**Frontend**
- [ ] Build production thành công (`npm run build`), không lỗi TypeScript/ESLint nghiêm trọng.
- [ ] Đủ 6 màn hình, có trạng thái trống/đang tải/lỗi, có banner "đánh thức máy chủ".
- [ ] POS dùng được bằng bàn phím (nhập mã → Enter, F9).
- [ ] Sáng/Tối đổi tức thì, tương phản đạt.
- [ ] Không còn URL backend cứng trong mã (chỉ trong biến môi trường/`vercel.json`).

**Swing (nếu bật)**
- [ ] Chạy được bằng `java -jar`, gọi API không treo giao diện, có xử lý ngoại lệ và sự kiện bằng lambda.

**Bàn giao**
- [ ] `README.md`, `DEPLOY.md`, `TESTING.md`, `docs/class-diagram.puml`, `docs/api.md`, `render.yaml`, `Dockerfile`, `docker-compose.yml`, `.env.example` (backend & frontend).
- [ ] Ảnh chụp giao diện **Hình 4.1–4.6** (nếu chụp được; nếu không, hướng dẫn cách chụp, **không** tạo ảnh giả): Tổng quan · Quản lý sản phẩm · Lập hóa đơn · Kết quả tìm kiếm · Thống kê doanh thu · Thông báo dữ liệu không hợp lệ.
- [ ] Danh sách **hạn chế** ghi trung thực (kể cả việc **chưa deploy thật**).

---

## 9. PHẠM VI

**Phải làm:** quản lý sản phẩm (thêm/sửa/xóa/hiển thị), tìm kiếm mã/tên, tồn kho, lập hóa đơn, tính tiền + giảm giá, kiểm tra tồn trước khi bán, thống kê doanh thu theo khoảng thời gian, thông báo thành công/thất bại, gọi API ngoài, lưu dữ liệu bền vững.
**Nên làm:** dashboard + biểu đồ, hủy hóa đơn, xuất CSV/in hóa đơn `.txt`, sáng/tối, phím tắt.
**Không làm:** đa chi nhánh, thanh toán ngân hàng thật, kế toán, phân quyền chi tiết, đăng nhập, đa người dùng đồng thời nâng cao, driver máy quét/máy in.

---

## 10. RỦI RO CẦN BIẾT (agent phải nêu trong README)

| Rủi ro | Ứng xử |
|---|---|
| Gói miễn phí tắt khi rảnh → lần gọi đầu chậm (JVM càng chậm) | Warm-up + retry + banner (mục 3.2); giữ ảnh Docker nhỏ, tinh chỉnh JVM |
| Không có đăng nhập, dữ liệu công khai | Chỉ dữ liệu mẫu; có `DEMO_READONLY` |
| Docx minh họa `double`, bản này dùng `BigDecimal` | Giải thích trong README |
| Yêu cầu Swing/JavaFX của docx | Module Swing (mục 4) hoặc ghi rõ nếu bỏ |
| Chính sách gói miễn phí của nhà cung cấp có thể thay đổi | Không hứa hẹn giá/giới hạn; nhắc kiểm tra trên trang chính thức |

---

## 11. KẾ HOẠCH THEO GIAI ĐOẠN

| GĐ | Nội dung | Hoàn thành khi |
|---|---|---|
| 1 | Khởi tạo monorepo, backend Spring Boot, domain (`Product`…`DiscountPolicy`), exception | `./mvnw compile` OK; test đóng gói/kế thừa/đa hình pass |
| 2 | Repository + Flyway + service nghiệp vụ + seed | Test BR1–BR11 pass trên H2 |
| 3 | Controller + DTO + `GlobalExceptionHandler` + OpenAPI | Test MockMvc pass; Swagger mở được |
| 4 | `ApiService` + endpoint `/external` + test HttpClient giả | 3 kịch bản (200/lỗi HTTP/mất mạng) pass |
| 5 | Dockerfile + docker-compose + health + cấu hình prod/CORS | `docker compose up` chạy trọn |
| 6 | Frontend: khung, design token, sáng/tối, apiClient, warm-up | Chạy dev, gọi được API |
| 7 | FE: Sản phẩm → POS → Hóa đơn → Báo cáo → Dashboard → API ngoài | Bán thử trọn quy trình |
| 8 | (Nếu bật) Module Swing | Thêm/bán/thống kê hoạt động qua REST |
| 9 | `render.yaml`, `vercel.json`, `DEPLOY.md`, README, TESTING, ảnh chụp | Đủ tệp bàn giao |
| 10 | Rà soát checklist mục 8, sửa lỗi, báo cáo cuối | Qua checklist hoặc nêu rõ mục chưa đạt |

---

## 12. ĐỊNH DẠNG BÁO CÁO CUỐI CỦA AGENT

1. Đã làm xong những gì (theo giai đoạn).
2. Kết quả test **thật** (số lượng, đạt/hỏng) cho backend, frontend, Swing.
3. Việc **chưa làm / chưa kiểm chứng** (đặc biệt: chưa deploy thật nếu không có tài khoản) và lý do.
4. Lệnh chạy nhanh local (2–3 lệnh) và các bước người dùng còn phải tự làm để deploy (tạo tài khoản, dán biến môi trường, sửa URL trong `vercel.json`).
5. Ba gợi ý cải tiến tiếp theo.

---

## 13. LƯU Ý CUỐI
- Ưu tiên **đúng và chạy được** trước, **đẹp** sau — nhưng thẩm mỹ là yêu cầu chính thức, đừng bỏ qua GĐ 6–7.
- Gặp yêu cầu mơ hồ: chọn phương án đơn giản nhất phù hợp phạm vi tiểu luận, ghi giả định vào README thay vì dừng lại.
- Không thêm thư viện nặng không cần thiết; không gọi API/dịch vụ nào ngoài `dummyjson.com` và các dịch vụ deploy đã nêu; không thu thập dữ liệu người dùng.
