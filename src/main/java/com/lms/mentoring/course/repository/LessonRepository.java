package com.lms.mentoring.course.repository;

import com.lms.mentoring.course.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {
    
    List<Lesson> findByCourseId(UUID courseId);
}