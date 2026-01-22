package com.bankhoahoc.controller;

import com.bankhoahoc.dto.ChapterDTO;
import com.bankhoahoc.dto.CourseContentDTO;
import com.bankhoahoc.repository.ChapterRepository;
import com.bankhoahoc.repository.CourseContentRepository;
import com.bankhoahoc.repository.EnrollmentRepository;
import com.bankhoahoc.security.UserPrincipal;
import com.bankhoahoc.service.ChapterService;
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

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/learning")
@Tag(name = "Learning", description = "API học tập dành cho học viên")
public class LearningController {

    @Autowired
    ChapterService chapterService;

    @Autowired
    ChapterRepository chapterRepository;

    @Autowired
    CourseContentRepository courseContentRepository;

    @Autowired
    EnrollmentRepository enrollmentRepository;

    @Operation(summary = "Lấy danh sách chương của khóa học đã mua", 
               description = "Lấy danh sách tất cả chương và nội dung của khóa học. Chỉ học viên đã mua khóa học mới có thể xem",
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
            // Kiểm tra enrollment
            boolean isEnrolled = enrollmentRepository.existsByStudentIdAndCourseId(
                    userPrincipal.getId(), courseId);
            
            // Admin và instructor sở hữu khóa học cũng có thể xem
            boolean isAdmin = userPrincipal.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            
            if (!isEnrolled && !isAdmin) {
                // Kiểm tra xem có phải instructor sở hữu khóa học không
                var chapters = chapterRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
                if (!chapters.isEmpty()) {
                    var firstChapter = chapters.get(0);
                    Hibernate.initialize(firstChapter.getCourse());
                    Hibernate.initialize(firstChapter.getCourse().getInstructor());
                    
                    if (!firstChapter.getCourse().getInstructor().getId().equals(userPrincipal.getId())) {
                        return ResponseEntity.status(403).body("Bạn chưa mua khóa học này");
                    }
                } else {
                    return ResponseEntity.status(403).body("Bạn chưa mua khóa học này");
                }
            }
            
            List<ChapterDTO> chapterDTOs = chapterService.getChaptersByCourse(
                    courseId, userPrincipal.getId());
            return ResponseEntity.ok(chapterDTOs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Lấy chi tiết chương để học", 
               description = "Lấy thông tin chi tiết của một chương bao gồm tất cả nội dung. Chỉ học viên đã mua khóa học mới có thể xem",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy chi tiết thành công"),
            @ApiResponse(responseCode = "403", description = "Chưa mua khóa học"),
            @ApiResponse(responseCode = "404", description = "Chương không tồn tại")
    })
    @GetMapping("/chapters/{chapterId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getChapterForLearning(
            @Parameter(description = "ID của chương") @PathVariable Long chapterId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            if (chapterId == null || chapterId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "ID chương không hợp lệ"));
            }
            ChapterDTO chapter = chapterService.getChapterById(chapterId, userPrincipal.getId());
            
            // Kiểm tra lại enrollment
            if (chapter.getContents() == null || chapter.getContents().isEmpty()) {
                // Có thể chưa enroll hoặc chapter không có content
                var chapterOpt = chapterRepository.findById(chapterId);
                if (chapterOpt.isPresent()) {
                    var chapterEntity = chapterOpt.get();
                    Hibernate.initialize(chapterEntity.getCourse());
                    Hibernate.initialize(chapterEntity.getCourse().getInstructor());
                    
                    Long courseId = chapterEntity.getCourse().getId();
                    boolean isEnrolled = enrollmentRepository.existsByStudentIdAndCourseId(
                            userPrincipal.getId(), courseId);
                    
                    boolean isAdmin = userPrincipal.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                    
                    if (!isEnrolled && !isAdmin) {
                        if (!chapterEntity.getCourse().getInstructor().getId().equals(userPrincipal.getId())) {
                            return ResponseEntity.status(403).body("Bạn chưa mua khóa học này");
                        }
                    }
                }
            }
            
            return ResponseEntity.ok(chapter);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Lấy chi tiết nội dung học (video, bài giảng)", 
               description = "Lấy thông tin chi tiết của một nội dung học (video, bài giảng). Chỉ học viên đã mua khóa học mới có thể xem",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy chi tiết thành công"),
            @ApiResponse(responseCode = "403", description = "Chưa mua khóa học"),
            @ApiResponse(responseCode = "404", description = "Nội dung không tồn tại")
    })
    @GetMapping("/contents/{contentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getContentForLearning(
            @Parameter(description = "ID của nội dung") @PathVariable Long contentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            if (contentId == null || contentId <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "ID nội dung không hợp lệ"));
            }
            var contentOpt = courseContentRepository.findById(contentId);
            if (contentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            var content = contentOpt.get();
            
            // Force initialize chapter, course và instructor trong transaction
            Hibernate.initialize(content.getChapter());
            Hibernate.initialize(content.getChapter().getCourse());
            Hibernate.initialize(content.getChapter().getCourse().getInstructor());
            
            Long courseId = content.getChapter().getCourse().getId();
            
            // Kiểm tra quyền
            boolean isEnrolled = enrollmentRepository.existsByStudentIdAndCourseId(
                    userPrincipal.getId(), courseId);
            
            boolean isAdmin = userPrincipal.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            
            boolean canView = isEnrolled || isAdmin;
            
            if (!canView) {
                // Kiểm tra xem có phải instructor sở hữu khóa học không
                if (content.getChapter().getCourse().getInstructor().getId().equals(userPrincipal.getId())) {
                    canView = true;
                }
            }
            
            if (!canView) {
                return ResponseEntity.status(403).body("Bạn chưa mua khóa học này");
            }
            
            // Convert to DTO
            CourseContentDTO dto = new CourseContentDTO();
            dto.setId(content.getId());
            dto.setTitle(content.getTitle());
            dto.setDescription(content.getDescription());
            dto.setVideoUrl(content.getVideoUrl());
            dto.setDuration(content.getDuration());
            dto.setOrderIndex(content.getOrderIndex());
            dto.setIsPreview(content.getIsPreview());
            dto.setCreatedAt(content.getCreatedAt());
            dto.setChapterId(content.getChapter().getId());
            dto.setChapterTitle(content.getChapter().getTitle());
            
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Lấy danh sách nội dung của chương", 
               description = "Lấy danh sách tất cả nội dung (video, bài giảng) của một chương. Chỉ học viên đã mua khóa học mới có thể xem",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
            @ApiResponse(responseCode = "403", description = "Chưa mua khóa học")
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
            
            // Force initialize course và instructor trong transaction
            Hibernate.initialize(chapter.getCourse());
            Hibernate.initialize(chapter.getCourse().getInstructor());
            
            Long courseId = chapter.getCourse().getId();
            
            // Kiểm tra quyền
            boolean isEnrolled = enrollmentRepository.existsByStudentIdAndCourseId(
                    userPrincipal.getId(), courseId);
            
            boolean isAdmin = userPrincipal.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            
            boolean canView = isEnrolled || isAdmin;
            
            if (!canView) {
                if (chapter.getCourse().getInstructor().getId().equals(userPrincipal.getId())) {
                    canView = true;
                }
            }
            
            if (!canView) {
                return ResponseEntity.status(403).body("Bạn chưa mua khóa học này");
            }
            
            // Lấy chapter với contents
            ChapterDTO chapterDTO = chapterService.getChapterById(chapterId, userPrincipal.getId());
            return ResponseEntity.ok(chapterDTO.getContents());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
