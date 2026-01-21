package com.bankhoahoc.service;

import com.bankhoahoc.dto.OrderDTO;
import com.bankhoahoc.dto.UserDTO;
import com.bankhoahoc.entity.Course;
import com.bankhoahoc.entity.Order;
import com.bankhoahoc.entity.OrderItem;
import com.bankhoahoc.entity.User;
import com.bankhoahoc.repository.*;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    EnrollmentRepository enrollmentRepository;

    @Autowired
    OrderService orderService;

    // ========== USER MANAGEMENT ==========

    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::convertUserToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertUserToDTO(user);
    }

    @Transactional
    public UserDTO updateUserRole(Long userId, User.Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Không cho phép thay đổi role của chính mình thành non-admin
        if (user.getRole() == User.Role.ADMIN && newRole != User.Role.ADMIN) {
            throw new RuntimeException("Cannot change role of admin user");
        }
        
        user.setRole(newRole);
        User updatedUser = userRepository.save(user);
        return convertUserToDTO(updatedUser);
    }

    @Transactional
    public UserDTO toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Không cho phép disable admin
        if (user.getRole() == User.Role.ADMIN) {
            throw new RuntimeException("Cannot disable admin user");
        }
        
        user.setIsActive(!user.getIsActive());
        User updatedUser = userRepository.save(user);
        return convertUserToDTO(updatedUser);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Không cho phép xóa admin
        if (user.getRole() == User.Role.ADMIN) {
            throw new RuntimeException("Cannot delete admin user");
        }
        
        userRepository.delete(user);
    }

    // ========== ORDER MANAGEMENT ==========

    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        
        // Force initialize tất cả dữ liệu cần thiết
        for (Order order : orders) {
            Hibernate.initialize(order.getItems());
            Hibernate.initialize(order.getUser());
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    Hibernate.initialize(item.getCourse());
                }
            }
        }
        
        return orders.stream()
                .map(orderService::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByStatus(Order.OrderStatus status) {
        List<Order> orders = orderRepository.findByStatus(status);
        
        // Force initialize
        for (Order order : orders) {
            Hibernate.initialize(order.getItems());
            Hibernate.initialize(order.getUser());
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    Hibernate.initialize(item.getCourse());
                }
            }
        }
        
        return orders.stream()
                .map(orderService::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long orderId) {
        return orderService.getOrderById(orderId);
    }

    @Transactional
    public OrderDTO confirmOrder(Long orderId) {
        // Xác nhận đơn hàng = chuyển status sang PAID
        // OrderService.updateOrderStatus sẽ tự động tạo enrollment
        return orderService.updateOrderStatus(orderId, Order.OrderStatus.PAID);
    }

    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, Order.OrderStatus status) {
        return orderService.updateOrderStatus(orderId, status);
    }

    @Transactional
    public OrderDTO cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // Chỉ có thể cancel đơn hàng PENDING
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("Only PENDING orders can be cancelled");
        }
        
        return orderService.updateOrderStatus(orderId, Order.OrderStatus.CANCELLED);
    }

    // ========== COURSE MANAGEMENT ==========

    @Transactional(readOnly = true)
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @Transactional
    public void deleteCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        courseRepository.delete(course);
    }

    // ========== STATISTICS ==========

    @Transactional(readOnly = true)
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // User statistics
        long totalUsers = userRepository.count();
        long totalStudents = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.STUDENT)
                .count();
        long totalInstructors = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.INSTRUCTOR)
                .count();
        long activeUsers = userRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .count();
        
        stats.put("totalUsers", totalUsers);
        stats.put("totalStudents", totalStudents);
        stats.put("totalInstructors", totalInstructors);
        stats.put("activeUsers", activeUsers);
        
        // Order statistics
        List<Order> allOrders = orderRepository.findAll();
        long totalOrders = allOrders.size();
        long pendingOrders = allOrders.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.PENDING)
                .count();
        long paidOrders = allOrders.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.PAID)
                .count();
        BigDecimal totalRevenue = allOrders.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.PAID)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        stats.put("totalOrders", totalOrders);
        stats.put("pendingOrders", pendingOrders);
        stats.put("paidOrders", paidOrders);
        stats.put("totalRevenue", totalRevenue);
        
        // Course statistics
        long totalCourses = courseRepository.count();
        stats.put("totalCourses", totalCourses);
        
        // Enrollment statistics
        long totalEnrollments = enrollmentRepository.count();
        stats.put("totalEnrollments", totalEnrollments);
        
        return stats;
    }

    // ========== HELPER METHODS ==========

    private UserDTO convertUserToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setAvatar(user.getAvatar());
        dto.setRole(user.getRole());
        dto.setIsActive(user.getIsActive());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
