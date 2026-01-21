package com.bankhoahoc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    
    private final Path fileStorageLocation;

    public FileStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        
        try {
            Files.createDirectories(this.fileStorageLocation);
            logger.info("File storage directory created: {}", this.fileStorageLocation);
        } catch (Exception ex) {
            logger.error("Could not create the directory where the uploaded files will be stored.", ex);
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file, String subfolder) {
        try {
            // Validate file
            if (file.isEmpty()) {
                throw new RuntimeException("File is empty");
            }

            // Normalize file name
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.contains("..")) {
                throw new RuntimeException("Invalid file name: " + originalFilename);
            }

            // Generate unique file name
            String fileExtension = "";
            if (originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + fileExtension;

            // Create subfolder path
            Path targetSubfolder = this.fileStorageLocation.resolve(subfolder);
            Files.createDirectories(targetSubfolder);

            // Copy file to the target location
            Path targetLocation = targetSubfolder.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Return relative path for URL
            String relativePath = subfolder + "/" + fileName;
            logger.info("File stored successfully: {}", relativePath);
            return relativePath;

        } catch (IOException ex) {
            logger.error("Could not store file: {}", file.getOriginalFilename(), ex);
            throw new RuntimeException("Could not store file: " + file.getOriginalFilename(), ex);
        }
    }

    public Path loadFileAsPath(String filePath) {
        return fileStorageLocation.resolve(filePath).normalize();
    }

    public boolean deleteFile(String filePath) {
        try {
            Path file = fileStorageLocation.resolve(filePath).normalize();
            return Files.deleteIfExists(file);
        } catch (IOException ex) {
            logger.error("Could not delete file: {}", filePath, ex);
            return false;
        }
    }
}
