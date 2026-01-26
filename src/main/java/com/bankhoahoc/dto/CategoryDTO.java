package com.bankhoahoc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CategoryDTO {
    private Long id;
    
    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(min = 1, max = 255, message = "Tên danh mục phải từ 1 đến 255 ký tự")
    private String name;
    
    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
    private String description;
    
    private String image;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
