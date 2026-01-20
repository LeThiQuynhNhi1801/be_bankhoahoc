# 🔧 Hướng Dẫn Fix Lỗi MySQL: "Host '10.145.37.19' is not allowed to connect"

## ❌ Lỗi hiện tại:
```
Host '10.145.37.19' is not allowed to connect to this MySQL server
```

## ✅ Giải pháp:

### TRÊN LAPTOP (Máy chứa MySQL)

#### Bước 1: Đăng nhập MySQL
```bash
mysql -u root -p
```

#### Bước 2: Tạo user và cấp quyền

**Cách 1: Cho phép từ IP cụ thể (10.145.37.19) - Bảo mật hơn**
```sql
CREATE USER 'remote_user'@'10.145.37.19' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON bankhoahoc.* TO 'remote_user'@'10.145.37.19';
FLUSH PRIVILEGES;
```

**Cách 2: Cho phép từ tất cả IP - Dễ hơn (kém bảo mật)**
```sql
CREATE USER 'remote_user'@'%' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON bankhoahoc.* TO 'remote_user'@'%';
FLUSH PRIVILEGES;
```

**Cách 3: Nếu đã có user rồi, chỉ cần update host**
```sql
-- Kiểm tra user hiện tại
SELECT user, host FROM mysql.user;

-- Update host cho user đã có
UPDATE mysql.user SET host='%' WHERE user='remote_user';
FLUSH PRIVILEGES;
```

#### Bước 3: Kiểm tra cấu hình bind-address
1. Mở file: `C:\ProgramData\MySQL\MySQL Server 8.0\my.ini`
2. Tìm dòng `bind-address`
3. Đảm bảo là: `bind-address = 0.0.0.0` hoặc comment: `#bind-address = 127.0.0.1`
4. Restart MySQL:
```bash
net stop MySQL80
net start MySQL80
```

#### Bước 4: Kiểm tra Firewall
- Windows Firewall → Advanced Settings
- Inbound Rules → Đảm bảo port 3306 được Allow

### TRÊN MÁY HIỆN TẠI

#### Bước 5: Cập nhật application.properties

Mở file `src/main/resources/application.properties` và cập nhật:

```properties
# Nếu dùng user mới
spring.datasource.username=remote_user
spring.datasource.password=your_secure_password

# Hoặc nếu dùng root (không khuyến nghị)
spring.datasource.username=root
spring.datasource.password=your_root_password
```

#### Bước 6: Test kết nối
```bash
# Test từ command line (nếu có MySQL client)
mysql -h 10.145.13.1 -u remote_user -p

# Hoặc test ping
ping 10.145.13.1
```

#### Bước 7: Chạy lại ứng dụng

---

## 🔍 Kiểm tra và Debug

### Kiểm tra users trong MySQL:
```sql
SELECT user, host FROM mysql.user;
```

### Kiểm tra quyền:
```sql
SHOW GRANTS FOR 'remote_user'@'%';
```

### Test kết nối từ máy hiện tại:
```bash
# Nếu có telnet
telnet 10.145.13.1 3306

# Hoặc test với mysql client
mysql -h 10.145.13.1 -u remote_user -p bankhoahoc
```

---

## ⚠️ Lưu ý:

1. **IP có thể thay đổi**: IP `10.145.37.19` có thể thay đổi khi reconnect Wi-Fi. 
   - **Giải pháp**: Dùng `'%'` thay vì IP cụ thể (kém bảo mật hơn nhưng tiện hơn)
   - Hoặc đặt IP tĩnh cho máy hiện tại

2. **Bảo mật**: 
   - Không dùng root user cho remote connection
   - Dùng password mạnh
   - Chỉ cho phép từ mạng nội bộ

3. **Nếu IP laptop thay đổi**: Cập nhật lại IP trong `application.properties`

---

## ✅ Sau khi fix xong:

1. Restart MySQL trên laptop
2. Cập nhật username/password trong application.properties
3. Chạy lại ứng dụng
4. Kiểm tra log không còn lỗi kết nối
