package com.bankhoahoc.controller;

import com.bankhoahoc.dto.ChapterDTO;
import com.bankhoahoc.dto.CourseContentDTO;
import com.bankhoahoc.dto.CourseDTO;
import com.bankhoahoc.repository.ChapterRepository;
import com.bankhoahoc.repository.EnrollmentRepository;
import com.bankhoahoc.security.UserPrincipal;
import com.bankhoahoc.service.ChapterService;
import com.bankhoahoc.service.CourseContentService;
import com.bankhoahoc.service.CourseService;
import com.bankhoahoc.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/learning")
@Tag(name = "Learning", description = "API học tập dành cho học viên")
public class LearningController {

    @Autowired
    EnrollmentService enrollmentService;

    @Autowired
    CourseService courseService;

    @Autowired
    ChapterService chapterService;

    @Autowired
    CourseContentService courseContentService;

    @Autowired
    ChapterRepository chapterRepository;

    @Autowired
    EnrollmentRepository enrollmentRepository;

    @Operation(summary = "Lấy danh sách khóa học đã mua", 
               description = "Lấy danh sách tất cả khóa học mà học viên đã mua",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    })
    @GetMapping("/courses")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<CourseDTO>> getMyCourses(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<com.bankhoahoc.dto.EnrollmentDTO> enrollments = enrollmentService.getEnrollmentsByStudent(userPrincipal.getId());
        List<CourseDTO> courses = enrollments.stream()
                .map(enrollment -> courseService.getCourseById(enrollment.getCourseId()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(courses);
    }

    @Operation(summary = "Lấy danh sách chương của khóa học", 
               description = "Lấy danh sách tất cả chương của khóa học. Chỉ học viên đã mua khóa học mới có thể xem",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
            @ApiResponse(responseCode = "403", description = "Chưa mua khóa học")
    })
    @GetMapping("/courses/{courseId}/chapters")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getChaptersForLearning(
            @Parameter(description = "ID của khóa học") @PathVariable Long courseId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            if (courseId == null || courseId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "ID khóa học không hợp lệ"));
            }
            
            boolean isEnrolled = enrollmentRepository.existsByStudentIdAndCourseId(
                    userPrincipal.getId(), courseId);
            
            boolean isAdmin = userPrincipal.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            
            if (!isEnrolled && !isAdmin) {
                var chapters = chapterRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
                if (!chapters.isEmpty()) {
                    var firstChapter = chapters.get(0);
                    Hibernate.initialize(firstChapter.getCourse());
                    Hibernate.initialize(firstChapter.getCourse().getInstructor());
                    
                    if (!firstChapter.getCourse().getInstructor().getId().equals(userPrincipal.getId())) {
                        return ResponseEntity.status(403).body(Map.of("message", "Bạn chưa mua khóa học này"));
                    }
                } else {
                    return ResponseEntity.status(403).body(Map.of("message", "Bạn chưa mua khóa học này"));
                }
            }
            
            List<ChapterDTO> chapterDTOs = chapterService.getChaptersByCourse(
                    courseId, userPrincipal.getId());
            return ResponseEntity.ok(chapterDTOs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "Lấy danh sách tài liệu của chương", 
               description = "Lấy danh sách tất cả tài liệu (course content) của một chương. Chỉ học viên đã mua khóa học mới có thể xem",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
            @ApiResponse(responseCode = "403", description = "Chưa mua khóa học"),
            @ApiResponse(responseCode = "404", description = "Chương không tồn tại")
    })
    @GetMapping("/chapters/{chapterId}/contents")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getContentsForLearning(
            @Parameter(description = "ID của chương") @PathVariable Long chapterId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            if (chapterId == null || chapterId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "ID chương không hợp lệ"));
            }
            
            var chapterOpt = chapterRepository.findById(chapterId);
            if (chapterOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            var chapter = chapterOpt.get();
            Hibernate.initialize(chapter.getCourse());
            Hibernate.initialize(chapter.getCourse().getInstructor());
            
            Long courseId = chapter.getCourse().getId();
            
            boolean isEnrolled = enrollmentRepository.existsByStudentIdAndCourseId(
                    userPrincipal.getId(), courseId);
            
            boolean isAdmin = userPrincipal.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            
            boolean canView = isEnrolled || isAdmin;
            
            if (!canView && chapter.getCourse().getInstructor().getId().equals(userPrincipal.getId())) {
                canView = true;
            }
            
            if (!canView) {
                return ResponseEntity.status(403).body(Map.of("message", "Bạn chưa mua khóa học này"));
            }
            
            List<CourseContentDTO> contents = courseContentService.getContentsByChapter(chapterId);
            return ResponseEntity.ok(contents);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
