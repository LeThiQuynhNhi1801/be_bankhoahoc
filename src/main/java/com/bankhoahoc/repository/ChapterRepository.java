package com.bankhoahoc.repository;

import com.bankhoahoc.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findByCourseIdOrderByOrderIndexAsc(Long courseId);
    List<Chapter> findByCourseIdAndIsPublishedTrueOrderByOrderIndexAsc(Long courseId);
    Optional<Chapter> findByIdAndCourseId(Long id, Long courseId);
    Long countByCourseId(Long courseId);
}
