package com.bankhoahoc.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        logger.warn("Method argument type mismatch: {}", ex.getMessage());
        
        Map<String, Object> error = new HashMap<>();
        error.put("message", "Giá trị không hợp lệ: " + ex.getValue() + ". Vui lòng kiểm tra lại ID.");
        error.put("error", "INVALID_PARAMETER");
        error.put("status", HttpStatus.BAD_REQUEST.value());
        
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex) {
        logger.warn("File upload size exceeded: {}", ex.getMessage());
        
        Map<String, Object> error = new HashMap<>();
        error.put("message", "Kích thước file vượt quá giới hạn cho phép. Giới hạn tối đa: 500MB cho mỗi file.");
        error.put("error", "FILE_TOO_LARGE");
        error.put("status", HttpStatus.PAYLOAD_TOO_LARGE.value());
        error.put("maxSize", "500MB");
        
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> handleMissingServletRequestPart(
            MissingServletRequestPartException ex) {
        logger.warn("Missing request part: {}", ex.getMessage());
        
        String partName = ex.getRequestPartName();
        String message = "Thiếu file upload. ";
        
        if (partName != null) {
            if (partName.equals("video")) {
                message = "Vui lòng gửi file video với parameter name là 'video' (multipart/form-data).";
            } else if (partName.equals("file")) {
                message = "Vui lòng gửi file với parameter name là 'file' (multipart/form-data).";
            } else {
                message = "Vui lòng gửi file với parameter name là '" + partName + "' (multipart/form-data).";
            }
        }
        
        Map<String, Object> error = new HashMap<>();
        error.put("message", message);
        error.put("error", "MISSING_FILE");
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("requiredPart", partName);
        
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        logger.warn("Validation error: {}", ex.getMessage());
        
        Map<String, Object> error = new HashMap<>();
        error.put("message", "Dữ liệu không hợp lệ");
        error.put("error", "VALIDATION_ERROR");
        error.put("status", HttpStatus.BAD_REQUEST.value());
        
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        logger.error("Runtime exception: {}", ex.getMessage(), ex);
        
        Map<String, Object> error = new HashMap<>();
        error.put("message", ex.getMessage());
        error.put("error", "RUNTIME_ERROR");
        error.put("status", HttpStatus.BAD_REQUEST.value());
        
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        
        Map<String, Object> error = new HashMap<>();
        error.put("message", "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau.");
        error.put("error", "INTERNAL_ERROR");
        error.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
