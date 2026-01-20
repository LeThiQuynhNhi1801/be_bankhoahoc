package com.bankhoahoc.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChapterDTO {
    private Long id;
    private String title;
    private String description;
    private Integer orderIndex;
    private Boolean isPublished;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long courseId;
    private String courseTitle;
    private List<CourseContentDTO> contents;
    private Integer contentCount;
    private Integer totalDuration; // total duration of all contents in minutes
}
