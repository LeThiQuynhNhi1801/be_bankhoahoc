package com.bankhoahoc.service;

import com.bankhoahoc.dto.OrderCreateDTO;
import com.bankhoahoc.dto.OrderDTO;
import com.bankhoahoc.dto.OrderItemDTO;
import com.bankhoahoc.entity.Course;
import com.bankhoahoc.entity.Order;
import com.bankhoahoc.entity.OrderItem;
import com.bankhoahoc.entity.User;
import com.bankhoahoc.repository.CourseRepository;
import com.bankhoahoc.repository.OrderRepository;
import com.bankhoahoc.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    EnrollmentService enrollmentService;

    public List<OrderDTO> getOrdersByUser(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public OrderDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return convertToDTO(order);
    }

    @Transactional
    public OrderDTO createOrder(OrderCreateDTO dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Order order = new Order();
        order.setUser(user);
        order.setPaymentMethod(dto.getPaymentMethod());
        order.setStatus(Order.OrderStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Long courseId : dto.getCourseIds()) {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setCourse(course);
            orderItem.setPrice(course.getPrice());

            order.getItems().add(orderItem);
            totalAmount = totalAmount.add(course.getPrice());
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);
        return convertToDTO(savedOrder);
    }

    @Transactional
    public OrderDTO updateOrderStatus(Long id, Order.OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(status);

        // If order is paid, create enrollments
        if (status == Order.OrderStatus.PAID) {
            for (OrderItem item : order.getItems()) {
                enrollmentService.enrollStudent(order.getUser().getId(), item.getCourse().getId());
            }
        }

        Order updatedOrder = orderRepository.save(order);
        return convertToDTO(updatedOrder);
    }

    private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setStatus(order.getStatus());
        dto.setCreatedAt(order.getCreatedAt());

        if (order.getUser() != null) {
            dto.setUserId(order.getUser().getId());
            dto.setUserName(order.getUser().getFullName() != null ?
                    order.getUser().getFullName() : order.getUser().getUsername());
        }

        dto.setItems(order.getItems().stream()
                .map(this::convertItemToDTO)
                .collect(Collectors.toList()));

        return dto;
    }

    private OrderItemDTO convertItemToDTO(OrderItem item) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(item.getId());
        dto.setPrice(item.getPrice());

        if (item.getCourse() != null) {
            dto.setCourseId(item.getCourse().getId());
            dto.setCourseTitle(item.getCourse().getTitle());
            dto.setCourseThumbnail(item.getCourse().getThumbnail());
        }

        return dto;
    }
}
