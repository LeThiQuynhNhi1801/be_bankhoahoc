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
    public CourseContentDTO uploadVideo(Long contentId, MultipartFile videoFile, Long instructorId) {
        CourseContent content = courseContentRepository.findById(contentId)
                .orElseThrow(() -> new RuntimeException("Course content not found"));

        Hibernate.initialize(content.getChapter());
        Hibernate.initialize(content.getChapter().getCourse());
        Hibernate.initialize(content.getChapter().getCourse().getInstructor());

        // Check if instructor owns the course
        if (!content.getChapter().getCourse().getInstructor().getId().equals(instructorId)) {
            throw new RuntimeException("You don't have permission to upload video to this content");
        }

        // Validate file
        if (videoFile == null || videoFile.isEmpty()) {
            throw new RuntimeException("Video file is required");
        }

        // Check if Bunny Stream is enabled
        if (!bunnyStreamService.isEnabled()) {
            throw new RuntimeException("Bunny Stream is not enabled. Please configure it in application.properties");
        }

        try {
            logger.info("Starting video upload for content ID: {}, Title: {}, File size: {} bytes", 
                    contentId, content.getTitle(), videoFile.getSize());
            
            // Upload video to Bunny Stream (KHÔNG lưu vào project)
            String videoUrl = bunnyStreamService.uploadVideo(videoFile, content.getTitle());
            
            logger.info("Video uploaded to Bunny Stream successfully. URL: {}", videoUrl);
            
            // Update content with video URL from Bunny Stream (KHÔNG lưu file vào project)
            content.setVideoUrl(videoUrl);
            
            CourseContent updatedContent = courseContentRepository.save(content);
            logger.info("Content updated with Bunny Stream video URL. Content ID: {}", contentId);
            
            return convertToDTO(updatedContent);
        } catch (Exception e) {
            logger.error("Failed to upload video to Bunny Stream: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload video: " + e.getMessage());
        }
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

        // Delete video from Bunny Stream if exists
        if (content.getVideoUrl() != null && bunnyStreamService.isEnabled()) {
            try {
                String videoId = bunnyStreamService.extractVideoId(content.getVideoUrl());
                if (videoId != null) {
                    bunnyStreamService.deleteVideo(videoId);
                }
            } catch (Exception e) {
                logger.warn("Failed to delete video from Bunny Stream: {}", e.getMessage());
            }
        }

        courseContentRepository.delete(content);
    }

    private CourseContentDTO convertToDTO(CourseContent content) {
        CourseContentDTO dto = new CourseContentDTO();
        dto.setId(content.getId());
        dto.setTitle(content.getTitle());
        dto.setDescription(content.getDescription());
        dto.setVideoUrl(content.getVideoUrl());
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

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CourseContentService.class);
}
