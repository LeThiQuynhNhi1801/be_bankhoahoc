package com.bankhoahoc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class BunnyStorageService {

    private static final Logger logger = LoggerFactory.getLogger(BunnyStorageService.class);

    @Value("${bunny.storage.zone-name}")
    private String zoneName;

    @Value("${bunny.storage.access-key}")
    private String accessKey;

    @Value("${bunny.storage.base-url}")
    private String baseUrl;

    @Value("${bunny.storage.cdn-url}")
    private String cdnUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String uploadFile(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is required");
        }

        if (accessKey == null || accessKey.isEmpty()) {
            throw new RuntimeException("Bunny Storage Access Key is not configured");
        }

        if (zoneName == null || zoneName.isEmpty()) {
            throw new RuntimeException("Bunny Storage Zone Name is not configured");
        }

        logger.info("=== Starting document upload to Bunny Storage ===");
        logger.info("Folder: {}, File size: {} bytes, File name: {}", 
                folder, file.getSize(), file.getOriginalFilename());

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + extension;

        String filePath = folder + "/" + fileName;
        String uploadUrl = String.format("%s/%s/%s", baseUrl, zoneName, filePath);
        logger.info("Upload URL: {}", uploadUrl);
        logger.info("Zone Name: '{}', Folder: '{}', File Name: '{}'", zoneName, folder, fileName);
        logger.info("Base URL: '{}', Zone Name: '{}'", baseUrl, zoneName);
        logger.info("Access Key length: {}, First 10 chars: {}", 
                accessKey != null ? accessKey.length() : 0, 
                accessKey != null && accessKey.length() > 10 ? accessKey.substring(0, 10) : "null");

        HttpHeaders headers = new HttpHeaders();
        headers.set("AccessKey", accessKey);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        byte[] fileBytes = file.getBytes();
        double fileSizeMB = fileBytes.length / (1024.0 * 1024.0);
        logger.info("Uploading file to Bunny Storage. Size: {} bytes ({} MB)", 
                fileBytes.length, String.format("%.2f", fileSizeMB));

        HttpEntity<byte[]> requestEntity = new HttpEntity<>(fileBytes, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    uploadUrl, 
                    HttpMethod.PUT, 
                    requestEntity, 
                    String.class
            );

            logger.info("Bunny Storage upload response: Status={}, Body={}", 
                    response.getStatusCode(), response.getBody() != null ? response.getBody() : "null");

            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorMsg = String.format("Failed to upload file to Bunny Storage. Status: %s, Body: %s", 
                        response.getStatusCode(), response.getBody());
                logger.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            String cdnUrl = String.format("%s/%s/%s", this.cdnUrl, folder, fileName);
            logger.info("✓ File uploaded successfully to Bunny Storage. CDN URL: {}", cdnUrl);
            logger.info("=== Document upload completed ===");

            return cdnUrl;

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            logger.error("=== HTTP Error uploading file to Bunny Storage ===");
            logger.error("Status: {}, Response Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            logger.error("Request URL: {}", uploadUrl);
            logger.error("Headers sent: AccessKey={}...", accessKey != null && accessKey.length() > 10 ? accessKey.substring(0, 10) : "null");
            String errorMsg = String.format("Failed to upload file to Bunny Storage. Status: %s, Response: %s", 
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException(errorMsg, e);
        } catch (RuntimeException e) {
            logger.error("=== Error uploading file to Bunny Storage ===");
            logger.error("Error: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("=== Unexpected error uploading file to Bunny Storage ===");
            logger.error("Error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file to Bunny Storage: " + e.getMessage(), e);
        }
    }
}