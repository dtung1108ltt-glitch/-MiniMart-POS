# Hướng dẫn Triển khai (Deployment Guide) — MiniMart POS

> **Lưu ý trung thực (Quy tắc 0.3.2):** Agent không có tài khoản Render và Vercel của người dùng nên **chưa thực hiện deploy lên môi trường đám mây thật**. Toàn bộ tệp cấu hình (`render.yaml`, `backend/Dockerfile`, `docker-compose.yml`, `frontend/vercel.json`, `.env.example`, `DatabaseUrlEnvironmentPostProcessor`) đã được chuẩn bị sẵn và kiểm thử build local. Bạn chỉ cần làm theo các bước dưới đây để đưa hệ thống lên Internet.

---

## 1. Triển khai Backend lên Render (Mặc định — Docker + PostgreSQL)

### Cách A: Tự động hóa bằng Blueprint (`render.yaml` — Khuyến nghị)
1. Đẩy toàn bộ thư mục dự án lên một kho lưu trữ GitHub của bạn.
2. Đăng nhập vào [Render Dashboard](https://dashboard.render.com/).
3. Nhấn **New +** → chọn **Blueprint**.
4. Kết nối tới repository GitHub vừa đẩy lên. Render sẽ tự động đọc tệp [`render.yaml`](render.yaml) ở thư mục gốc và khởi tạo:
   - Cơ sở dữ liệu PostgreSQL: `minimart-postgres`
   - Web Service Docker: `minimart-backend` (Root Directory: `backend`, Health Check Path: `/actuator/health`).
5. Sau khi tạo xong, vào mục **Environment** của `minimart-backend`, cập nhật biến `ALLOWED_ORIGINS` thành địa chỉ Vercel của bạn (ví dụ: `https://minimart-pos.vercel.app,http://localhost:5173`).

### Cách B: Tạo thủ công trên giao diện Render
1. **Tạo CSDL PostgreSQL:**
   - Nhấn **New +** → **PostgreSQL** (hoặc dùng [Neon.tech](https://neon.tech)).
   - Sao chép chuỗi **Internal Database URL** (có dạng `postgres://user:pass@host:5432/minimart`).
   - *Lưu ý kỹ thuật:* Lớp `DatabaseUrlEnvironmentPostProcessor` trong backend tự động chuyển đổi biến `DATABASE_URL` dạng `postgres://...` của Render sang chuẩn `jdbc:postgresql://...` kèm username/password cho Spring Boot.
2. **Tạo Web Service:**
   - Nhấn **New +** → **Web Service** → chọn repo GitHub.
   - **Root Directory:** `backend`
   - **Runtime:** `Docker`
   - **Health Check Path:** `/actuator/health`
3. **Khai báo biến môi trường (Environment Variables):**
   - `SPRING_PROFILES_ACTIVE=prod`
   - `DATABASE_URL=<dán chuỗi postgres://... ở bước 1>` *(hoặc khai báo 3 biến `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`)*
   - `ALLOWED_ORIGINS=https://TEN-FRONTEND.vercel.app,http://localhost:5173`
   - `APP_USD_VND_RATE=25000`
   - `DEMO_READONLY=false` *(đặt `true` nếu muốn khóa xóa/hủy trên môi trường công khai)*
   - `TZ=Asia/Ho_Chi_Minh`
4. **Kiểm tra sau khi deploy Backend:**
   - Kiểm tra Health Check:
     ```bash
     curl https://<TEN-BACKEND>.onrender.com/actuator/health
     # Kỳ vọng: {"status":"UP"}
     ```
   - Mở tài liệu OpenAPI / Swagger UI: `https://<TEN-BACKEND>.onrender.com/swagger-ui.html`.

> **Lưu ý về gói miễn phí (Free Tier):**
> - Gói Web Service miễn phí của Render sẽ **tắt container khi không có truy cập** và cần 30–50 giây để khởi động lại JVM ở lần gọi đầu tiên. Giao diện Frontend đã tích hợp sẵn cơ chế `warmUpServer()` gọi `/actuator/health` kèm thanh tiến trình *"Đang đánh thức máy chủ, vui lòng chờ…"* và tự động retry tối đa 60 giây.
> - Cơ sở dữ liệu PostgreSQL miễn phí trên Render có giới hạn thời hạn/dung lượng; hãy kiểm tra chính sách hiện hành của nhà cung cấp hoặc dùng Neon PostgreSQL nếu cần giữ dữ liệu lâu dài.

---

## 2. Phương án thay thế cho Backend

- **Railway:** Chọn *Deploy from GitHub repo* → đặt Root Directory là `backend`, thêm plugin PostgreSQL; biến `DATABASE_URL` được tự động tiêm và chuyển đổi qua `DatabaseUrlEnvironmentPostProcessor`.
- **Fly.io:** Tại thư mục `backend/`, chạy `fly launch` (chọn cổng nội bộ `8080` trong `fly.toml`), gắn Postgres qua `fly postgres attach` và chạy `fly deploy`.
- **Google Cloud Run:**
  ```bash
  cd backend
  gcloud run deploy minimart-backend --source . --region asia-southeast1 --allow-unauthenticated
  ```
  *(Container tự động lắng nghe biến môi trường `PORT` do Cloud Run cấp).*

---

## 3. Triển khai Frontend lên Vercel

1. Mở tệp [`frontend/vercel.json`](frontend/vercel.json) và thay `https://TEN-BACKEND.onrender.com` bằng địa chỉ Backend thực tế của bạn trên Render:
   ```json
   {
     "rewrites": [
       {
         "source": "/api/:path*",
         "destination": "https://minimart-backend-xxxx.onrender.com/api/:path*"
       },
       {
         "source": "/actuator/:path*",
         "destination": "https://minimart-backend-xxxx.onrender.com/actuator/:path*"
       },
       {
         "source": "/((?!api/|actuator/).*)",
         "destination": "/index.html"
       }
     ]
   }
   ```
   *(Phương án A — Khuyến nghị: Để trống `VITE_API_BASE_URL`, mọi lời gọi `/api/**` sẽ đi qua Vercel Rewrite cùng domain, giúp tránh hoàn toàn lỗi CORS và ẩn URL backend).*
2. Đăng nhập [Vercel Dashboard](https://vercel.com/dashboard) → **Add New...** → **Project**.
3. Chọn repository GitHub của dự án.
4. Tại mục **Root Directory**, nhấn **Edit** và chọn **`frontend`** (Vercel sẽ tự động nhận diện framework **Vite**).
5. *(Tùy chọn — Phương án B)*: Nếu không dùng `vercel.json` rewrite, bạn có thể thêm biến môi trường `VITE_API_BASE_URL=https://minimart-backend-xxxx.onrender.com` trên Vercel và đảm bảo `ALLOWED_ORIGINS` ở Backend có chứa domain Vercel.
6. Nhấn **Deploy** và mở URL được cấp.

---

## 4. Chạy trên máy cá nhân (Local Development)

### Cách 1: Chạy trực tiếp bằng Maven (H2 in-memory, nhanh nhất, không cần cài Docker)
```powershell
# Terminal 1 — Chạy Backend Spring Boot (profile mặc định: local với H2 + dữ liệu mẫu tự động)
cd backend
./mvnw spring-boot:run

# Terminal 2 — Chạy Frontend React + Vite (cổng 5173, tự proxy /api sang localhost:8080)
cd frontend
npm install
npm run dev

# Terminal 3 (Tùy chọn) — Chạy ứng dụng Desktop Java Swing gọi chung REST API
cd desktop-swing
./mvnw clean package
java -jar target/minimart-desktop-swing-1.0.0.jar
```
- Giao diện Web: `http://localhost:5173`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- H2 Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:minimartdb`)

### Cách 2: Chạy bằng Docker Compose (Backend + PostgreSQL 16)
```bash
docker compose up --build
```

---

## 5. Kịch bản Kiểm tra sau khi Deploy (Smoke Test)

Thay `BASE_URL=http://localhost:8080` (hoặc URL Render/Vercel của bạn) và chạy lần lượt:

```bash
# 1. Kiểm tra trạng thái sức khỏe máy chủ
curl -s http://localhost:8080/actuator/health

# 2. Liệt kê danh sách sản phẩm (dữ liệu mẫu)
curl -s "http://localhost:8080/api/products?page=0&size=5"

# 3. Thêm một sản phẩm mới
curl -s -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"productId":"SP88","name":"Nước khoáng LaVie 500ml","price":6000,"quantity":50,"category":"FOOD","expiryDate":"2027-06-01"}'

# 4. Tính thử đơn hàng (Preview) với chính sách giảm giá
curl -s -X POST http://localhost:8080/api/sales/preview \
  -H "Content-Type: application/json" \
  -d '{"items":[{"productId":"SP88","quantity":2}],"discountPolicy":"NONE"}'

# 5. Lập hóa đơn thanh toán (Checkout)
curl -s -X POST http://localhost:8080/api/sales/checkout \
  -H "Content-Type: application/json" \
  -d '{"items":[{"productId":"SP88","quantity":2}],"discountPolicy":"NONE","amountPaid":20000}'

# 6. Xem danh sách hóa đơn
curl -s "http://localhost:8080/api/invoices?page=0&size=5"

# 7. Xem thống kê doanh thu (chỉ tính hóa đơn COMPLETED)
curl -s http://localhost:8080/api/stats/summary

# 8. Gọi API sản phẩm bên ngoài (dummyjson.com)
curl -s "http://localhost:8080/api/external/products?limit=3"

# 9. Hủy hóa đơn và hoàn trả tồn kho (thay <INVOICE_ID> bằng mã vừa tạo ở bước 5)
curl -s -X POST http://localhost:8080/api/invoices/<INVOICE_ID>/cancel
```
