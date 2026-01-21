package com.bankhoahoc.controller;

import com.bankhoahoc.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @Operation(summary = "Tải file", 
               description = "Tải file đã được upload (tài liệu, hình ảnh, v.v.)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File được tải thành công"),
            @ApiResponse(responseCode = "404", description = "File không tồn tại")
    })
    @GetMapping
    public ResponseEntity<Resource> downloadFile(@RequestParam("path") String filePath) {
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
                                "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
