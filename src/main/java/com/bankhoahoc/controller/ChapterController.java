package com.bankhoahoc.controller;

import com.bankhoahoc.dto.ChapterCreateDTO;
import com.bankhoahoc.dto.ChapterDTO;
import com.bankhoahoc.security.UserPrincipal;
import com.bankhoahoc.service.ChapterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/chapters")
@Tag(name = "Chapters", description = "API quản lý chương trong khóa học")
public class ChapterController {

    @Autowired
    ChapterService chapterService;

    @Operation(summary = "Lấy danh sách chương của khóa học", 
               description = "Trả về tất cả chương của một khóa học, sắp xếp theo thứ tự. Ai cũng có thể xem, nhưng chỉ học viên đã mua mới thấy nội dung chi tiết.")
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<ChapterDTO>> getChaptersByCourse(
            @Parameter(description = "ID của khóa học") @PathVariable Long courseId) {
        // Lấy user từ security context nếu có (optional)
        org.springframework.security.core.Authentication auth = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Long studentId = null;
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal) {
            studentId = ((UserPrincipal) auth.getPrincipal()).getId();
        }
        return ResponseEntity.ok(chapterService.getChaptersByCourse(courseId, studentId));
    }

    @Operation(summary = "Lấy chi tiết chương", 
               description = "Trả về thông tin chi tiết của một chương. Nếu đã mua khóa học sẽ thấy đầy đủ nội dung, nếu chưa chỉ thấy thông tin cơ bản.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tìm thấy chương"),
            @ApiResponse(responseCode = "400", description = "Chương không tồn tại")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getChapterById(
            @Parameter(description = "ID của chương") @PathVariable Long id) {
        try {
            // Lấy user từ security context nếu có (optional)
            org.springframework.security.core.Authentication auth = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            Long studentId = null;
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal) {
                studentId = ((UserPrincipal) auth.getPrincipal()).getId();
            }
            return ResponseEntity.ok(chapterService.getChapterById(id, studentId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Tạo chương mới", 
               description = "Tạo một chương mới trong khóa học. Chỉ INSTRUCTOR sở hữu khóa học mới có quyền",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tạo chương thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc không có quyền")
    })
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @PostMapping
    public ResponseEntity<?> createChapter(@Valid @RequestBody ChapterCreateDTO dto,
                                            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            return ResponseEntity.ok(chapterService.createChapter(dto, userPrincipal.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Cập nhật chương", 
               description = "Cập nhật thông tin chương. Chỉ INSTRUCTOR sở hữu khóa học mới có quyền",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<?> updateChapter(
            @Parameter(description = "ID của chương") @PathVariable Long id,
            @Valid @RequestBody ChapterCreateDTO dto,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            return ResponseEntity.ok(chapterService.updateChapter(id, dto, userPrincipal.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Xóa chương", 
               description = "Xóa một chương. Chỉ INSTRUCTOR sở hữu khóa học mới có quyền",
               security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<?> deleteChapter(
            @Parameter(description = "ID của chương") @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            chapterService.deleteChapter(id, userPrincipal.getId());
            return ResponseEntity.ok("Chapter deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Upload tài liệu cho chương", 
               description = "Upload tài liệu đính kèm cho một chương. Chỉ INSTRUCTOR sở hữu khóa học mới có quyền",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Upload tài liệu thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi upload hoặc không có quyền")
    })
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @PostMapping("/{chapterId}/documents")
    public ResponseEntity<?> uploadDocument(
            @Parameter(description = "ID của chương") @PathVariable Long chapterId,
            @Parameter(description = "File tài liệu cần upload") @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            return ResponseEntity.ok(chapterService.uploadDocument(chapterId, file, userPrincipal.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "Upload video cho chương", 
               description = "Upload video lên Bunny Stream cho một chương. Video được lưu trực tiếp vào chapter, không cần tạo CourseContent. Chỉ INSTRUCTOR sở hữu khóa học mới có quyền",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Upload video thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi upload hoặc không có quyền")
    })
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @PostMapping("/{chapterId}/video")
    public ResponseEntity<?> uploadVideo(
            @Parameter(description = "ID của chương") @PathVariable Long chapterId,
            @Parameter(description = "File video cần upload") @RequestParam("video") MultipartFile videoFile,
            @Parameter(description = "Tiêu đề cho video (tùy chọn, dùng để đặt tên trên Bunny Stream)") @RequestParam(required = false) String title,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            return ResponseEntity.ok(chapterService.uploadVideo(chapterId, videoFile, title, userPrincipal.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
