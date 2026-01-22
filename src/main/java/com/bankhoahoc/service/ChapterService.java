package com.bankhoahoc.service;

import com.bankhoahoc.dto.ChapterCreateDTO;
import com.bankhoahoc.dto.ChapterDTO;
import com.bankhoahoc.dto.CourseContentDTO;
import com.bankhoahoc.entity.Chapter;
import com.bankhoahoc.entity.Course;
import com.bankhoahoc.entity.CourseContent;
import com.bankhoahoc.repository.ChapterRepository;
import com.bankhoahoc.repository.CourseContentRepository;
import com.bankhoahoc.repository.CourseRepository;
import com.bankhoahoc.repository.EnrollmentRepository;
import com.bankhoahoc.service.FileStorageService;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChapterService {

    @Autowired
    ChapterRepository chapterRepository;

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    CourseContentRepository courseContentRepository;

    @Autowired
    EnrollmentRepository enrollmentRepository;

    @Autowired
    FileStorageService fileStorageService;

    @Autowired
    BunnyStreamService bunnyStreamService;

    @Transactional(readOnly = true)
    public List<ChapterDTO> getChaptersByCourse(Long courseId, Long studentId) {
        List<Chapter> chapters = chapterRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        
        // Force initialize Course, Instructor và Contents trong transaction
        for (Chapter chapter : chapters) {
            Hibernate.initialize(chapter.getCourse());
            if (chapter.getCourse() != null) {
                Hibernate.initialize(chapter.getCourse().getInstructor());
            }
            if (studentId != null) {
                // Nếu đã enroll, load contents
                boolean isEnrolled = enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId);
                if (isEnrolled) {
                    Hibernate.initialize(chapter.getContents());
                }
            }
        }
        
        boolean isEnrolled = studentId != null && 
            enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId);
        
        return chapters.stream()
                .map(chapter -> {
                    if (isEnrolled) {
                        return convertToDTO(chapter);
                    } else {
                        return convertToDTOWithoutContents(chapter);
                    }
                })
                .collect(Collectors.toList());
    }

    // Bỏ getPublishedChaptersByCourse, dùng getChaptersByCourse cho tất cả

    @Transactional(readOnly = true)
    public ChapterDTO getChapterById(Long id, Long studentId) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));
        
        // Force initialize Course và Instructor trong transaction
        Hibernate.initialize(chapter.getCourse());
        if (chapter.getCourse() != null) {
            Hibernate.initialize(chapter.getCourse().getInstructor());
        }
        
        // Nếu có studentId, kiểm tra enrollment
        if (studentId != null) {
            Long courseId = chapter.getCourse().getId();
            boolean isEnrolled = enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId);
            
            // Nếu đã enroll, load contents
            if (isEnrolled) {
                Hibernate.initialize(chapter.getContents());
                return convertToDTO(chapter);
            } else {
                // Chưa enroll, không trả về contents
                return convertToDTOWithoutContents(chapter);
            }
        } else {
            // Không có studentId (chưa đăng nhập), không trả về contents
            return convertToDTOWithoutContents(chapter);
        }
    }

    @Transactional(readOnly = true)
    public ChapterDTO getChapterByIdAndCourse(Long id, Long courseId) {
        Chapter chapter = chapterRepository.findByIdAndCourseId(id, courseId)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));
        
        // Force initialize Course, Instructor và Contents trong transaction
        Hibernate.initialize(chapter.getCourse());
        if (chapter.getCourse() != null) {
            Hibernate.initialize(chapter.getCourse().getInstructor());
        }
        Hibernate.initialize(chapter.getContents());
        
        return convertToDTO(chapter);
    }

    @Transactional
    public ChapterDTO createChapter(ChapterCreateDTO dto, Long instructorId) {
        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        // Check if instructor owns the course
        if (!course.getInstructor().getId().equals(instructorId)) {
            throw new RuntimeException("You don't have permission to add chapters to this course");
        }

        Chapter chapter = new Chapter();
        chapter.setTitle(dto.getTitle());
        chapter.setDescription(dto.getDescription());
        
        // Tự động tính orderIndex nếu không có
        if (dto.getOrderIndex() == null) {
            Long maxOrderIndex = chapterRepository.countByCourseId(dto.getCourseId());
            chapter.setOrderIndex(maxOrderIndex.intValue() + 1);
        } else {
            chapter.setOrderIndex(dto.getOrderIndex());
        }
        
        // Bỏ isPublished, tất cả chapters đều public
        chapter.setIsPublished(true);
        chapter.setCourse(course);

        Chapter savedChapter = chapterRepository.save(chapter);
        return convertToDTO(savedChapter);
    }

    @Transactional
    public ChapterDTO createChapterForCourse(Long courseId, ChapterCreateDTO dto, Long instructorId) {
        // Set courseId từ path variable
        dto.setCourseId(courseId);
        return createChapter(dto, instructorId);
    }

    @Transactional
    public ChapterDTO updateChapter(Long id, ChapterCreateDTO dto, Long instructorId) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));

        // Check if instructor owns the course
        if (!chapter.getCourse().getInstructor().getId().equals(instructorId)) {
            throw new RuntimeException("You don't have permission to update this chapter");
        }

        chapter.setTitle(dto.getTitle());
        chapter.setDescription(dto.getDescription());
        chapter.setOrderIndex(dto.getOrderIndex());
        if (dto.getIsPublished() != null) {
            chapter.setIsPublished(dto.getIsPublished());
        }

        Chapter updatedChapter = chapterRepository.save(chapter);
        return convertToDTO(updatedChapter);
    }

    @Transactional
    public void deleteChapter(Long id, Long instructorId) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));

        // Check if instructor owns the course
        if (!chapter.getCourse().getInstructor().getId().equals(instructorId)) {
            throw new RuntimeException("You don't have permission to delete this chapter");
        }

        // Delete associated document if exists
        if (chapter.getDocumentUrl() != null) {
            fileStorageService.deleteFile(chapter.getDocumentUrl());
        }

        chapterRepository.delete(chapter);
    }

    @Transactional
    public ChapterDTO uploadDocument(Long chapterId, MultipartFile file, Long instructorId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));

        // Force initialize Course để kiểm tra instructor
        Hibernate.initialize(chapter.getCourse());

        // Check if instructor owns the course
        if (!chapter.getCourse().getInstructor().getId().equals(instructorId)) {
            throw new RuntimeException("You don't have permission to upload document to this chapter");
        }

        // Validate file type - CHỈ cho phép tài liệu, KHÔNG cho phép video
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();
        
        // Danh sách các loại file video không được phép
        String[] videoMimeTypes = {
            "video/mp4", "video/avi", "video/quicktime", "video/x-msvideo",
            "video/x-ms-wmv", "video/webm", "video/ogg", "video/mpeg"
        };
        
        // Danh sách các extension video không được phép
        String[] videoExtensions = {".mp4", ".avi", ".mov", ".wmv", ".webm", ".ogg", ".mpeg", ".mpg", ".mkv", ".flv"};
        
        // Kiểm tra MIME type
        if (contentType != null) {
            for (String videoType : videoMimeTypes) {
                if (contentType.toLowerCase().contains(videoType)) {
                    throw new RuntimeException(
                        "Video files are not allowed here. " +
                        "Please use POST /api/course-contents/{contentId}/video to upload videos to Bunny Stream. " +
                        "This endpoint is only for documents (PDF, DOC, DOCX, TXT, etc.)"
                    );
                }
            }
        }
        
        // Kiểm tra file extension
        if (fileName != null) {
            String lowerFileName = fileName.toLowerCase();
            for (String ext : videoExtensions) {
                if (lowerFileName.endsWith(ext)) {
                    throw new RuntimeException(
                        "Video files are not allowed here. " +
                        "Please use POST /api/course-contents/{contentId}/video to upload videos to Bunny Stream. " +
                        "This endpoint is only for documents (PDF, DOC, DOCX, TXT, etc.)"
                    );
                }
            }
        }

        // Delete old document if exists
        if (chapter.getDocumentUrl() != null) {
            fileStorageService.deleteFile(chapter.getDocumentUrl());
        }

        // Store new document (CHỈ tài liệu, không phải video)
        String filePath = fileStorageService.storeFile(file, "documents/chapters/" + chapterId);
        chapter.setDocumentUrl(filePath);

        Chapter updatedChapter = chapterRepository.save(chapter);
        return convertToDTO(updatedChapter);
    }

    @Transactional
    public ChapterDTO uploadVideo(Long chapterId, MultipartFile videoFile, String title, Long instructorId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));

        // Force initialize Course để kiểm tra instructor
        Hibernate.initialize(chapter.getCourse());

        // Check if instructor owns the course
        if (!chapter.getCourse().getInstructor().getId().equals(instructorId)) {
            throw new RuntimeException("You don't have permission to upload video to this chapter");
        }

        // Validate file
        if (videoFile == null || videoFile.isEmpty()) {
            throw new RuntimeException("Video file is required");
        }

        // Validate file type - CHỈ cho phép video
        String contentType = videoFile.getContentType();
        String fileName = videoFile.getOriginalFilename();
        
        // Danh sách các loại file video được phép
        String[] allowedVideoMimeTypes = {
            "video/mp4", "video/avi", "video/quicktime", "video/x-msvideo",
            "video/x-ms-wmv", "video/webm", "video/ogg", "video/mpeg"
        };
        
        boolean isVideo = false;
        if (contentType != null) {
            for (String videoType : allowedVideoMimeTypes) {
                if (contentType.toLowerCase().contains(videoType)) {
                    isVideo = true;
                    break;
                }
            }
        }
        
        // Kiểm tra extension nếu MIME type không rõ
        if (!isVideo && fileName != null) {
            String lowerFileName = fileName.toLowerCase();
            String[] videoExtensions = {".mp4", ".avi", ".mov", ".wmv", ".webm", ".ogg", ".mpeg", ".mpg", ".mkv", ".flv"};
            for (String ext : videoExtensions) {
                if (lowerFileName.endsWith(ext)) {
                    isVideo = true;
                    break;
                }
            }
        }
        
        if (!isVideo) {
            throw new RuntimeException("File must be a video. Allowed formats: MP4, AVI, MOV, WMV, WEBM, OGG, MPEG, MKV, FLV");
        }

        // Check if Bunny Stream is enabled
        if (!bunnyStreamService.isEnabled()) {
            throw new RuntimeException("Bunny Stream is not enabled. Please configure it in application.properties");
        }

        try {
            // Upload video to Bunny Stream (KHÔNG lưu vào project)
            // Sử dụng title từ request hoặc title của chapter
            String videoTitle = title != null && !title.isEmpty() ? title : chapter.getTitle();
            String videoUrl = bunnyStreamService.uploadVideo(videoFile, videoTitle);
            
            // Lưu video URL trực tiếp vào Chapter (KHÔNG cần tạo CourseContent)
            chapter.setVideoUrl(videoUrl);
            
            Chapter updatedChapter = chapterRepository.save(chapter);
            
            // Trả về ChapterDTO với videoUrl
            return convertToDTO(updatedChapter);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload video: " + e.getMessage());
        }
    }

    private ChapterDTO convertToDTO(Chapter chapter) {
        ChapterDTO dto = new ChapterDTO();
        dto.setId(chapter.getId());
        dto.setTitle(chapter.getTitle());
        dto.setDescription(chapter.getDescription());
        dto.setOrderIndex(chapter.getOrderIndex());
        dto.setIsPublished(chapter.getIsPublished());
        dto.setCreatedAt(chapter.getCreatedAt());
        dto.setUpdatedAt(chapter.getUpdatedAt());

        if (chapter.getCourse() != null) {
            // Force initialize course để lấy title
            Hibernate.initialize(chapter.getCourse());
            dto.setCourseId(chapter.getCourse().getId());
            dto.setCourseTitle(chapter.getCourse().getTitle());
        }

        dto.setDocumentUrl(chapter.getDocumentUrl());
        dto.setVideoUrl(chapter.getVideoUrl());

        // Convert contents
        if (chapter.getContents() != null && !chapter.getContents().isEmpty()) {
            List<CourseContentDTO> contentDTOs = chapter.getContents().stream()
                    .sorted((c1, c2) -> {
                        if (c1.getOrderIndex() == null) return 1;
                        if (c2.getOrderIndex() == null) return -1;
                        return c1.getOrderIndex().compareTo(c2.getOrderIndex());
                    })
                    .map(this::convertContentToDTO)
                    .collect(Collectors.toList());
            dto.setContents(contentDTOs);
            dto.setContentCount(contentDTOs.size());
            
            // Calculate total duration
            int totalDuration = contentDTOs.stream()
                    .mapToInt(content -> content.getDuration() != null ? content.getDuration() : 0)
                    .sum();
            dto.setTotalDuration(totalDuration);
        } else {
            dto.setContentCount(0);
            dto.setTotalDuration(0);
        }

        return dto;
    }

    private CourseContentDTO convertContentToDTO(CourseContent content) {
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

    private ChapterDTO convertToDTOWithoutContents(Chapter chapter) {
        ChapterDTO dto = new ChapterDTO();
        dto.setId(chapter.getId());
        dto.setTitle(chapter.getTitle());
        dto.setDescription(chapter.getDescription());
        dto.setOrderIndex(chapter.getOrderIndex());
        dto.setIsPublished(chapter.getIsPublished());
        dto.setCreatedAt(chapter.getCreatedAt());
        dto.setUpdatedAt(chapter.getUpdatedAt());

        if (chapter.getCourse() != null) {
            // Force initialize course để lấy title
            Hibernate.initialize(chapter.getCourse());
            dto.setCourseId(chapter.getCourse().getId());
            dto.setCourseTitle(chapter.getCourse().getTitle());
        }

        dto.setDocumentUrl(chapter.getDocumentUrl());
        dto.setVideoUrl(chapter.getVideoUrl());

        // Không trả về contents nếu chưa enroll
        dto.setContents(null);
        dto.setContentCount(0);
        dto.setTotalDuration(0);

        return dto;
    }
}
