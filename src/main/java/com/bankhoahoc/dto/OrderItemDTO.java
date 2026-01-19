package com.bankhoahoc.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemDTO {
    private Long id;
    private BigDecimal price;
    private Long courseId;
    private String courseTitle;
    private String courseThumbnail;
}
