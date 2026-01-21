# Cấu Hình Format QR Code

## Vấn đề Format

Mỗi app ngân hàng Việt Nam có thể yêu cầu format QR code khác nhau. Dưới đây là các format có thể thử:

## Các Format Hỗ Trợ

### Format 1: Đơn giản (Khuyến nghị)
Chỉ có số tài khoản và số tiền:
```properties
payment.qr.template=STK:%s|ST:%s
```
**Kết quả:** `STK:0397492326|ST:190`

### Format 2: Có tên ngân hàng
```properties
payment.qr.template=NH:%s|STK:%s|ST:%s
```
**Kết quả:** `NH:MBBank|STK:0397492326|ST:190`

### Format 3: Có nội dung (Format đầy đủ)
```properties
payment.qr.template=NH:%s|STK:%s|ST:%s|ND:%s
```
**Kết quả:** `NH:MBBank|STK:0397492326|ST:190|ND:Thanh toan don hang ORD...`

### Format 4: Chỉ số tài khoản (cho một số app)
```properties
payment.qr.template=STK:%s
```
**Kết quả:** `STK:0397492326`

## Cách Test

1. **Thay đổi format** trong `application.properties`:
```properties
payment.qr.template=STK:%s|ST:%s
```

2. **Khởi động lại application**

3. **Tạo order mới** và kiểm tra log:
```
Generated QR Content: STK:0397492326|ST:190
```

4. **Quét QR code** bằng app ngân hàng:
   - MBBank
   - Vietcombank
   - VietinBank
   - BIDV
   - v.v.

## Ghi Chú

- **Placeholders:**
  - `%s` đầu tiên = Bank Name (nếu có)
  - `%s` thứ hai = Account Number (STK)
  - `%s` thứ ba = Amount (ST)
  - `%s` thứ tư = Content (ND) (nếu có)

- **Nếu format có 2 placeholder:** `STK:%s|ST:%s` → chỉ dùng STK và ST
- **Nếu format có 3 placeholder:** `NH:%s|STK:%s|ST:%s` → dùng NH, STK, ST
- **Nếu format có 4 placeholder:** `NH:%s|STK:%s|ST:%s|ND:%s` → dùng đầy đủ

## Khuyến nghị

Bắt đầu với **Format 1 (đơn giản)** - hầu hết app ngân hàng đều hỗ trợ:
```properties
payment.qr.template=STK:%s|ST:%s
```

Nếu không hoạt động, thử **Format 2**:
```properties
payment.qr.template=NH:%s|STK:%s|ST:%s
```

## Debug

Kiểm tra log console khi tạo order để xem QR content chính xác:
```
Generated QR Content: STK:0397492326|ST:190
Template used: STK:%s|ST:%s
Amount: 190, Account: 0397492326
```
