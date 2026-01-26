package com.bankhoahoc.controller;

import com.bankhoahoc.dto.CategoryDTO;
import com.bankhoahoc.service.CategoryService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/categories")
@Tag(name = "Categories", description = "API quản lý danh mục khóa học")
public class CategoryController {

    @Autowired
    CategoryService categoryService;

    @Operation(summary = "Lấy danh sách tất cả danh mục", 
               description = "Trả về tất cả danh mục khóa học, không cần đăng nhập")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách danh mục thành công")
    })
    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @Operation(summary = "Lấy chi tiết danh mục", 
               description = "Trả về thông tin chi tiết của một danh mục theo ID, không cần đăng nhập")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tìm thấy danh mục"),
            @ApiResponse(responseCode = "400", description = "Danh mục không tồn tại")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoryById(
            @Parameter(description = "ID của danh mục") @PathVariable Long id) {
        try {
            return ResponseEntity.ok(categoryService.getCategoryById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Tạo danh mục mới", 
               description = "Tạo danh mục mới. Chỉ ADMIN mới có quyền",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tạo danh mục thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createCategory(@Valid @RequestBody CategoryDTO dto) {
        try {
            return ResponseEntity.ok(categoryService.createCategory(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Cập nhật danh mục", 
               description = "Cập nhật thông tin danh mục. Chỉ ADMIN mới có quyền",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cập nhật danh mục thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc danh mục không tồn tại"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateCategory(
            @Parameter(description = "ID của danh mục") @PathVariable Long id, 
            @Valid @RequestBody CategoryDTO dto) {
        try {
            return ResponseEntity.ok(categoryService.updateCategory(id, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Xóa danh mục", 
               description = "Xóa một danh mục. Chỉ ADMIN mới có quyền",
               security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Xóa danh mục thành công"),
            @ApiResponse(responseCode = "400", description = "Danh mục không tồn tại"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCategory(
            @Parameter(description = "ID của danh mục") @PathVariable Long id) {
        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.ok("Category deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
