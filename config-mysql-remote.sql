-- ============================================
-- Script cấu hình MySQL để cho phép Remote Connection
-- Chạy script này trên LAPTOP (máy chứa MySQL)
-- ============================================

-- Bước 1: Tạo user mới cho remote connection (KHUYẾN NGHỊ)
-- Thay YOUR_SECURE_PASSWORD bằng password bạn muốn
CREATE USER 'remote_user'@'%' IDENTIFIED BY 'YOUR_SECURE_PASSWORD';

-- Bước 2: Cấp quyền cho user
GRANT ALL PRIVILEGES ON bankhoahoc.* TO 'remote_user'@'%';

-- Nếu muốn cho phép tất cả databases (KHÔNG KHUYẾN NGHỊ):
-- GRANT ALL PRIVILEGES ON *.* TO 'remote_user'@'%';

-- Bước 3: Apply changes
FLUSH PRIVILEGES;

-- Bước 4: Kiểm tra users đã tạo
SELECT user, host FROM mysql.user WHERE user = 'remote_user';

-- ============================================
-- Nếu muốn dùng root user (KHÔNG KHUYẾN NGHỊ - Chỉ để test)
-- ============================================
-- CREATE USER 'root'@'%' IDENTIFIED BY 'your_root_password';
-- GRANT ALL PRIVILEGES ON *.* TO 'root'@'%';
-- FLUSH PRIVILEGES;
