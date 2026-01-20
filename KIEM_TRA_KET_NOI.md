# Hướng Dẫn Kiểm Tra Kết Nối MySQL Remote

## Trên LAPTOP (Máy chứa MySQL)

### 1. Kiểm tra MySQL đang chạy:
```bash
# Windows
Get-Service -Name "*mysql*"

# Hoặc kiểm tra port 3306
netstat -an | findstr 3306
```

### 2. Cấu hình MySQL cho phép remote:

#### a) Mở MySQL Command Line:
```bash
mysql -u root -p
```

#### b) Chạy script config-mysql-remote.sql:
```sql
-- Tạo user mới
CREATE USER 'remote_user'@'%' IDENTIFIED BY 'your_password_here';

-- Cấp quyền
GRANT ALL PRIVILEGES ON bankhoahoc.* TO 'remote_user'@'%';

-- Apply
FLUSH PRIVILEGES;
```

#### c) Cấu hình bind-address:
1. Tìm file config: `C:\ProgramData\MySQL\MySQL Server 8.0\my.ini`
2. Tìm dòng `bind-address = 127.0.0.1`
3. Đổi thành: `bind-address = 0.0.0.0` hoặc comment: `#bind-address = 127.0.0.1`
4. Restart MySQL:
```bash
net stop MySQL80
net start MySQL80
```

#### d) Mở Firewall:
1. Windows Firewall -> Advanced Settings
2. Inbound Rules -> New Rule
3. Port -> TCP -> 3306 -> Allow connection
4. Chọn tất cả profiles -> Đặt tên "MySQL Remote"

## Trên MÁY HIỆN TẠI (Máy chạy ứng dụng)

### 1. Test kết nối từ command line:
```bash
# Test kết nối (nếu có MySQL client)
mysql -h 10.145.13.1 -u remote_user -p

# Hoặc test bằng telnet (kiểm tra port)
telnet 10.145.13.1 3306
```

### 2. Kiểm tra ping:
```bash
ping 10.145.13.1
```

### 3. Cập nhật application.properties:
Đảm bảo đã cập nhật:
- IP: `10.145.13.1` (hoặc IP đúng của laptop)
- Username: `remote_user` (hoặc user bạn đã tạo)
- Password: password bạn đã đặt

### 4. Chạy ứng dụng và kiểm tra log:
Nếu kết nối thành công, sẽ thấy trong log:
```
HikariPool-1 - Starting...
HikariPool-1 - Start completed.
```

Nếu lỗi, sẽ thấy:
- `Access denied`: Sai username/password hoặc chưa grant quyền
- `Can't connect`: Firewall hoặc bind-address chưa đúng
- `Connection refused`: MySQL chưa chạy hoặc port sai

## Troubleshooting

### Lỗi: "Access denied for user"
```sql
-- Trên laptop, kiểm tra lại:
SELECT user, host FROM mysql.user;
SHOW GRANTS FOR 'remote_user'@'%';
```

### Lỗi: "Can't connect to MySQL server"
1. Kiểm tra MySQL đang chạy: `Get-Service MySQL80`
2. Kiểm tra bind-address trong my.ini
3. Kiểm tra firewall đã mở port 3306
4. Test: `telnet 10.145.13.1 3306`

### Lỗi: "Connection timeout"
1. Kiểm tra cả 2 máy cùng mạng: `ping 10.145.13.1`
2. Kiểm tra firewall trên cả 2 máy
3. Kiểm tra IP đúng chưa (có thể IP thay đổi khi reconnect Wi-Fi)
