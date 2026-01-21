package com.bankhoahoc.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OrderCreateDTO {
    @NotEmpty(message = "Course IDs are required")
    private List<Long> courseIds;
    
    // Chỉ hỗ trợ QR code, không cần truyền paymentMethod
}
