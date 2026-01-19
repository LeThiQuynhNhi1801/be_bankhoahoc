package com.bankhoahoc.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EnrollmentDTO {
    private Long id;
    private Integer progress;
    private LocalDateTime completedAt;
    private LocalDateTime enrolledAt;
    private Long studentId;
    private String studentName;
    private Long courseId;
    private String courseTitle;
}
