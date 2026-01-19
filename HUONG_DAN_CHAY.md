# Hướng Dẫn Chạy Ứng Dụng

## Yêu cầu
- Java JDK 17+ (đã có: Java 21)
- Maven 3.6+ (cần cài đặt)
- MySQL 8.0+

## Cách 1: Sử dụng Maven (Khuyến nghị)

### Bước 1: Cài đặt Maven
1. Tải Maven từ: https://maven.apache.org/download.cgi
2. Giải nén và thêm vào PATH environment variable
3. Kiểm tra: `mvn -version`

### Bước 2: Cấu hình MySQL
1. Tạo database:
```sql
CREATE DATABASE bankhoahoc;
```

2. Cập nhật `src/main/resources/application.properties` nếu cần:
```properties
spring.datasource.username=root
spring.datasource.password=your_password
```

### Bước 3: Chạy ứng dụng
```bash
# Build và chạy
mvn clean install
mvn spring-boot:run

# Hoặc chỉ chạy (không build lại)
mvn spring-boot:run
```

### Bước 4: Truy cập Swagger UI
Sau khi ứng dụng chạy thành công, mở trình duyệt và truy cập:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs JSON**: http://localhost:8080/api-docs
- **API Base**: http://localhost:8080/api

## Cách 2: Sử dụng IDE (IntelliJ IDEA / Eclipse / VS Code)

### IntelliJ IDEA:
1. Mở project trong IntelliJ IDEA
2. IDEA sẽ tự động nhận diện Maven project
3. Mở file `src/main/java/com/bankhoahoc/BanKhoaHocApplication.java`
4. Click chuột phải → Run 'BanKhoaHocApplication'
5. Truy cập Swagger UI: http://localhost:8080/swagger-ui.html

### Eclipse:
1. Import project như Maven project
2. Click chuột phải vào project → Run As → Spring Boot App
3. Truy cập Swagger UI: http://localhost:8080/swagger-ui.html

### VS Code:
1. Cài extension "Extension Pack for Java"
2. Mở file `BanKhoaHocApplication.java`
3. Click "Run" button ở trên class
4. Truy cập Swagger UI: http://localhost:8080/swagger-ui.html

## Cách 3: Build JAR và chạy

```bash
# Build JAR file
mvn clean package

# Chạy JAR
java -jar target/be-bankhoahoc-1.0.0.jar
```

## Kiểm tra ứng dụng đã chạy

Khi ứng dụng chạy thành công, bạn sẽ thấy log như:
```
Started BanKhoaHocApplication in X.XXX seconds
```

## Test Swagger UI

1. Mở trình duyệt: http://localhost:8080/swagger-ui.html
2. Bạn sẽ thấy giao diện Swagger với tất cả các API endpoints
3. Để test API cần authentication:
   - Đăng nhập qua `/auth/login` để lấy token
   - Click nút "Authorize" ở góc trên bên phải
   - Nhập: `Bearer <your-token>`
   - Click "Authorize"

## Troubleshooting

### Lỗi kết nối database:
- Kiểm tra MySQL đã chạy chưa
- Kiểm tra username/password trong application.properties
- Đảm bảo database `bankhoahoc` đã được tạo

### Port 8080 đã được sử dụng:
- Thay đổi port trong `application.properties`:
```properties
server.port=8081
```
- Sau đó truy cập: http://localhost:8081/swagger-ui.html

### Maven không tìm thấy:
- Cài đặt Maven và thêm vào PATH
- Hoặc sử dụng IDE có tích hợp Maven
