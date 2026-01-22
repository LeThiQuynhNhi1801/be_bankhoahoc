package com.bankhoahoc.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class BunnyStreamService {

    private static final Logger logger = LoggerFactory.getLogger(BunnyStreamService.class);

    @Value("${bunny.stream.api-key:}")
    private String apiKey;

    @Value("${bunny.stream.library-id:}")
    private String libraryId;

    @Value("${bunny.stream.enabled:false}")
    private boolean enabled;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public BunnyStreamService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Upload video to Bunny Stream
     * @param file Video file to upload
     * @param title Title for the video
     * @return Video URL from Bunny Stream (embed URL)
     */
    public String uploadVideo(MultipartFile file, String title) {
        if (!enabled) {
            throw new RuntimeException("Bunny Stream is not enabled. Please configure bunny.stream.enabled=true");
        }

        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("Bunny Stream API key is not configured");
        }

        if (libraryId == null || libraryId.isEmpty()) {
            throw new RuntimeException("Bunny Stream Library ID is not configured");
        }

        try {
            logger.info("=== Starting video upload to Bunny Stream ===");
            logger.info("Title: {}, File size: {} bytes, File name: {}", 
                    title, file.getSize(), file.getOriginalFilename());
            
            // Step 1: Create video in Bunny Stream
            logger.info("Step 1: Creating video entry in Bunny Stream...");
            String videoId = createVideo(title);
            logger.info("✓ Created video in Bunny Stream with ID: {}", videoId);

            // Step 2: Upload video file
            logger.info("Step 2: Uploading video file to Bunny Stream...");
            uploadVideoFile(videoId, file);
            logger.info("✓ Video file uploaded successfully. Video ID: {}", videoId);

            // Step 3: Wait a bit for processing (Bunny Stream needs time to process)
            logger.info("Step 3: Waiting for Bunny Stream to process video (2 seconds)...");
            Thread.sleep(2000);

            // Step 4: Get video details and construct embed URL
            logger.info("Step 4: Getting video embed URL...");
            String videoUrl = getVideoEmbedUrl(videoId);
            logger.info("✓ Video uploaded successfully. Embed URL: {}", videoUrl);
            logger.info("=== Video upload completed ===");

            return videoUrl;

        } catch (RuntimeException e) {
            logger.error("=== Error uploading video to Bunny Stream ===");
            logger.error("Error: {}", e.getMessage(), e);
            throw e; // Re-throw to preserve original error
        } catch (Exception e) {
            logger.error("=== Unexpected error uploading video to Bunny Stream ===");
            logger.error("Error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload video to Bunny Stream: " + e.getMessage(), e);
        }
    }

    /**
     * Create a new video in Bunny Stream library
     */
    private String createVideo(String title) throws IOException {
        String url = "https://video.bunnycdn.com/library/" + libraryId + "/videos";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("AccessKey", apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("title", title != null ? title : "Untitled Video");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        logger.debug("Creating video in Bunny Stream. URL: {}, Title: {}", url, title);
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
        );

        logger.debug("Bunny Stream create video response: Status={}, Body={}", 
                response.getStatusCode(), response.getBody());

        if (response.getStatusCode().is2xxSuccessful()) {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            String videoId = jsonNode.get("guid").asText();
            logger.info("Video created successfully. Video ID (guid): {}", videoId);
            return videoId;
        } else {
            logger.error("Failed to create video in Bunny Stream. Status: {}, Body: {}", 
                    response.getStatusCode(), response.getBody());
            throw new RuntimeException("Failed to create video in Bunny Stream: " + response.getBody());
        }
    }

    /**
     * Upload video file to Bunny Stream
     * Note: Bunny Stream requires PUT request with binary data
     */
    private void uploadVideoFile(String videoId, MultipartFile file) throws IOException {
        String url = "https://video.bunnycdn.com/library/" + libraryId + "/videos/" + videoId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("AccessKey", apiKey);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(file.getSize());

        // Read file bytes
        byte[] fileBytes = file.getBytes();
        double fileSizeMB = fileBytes.length / (1024.0 * 1024.0);
        logger.info("Uploading video file to Bunny Stream. URL: {}, Size: {} bytes ({} MB), Video ID: {}, File name: {}", 
                url, fileBytes.length, String.format("%.2f", fileSizeMB), videoId, file.getOriginalFilename());

        HttpEntity<byte[]> request = new HttpEntity<>(fileBytes, headers);

        try {
            logger.debug("Sending PUT request to Bunny Stream with headers: AccessKey={}, ContentType={}, ContentLength={}", 
                    apiKey.substring(0, Math.min(10, apiKey.length())) + "...", 
                    MediaType.APPLICATION_OCTET_STREAM, 
                    file.getSize());
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    request,
                    String.class
            );

            logger.info("Bunny Stream upload file response: Status={}, Body={}", 
                    response.getStatusCode(), response.getBody() != null ? response.getBody() : "null");

            // Check response status
            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorMsg = String.format("Failed to upload video file. Status: %s, Body: %s", 
                        response.getStatusCode(), response.getBody());
                logger.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }
            
            // Even if status is 2xx, check response body for errors
            String responseBody = response.getBody();
            if (responseBody != null && !responseBody.isEmpty()) {
                try {
                    JsonNode jsonNode = objectMapper.readTree(responseBody);
                    if (jsonNode.has("Message") || jsonNode.has("message")) {
                        String errorMessage = jsonNode.has("Message") ? 
                                jsonNode.get("Message").asText() : 
                                jsonNode.get("message").asText();
                        if (errorMessage != null && !errorMessage.isEmpty()) {
                            logger.error("Bunny Stream returned error message: {}", errorMessage);
                            throw new RuntimeException("Bunny Stream error: " + errorMessage);
                        }
                    }
                } catch (Exception e) {
                    // If response is not JSON, it might be OK (empty response is also OK)
                    logger.debug("Response body is not JSON, treating as success: {}", responseBody);
                }
            }
            
            logger.info("Video file uploaded successfully to Bunny Stream. Video ID: {}", videoId);
        } catch (RuntimeException e) {
            // Re-throw runtime exceptions
            throw e;
        } catch (Exception e) {
            logger.error("Exception during video upload to Bunny Stream: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload video file to Bunny Stream: " + e.getMessage(), e);
        }
    }

    /**
     * Get video embed URL from Bunny Stream
     * Format: https://iframe.mediadelivery.net/embed/{libraryId}/{videoId}
     * Or: https://vz-{libraryId}.b-cdn.net/{videoId}/play_480p.mp4 (direct playback)
     */
    private String getVideoEmbedUrl(String videoId) throws IOException {
        String url = "https://video.bunnycdn.com/library/" + libraryId + "/videos/" + videoId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("AccessKey", apiKey);

        HttpEntity<String> request = new HttpEntity<>(headers);

        logger.debug("Getting video details from Bunny Stream. Video ID: {}", videoId);
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                String.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            logger.debug("Video details response: {}", jsonNode.toString());
            
            // Check video status
            if (jsonNode.has("status")) {
                int status = jsonNode.get("status").asInt();
                logger.info("Video status: {}", status);
                // Status 4 = Finished/Ready, Status 3 = Processing, Status 2 = Uploading
                if (status != 4) {
                    logger.warn("Video is still processing. Status: {}", status);
                }
            }
            
            // Get video library ID (may be different from libraryId)
            String videoLibId = libraryId;
            if (jsonNode.has("videoLibraryId")) {
                videoLibId = jsonNode.get("videoLibraryId").asText();
            }
            
            // Construct embed URL: https://iframe.mediadelivery.net/embed/{libraryId}/{videoId}
            String embedUrl = "https://iframe.mediadelivery.net/embed/" + videoLibId + "/" + videoId;
            
            logger.info("Video embed URL: {}", embedUrl);
            return embedUrl;
        } else {
            logger.error("Failed to get video URL. Status: {}, Body: {}", 
                    response.getStatusCode(), response.getBody());
            // Fallback: construct embed URL from known library ID
            String fallbackUrl = "https://iframe.mediadelivery.net/embed/" + libraryId + "/" + videoId;
            logger.warn("Using fallback URL: {}", fallbackUrl);
            return fallbackUrl;
        }
    }

    /**
     * Delete video from Bunny Stream
     */
    public void deleteVideo(String videoId) {
        if (!enabled || apiKey == null || apiKey.isEmpty() || libraryId == null || libraryId.isEmpty()) {
            logger.warn("Bunny Stream is not configured. Skipping video deletion.");
            return;
        }

        try {
            String url = "https://video.bunnycdn.com/library/" + libraryId + "/videos/" + videoId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("AccessKey", apiKey);

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Video {} deleted from Bunny Stream", videoId);
            } else {
                logger.warn("Failed to delete video from Bunny Stream. Status: {}, Body: {}", 
                        response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            logger.error("Error deleting video from Bunny Stream: {}", e.getMessage(), e);
        }
    }

    /**
     * Extract video ID from Bunny Stream URL
     */
    public String extractVideoId(String videoUrl) {
        if (videoUrl == null || videoUrl.isEmpty()) {
            return null;
        }
        
        // URL format: https://iframe.mediadelivery.net/embed/{libraryId}/{videoId}
        String[] parts = videoUrl.split("/");
        if (parts.length > 0) {
            return parts[parts.length - 1];
        }
        return null;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
