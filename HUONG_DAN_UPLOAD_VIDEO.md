# Hướng dẫn Upload Video lên Bunny Stream

## ⚠️ QUAN TRỌNG: Sử dụng đúng endpoint

### ✅ Endpoint ĐÚNG để upload VIDEO (KHUYẾN NGHỊ):
```
POST /api/chapters/{chapterId}/video
```

**Cách sử dụng (Đơn giản nhất):**
```
POST /api/chapters/{chapterId}/video
Body (form-data):
- video: [file video]
- title: "Tiêu đề video" (tùy chọn, dùng để đặt tên trên Bunny Stream)
```
→ Video được upload lên Bunny Stream
→ URL được lưu trực tiếp vào Chapter (KHÔNG cần tạo CourseContent)
→ Đơn giản, nhanh chóng, không cần bước trung gian

### ✅ Endpoint ALTERNATIVE (Nếu muốn tạo CourseContent trước):
```
POST /api/course-contents/{contentId}/video
```

**Cách sử dụng:**
1. Tạo CourseContent trước (bài giảng) cho chapter:
   ```
   POST /api/course-contents/chapters/{chapterId}
   Body (form-data):
   - title: "Bài 1: Giới thiệu"
   - description: "Mô tả bài học"
   - orderIndex: 1
   ```
   → Lấy `contentId` từ response

2. Upload video lên Bunny Stream:
   ```
   POST /api/course-contents/{contentId}/video
   Body (form-data):
   - video: [file video]
   ```
   → Video sẽ được upload lên Bunny Stream, URL được lưu vào database

### ❌ Endpoint SAI (KHÔNG dùng cho video):
```
POST /api/chapters/{chapterId}/documents
```
**Endpoint này CHỈ dành cho tài liệu (PDF, DOC, DOCX, TXT, v.v.), KHÔNG phải video!**

Nếu bạn upload video vào endpoint này:
- ❌ Video sẽ bị lưu vào project (không phải Bunny Stream)
- ❌ Sẽ bị từ chối với thông báo lỗi rõ ràng
- ❌ Tốn dung lượng server

## Luồng upload video đúng:

### Cách 1: Upload trực tiếp cho chapter (KHUYẾN NGHỊ - Đơn giản nhất)
```
1. Upload video lên Bunny Stream cho chapter
   POST /api/chapters/{chapterId}/video
   Body: video file + title (tùy chọn)
   → Video được upload lên Bunny Stream
   → URL được lưu trực tiếp vào Chapter.videoUrl
   → KHÔNG cần tạo CourseContent, đơn giản và nhanh chóng

2. Xem video
   GET /api/chapters/{chapterId}
   → Trả về ChapterDTO với videoUrl từ Bunny Stream
   → Hoặc GET /api/chapters/course/{courseId} để xem tất cả chapters với video
```

### Cách 2: Tạo CourseContent trước rồi upload video
```
1. Tạo CourseContent (bài giảng)
   POST /api/course-contents/chapters/{chapterId}
   → Nhận contentId

2. Upload video lên Bunny Stream
   POST /api/course-contents/{contentId}/video
   → Video được upload lên Bunny Stream
   → URL từ Bunny Stream được lưu vào database

3. Xem video
   GET /api/learning/contents/{contentId}
   → Trả về videoUrl từ Bunny Stream
```

## Kiểm tra video đã upload:

1. Xem logs trong console:
   ```
   === Starting video upload to Bunny Stream ===
   Step 1: Creating video entry in Bunny Stream...
   ✓ Created video in Bunny Stream with ID: ...
   Step 2: Uploading video file to Bunny Stream...
   ✓ Video file uploaded successfully. Video ID: ...
   Step 3: Waiting for Bunny Stream to process video...
   Step 4: Getting video embed URL...
   ✓ Video uploaded successfully. Embed URL: https://iframe.mediadelivery.net/embed/...
   === Video upload completed ===
   ```

2. Kiểm tra database:
   ```sql
   SELECT video_url FROM course_contents WHERE id = {contentId};
   ```
   → URL phải có dạng: `https://iframe.mediadelivery.net/embed/{libraryId}/{videoId}`

3. Kiểm tra trên Bunny Stream Dashboard:
   - Đăng nhập vào https://bunny.net/stream
   - Vào Library → Videos
   - Video sẽ hiển thị trong danh sách

## Lưu ý:

- ✅ Video được upload lên Bunny Stream (CDN), không lưu vào project
- ✅ Chỉ lưu URL từ Bunny Stream vào database
- ✅ Video có thể cần 5-10 giây để xử lý trên Bunny Stream
- ✅ Sau khi upload, video sẽ có status 2 (Uploading) → 3 (Processing) → 4 (Finished)

## Troubleshooting:

### Video không hiện trên Bunny Stream:
1. Kiểm tra logs xem có lỗi không
2. Kiểm tra `bunny.stream.enabled=true` trong `application.properties`
3. Kiểm tra API Key và Library ID có đúng không
4. Đợi thêm vài giây (video cần thời gian xử lý)

### Lỗi "Video files are not allowed here":
- Bạn đang upload video vào endpoint upload document
- Hãy dùng endpoint đúng: `POST /api/course-contents/{contentId}/video`
