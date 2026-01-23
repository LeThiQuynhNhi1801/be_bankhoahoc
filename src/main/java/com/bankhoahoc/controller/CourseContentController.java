package com.bankhoahoc.controller;

import com.bankhoahoc.dto.CourseContentDTO;
import com.bankhoahoc.security.UserPrincipal;
import com.bankhoahoc.service.CourseContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/course-contents")
@Tag(name = "Course Content", description = "API quản lý nội dung khóa học (video, bài giảng)")
public class CourseContentController {

    @Autowired
    CourseContentService courseContentService;

    @Operation(summary = "Lấy danh sách nội dung của chương", 
               description = "Lấy tất cả nội dung (video, bài giảng) của một chương")
    @GetMapping("/chapters/{chapterId}")
    public ResponseEntity<List<CourseContentDTO>> getContentsByChapter(
            @Parameter(description = "ID của chương") @PathVariable Long chapterId) {
        return ResponseEntity.ok(courseContentService.getContentsByChapter(chapterId));
    }

    @Operation(summary = "Lấy chi tiết nội dung", 
               description = "Lấy thông tin chi tiết của một nội dung")
    @GetMapping("/{contentId}")
    public ResponseEntity<?> getContentById(
            @Parameter(description = "ID của nội dung") @PathVariable Long contentId) {
        try {
            return ResponseEntity.ok(courseContentService.getContentById(contentId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "Tạo nội dung mới", 
               description = "Tạo một nội dung mới (bài giảng) cho chương. Chỉ INSTRUCTOR sở hữu khóa học mới có quyền",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tạo nội dung thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc không có quyền")
    })
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @PostMapping("/chapters/{chapterId}")
    public ResponseEntity<?> createContent(
            @Parameter(description = "ID của chương") @PathVariable Long chapterId,
            @Parameter(description = "Tiêu đề") @RequestParam(required = false) String title,
            @Parameter(description = "Mô tả") @RequestParam(required = false) String description,
            @Parameter(description = "Thứ tự") @RequestParam(required = false) Integer orderIndex,
            @Parameter(description = "Có phải preview không") @RequestParam(required = false) Boolean isPreview,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            return ResponseEntity.ok(courseContentService.createContent(
                    chapterId, title, description, orderIndex, isPreview, userPrincipal.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "Upload file cho nội dung", 
               description = "Upload file (video hoặc tài liệu) cho nội dung. Tự động phát hiện loại file: video → Bunny Stream, tài liệu → Bunny Storage. Chỉ INSTRUCTOR sở hữu khóa học mới có quyền",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Upload file thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi upload hoặc không có quyền")
    })
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @PostMapping("/{contentId}/file")
    public ResponseEntity<?> uploadFile(
            @Parameter(description = "ID của nội dung") @PathVariable Long contentId,
            @Parameter(description = "File cần upload (video hoặc tài liệu)") @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            return ResponseEntity.ok(courseContentService.uploadFile(contentId, file, userPrincipal.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "Cập nhật nội dung", 
               description = "Cập nhật thông tin nội dung. Chỉ INSTRUCTOR sở hữu khóa học mới có quyền",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @PutMapping("/{contentId}")
    public ResponseEntity<?> updateContent(
            @Parameter(description = "ID của nội dung") @PathVariable Long contentId,
            @Parameter(description = "Tiêu đề") @RequestParam(required = false) String title,
            @Parameter(description = "Mô tả") @RequestParam(required = false) String description,
            @Parameter(description = "Thứ tự") @RequestParam(required = false) Integer orderIndex,
            @Parameter(description = "Có phải preview không") @RequestParam(required = false) Boolean isPreview,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            return ResponseEntity.ok(courseContentService.updateContent(
                    contentId, title, description, orderIndex, isPreview, userPrincipal.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Operation(summary = "Xóa nội dung", 
               description = "Xóa nội dung và video trên Bunny Stream. Chỉ INSTRUCTOR sở hữu khóa học mới có quyền",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @DeleteMapping("/{contentId}")
    public ResponseEntity<?> deleteContent(
            @Parameter(description = "ID của nội dung") @PathVariable Long contentId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            courseContentService.deleteContent(contentId, userPrincipal.getId());
            return ResponseEntity.ok(Map.of("message", "Content deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
