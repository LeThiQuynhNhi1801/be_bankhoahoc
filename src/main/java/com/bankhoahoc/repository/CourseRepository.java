package com.bankhoahoc.repository;

import com.bankhoahoc.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByIsPublishedTrue();
    List<Course> findByCategoryId(Long categoryId);
    List<Course> findByInstructorId(Long instructorId);
    
    @Query("SELECT c FROM Course c WHERE " +
           "(LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Course> searchCourses(@Param("keyword") String keyword);
    
    Optional<Course> findByIdAndIsPublishedTrue(Long id);
}
