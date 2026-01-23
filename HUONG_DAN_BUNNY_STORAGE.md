# Hướng dẫn lấy thông tin Bunny Storage từ Dashboard

## Bước 1: Lấy Zone Name

1. Đăng nhập vào https://dash.bunny.net
2. Vào **Storage** → Chọn zone của bạn
3. Zone Name sẽ hiển thị ở đầu trang (ví dụ: `bankhoahoc1`)
4. Hoặc xem trong tab **Overview** → **Storage Zone Name**

**Lưu ý:** Zone Name có thể có hoặc không có dấu gạch ngang, cần copy chính xác.

---

## Bước 2: Lấy HTTP API Password (Read-Write)

1. Vào **Storage** → Chọn zone của bạn
2. Click vào tab **"FTP & API access"** (hoặc **"FTP & HTTP API"**)
3. Tìm mục **"Password"** (KHÔNG phải "Read-only password")
4. Click nút **"Show/Hide"** để hiển thị password
5. Copy password đó (đây là Read-Write password, có quyền upload)

**⚠️ QUAN TRỌNG:**
- Phải dùng **Password chính** (Read-Write), KHÔNG dùng "Read-only password"
- Read-only password chỉ có quyền đọc, không thể upload file
- Password này sẽ được dùng cho `bunny.storage.access-key`

---

## Bước 3: Lấy Base URL (Hostname) cho Storage API

1. Vào **Storage** → Chọn zone → tab **"FTP & API access"**
2. Copy đúng **Hostname** (ví dụ: `storage.bunnycdn.com`)
3. Base URL sẽ là: `https://<hostname>` (ví dụ: `https://storage.bunnycdn.com`)

---

## Bước 4: Lấy CDN URL (Pull Zone)

CDN URL dùng để truy cập file sau khi upload:

1. Vào **Storage** → Chọn zone → Tab **"CDN"** hoặc **"Pull Zones"**
2. Xem danh sách **Connected pull zones**
3. Copy **hostname** của Pull Zone (ví dụ: `bankhoahocfiles.b-cdn.net`)
4. Format CDN URL: `https://<hostname>` (ví dụ: `https://bankhoahocfiles.b-cdn.net`)

**Lưu ý:** Nếu chưa có Pull Zone, có thể tạo mới hoặc dùng CDN URL mặc định.

---

## Cập nhật vào application.properties

Sau khi lấy đủ thông tin, cập nhật vào file `application.properties`:

```properties
# Zone Name (từ Bước 1)
bunny.storage.zone-name=bankhoahoc1

# HTTP API Password - Read-Write (từ Bước 2)
bunny.storage.access-key=<PASSWORD_READ_WRITE>

# Base URL theo Hostname (từ Bước 3)
bunny.storage.base-url=https://storage.bunnycdn.com

# CDN URL từ Pull Zone (từ Bước 4)
bunny.storage.cdn-url=https://bankhoahocfiles.b-cdn.net
```

---

## Kiểm tra lại sau khi cập nhật

1. **Restart application**
2. **Test upload file Word** → Nếu thành công, không còn lỗi 401
3. **Kiểm tra log** → Xem URL upload có đúng format không

---

## Format URL Upload đúng

URL upload sẽ có format:
```
https://<base-url>/<zone-name>/<folder>/<filename>
```

Ví dụ:
```
https://storage.bunnycdn.com/bankhoahoc1/documents/course-contents/14/file.docx
```

---

## Troubleshooting

### Lỗi 401 Unauthorized
- ✅ Kiểm tra đã dùng **Read-Write password** chưa (không phải Read-only)
- ✅ Kiểm tra **Zone Name** có đúng không
- ✅ Kiểm tra **Access Key** có copy đầy đủ không (không thiếu ký tự)

### Lỗi 404 Not Found
- ✅ Kiểm tra **Zone Name** có đúng không
- ✅ Kiểm tra **Base URL** có đúng region không

### Upload thành công nhưng không truy cập được file
- ✅ Kiểm tra **CDN URL** có đúng Pull Zone không
- ✅ Kiểm tra Pull Zone đã kết nối với Storage Zone chưa
