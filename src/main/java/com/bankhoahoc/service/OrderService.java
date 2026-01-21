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
import com.bankhoahoc.util.QRCodeUtil;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    EnrollmentService enrollmentService;

    @Autowired
    QRCodeUtil qrCodeUtil;

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByUser(Long userId) {
        logger.info("Getting orders for user ID: {}", userId);
        try {
            List<Order> orders = orderRepository.findByUserId(userId);
            logger.info("Found {} orders for user {}", orders.size(), userId);
            
            // Force initialize tất cả dữ liệu cần thiết trong transaction
            for (Order order : orders) {
                logger.debug("Processing order ID: {}, orderNumber: {}", order.getId(), order.getOrderNumber());
                
                Hibernate.initialize(order.getItems());
                logger.debug("Order {} has {} items", order.getId(), 
                    order.getItems() != null ? order.getItems().size() : 0);
                
                if (order.getItems() != null) {
                    for (OrderItem item : order.getItems()) {
                        logger.debug("Processing order item ID: {}", item.getId());
                        
                        if (item.getCourse() != null) {
                            try {
                                Course course = item.getCourse();
                                logger.debug("Accessing course ID: {}, title: {}", 
                                    course.getId(), course.getTitle());
                                
                                // Chỉ access các field cần thiết
                                course.getId();
                                course.getTitle();
                                course.getThumbnail();
                                logger.debug("Successfully accessed course fields for course ID: {}", course.getId());
                            } catch (Exception e) {
                                logger.error("Error accessing course for item {}: {}", item.getId(), e.getMessage(), e);
                                throw e;
                            }
                        } else {
                            logger.warn("Order item {} has no course", item.getId());
                        }
                    }
                }
                
                try {
                    Hibernate.initialize(order.getUser());
                    logger.debug("Initialized user for order {}", order.getId());
                } catch (Exception e) {
                    logger.error("Error initializing user for order {}: {}", order.getId(), e.getMessage(), e);
                    throw e;
                }
            }
            
            logger.info("Starting to convert {} orders to DTO", orders.size());
            // Convert ngay trong transaction
            List<OrderDTO> dtos = orders.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            logger.info("Successfully converted {} orders to DTO", dtos.size());
            
            return dtos;
        } catch (Exception e) {
            logger.error("Error in getOrdersByUser for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long id) {
        logger.info("Getting order by ID: {}", id);
        try {
            // Sử dụng EntityGraph để load items, items.course và user cùng lúc
            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> {
                        logger.error("Order not found with ID: {}", id);
                        return new RuntimeException("Order not found");
                    });
            
            logger.debug("Found order ID: {}, orderNumber: {}", order.getId(), order.getOrderNumber());
            
            // Force initialize tất cả dữ liệu cần thiết trong transaction
            try {
                Hibernate.initialize(order.getItems());
                logger.debug("Initialized items for order {}, count: {}", order.getId(), 
                    order.getItems() != null ? order.getItems().size() : 0);
            } catch (Exception e) {
                logger.error("Error initializing items for order {}: {}", order.getId(), e.getMessage(), e);
                throw e;
            }
            
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    logger.debug("Processing order item ID: {}", item.getId());
                    
                    // Force initialize course nhưng chỉ lấy các field cần thiết
                    if (item.getCourse() != null) {
                        try {
                            Course course = item.getCourse();
                            logger.debug("Accessing course ID: {}, title: {}", course.getId(), course.getTitle());
                            
                            // Chỉ access các field cần thiết, không trigger lazy loading của quan hệ khác
                            course.getId();
                            course.getTitle();
                            course.getThumbnail();
                            logger.debug("Successfully accessed course fields for course ID: {}", course.getId());
                        } catch (Exception e) {
                            logger.error("Error accessing course for item {}: {}", item.getId(), e.getMessage(), e);
                            throw e;
                        }
                    } else {
                        logger.warn("Order item {} has no course", item.getId());
                    }
                }
            }
            
            try {
                Hibernate.initialize(order.getUser());
                logger.debug("Initialized user for order {}", order.getId());
            } catch (Exception e) {
                logger.error("Error initializing user for order {}: {}", order.getId(), e.getMessage(), e);
                throw e;
            }
            
            logger.info("Starting to convert order {} to DTO", order.getId());
            // Convert ngay trong transaction để tránh lazy loading exception
            OrderDTO dto = convertToDTO(order);
            logger.info("Successfully converted order {} to DTO", order.getId());
            
            return dto;
        } catch (Exception e) {
            logger.error("Error in getOrderById for order {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public OrderDTO createOrder(OrderCreateDTO dto, Long userId) {
        logger.info("Creating order for user ID: {} with {} courses", userId, dto.getCourseIds().size());
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        logger.error("User not found with ID: {}", userId);
                        return new RuntimeException("User not found");
                    });

            Order order = new Order();
            order.setUser(user);
            order.setPaymentMethod("QR_CODE"); // Chỉ hỗ trợ QR code
            order.setStatus(Order.OrderStatus.PENDING);
            
            // Đảm bảo items collection được khởi tạo
            if (order.getItems() == null) {
                order.setItems(new HashSet<>());
            }
            logger.debug("Initialized order with items collection");

            BigDecimal totalAmount = BigDecimal.ZERO;

            int itemIndex = 0;
            for (Long courseId : dto.getCourseIds()) {
                itemIndex++;
                logger.debug("Processing course {} of {} (courseId: {})", itemIndex, dto.getCourseIds().size(), courseId);
                
                try {
                    Course course = courseRepository.findById(courseId)
                            .orElseThrow(() -> {
                                logger.error("Course not found with ID: {}", courseId);
                                return new RuntimeException("Course not found: " + courseId);
                            });
                    
                    logger.debug("Found course ID: {}, title: {}", course.getId(), course.getTitle());

                    OrderItem orderItem = new OrderItem();
                    logger.debug("Created new OrderItem");
                    
                    orderItem.setOrder(order);
                    logger.debug("Set order for item {}", itemIndex);
                    
                    orderItem.setCourse(course);
                    logger.debug("Set course for item {}", itemIndex);
                    
                    orderItem.setPrice(course.getPrice());
                    logger.debug("Set price {} for item {}", course.getPrice(), itemIndex);

                    // Đảm bảo items collection không null trước khi add
                    Set<OrderItem> items = order.getItems();
                    if (items == null) {
                        logger.error("Items collection is null for order, initializing...");
                        items = new HashSet<>();
                        order.setItems(items);
                    }
                    
                    logger.debug("Before adding item {} to collection. Current collection size: {}", 
                        itemIndex, items.size());
                    
                    boolean added = items.add(orderItem);
                    logger.debug("Item {} added: {}. New collection size: {}", 
                        itemIndex, added, items.size());
                    
                    if (!added) {
                        logger.warn("Item {} was not added (duplicate?). Collection size still: {}", 
                            itemIndex, items.size());
                    }
                    
                    totalAmount = totalAmount.add(course.getPrice());
                    logger.debug("Updated total amount to: {}", totalAmount);
                    
                } catch (Exception e) {
                    logger.error("Error processing course {} (courseId: {}): {}", 
                        itemIndex, courseId, e.getMessage(), e);
                    logger.error("Exception class: {}", e.getClass().getName());
                    logger.error("Stack trace:", e);
                    throw new RuntimeException("Error adding course " + courseId + " to order: " + e.getMessage(), e);
                }
            }

            order.setTotalAmount(totalAmount);
            logger.info("Order created with total amount: {}, {} items", totalAmount, order.getItems().size());
            
            // Generate QR code sau khi có orderNumber
            logger.debug("Saving order to database...");
            Order savedOrder = orderRepository.save(order);
            logger.info("Order saved with ID: {}, orderNumber: {}", savedOrder.getId(), savedOrder.getOrderNumber());
            
            // Tạo QR code content và image (sử dụng VietQR.io)
            logger.debug("Generating QR code...");
            // Sử dụng method mới với amount và orderNumber để hỗ trợ VietQR
            String qrCodeImage = qrCodeUtil.generateQRCodeImage(savedOrder.getTotalAmount(), savedOrder.getOrderNumber(), 300, 300);
            savedOrder.setQrCodeUrl(qrCodeImage);
            logger.debug("QR code generated");
            
            // Update lại order với QR code
            savedOrder = orderRepository.save(savedOrder);
            logger.info("Order {} updated with QR code", savedOrder.getId());
            
            return convertToDTO(savedOrder);
        } catch (Exception e) {
            logger.error("Error in createOrder for user {}: {}", userId, e.getMessage(), e);
            logger.error("Exception class: {}", e.getClass().getName());
            if (e.getCause() != null) {
                logger.error("Caused by: {}", e.getCause().getClass().getName());
                logger.error("Caused by message: {}", e.getCause().getMessage());
            }
            throw e;
        }
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

    public OrderDTO convertToDTO(Order order) {
        logger.debug("Converting order {} to DTO", order.getId());
        try {
            OrderDTO dto = new OrderDTO();
            dto.setId(order.getId());
            dto.setOrderNumber(order.getOrderNumber());
            dto.setTotalAmount(order.getTotalAmount());
            dto.setPaymentMethod(order.getPaymentMethod());
            dto.setQrCodeUrl(order.getQrCodeUrl());
            logger.debug("Set basic fields for order {}", order.getId());
            
            // Thêm thông tin QR code nếu có
            if (order.getQrCodeUrl() != null) {
                try {
                    dto.setQrCodeContent(qrCodeUtil.generateQRContent(order.getTotalAmount(), order.getOrderNumber()));
                    logger.debug("Generated QR code content for order {}", order.getId());
                } catch (Exception e) {
                    logger.error("Error generating QR code content for order {}: {}", order.getId(), e.getMessage(), e);
                }
            }
            
            dto.setBankName(qrCodeUtil.getBankName());
            dto.setAccountNumber(qrCodeUtil.getAccountNumber());
            dto.setAccountName(qrCodeUtil.getAccountName());
            logger.debug("Set payment info for order {}", order.getId());
            
            dto.setStatus(order.getStatus());
            dto.setCreatedAt(order.getCreatedAt());

            if (order.getUser() != null) {
                try {
                    dto.setUserId(order.getUser().getId());
                    dto.setUserName(order.getUser().getFullName() != null ?
                            order.getUser().getFullName() : order.getUser().getUsername());
                    logger.debug("Set user info for order {}", order.getId());
                } catch (Exception e) {
                    logger.error("Error accessing user for order {}: {}", order.getId(), e.getMessage(), e);
                    throw e;
                }
            }

            logger.debug("Starting to convert {} items for order {}", 
                order.getItems() != null ? order.getItems().size() : 0, order.getId());
            try {
                dto.setItems(order.getItems().stream()
                        .map(this::convertItemToDTO)
                        .collect(Collectors.toList()));
                logger.debug("Successfully converted items for order {}", order.getId());
            } catch (Exception e) {
                logger.error("Error converting items for order {}: {}", order.getId(), e.getMessage(), e);
                throw e;
            }

            logger.debug("Successfully converted order {} to DTO", order.getId());
            return dto;
        } catch (Exception e) {
            logger.error("Error in convertToDTO for order {}: {}", order.getId(), e.getMessage(), e);
            throw e;
        }
    }

    private OrderItemDTO convertItemToDTO(OrderItem item) {
        logger.debug("Converting order item {} to DTO", item.getId());
        try {
            OrderItemDTO dto = new OrderItemDTO();
            dto.setId(item.getId());
            dto.setPrice(item.getPrice());
            logger.debug("Set basic fields for order item {}", item.getId());

            // Course đã được eager load trong query (JOIN FETCH), chỉ lấy thông tin cần thiết
            if (item.getCourse() != null) {
                try {
                    Course course = item.getCourse();
                    logger.debug("Accessing course for item {}, course ID: {}", item.getId(), course.getId());
                    
                    dto.setCourseId(course.getId());
                    logger.debug("Set courseId: {}", course.getId());
                    
                    dto.setCourseTitle(course.getTitle());
                    logger.debug("Set courseTitle: {}", course.getTitle());
                    
                    dto.setCourseThumbnail(course.getThumbnail());
                    logger.debug("Set courseThumbnail for item {}", item.getId());
                    
                    // KHÔNG truy cập các quan hệ khác (chapters, enrollments, orderItems)
                    // vì đã có @JsonIgnore trên Course entity và OrderItem.course
                    
                    logger.debug("Successfully converted order item {} with course {}", item.getId(), course.getId());
                } catch (Exception e) {
                    logger.error("Error accessing course for order item {}: {}", item.getId(), e.getMessage(), e);
                    // Log stack trace để debug
                    logger.error("Stack trace:", e);
                    throw e;
                }
            } else {
                logger.warn("Order item {} has no course attached", item.getId());
            }

            return dto;
        } catch (Exception e) {
            logger.error("Error in convertItemToDTO for item {}: {}", item.getId(), e.getMessage(), e);
            logger.error("Stack trace:", e);
            throw e;
        }
    }
}
