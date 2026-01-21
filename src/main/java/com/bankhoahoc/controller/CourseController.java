package com.bankhoahoc.controller;

import com.bankhoahoc.dto.ChapterCreateDTO;
import com.bankhoahoc.dto.ChapterDTO;
import com.bankhoahoc.dto.CourseCreateDTO;
import com.bankhoahoc.dto.CourseDTO;
import com.bankhoahoc.security.UserPrincipal;
import com.bankhoahoc.service.ChapterService;
import com.bankhoahoc.service.CourseService;
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

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/courses")
@Tag(name = "Courses", description = "API quản lý khóa học")
public class CourseController {

    @Autowired
    CourseService courseService;

    @Autowired
    ChapterService chapterService;

    @Operation(summary = "Lấy danh sách tất cả khóa học", description = "Trả về tất cả khóa học, không cần đăng nhập")
    @GetMapping
    public ResponseEntity<List<CourseDTO>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @Operation(summary = "Lấy chi tiết khóa học", description = "Trả về thông tin chi tiết của một khóa học, không cần đăng nhập")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tìm thấy khóa học"),
            @ApiResponse(responseCode = "400", description = "Khóa học không tồn tại")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getCourseById(
            @Parameter(description = "ID của khóa học") @PathVariable Long id) {
        try {
            return ResponseEntity.ok(courseService.getCourseById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Tìm kiếm khóa học", description = "Tìm kiếm khóa học theo từ khóa trong title hoặc description")
    @GetMapping("/search")
    public ResponseEntity<List<CourseDTO>> searchCourses(
            @Parameter(description = "Từ khóa tìm kiếm") @RequestParam String keyword) {
        return ResponseEntity.ok(courseService.searchCourses(keyword));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<CourseDTO>> getCoursesByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(courseService.getCoursesByCategory(categoryId));
    }

    @Operation(summary = "Lấy danh sách khóa học của instructor", 
               description = "Trả về tất cả khóa học do instructor hiện tại tạo",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('INSTRUCTOR')")
    @GetMapping("/my-courses")
    public ResponseEntity<List<CourseDTO>> getMyCourses(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(courseService.getCoursesByInstructor(userPrincipal.getId()));
    }

    // Method getCourseById đã được di chuyển lên trên

    @Operation(summary = "Tạo khóa học mới", 
               description = "Tạo một khóa học mới. Chỉ INSTRUCTOR và ADMIN mới có quyền",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tạo khóa học thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @PostMapping
    public ResponseEntity<?> createCourse(@Valid @RequestBody CourseCreateDTO dto,
                                          @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            return ResponseEntity.ok(courseService.createCourse(dto, userPrincipal.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Cập nhật khóa học", 
               description = "Cập nhật thông tin khóa học. Chỉ instructor sở hữu khóa học hoặc ADMIN mới có quyền",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<?> updateCourse(
            @Parameter(description = "ID của khóa học") @PathVariable Long id,
            @Valid @RequestBody CourseCreateDTO dto,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            return ResponseEntity.ok(courseService.updateCourse(id, dto, userPrincipal.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<?> deleteCourse(@PathVariable Long id,
                                          @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            courseService.deleteCourse(id, userPrincipal.getId());
            return ResponseEntity.ok("Course deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Bỏ endpoint publish vì tất cả courses đều public

    @Operation(summary = "Thêm chương mới cho khóa học", 
               description = "Thêm một chương mới vào khóa học đã tạo. Chỉ INSTRUCTOR sở hữu khóa học mới có quyền",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tạo chương thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc không có quyền")
    })
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @PostMapping("/{courseId}/chapters")
    public ResponseEntity<?> addChapterToCourse(
            @Parameter(description = "ID của khóa học") @PathVariable Long courseId,
            @Valid @RequestBody ChapterCreateDTO dto,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            return ResponseEntity.ok(chapterService.createChapterForCourse(courseId, dto, userPrincipal.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Lấy danh sách chương của khóa học", 
               description = "Lấy tất cả chương của một khóa học")
    @GetMapping("/{courseId}/chapters")
    public ResponseEntity<List<ChapterDTO>> getChaptersOfCourse(
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

    @Operation(summary = "Upload tài liệu cho chương", 
               description = "Upload tài liệu đính kèm cho một chương. Chỉ INSTRUCTOR sở hữu khóa học mới có quyền",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Upload tài liệu thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi upload hoặc không có quyền")
    })
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @PostMapping("/{courseId}/chapters/{chapterId}/documents")
    public ResponseEntity<?> uploadChapterDocument(
            @Parameter(description = "ID của khóa học") @PathVariable Long courseId,
            @Parameter(description = "ID của chương") @PathVariable Long chapterId,
            @Parameter(description = "File tài liệu cần upload") @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            return ResponseEntity.ok(chapterService.uploadDocument(chapterId, file, userPrincipal.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
