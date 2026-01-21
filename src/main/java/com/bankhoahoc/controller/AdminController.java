package com.bankhoahoc.controller;

import com.bankhoahoc.dto.CourseDTO;
import com.bankhoahoc.dto.OrderDTO;
import com.bankhoahoc.dto.UserDTO;
import com.bankhoahoc.entity.Order;
import com.bankhoahoc.entity.User;
import com.bankhoahoc.service.AdminService;
import com.bankhoahoc.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/admin")
@Tag(name = "Admin", description = "API quản trị dành cho ADMIN")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    AdminService adminService;

    @Autowired
    CourseService courseService;

    // ========== USER MANAGEMENT ==========

    @Operation(summary = "Lấy danh sách tất cả người dùng", 
               description = "Lấy danh sách tất cả người dùng trong hệ thống. Chỉ ADMIN có quyền truy cập",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
    })
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @Operation(summary = "Lấy thông tin chi tiết người dùng", 
               description = "Lấy thông tin chi tiết của một người dùng theo ID",
               security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserById(
            @Parameter(description = "ID của người dùng") @PathVariable Long userId) {
        try {
            return ResponseEntity.ok(adminService.getUserById(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Cập nhật role của người dùng", 
               description = "Thay đổi role của người dùng (STUDENT, INSTRUCTOR, ADMIN). Không thể thay đổi role của admin",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<?> updateUserRole(
            @Parameter(description = "ID của người dùng") @PathVariable Long userId,
            @Parameter(description = "Role mới (STUDENT, INSTRUCTOR, ADMIN)") @RequestParam User.Role role) {
        try {
            return ResponseEntity.ok(adminService.updateUserRole(userId, role));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Bật/tắt trạng thái người dùng", 
               description = "Kích hoạt hoặc vô hiệu hóa tài khoản người dùng. Không thể disable admin",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/users/{userId}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(
            @Parameter(description = "ID của người dùng") @PathVariable Long userId) {
        try {
            return ResponseEntity.ok(adminService.toggleUserStatus(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Xóa người dùng", 
               description = "Xóa người dùng khỏi hệ thống. Không thể xóa admin",
               security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(
            @Parameter(description = "ID của người dùng") @PathVariable Long userId) {
        try {
            adminService.deleteUser(userId);
            return ResponseEntity.ok("User deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ========== ORDER MANAGEMENT ==========

    @Operation(summary = "Lấy danh sách tất cả đơn hàng", 
               description = "Lấy danh sách tất cả đơn hàng trong hệ thống, sắp xếp theo thời gian tạo mới nhất",
               security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/orders")
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        return ResponseEntity.ok(adminService.getAllOrders());
    }

    @Operation(summary = "Lấy đơn hàng theo trạng thái", 
               description = "Lấy danh sách đơn hàng theo trạng thái (PENDING, PAID, CANCELLED, REFUNDED)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/orders/status/{status}")
    public ResponseEntity<List<OrderDTO>> getOrdersByStatus(
            @Parameter(description = "Trạng thái đơn hàng") @PathVariable Order.OrderStatus status) {
        return ResponseEntity.ok(adminService.getOrdersByStatus(status));
    }

    @Operation(summary = "Lấy chi tiết đơn hàng", 
               description = "Lấy thông tin chi tiết của một đơn hàng theo ID",
               security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> getOrderById(
            @Parameter(description = "ID của đơn hàng") @PathVariable Long orderId) {
        try {
            return ResponseEntity.ok(adminService.getOrderById(orderId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Xác nhận đơn hàng", 
               description = "Xác nhận đơn hàng đã thanh toán. Hệ thống sẽ tự động enroll học viên vào các khóa học",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Xác nhận đơn hàng thành công. Học viên đã được enroll vào khóa học"),
            @ApiResponse(responseCode = "400", description = "Đơn hàng không tồn tại hoặc đã được xác nhận")
    })
    @PutMapping("/orders/{orderId}/confirm")
    public ResponseEntity<?> confirmOrder(
            @Parameter(description = "ID của đơn hàng") @PathVariable Long orderId) {
        try {
            return ResponseEntity.ok(adminService.confirmOrder(orderId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Hủy đơn hàng", 
               description = "Hủy đơn hàng. Chỉ có thể hủy đơn hàng ở trạng thái PENDING",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/orders/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(
            @Parameter(description = "ID của đơn hàng") @PathVariable Long orderId) {
        try {
            return ResponseEntity.ok(adminService.cancelOrder(orderId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Cập nhật trạng thái đơn hàng", 
               description = "Cập nhật trạng thái đơn hàng. Nếu đổi sang PAID, hệ thống sẽ tự động enroll học viên",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/orders/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @Parameter(description = "ID của đơn hàng") @PathVariable Long orderId,
            @Parameter(description = "Trạng thái mới") @RequestParam Order.OrderStatus status) {
        try {
            return ResponseEntity.ok(adminService.updateOrderStatus(orderId, status));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ========== COURSE MANAGEMENT ==========

    @Operation(summary = "Lấy danh sách tất cả khóa học", 
               description = "Lấy danh sách tất cả khóa học trong hệ thống",
               security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/courses")
    public ResponseEntity<List<CourseDTO>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @Operation(summary = "Xóa khóa học", 
               description = "Xóa khóa học khỏi hệ thống. Hành động này không thể hoàn tác",
               security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/courses/{courseId}")
    public ResponseEntity<?> deleteCourse(
            @Parameter(description = "ID của khóa học") @PathVariable Long courseId) {
        try {
            adminService.deleteCourse(courseId);
            return ResponseEntity.ok("Course deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ========== STATISTICS ==========

    @Operation(summary = "Lấy thống kê tổng quan", 
               description = "Lấy thống kê tổng quan về hệ thống (số lượng user, đơn hàng, doanh thu, v.v.)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(adminService.getStatistics());
    }
}
