# Troubleshooting Guide

## Lỗi: EntityManagerFactory not found

### Nguyên nhân:
1. MySQL chưa chạy hoặc không kết nối được
2. Thiếu cấu hình JPA repositories scan
3. Database chưa được tạo

### Giải pháp:

#### 1. Kiểm tra MySQL đã chạy chưa:
```bash
# Windows (PowerShell)
Get-Service -Name "*mysql*"

# Hoặc kiểm tra qua MySQL Workbench hoặc command line
mysql -u root -p
```

#### 2. Tạo database thủ công (nếu cần):
```sql
CREATE DATABASE IF NOT EXISTS bankhoahoc CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### 3. Kiểm tra kết nối:
Cập nhật `application.properties` với thông tin đúng:
```properties
spring.datasource.username=root
spring.datasource.password=your_password_here
```

#### 4. Nếu MySQL chưa cài đặt:
- Tải MySQL: https://dev.mysql.com/downloads/mysql/
- Hoặc sử dụng XAMPP/WAMP có tích hợp MySQL
- Hoặc sử dụng Docker:
```bash
docker run --name mysql-bankhoahoc -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=bankhoahoc -p 3306:3306 -d mysql:8.0
```

#### 5. Test kết nối đơn giản:
Tạo file test: `test-connection.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/bankhoahoc
spring.datasource.username=root
spring.datasource.password=your_password
```

## Lỗi khác:

### Port 8080 đã được sử dụng:
```properties
server.port=8081
```

### Lỗi JWT:
Kiểm tra `jwt.secret` trong `application.properties` đã được cấu hình

### Lỗi CORS:
Kiểm tra `app.cors.allowed-origins` trong `application.properties`

## Kiểm tra log chi tiết:
Thêm vào `application.properties`:
```properties
logging.level.org.springframework.boot=DEBUG
logging.level.org.hibernate=DEBUG
```
