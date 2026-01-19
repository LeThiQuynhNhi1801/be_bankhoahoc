package com.bankhoahoc.dto;

import com.bankhoahoc.entity.Course;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourseCreateDTO {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    private String thumbnail;
    
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    private BigDecimal originalPrice;
    private Course.Level level;
    private String language;
    private Integer estimatedDuration;
    
    @NotNull(message = "Category ID is required")
    private Long categoryId;
}
