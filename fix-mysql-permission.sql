-- ============================================
-- Script FIX lỗi: Host '10.145.37.19' is not allowed to connect
-- Chạy script này trên LAPTOP (máy chứa MySQL)
-- ============================================

-- Bước 1: Đăng nhập MySQL
-- mysql -u root -p

-- Bước 2: Cho phép kết nối từ IP cụ thể (10.145.37.19)
-- Cách 1: Tạo user mới cho IP này (KHUYẾN NGHỊ)
CREATE USER 'remote_user'@'10.145.37.19' IDENTIFIED BY 'your_password_here';
GRANT ALL PRIVILEGES ON bankhoahoc.* TO 'remote_user'@'10.145.37.19';
FLUSH PRIVILEGES;

-- Cách 2: Cho phép từ tất cả IP (Dễ hơn nhưng kém bảo mật hơn)
CREATE USER 'remote_user'@'%' IDENTIFIED BY 'your_password_here';
GRANT ALL PRIVILEGES ON bankhoahoc.* TO 'remote_user'@'%';
FLUSH PRIVILEGES;

-- Cách 3: Nếu đã có user 'remote_user'@'%' rồi, chỉ cần update:
-- UPDATE mysql.user SET host='%' WHERE user='remote_user' AND host='localhost';
-- FLUSH PRIVILEGES;

-- Kiểm tra users
SELECT user, host FROM mysql.user WHERE user LIKE '%remote%' OR user = 'root';

-- Kiểm tra quyền
SHOW GRANTS FOR 'remote_user'@'10.145.37.19';
-- Hoặc
SHOW GRANTS FOR 'remote_user'@'%';
