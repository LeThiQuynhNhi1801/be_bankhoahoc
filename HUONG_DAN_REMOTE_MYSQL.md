# Hướng Dẫn Kết Nối MySQL Remote

## Bước 1: Cấu hình MySQL trên Laptop (Máy chứa database)

### 1.1. Kiểm tra MySQL đang chạy trên port nào
Mặc định MySQL chạy trên port 3306.

### 1.2. Cấu hình MySQL cho phép kết nối từ xa

#### Cách 1: Sử dụng MySQL Workbench hoặc Command Line

1. **Đăng nhập MySQL trên laptop:**
```bash
mysql -u root -p
```

2. **Tạo user cho phép kết nối từ xa (Khuyến nghị):**
```sql
-- Tạo user mới (thay YOUR_PASSWORD bằng password bạn muốn)
CREATE USER 'remote_user'@'%' IDENTIFIED BY 'YOUR_PASSWORD';

-- Cấp quyền cho user
GRANT ALL PRIVILEGES ON bankhoahoc.* TO 'remote_user'@'%';

-- Hoặc nếu muốn dùng root user (KHÔNG KHUYẾN NGHỊ vì bảo mật)
-- GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY 'YOUR_PASSWORD';

-- Apply changes
FLUSH PRIVILEGES;

-- Kiểm tra users
SELECT user, host FROM mysql.user;
```

3. **Kiểm tra bind-address trong MySQL config:**
   - Tìm file `my.ini` (Windows) hoặc `my.cnf` (Linux/Mac)
   - Thường ở: `C:\ProgramData\MySQL\MySQL Server 8.0\my.ini` hoặc `/etc/mysql/my.cnf`
   - Tìm dòng `bind-address` và đổi thành:
   ```
   bind-address = 0.0.0.0
   ```
   - Hoặc comment dòng đó:
   ```
   #bind-address = 127.0.0.1
   ```

4. **Restart MySQL service:**
```bash
# Windows
net stop mysql80
net start mysql80

# Hoặc qua Services
# Services -> MySQL80 -> Restart
```

### 1.3. Cấu hình Firewall trên Laptop

1. **Mở Windows Firewall:**
   - Control Panel -> Windows Defender Firewall -> Advanced Settings

2. **Tạo Inbound Rule:**
   - New Rule -> Port -> TCP -> Specific local ports: `3306`
   - Allow the connection
   - Chọn profiles (Domain, Private, Public)
   - Đặt tên: "MySQL Port 3306"

## Bước 2: Cấu hình Application trên Máy Hiện Tại

### 2.1. Xác định IP của Laptop

Từ kết quả `ipconfig`, bạn có 2 IP:
- **Wi-Fi**: `10.145.13.1` (Dùng IP này nếu cả 2 máy cùng mạng Wi-Fi)
- **vEthernet (WSL)**: `172.27.224.1` (Nếu dùng WSL)

**Chọn IP phù hợp:**
- Nếu cả 2 máy cùng mạng Wi-Fi: dùng `10.145.13.1`
- Nếu máy hiện tại dùng WSL: có thể cần dùng `172.27.224.1`

### 2.2. Cập nhật application.properties

```properties
# Thay localhost bằng IP của laptop
spring.datasource.url=jdbc:mysql://10.145.13.1:3306/bankhoahoc?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
spring.datasource.username=remote_user
spring.datasource.password=YOUR_PASSWORD
```

## Bước 3: Test Kết Nối

### 3.1. Test từ máy hiện tại:

```bash
# Test kết nối MySQL
mysql -h 10.145.13.1 -u remote_user -p

# Hoặc từ Java application
# Chạy ứng dụng và xem log
```

### 3.2. Kiểm tra kết nối:

Nếu thấy lỗi:
- **Access denied**: Kiểm tra user/password và GRANT privileges
- **Can't connect**: Kiểm tra firewall và bind-address
- **Timeout**: Kiểm tra cả 2 máy có cùng mạng không

## Lưu Ý Bảo Mật

1. **KHÔNG dùng root user** cho remote connection
2. **Tạo user riêng** với password mạnh
3. **Chỉ grant quyền cần thiết** (không dùng `ALL PRIVILEGES` nếu không cần)
4. **Cân nhắc sử dụng SSL** cho production
5. **Chỉ cho phép kết nối từ IP cụ thể** nếu biết:
   ```sql
   CREATE USER 'remote_user'@'10.145.13.XX' IDENTIFIED BY 'password';
   ```

## Troubleshooting

### Lỗi: Access denied for user
- Kiểm tra user đã được tạo với host '%'
- Kiểm tra password đúng chưa
- Chạy lại `FLUSH PRIVILEGES;`

### Lỗi: Can't connect to MySQL server
- Kiểm tra MySQL đang chạy trên laptop
- Kiểm tra firewall đã mở port 3306
- Kiểm tra bind-address = 0.0.0.0
- Kiểm tra cả 2 máy cùng mạng

### Lỗi: Connection timeout
- Ping IP của laptop: `ping 10.145.13.1`
- Kiểm tra port: `telnet 10.145.13.1 3306`
- Kiểm tra firewall Windows
