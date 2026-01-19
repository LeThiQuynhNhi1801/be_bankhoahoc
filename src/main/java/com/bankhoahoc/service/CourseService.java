package com.bankhoahoc.service;

import com.bankhoahoc.dto.CourseCreateDTO;
import com.bankhoahoc.dto.CourseDTO;
import com.bankhoahoc.entity.Course;
import com.bankhoahoc.repository.CategoryRepository;
import com.bankhoahoc.repository.CourseRepository;
import com.bankhoahoc.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseService {

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    UserRepository userRepository;

    public List<CourseDTO> getAllPublishedCourses() {
        return courseRepository.findByIsPublishedTrue().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<CourseDTO> searchCourses(String keyword) {
        return courseRepository.searchPublishedCourses(keyword).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<CourseDTO> getCoursesByCategory(Long categoryId) {
        return courseRepository.findByCategoryId(categoryId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public CourseDTO getCourseById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        return convertToDTO(course);
    }

    public CourseDTO getPublishedCourseById(Long id) {
        Course course = courseRepository.findByIdAndIsPublishedTrue(id)
                .orElseThrow(() -> new RuntimeException("Course not found or not published"));
        return convertToDTO(course);
    }

    @Transactional
    public CourseDTO createCourse(CourseCreateDTO dto, Long instructorId) {
        Course course = new Course();
        course.setTitle(dto.getTitle());
        course.setDescription(dto.getDescription());
        course.setThumbnail(dto.getThumbnail());
        course.setPrice(dto.getPrice());
        course.setOriginalPrice(dto.getOriginalPrice());
        course.setLevel(dto.getLevel() != null ? dto.getLevel() : Course.Level.BEGINNER);
        course.setLanguage(dto.getLanguage() != null ? dto.getLanguage() : "Tiếng Việt");
        course.setEstimatedDuration(dto.getEstimatedDuration());
        course.setIsPublished(false);

        course.setCategory(categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found")));

        course.setInstructor(userRepository.findById(instructorId)
                .orElseThrow(() -> new RuntimeException("Instructor not found")));

        Course savedCourse = courseRepository.save(course);
        return convertToDTO(savedCourse);
    }

    @Transactional
    public CourseDTO updateCourse(Long id, CourseCreateDTO dto, Long instructorId) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (!course.getInstructor().getId().equals(instructorId)) {
            throw new RuntimeException("You don't have permission to update this course");
        }

        course.setTitle(dto.getTitle());
        course.setDescription(dto.getDescription());
        course.setThumbnail(dto.getThumbnail());
        course.setPrice(dto.getPrice());
        course.setOriginalPrice(dto.getOriginalPrice());
        course.setLevel(dto.getLevel());
        course.setLanguage(dto.getLanguage());
        course.setEstimatedDuration(dto.getEstimatedDuration());

        if (dto.getCategoryId() != null && !course.getCategory().getId().equals(dto.getCategoryId())) {
            course.setCategory(categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found")));
        }

        Course updatedCourse = courseRepository.save(course);
        return convertToDTO(updatedCourse);
    }

    @Transactional
    public void deleteCourse(Long id, Long instructorId) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (!course.getInstructor().getId().equals(instructorId)) {
            throw new RuntimeException("You don't have permission to delete this course");
        }

        courseRepository.delete(course);
    }

    @Transactional
    public CourseDTO publishCourse(Long id, Long instructorId) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (!course.getInstructor().getId().equals(instructorId)) {
            throw new RuntimeException("You don't have permission to publish this course");
        }

        course.setIsPublished(true);
        Course updatedCourse = courseRepository.save(course);
        return convertToDTO(updatedCourse);
    }

    public List<CourseDTO> getCoursesByInstructor(Long instructorId) {
        return courseRepository.findByInstructorId(instructorId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private CourseDTO convertToDTO(Course course) {
        CourseDTO dto = new CourseDTO();
        dto.setId(course.getId());
        dto.setTitle(course.getTitle());
        dto.setDescription(course.getDescription());
        dto.setThumbnail(course.getThumbnail());
        dto.setPrice(course.getPrice());
        dto.setOriginalPrice(course.getOriginalPrice());
        dto.setStudentCount(course.getStudentCount());
        dto.setRating(course.getRating());
        dto.setReviewCount(course.getReviewCount());
        dto.setIsPublished(course.getIsPublished());
        dto.setLevel(course.getLevel());
        dto.setLanguage(course.getLanguage());
        dto.setEstimatedDuration(course.getEstimatedDuration());
        dto.setCreatedAt(course.getCreatedAt());
        dto.setUpdatedAt(course.getUpdatedAt());

        if (course.getCategory() != null) {
            dto.setCategoryId(course.getCategory().getId());
            dto.setCategoryName(course.getCategory().getName());
        }

        if (course.getInstructor() != null) {
            dto.setInstructorId(course.getInstructor().getId());
            dto.setInstructorName(course.getInstructor().getFullName() != null ?
                    course.getInstructor().getFullName() : course.getInstructor().getUsername());
        }

        return dto;
    }
}
