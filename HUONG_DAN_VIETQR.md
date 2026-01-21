# Hướng Dẫn Tích Hợp VietQR.io API

## Đã tích hợp VietQR.io

Hệ thống đã được tích hợp với **VietQR.io** để tạo QR code thanh toán theo chuẩn VietQR/VNPay. Dịch vụ này **miễn phí** và không cần API key.

## Cấu hình

### 1. Cấu hình trong application.properties

```properties
# Thông tin tài khoản ngân hàng
payment.qr.bank-name=MBBank
payment.qr.account-number=0397492326
payment.qr.account-name=LE THI QUYNH NHI

# Mã ngân hàng (BIN Code)
# MBBank = 970422
# Vietcombank = 970436
# VietinBank = 970415
# BIDV = 970418
# Techcombank = 970407
# ACB = 970416
# ... (xem danh sách đầy đủ tại https://vietqr.io/)
payment.qr.bank-code=970422

# VietQR API Configuration
vietqr.api.url=https://img.vietqr.io/image/
vietqr.enabled=true
```

### 2. Mã ngân hàng (Bank Code/BIN)

Cập nhật `payment.qr.bank-code` theo ngân hàng của bạn:
- **MBBank**: 970422
- **Vietcombank**: 970436
- **VietinBank**: 970415
- **BIDV**: 970418
- **Techcombank**: 970407
- **ACB**: 970416

Xem danh sách đầy đủ tại: https://vietqr.io/

## Cách hoạt động

1. Khi tạo order, hệ thống sẽ gọi VietQR.io API
2. API trả về URL ảnh QR code chuẩn VietQR
3. Hệ thống download ảnh và convert sang Base64
4. QR code được lưu vào database và trả về cho frontend

## Format QR Code

QR code được tạo theo chuẩn VietQR với đầy đủ thông tin:
- Tên ngân hàng
- Số tài khoản
- Tên chủ tài khoản
- Số tiền
- Nội dung thanh toán

## Ưu điểm

✅ **Chuẩn VietQR/VNPay** - Tương thích với tất cả app ngân hàng Việt Nam
✅ **Miễn phí** - Không cần đăng ký API key
✅ **Đầy đủ thông tin** - Hiển thị logo ngân hàng, tên ngân hàng
✅ **Dễ quét** - QR code chất lượng cao, dễ đọc

## Test

1. Tạo order mới
2. Kiểm tra QR code trong response
3. Quét bằng app ngân hàng - sẽ hiển thị đầy đủ thông tin:
   - Tên ngân hàng
   - Số tài khoản
   - Tên chủ tài khoản
   - Số tiền
   - Nội dung thanh toán

## Troubleshooting

### QR code không hiển thị
- Kiểm tra `vietqr.enabled=true`
- Kiểm tra internet connection
- Kiểm tra bank code có đúng không

### Lỗi khi gọi API
- Hệ thống sẽ tự động fallback về phương thức tạo QR code local
- Kiểm tra log để xem lỗi cụ thể

## Tắt VietQR (sử dụng local generation)

Nếu muốn tắt VietQR và dùng phương thức tự tạo:
```properties
vietqr.enabled=false
```
