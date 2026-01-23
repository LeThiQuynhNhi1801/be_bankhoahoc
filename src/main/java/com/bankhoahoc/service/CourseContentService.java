package com.bankhoahoc.service;

import com.bankhoahoc.dto.CourseContentDTO;
import com.bankhoahoc.entity.Chapter;
import com.bankhoahoc.entity.CourseContent;
import com.bankhoahoc.repository.ChapterRepository;
import com.bankhoahoc.repository.CourseContentRepository;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseContentService {

    @Autowired
    CourseContentRepository courseContentRepository;

    @Autowired
    ChapterRepository chapterRepository;

    @Autowired
    BunnyStreamService bunnyStreamService;

    @Autowired
    BunnyStorageService bunnyStorageService;

    @Transactional(readOnly = true)
    public List<CourseContentDTO> getContentsByChapter(Long chapterId) {
        List<CourseContent> contents = courseContentRepository.findByChapterIdOrderByOrderIndexAsc(chapterId);
        return contents.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CourseContentDTO getContentById(Long contentId) {
        CourseContent content = courseContentRepository.findById(contentId)
                .orElseThrow(() -> new RuntimeException("Course content not found"));
        
        Hibernate.initialize(content.getChapter());
        return convertToDTO(content);
    }

    @Transactional
    public CourseContentDTO createContent(Long chapterId, String title, String description, 
                                         Integer orderIndex, Boolean isPreview, Long instructorId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));

        Hibernate.initialize(chapter.getCourse());
        Hibernate.initialize(chapter.getCourse().getInstructor());

        // Check if instructor owns the course
        if (!chapter.getCourse().getInstructor().getId().equals(instructorId)) {
            throw new RuntimeException("You don't have permission to add content to this chapter");
        }

        CourseContent content = new CourseContent();
        // Nếu không truyền title hoặc rỗng thì gán mặc định
        String finalTitle = (title == null || title.trim().isEmpty())
                ? "Bài học của chương " + chapter.getTitle()
                : title;
        content.setTitle(finalTitle);
        content.setDescription(description);
        content.setOrderIndex(orderIndex);
        content.setIsPreview(isPreview != null ? isPreview : false);
        content.setChapter(chapter);
        // Set course_id từ chapter để tránh lỗi database
        content.setCourse(chapter.getCourse());

        CourseContent savedContent = courseContentRepository.save(content);
        return convertToDTO(savedContent);
    }

    @Transactional
    public CourseContentDTO uploadFile(Long contentId, MultipartFile file, Long instructorId) {
        CourseContent content = courseContentRepository.findById(contentId)
                .orElseThrow(() -> new RuntimeException("Course content not found"));

        Hibernate.initialize(content.getChapter());
        Hibernate.initialize(content.getChapter().getCourse());
        Hibernate.initialize(content.getChapter().getCourse().getInstructor());

        // Check if instructor owns the course
        if (!content.getChapter().getCourse().getInstructor().getId().equals(instructorId)) {
            throw new RuntimeException("You don't have permission to upload file to this content");
        }

        // Validate file
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is required");
        }

        try {
            logger.info("Starting file upload for content ID: {}, Title: {}, File size: {} bytes, File name: {}", 
                    contentId, content.getTitle(), file.getSize(), file.getOriginalFilename());
            
            String fileUrl;
            boolean isVideo = isVideoFile(file);
            
            if (isVideo) {
                // Upload video to Bunny Stream
                if (!bunnyStreamService.isEnabled()) {
                    throw new RuntimeException("Bunny Stream is not enabled. Please configure it in application.properties");
                }
                
                logger.info("Detected video file, uploading to Bunny Stream...");
                fileUrl = bunnyStreamService.uploadVideo(file, content.getTitle());
                logger.info("Video uploaded to Bunny Stream successfully. URL: {}", fileUrl);
            } else {
                // Upload document to Bunny Storage
                logger.info("Detected document file, uploading to Bunny Storage...");
                String folder = "documents/course-contents/" + contentId;
                fileUrl = bunnyStorageService.uploadFile(file, folder);
                logger.info("Document uploaded to Bunny Storage successfully. URL: {}", fileUrl);
            }
            
            // Update content with file URL (KHÔNG lưu file vào project)
            content.setFileUrl(fileUrl);
            
            CourseContent updatedContent = courseContentRepository.save(content);
            logger.info("Content updated with file URL. Content ID: {}, URL: {}", contentId, fileUrl);
            
            return convertToDTO(updatedContent);
        } catch (Exception e) {
            logger.error("Failed to upload file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        }
    }

    private boolean isVideoFile(MultipartFile file) {
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();
        
        // Check MIME type
        if (contentType != null) {
            String[] videoMimeTypes = {
                "video/mp4", "video/avi", "video/quicktime", "video/x-msvideo",
                "video/x-ms-wmv", "video/webm", "video/ogg", "video/mpeg"
            };
            for (String videoType : videoMimeTypes) {
                if (contentType.toLowerCase().contains(videoType)) {
                    return true;
                }
            }
        }
        
        // Check file extension
        if (fileName != null) {
            String lowerFileName = fileName.toLowerCase();
            String[] videoExtensions = {".mp4", ".avi", ".mov", ".wmv", ".webm", ".ogg", ".mpeg", ".mpg", ".mkv", ".flv"};
            for (String ext : videoExtensions) {
                if (lowerFileName.endsWith(ext)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    @Transactional
    public CourseContentDTO updateContent(Long contentId, String title, String description,
                                          Integer orderIndex, Boolean isPreview, Long instructorId) {
        CourseContent content = courseContentRepository.findById(contentId)
                .orElseThrow(() -> new RuntimeException("Course content not found"));

        Hibernate.initialize(content.getChapter());
        Hibernate.initialize(content.getChapter().getCourse());
        Hibernate.initialize(content.getChapter().getCourse().getInstructor());

        // Check if instructor owns the course
        if (!content.getChapter().getCourse().getInstructor().getId().equals(instructorId)) {
            throw new RuntimeException("You don't have permission to update this content");
        }

        if (title != null) {
            content.setTitle(title);
        }
        if (description != null) {
            content.setDescription(description);
        }
        if (orderIndex != null) {
            content.setOrderIndex(orderIndex);
        }
        if (isPreview != null) {
            content.setIsPreview(isPreview);
        }

        CourseContent updatedContent = courseContentRepository.save(content);
        return convertToDTO(updatedContent);
    }

    @Transactional
    public void deleteContent(Long contentId, Long instructorId) {
        CourseContent content = courseContentRepository.findById(contentId)
                .orElseThrow(() -> new RuntimeException("Course content not found"));

        Hibernate.initialize(content.getChapter());
        Hibernate.initialize(content.getChapter().getCourse());
        Hibernate.initialize(content.getChapter().getCourse().getInstructor());

        // Check if instructor owns the course
        if (!content.getChapter().getCourse().getInstructor().getId().equals(instructorId)) {
            throw new RuntimeException("You don't have permission to delete this content");
        }

        // Delete file from Bunny Stream or Storage if exists
        if (content.getFileUrl() != null) {
            try {
                if (isBunnyStreamUrl(content.getFileUrl())) {
                    // Delete video from Bunny Stream
                    if (bunnyStreamService.isEnabled()) {
                        String videoId = bunnyStreamService.extractVideoId(content.getFileUrl());
                        if (videoId != null) {
                            bunnyStreamService.deleteVideo(videoId);
                            logger.info("Video deleted from Bunny Stream. Video ID: {}", videoId);
                        }
                    }
                } else if (isBunnyStorageUrl(content.getFileUrl())) {
                    // Note: Bunny Storage doesn't have delete API in current implementation
                    // Files on Bunny Storage will remain, but that's acceptable
                    logger.info("Document URL is from Bunny Storage. File will remain on storage.");
                }
            } catch (Exception e) {
                logger.warn("Failed to delete file from Bunny: {}", e.getMessage());
            }
        }

        courseContentRepository.delete(content);
    }

    private CourseContentDTO convertToDTO(CourseContent content) {
        CourseContentDTO dto = new CourseContentDTO();
        dto.setId(content.getId());
        dto.setTitle(content.getTitle());
        dto.setDescription(content.getDescription());
        dto.setFileUrl(content.getFileUrl());
        dto.setDuration(content.getDuration());
        dto.setOrderIndex(content.getOrderIndex());
        dto.setIsPreview(content.getIsPreview());
        dto.setCreatedAt(content.getCreatedAt());
        
        if (content.getChapter() != null) {
            dto.setChapterId(content.getChapter().getId());
            dto.setChapterTitle(content.getChapter().getTitle());
        }
        
        return dto;
    }

    private boolean isBunnyStreamUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        return url.contains("mediadelivery.net") || url.contains("bunnycdn.com");
    }

    private boolean isBunnyStorageUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        return url.startsWith("https://") && url.contains(".b-cdn.net");
    }

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CourseContentService.class);
}
