package com.bankhoahoc.controller;

import com.bankhoahoc.dto.EnrollmentDTO;
import com.bankhoahoc.security.UserPrincipal;
import com.bankhoahoc.service.EnrollmentService;
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

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/enrollments")
@Tag(name = "Enrollments", description = "API quản lý ghi danh khóa học")
public class EnrollmentController {

    @Autowired
    EnrollmentService enrollmentService;

    @Operation(summary = "Lấy danh sách khóa học đã ghi danh", 
               description = "Trả về tất cả khóa học mà người dùng hiện tại đã ghi danh",
               security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/my-enrollments")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<List<EnrollmentDTO>> getMyEnrollments(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByStudent(userPrincipal.getId()));
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<List<EnrollmentDTO>> getEnrollmentsByCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByCourse(courseId));
    }

    @GetMapping("/check")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Boolean> checkEnrollment(@RequestParam Long courseId,
                                                    @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(enrollmentService.isEnrolled(userPrincipal.getId(), courseId));
    }

    @Operation(summary = "Ghi danh vào khóa học", 
               description = "Ghi danh người dùng hiện tại vào một khóa học",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ghi danh thành công"),
            @ApiResponse(responseCode = "400", description = "Đã ghi danh hoặc khóa học không tồn tại")
    })
    @PostMapping("/enroll/{courseId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<?> enrollInCourse(
            @Parameter(description = "ID của khóa học") @PathVariable Long courseId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            return ResponseEntity.ok(enrollmentService.enrollStudent(userPrincipal.getId(), courseId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/progress")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<?> updateProgress(@PathVariable Long id,
                                            @RequestParam Integer progress,
                                            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            return ResponseEntity.ok(enrollmentService.updateProgress(id, progress));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
