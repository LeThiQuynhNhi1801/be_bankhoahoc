package com.bankhoahoc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class CourseContentDTO {
    private Long id;
    private String title;
    private String description;
    private String fileUrl;
    private Integer duration;
    private Integer orderIndex;
    private Boolean isPreview;
    private LocalDateTime createdAt;
    private Long chapterId;
    private String chapterTitle;
}
