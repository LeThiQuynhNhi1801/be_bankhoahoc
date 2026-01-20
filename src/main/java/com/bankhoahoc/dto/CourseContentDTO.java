package com.bankhoahoc.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseContentDTO {
    private Long id;
    private String title;
    private String description;
    private String videoUrl;
    private Integer duration;
    private Integer orderIndex;
    private Boolean isPreview;
    private LocalDateTime createdAt;
    private Long chapterId;
    private String chapterTitle;
}
