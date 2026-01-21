package com.bankhoahoc.controller;

import com.bankhoahoc.dto.OrderCreateDTO;
import com.bankhoahoc.dto.OrderDTO;
import com.bankhoahoc.entity.Order;
import com.bankhoahoc.security.UserPrincipal;
import com.bankhoahoc.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/orders")
@Tag(name = "Orders", description = "API quản lý đơn hàng")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    OrderService orderService;

    @Operation(summary = "Lấy danh sách đơn hàng của tôi", 
               description = "Trả về tất cả đơn hàng của người dùng hiện tại",
               security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/my-orders")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<List<OrderDTO>> getMyOrders(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(orderService.getOrdersByUser(userPrincipal.getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<?> getOrderById(@PathVariable Long id,
                                          @AuthenticationPrincipal UserPrincipal userPrincipal) {
        logger.info("Received request to get order by ID: {} from user: {}", id, 
            userPrincipal != null ? userPrincipal.getId() : "anonymous");
        try {
            OrderDTO order = orderService.getOrderById(id);
            logger.info("Retrieved order {} successfully", id);
            
            // Check if user owns this order or is admin
            if (!order.getUserId().equals(userPrincipal.getId()) && !userPrincipal.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                logger.warn("User {} attempted to access order {} which they don't own", 
                    userPrincipal.getId(), id);
                return ResponseEntity.status(403).body("Access denied");
            }
            
            logger.info("Returning order {} to user {}", id, userPrincipal.getId());
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            logger.error("RuntimeException when getting order {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected exception when getting order {}: {}", id, e.getMessage(), e);
            logger.error("Exception class: {}", e.getClass().getName());
            logger.error("Stack trace:", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Tạo đơn hàng mới với QR code thanh toán", 
               description = "Tạo đơn hàng với danh sách các khóa học. Hệ thống sẽ tự động tạo QR code thanh toán với số tiền tương ứng",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tạo đơn hàng thành công. Response sẽ chứa QR code để thanh toán"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc khóa học không tồn tại")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<?> createOrder(@Valid @RequestBody OrderCreateDTO dto,
                                         @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            return ResponseEntity.ok(orderService.createOrder(dto, userPrincipal.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id,
                                               @RequestParam Order.OrderStatus status) {
        try {
            return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
