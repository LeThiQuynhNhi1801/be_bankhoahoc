package com.bankhoahoc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChapterCreateDTO {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    
    // OrderIndex không bắt buộc, sẽ tự động tính nếu không có
    private Integer orderIndex;
    
    private Boolean isPublished = true;
    
    // CourseId không bắt buộc nếu dùng endpoint /courses/{courseId}/chapters
    private Long courseId;
}
