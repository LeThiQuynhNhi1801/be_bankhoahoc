# Hướng Dẫn Cấu Hình Thanh Toán QR Code

## Cấu hình trong application.properties

Đã cấu hình mặc định:
```properties
payment.qr.bank-name=Vietcombank
payment.qr.account-number=1234567890
payment.qr.account-name=CONG TY BAN KHOA HOC
payment.qr.template=NH:%s|STK:%s|ST:%s|ND:%s
```

## Cập nhật thông tin tài khoản

Cập nhật trong `src/main/resources/application.properties`:

```properties
# Thay đổi thành số tài khoản thực tế
payment.qr.account-number=0987654321

# Thay đổi tên chủ tài khoản
payment.qr.account-name=TEN CONG TY BAN KHOA HOC

# Thay đổi tên ngân hàng
payment.qr.bank-name=VietinBank
```

## Format QR Code

QR code được tạo theo chuẩn Việt Nam với format:
```
NH:{bankName}|STK:{accountNumber}|ST:{amount}|ND:{content}
```

Ví dụ:
```
NH:Vietcombank|STK:1234567890|ST:500000|ND:Thanh toan don hang ORD1234567890
```

## Cách sử dụng

### 1. Tạo đơn hàng
```http
POST /api/orders
Authorization: Bearer <token>

{
  "courseIds": [1, 2, 3]
}
```

### 2. Response sẽ chứa QR code
```json
{
  "id": 1,
  "orderNumber": "ORD1234567890",
  "totalAmount": 500000,
  "paymentMethod": "QR_CODE",
  "qrCodeUrl": "data:image/png;base64,iVBORw0KGgoAAAANS...",
  "qrCodeContent": "NH:Vietcombank|STK:1234567890|ST:500000|ND:...",
  "bankName": "Vietcombank",
  "accountNumber": "1234567890",
  "accountName": "CONG TY BAN KHOA HOC",
  "status": "PENDING",
  "items": [...]
}
```

### 3. Hiển thị QR code
- Frontend có thể sử dụng trực tiếp `qrCodeUrl` (Base64 image) để hiển thị
- Hoặc quét `qrCodeContent` để thanh toán bằng app ngân hàng

## Lưu ý

1. **Số tài khoản**: Cập nhật `payment.qr.account-number` với số tài khoản thực tế
2. **Tên tài khoản**: Đảm bảo tên chính xác để người dùng nhận diện
3. **Ngân hàng**: Chọn ngân hàng phù hợp
4. **Format**: Format mặc định phù hợp với hầu hết app ngân hàng Việt Nam

## Cập nhật trạng thái đơn hàng

Sau khi người dùng thanh toán, Admin cần cập nhật trạng thái:
```http
PUT /api/orders/{id}/status?status=PAID
Authorization: Bearer <admin-token>
```

Khi status = PAID, hệ thống sẽ tự động enroll học viên vào các khóa học.
