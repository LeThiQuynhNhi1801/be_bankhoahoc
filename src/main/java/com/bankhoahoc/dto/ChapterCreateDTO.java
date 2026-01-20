package com.bankhoahoc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChapterCreateDTO {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    
    @NotNull(message = "Order index is required")
    private Integer orderIndex;
    
    private Boolean isPublished = true;
    
    @NotNull(message = "Course ID is required")
    private Long courseId;
}
