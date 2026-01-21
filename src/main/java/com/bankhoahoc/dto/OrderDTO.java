package com.bankhoahoc.dto;

import com.bankhoahoc.entity.Order;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDTO {
    private Long id;
    private String orderNumber;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String qrCodeUrl;
    private String qrCodeContent;
    private String bankName;
    private String accountNumber;
    private String accountName;
    private Order.OrderStatus status;
    private LocalDateTime createdAt;
    private Long userId;
    private String userName;
    private List<OrderItemDTO> items;
}
