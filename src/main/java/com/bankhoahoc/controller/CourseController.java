package com.bankhoahoc.controller;

import com.bankhoahoc.dto.CourseCreateDTO;
import com.bankhoahoc.dto.CourseDTO;
import com.bankhoahoc.security.UserPrincipal;
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

    @Operation(summary = "Lấy danh sách khóa học đã publish", description = "Trả về tất cả khóa học đã được publish, không cần đăng nhập")
    @GetMapping("/public")
    public ResponseEntity<List<CourseDTO>> getAllPublishedCourses() {
        return ResponseEntity.ok(courseService.getAllPublishedCourses());
    }

    @Operation(summary = "Lấy chi tiết khóa học đã publish", description = "Trả về thông tin chi tiết của một khóa học đã publish")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tìm thấy khóa học"),
            @ApiResponse(responseCode = "400", description = "Khóa học không tồn tại hoặc chưa publish")
    })
    @GetMapping("/public/{id}")
    public ResponseEntity<?> getPublishedCourseById(
            @Parameter(description = "ID của khóa học") @PathVariable Long id) {
        try {
            return ResponseEntity.ok(courseService.getPublishedCourseById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Tìm kiếm khóa học", description = "Tìm kiếm khóa học theo từ khóa trong title hoặc description")
    @GetMapping("/public/search")
    public ResponseEntity<List<CourseDTO>> searchCourses(
            @Parameter(description = "Từ khóa tìm kiếm") @RequestParam String keyword) {
        return ResponseEntity.ok(courseService.searchCourses(keyword));
    }

    @GetMapping("/public/category/{categoryId}")
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

    @GetMapping("/{id}")
    public ResponseEntity<?> getCourseById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(courseService.getCourseById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

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

    @Operation(summary = "Publish khóa học", 
               description = "Publish khóa học để hiển thị công khai",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<?> publishCourse(
            @Parameter(description = "ID của khóa học") @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            return ResponseEntity.ok(courseService.publishCourse(id, userPrincipal.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
