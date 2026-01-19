package com.bankhoahoc.dto;

import com.bankhoahoc.entity.Course;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CourseDTO {
    private Long id;
    private String title;
    private String description;
    private String thumbnail;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer studentCount;
    private Double rating;
    private Integer reviewCount;
    private Boolean isPublished;
    private Course.Level level;
    private String language;
    private Integer estimatedDuration;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long categoryId;
    private String categoryName;
    private Long instructorId;
    private String instructorName;
}
