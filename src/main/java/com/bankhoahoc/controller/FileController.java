package com.bankhoahoc.controller;

import com.bankhoahoc.repository.ChapterRepository;
import com.bankhoahoc.repository.EnrollmentRepository;
import com.bankhoahoc.security.UserPrincipal;
import com.bankhoahoc.service.FileStorageService;
import org.hibernate.Hibernate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/files")
@Tag(name = "Files", description = "API để tải và xem file")
public class FileController {

    @Autowired
    FileStorageService fileStorageService;

    @Autowired
    EnrollmentRepository enrollmentRepository;

    @Autowired
    ChapterRepository chapterRepository;

    @Operation(summary = "Tải file (Admin/Instructor only)", 
               description = "Tải file đã được upload. Chỉ ADMIN và INSTRUCTOR có quyền tải file",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File được tải thành công"),
            @ApiResponse(responseCode = "403", description = "Không có quyền tải file"),
            @ApiResponse(responseCode = "404", description = "File không tồn tại")
    })
    @GetMapping("/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "Đường dẫn file") @RequestParam("path") String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Path path = fileStorageService.loadFileAsPath(filePath);
            Resource resource = new UrlResource(path.toUri());

            if (resource.exists() && resource.isReadable()) {
                // Determine content type
                String contentType = null;
                try {
                    contentType = Files.probeContentType(path);
                } catch (Exception e) {
                    // Ignore
                }
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, 
                                "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Xem tài liệu của chapter (Student only)", 
               description = "Xem tài liệu của chapter. Chỉ học viên đã mua khóa học mới có thể xem. Không cho phép tải về",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File được hiển thị thành công"),
            @ApiResponse(responseCode = "403", description = "Chưa mua khóa học hoặc không có quyền"),
            @ApiResponse(responseCode = "404", description = "File không tồn tại")
    })
    @GetMapping("/chapters/{chapterId}/document")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> viewChapterDocument(
            @Parameter(description = "ID của chapter") @PathVariable Long chapterId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            // Lấy chapter và kiểm tra enrollment
            var chapterOpt = chapterRepository.findById(chapterId);
            if (chapterOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            var chapter = chapterOpt.get();
            
            // Force initialize course và instructor trong transaction
            Hibernate.initialize(chapter.getCourse());
            if (chapter.getCourse() != null) {
                Hibernate.initialize(chapter.getCourse().getInstructor());
            }
            Long courseId = chapter.getCourse().getId();
            
            // Kiểm tra quyền: ADMIN và INSTRUCTOR sở hữu khóa học có thể xem
            boolean isAdmin = userPrincipal.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean isInstructor = userPrincipal.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_INSTRUCTOR"));
            
            boolean canView = false;
            if (isAdmin) {
                canView = true;
            } else if (isInstructor && chapter.getCourse().getInstructor().getId().equals(userPrincipal.getId())) {
                canView = true;
            } else {
                // Student phải đã enroll
                canView = enrollmentRepository.existsByStudentIdAndCourseId(userPrincipal.getId(), courseId);
            }
            
            if (!canView) {
                return ResponseEntity.status(403).build();
            }
            
            if (chapter.getDocumentUrl() == null || chapter.getDocumentUrl().isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Path path = fileStorageService.loadFileAsPath(chapter.getDocumentUrl());
            Resource resource = new UrlResource(path.toUri());

            if (resource.exists() && resource.isReadable()) {
                // Determine content type
                String contentType = null;
                try {
                    contentType = Files.probeContentType(path);
                } catch (Exception e) {
                    // Ignore
                }
                if (contentType == null) {
                    contentType = "application/pdf"; // Default to PDF for documents
                }

                // Sử dụng "inline" để hiển thị trong browser, không cho download
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, 
                                "inline; filename=\"" + resource.getFilename() + "\"")
                        .header("X-Content-Type-Options", "nosniff")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
