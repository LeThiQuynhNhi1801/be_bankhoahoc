# Hướng Dẫn Chạy Dự Án

## ⚡ Cách NHANH NHẤT - Chạy từ IDE

### IntelliJ IDEA:
1. Mở project trong IntelliJ IDEA
2. IDE sẽ tự động nhận diện Maven project
3. Mở file: `src/main/java/com/bankhoahoc/BanKhoaHocApplication.java`
4. Click chuột phải → **Run 'BanKhoaHocApplication'**
   - Hoặc click icon ▶️ bên cạnh class name
   - Hoặc nhấn `Shift + F10`

### Eclipse:
1. Import project: File → Import → Existing Maven Projects
2. Click chuột phải vào project → **Run As** → **Spring Boot App**

### VS Code:
1. Cài extension: **Extension Pack for Java**
2. Mở file `BanKhoaHocApplication.java`
3. Click icon **Run** ở trên class

---

## 🔧 Cách 2 - Cài Maven và chạy từ Command Line

### Bước 1: Cài Maven
1. Tải Maven: https://maven.apache.org/download.cgi (Binary zip archive)
2. Giải nén vào: `C:\Program Files\Apache\maven`
3. Thêm vào PATH:
   - System Properties → Environment Variables
   - Path → New → Thêm: `C:\Program Files\Apache\maven\bin`
4. Kiểm tra: `mvn -version`

### Bước 2: Chạy ứng dụng
```bash
cd C:\Users\quynhnhi\Documents\fe\be_bankhoahoc
mvn clean spring-boot:run
```

---

## ✅ Kiểm tra ứng dụng đã chạy thành công

Sau khi chạy, bạn sẽ thấy log:
```
Started BanKhoaHocApplication in X.XXX seconds
```

## 🌐 Truy cập Swagger UI

Mở trình duyệt và vào:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/api-docs
- **API Base**: http://localhost:8080/api

---

## ⚠️ Lưu ý trước khi chạy

1. **Kiểm tra MySQL trên laptop đã cấu hình:**
   - MySQL đang chạy
   - Đã tạo user remote_user
   - Firewall đã mở port 3306
   - bind-address = 0.0.0.0

2. **Kiểm tra IP trong application.properties:**
   - Đảm bảo IP đúng (hiện tại: 10.145.13.1)
   - Nếu IP laptop thay đổi, cập nhật lại

3. **Kiểm tra username/password:**
   - Đảm bảo đúng với user đã tạo trên MySQL

---

## 🐛 Nếu gặp lỗi

### Lỗi kết nối database:
- Kiểm tra MySQL trên laptop đang chạy
- Kiểm tra IP, username, password trong application.properties
- Test kết nối: `mysql -h 10.145.13.1 -u remote_user -p`

### Lỗi port 8080 đã được sử dụng:
- Thay đổi port trong application.properties:
  ```properties
  server.port=8081
  ```

### Lỗi compile:
- Clean project và build lại
- Kiểm tra Java version (cần JDK 17+)
