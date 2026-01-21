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

    @Transactional(readOnly = true)
    public List<ChapterDTO> getChaptersByCourse(Long courseId, Long studentId) {
        List<Chapter> chapters = chapterRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        
        // Force initialize Course và Contents trong transaction
        for (Chapter chapter : chapters) {
            Hibernate.initialize(chapter.getCourse());
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
        
        // Force initialize Course trong transaction
        Hibernate.initialize(chapter.getCourse());
        
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
        
        // Force initialize Course và Contents trong transaction
        Hibernate.initialize(chapter.getCourse());
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

        // Delete old document if exists
        if (chapter.getDocumentUrl() != null) {
            fileStorageService.deleteFile(chapter.getDocumentUrl());
        }

        // Store new document
        String filePath = fileStorageService.storeFile(file, "documents/chapters/" + chapterId);
        chapter.setDocumentUrl(filePath);

        Chapter updatedChapter = chapterRepository.save(chapter);
        return convertToDTO(updatedChapter);
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
            dto.setCourseId(chapter.getCourse().getId());
            dto.setCourseTitle(chapter.getCourse().getTitle());
        }

        dto.setDocumentUrl(chapter.getDocumentUrl());

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
            dto.setCourseId(chapter.getCourse().getId());
            dto.setCourseTitle(chapter.getCourse().getTitle());
        }

        dto.setDocumentUrl(chapter.getDocumentUrl());

        // Không trả về contents nếu chưa enroll
        dto.setContents(null);
        dto.setContentCount(0);
        dto.setTotalDuration(0);

        return dto;
    }
}
