# Kiểm Tra Format QR Code

## Vấn đề
QR code được generate thành công nhưng khi quét bằng app ngân hàng thì báo lỗi sai định dạng.

## Kiểm tra

### 1. Xem QR Content trong Log
Khi tạo order, kiểm tra log console sẽ hiển thị:
```
Generated QR Content: NH:MBBank|STK:0397492326|ST:190|ND:Thanh toan don hang ORD...
```

### 2. Test với các format khác nhau

#### Format hiện tại:
```
NH:{bankName}|STK:{accountNumber}|ST:{amount}|ND:{content}
```

#### Format đơn giản hơn (thử):
```
NH:{bankName}|STK:{accountNumber}|ST:{amount}
```

#### Format với Bank Code (nếu cần):
Một số app yêu cầu mã ngân hàng thay vì tên:
- MBBank: mã 970422
- Vietcombank: mã 970436
- ...

### 3. Cách test

1. Tạo order mới
2. Copy QR content từ response hoặc log
3. Test QR content bằng cách:
   - Quét QR code image
   - Hoặc nhập trực tiếp QR content vào app ngân hàng (nếu app hỗ trợ)

### 4. Format có thể cần điều chỉnh

Thử format trong `application.properties`:
```properties
# Format đơn giản (bỏ ND)
payment.qr.template=NH:%s|STK:%s|ST:%s

# Hoặc format với bank code
payment.qr.template=00020101021238570010A0000007270127%02d%s01%02d%s0208%s03%d%s
```

## Giải pháp tạm thời

Nếu format vẫn không đúng, có thể:
1. Dùng thư viện chuyên dụng để generate VietQR (nếu có)
2. Hoặc tạo QR code đơn giản chỉ chứa số tài khoản và số tiền
3. Hoặc sử dụng API của ngân hàng để generate QR code chuẩn

## Debug

1. Kiểm tra log console để xem QR content chính xác
2. Test QR content bằng cách copy vào text editor
3. Thử quét bằng nhiều app ngân hàng khác nhau
