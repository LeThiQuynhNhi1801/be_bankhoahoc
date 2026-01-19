# Backend Bán Khóa Học

Backend API cho hệ thống bán khóa học được xây dựng bằng Java Spring Boot.

## Công nghệ sử dụng

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Security** - Xác thực và phân quyền
- **JWT** - Token-based authentication
- **Spring Data JPA** - ORM và database access
- **MySQL** - Database
- **Maven** - Build tool
- **Lombok** - Giảm boilerplate code
- **Springdoc OpenAPI (Swagger)** - API documentation và testing

## Cấu trúc dự án

```
src/main/java/com/bankhoahoc/
├── controller/      # REST Controllers
├── service/         # Business logic
├── repository/      # Data access layer
├── entity/          # JPA Entities
├── dto/             # Data Transfer Objects
├── security/        # Security configuration
├── config/          # Configuration classes (Swagger, etc.)
├── util/            # Utility classes
└── exception/       # Exception handlers
```

## Tính năng

### Authentication & Authorization
- Đăng ký tài khoản
- Đăng nhập với JWT token
- Phân quyền: ADMIN, INSTRUCTOR, STUDENT

### Quản lý khóa học
- Xem danh sách khóa học đã publish
- Tìm kiếm khóa học
- Tạo, sửa, xóa khóa học (Instructor)
- Publish/Unpublish khóa học
- Quản lý nội dung khóa học

### Quản lý danh mục
- Xem danh sách danh mục
- CRUD danh mục (Admin)

### Đặt hàng & Thanh toán
- Tạo đơn hàng
- Xem lịch sử đơn hàng
- Cập nhật trạng thái đơn hàng (Admin)

### Ghi danh khóa học
- Ghi danh vào khóa học
- Theo dõi tiến độ học tập
- Xem danh sách khóa học đã ghi danh

## Cài đặt và chạy

### Yêu cầu
- JDK 17 hoặc cao hơn
- Maven 3.6+
- MySQL 8.0+

### Cấu hình database

1. Tạo database MySQL:
```sql
CREATE DATABASE bankhoahoc;
```

2. Cập nhật file `application.properties`:
```properties
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### Chạy ứng dụng

```bash
# Build project
mvn clean install

# Chạy ứng dụng
mvn spring-boot:run
```

Ứng dụng sẽ chạy tại: `http://localhost:8080/api`

## Swagger UI

Sau khi chạy ứng dụng, bạn có thể truy cập Swagger UI để xem và test các API:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **API Docs (JSON)**: `http://localhost:8080/api-docs`

Swagger UI cung cấp:
- Tài liệu API đầy đủ và tương tác
- Test các endpoints trực tiếp từ trình duyệt
- Xem các request/response schemas
- Xác thực JWT token để test các protected endpoints

**Lưu ý**: Để test các API yêu cầu authentication:
1. Đăng nhập qua endpoint `/auth/login` để lấy JWT token
2. Nhấn nút "Authorize" ở góc trên bên phải của Swagger UI
3. Nhập token theo format: `Bearer <your-token>`
4. Nhấn "Authorize" để xác thực

## API Endpoints

### Authentication
- `POST /api/auth/register` - Đăng ký
- `POST /api/auth/login` - Đăng nhập

### Courses (Public)
- `GET /api/courses/public` - Danh sách khóa học đã publish
- `GET /api/courses/public/{id}` - Chi tiết khóa học
- `GET /api/courses/public/search?keyword=...` - Tìm kiếm
- `GET /api/courses/public/category/{categoryId}` - Khóa học theo danh mục

### Courses (Protected - Instructor/Admin)
- `GET /api/courses/my-courses` - Khóa học của tôi
- `POST /api/courses` - Tạo khóa học
- `PUT /api/courses/{id}` - Cập nhật khóa học
- `DELETE /api/courses/{id}` - Xóa khóa học
- `PUT /api/courses/{id}/publish` - Publish khóa học

### Categories
- `GET /api/categories` - Danh sách danh mục
- `GET /api/categories/{id}` - Chi tiết danh mục
- `POST /api/categories` - Tạo danh mục (Admin)
- `PUT /api/categories/{id}` - Cập nhật danh mục (Admin)
- `DELETE /api/categories/{id}` - Xóa danh mục (Admin)

### Orders
- `GET /api/orders/my-orders` - Đơn hàng của tôi
- `GET /api/orders/{id}` - Chi tiết đơn hàng
- `POST /api/orders` - Tạo đơn hàng
- `PUT /api/orders/{id}/status` - Cập nhật trạng thái (Admin)

### Enrollments
- `GET /api/enrollments/my-enrollments` - Khóa học đã ghi danh
- `GET /api/enrollments/check?courseId=...` - Kiểm tra đã ghi danh chưa
- `POST /api/enrollments/enroll/{courseId}` - Ghi danh khóa học
- `PUT /api/enrollments/{id}/progress` - Cập nhật tiến độ

## Authentication

Sử dụng JWT token trong header:
```
Authorization: Bearer <token>
```

## License

MIT
