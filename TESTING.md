# Báo cáo Kiểm thử Hệ thống (TESTING.md)

Tài liệu này ghi nhận **kết quả thực tế** từ các bộ kiểm thử tự động đã chạy trên môi trường phát triển cục bộ (Local), bao gồm:
1. **12 ca kiểm thử bắt buộc** trong mục 4.4 của báo cáo `tiểu luận lập trình hướng đối tượng.docx`.
2. **Các ca kiểm thử mở rộng** cho quy tắc nghiệp vụ `BR1–BR11`, kỹ thuật OOP, tích hợp API ngoài (`HttpClient` giả), kiểm thử đơn vị/thành phần Frontend (`Vitest`), và kiểm thử module `desktop-swing`.

---

## 1. Bảng kết quả kiểm thử chức năng (Khớp 12 ca mục 4.4 docx & mở rộng)

| STT | Chức năng | Dữ liệu kiểm thử | Kỳ vọng | Kết quả thực tế | Đạt/Chưa đạt | Ghi chú (Lớp kiểm thử) |
|---:|---|---|---|---|---|---|
| 1 | Thêm sản phẩm | Mã `SP01`, giá `30000`, SL `20`, loại `FOOD` | `201 Created`, lưu đúng sản phẩm | Trả về HTTP `201`, JSON chứa `SP01`, giá `30000`, SL `20`, `categoryName="Thực phẩm"` | **Đạt** | `MinimartIntegrationTest#testCase01` |
| 2 | Thêm sản phẩm | Mã sản phẩm bị trống (`"   "`) | `400 Bad Request` + thông báo tiếng Việt | Trả về HTTP `400`, `detail`: `"Mã sản phẩm không được để trống"` | **Đạt** | `MinimartIntegrationTest#testCase02` & `DomainOopTest` |
| 3 | Thêm sản phẩm | Giá nhập bằng chữ (`"ba_muoi_nghin"`) | `400 Bad Request` (bắt lỗi parse, không bị 500) | Trả về HTTP `400` chuẩn `ProblemDetail`, trên Swing bắt `NumberFormatException` | **Đạt** | `MinimartIntegrationTest#testCase03` & `SwingClientTest` |
| 4 | Thêm sản phẩm | Giá âm (`-5000`) | Từ chối dữ liệu (`400 Bad Request`) | Trả về HTTP `400`, `detail`: `"Đơn giá không được âm"` | **Đạt** | `MinimartIntegrationTest#testCase04` & `DomainOopTest` |
| 5 | Tìm kiếm | Từ khóa `"vinamilk"` trùng tên sản phẩm | Trả về đúng sản phẩm khớp tên | Trả về danh sách chứa đúng `SP01 - Sữa tươi Vinamilk` | **Đạt** | `MinimartIntegrationTest#testCase05` |
| 6 | Bán hàng | Số lượng bán (`20`) < tồn kho (`25`) | Lập hóa đơn `COMPLETED`, tồn kho giảm còn `5` | Hóa đơn tạo thành công (`201`), tồn kho trong DB giảm chính xác từ `25` xuống `5` | **Đạt** | `MinimartIntegrationTest#testCase06_08_09` |
| 7 | Bán hàng | Số lượng bán (`15`) > tồn kho (`10`) | Từ chối giao dịch (`409`), **tồn kho không đổi** | Trả về HTTP `409 Conflict` (`InsufficientStockException`), tồn kho giữ nguyên `10` | **Đạt** | `MinimartIntegrationTest#testCase07` |
| 8 | Tính tiền | 1 sản phẩm giá `30000`, số lượng `2` | Thành tiền `60000` | `CartItem.getSubtotal()` và `/api/sales/preview` trả về đúng `60000` | **Đạt** | `DomainOopTest#testCartItemSubtotalAndPriceSnapshot` & `MinimartIntegrationTest` |
| 9 | Giảm giá | Hóa đơn `600000`, giảm 5% (`PERCENT5_OVER_500K`) | Tiền giảm `30000`, còn `570000` | Lambda `DiscountPolicy` tính `discount = 30000`, `totalAmount = 570000` | **Đạt** | `DomainOopTest#testDiscountPolicyLambda` & `MinimartIntegrationTest` |
| 10 | API ngoài | `dummyjson.com` phản hồi `200 OK` (`HttpClient` giả) | Nhận dữ liệu và chuẩn hóa sang VND | Nhận đúng 2 sản phẩm mẫu, quy đổi USD→VND (`$10 -> 250.000 đ`) | **Đạt** | `ApiServiceTest#testFetchExternalProductsSuccess200` & `MinimartIntegrationTest#testCase10_11` |
| 11 | API ngoài | Mất kết nối mạng / Timeout / HTTP 500 | Hiển thị thông báo lỗi phù hợp (`502`/`504`) | Ném `IOException`/`HttpTimeoutException`, Controller trả về `502 Bad Gateway` / `504` | **Đạt** | `ApiServiceTest` & `MinimartIntegrationTest#testCase10_11` |
| 12 | Thống kê | Có hóa đơn hoàn tất (`60000`) + hóa đơn đã hủy (`90000`) | Doanh thu **chỉ tính đơn hoàn tất**, loại đơn hủy | `totalRevenue = 60000`, `completedInvoices = 1`, `cancelledInvoices = 1` | **Đạt** | `MinimartIntegrationTest#testCase12` |
| 13 | Kiểm tra trùng mã (BR1) | Thêm lại mã `SP01` đã tồn tại | `409 Conflict` | Trả về HTTP `409` (`DuplicateProductException`) | **Đạt** | `MinimartIntegrationTest#testCase01` |
| 14 | Hủy hóa đơn hoàn kho (BR8) | Hủy hóa đơn `COMPLETED` (3 SP), sau đó hủy lần 2 | Lần 1: hoàn kho +3 SP; Lần 2: `409 Conflict` | Tồn kho tăng từ `15` lên `18`; gọi hủy lần 2 trả về `409 Conflict` | **Đạt** | `MinimartIntegrationTest#testCase12` |
| 15 | Chặn thực phẩm hết hạn (BR9) | Bán `FoodProduct` có `expiryDate` hôm qua | `409 Conflict`, không cho bán | Trả về HTTP `409` với thông báo sản phẩm đã hết hạn sử dụng | **Đạt** | `MinimartIntegrationTest#testExpiredFoodProductBlockedFromSale` |
| 16 | Chống bán vượt đồng thời (BR11) | 2 luồng `checkout` cùng lúc mua 4 SP khi tồn = 5 | Chỉ 1 luồng thành công, 1 luồng bị từ chối, tồn = 1 | `successCount = 1`, `conflictCount = 1`, tồn kho cuối cùng = `1` (không bị âm) | **Đạt** | `MinimartIntegrationTest#testConcurrentCheckoutDoesNotOversell` |
| 17 | Đa hình & Kế thừa OOP | `List<Product>` chứa `FoodProduct` & `HouseholdProduct` | `getCategory()` trả về `"Thực phẩm"` / `"Đồ gia dụng"` | Trả về đúng chuỗi của từng lớp con lúc chạy (Runtime Polymorphism) | **Đạt** | `DomainOopTest#testInheritanceAndPolymorphism` |
| 18 | Frontend `apiClient` & Giỏ hàng | Định dạng tiền, retry 503, nút `+` vượt tồn kho | Định dạng `1.234.000 đ`, retry thành công, chặn vượt tồn | Toàn bộ 5 test Vitest đều pass | **Đạt** | `frontend/src/test/frontend.test.tsx` |
| 19 | Kịch bản E2E trình duyệt (Playwright) | Bán một đơn hàng trên trình duyệt thật | Hoàn tất đơn hàng | Chưa kiểm thử | **Chưa kiểm thử** | Đã kiểm thử tích hợp qua `MockMvc` + `Vitest` component test; không cài đặt trình duyệt Playwright trong môi trường hiện tại |
| 20 | Kiểm thử Docker & PostgreSQL thực tế | `docker compose up` với container PostgreSQL | Chạy container thành công | Chưa kiểm thử | **Chưa kiểm thử** | Máy hiện tại không cài đặt `docker` CLI; đã kiểm thử Flyway migration trên H2 ở chế độ `MODE=PostgreSQL` |

---

## 2. Nhật ký chạy kiểm thử thực tế

### 2.1. Backend (`cd backend && .\mvnw.cmd test`)
```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.minimart.api.ApiServiceTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in com.minimart.api.ApiServiceTest
[INFO] Running com.minimart.domain.DomainOopTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- in com.minimart.domain.DomainOopTest
[INFO] Running com.minimart.MinimartIntegrationTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0 -- in com.minimart.MinimartIntegrationTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 2.2. Frontend (`cd frontend && npm test`)
```text
 RUN  v2.1.9 D:/tung/OOP-26-27/TieuLuan_Nhom8/frontend

 ✓ src/test/frontend.test.tsx (5 tests) 141ms

 Test Files  1 passed (1)
      Tests  5 passed (5)
```

### 2.3. Module Desktop Swing (`cd desktop-swing && .\mvnw.cmd test`)
```text
[INFO] Running com.minimart.swing.SwingClientTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in com.minimart.swing.SwingClientTest
[INFO] BUILD SUCCESS
```
