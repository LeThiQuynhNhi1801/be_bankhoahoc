# Hướng Dẫn Sửa Lỗi QR Code Column

## Vấn đề
Lỗi `Data too long for column 'qr_code_url'` xảy ra vì column `qr_code_url` trong database có kiểu `VARCHAR` quá nhỏ để chứa QR code image dạng Base64 (có thể dài vài chục KB).

## Giải pháp

### Cách 1: Chạy SQL Script (Khuyến nghị)

1. Kết nối với MySQL database:
```bash
mysql -u remote_user -p bankhoahoc
```

2. Chạy script:
```sql
ALTER TABLE orders MODIFY COLUMN qr_code_url LONGTEXT;
```

Hoặc chạy file `fix_qr_code_column.sql`:
```bash
mysql -u remote_user -p bankhoahoc < fix_qr_code_column.sql
```

### Cách 2: Chạy qua MySQL Workbench hoặc Client

1. Mở MySQL Workbench hoặc MySQL client
2. Kết nối với database `bankhoahoc`
3. Chạy lệnh:
```sql
ALTER TABLE orders MODIFY COLUMN qr_code_url LONGTEXT;
```

### Cách 3: Tạo database mới (Nếu dữ liệu không quan trọng)

1. Xóa table `orders`:
```sql
DROP TABLE orders;
```

2. Khởi động lại application - Hibernate sẽ tự tạo lại table với column type đúng

## Kiểm tra

Sau khi chạy script, kiểm tra column type:
```sql
DESCRIBE orders;
```

Column `qr_code_url` phải có Type là `longtext` (không phải `varchar`).

## Sau khi sửa

Khởi động lại application và thử tạo order mới. QR code sẽ được lưu thành công!
