package com.lms.mentoring.course.repository;

import com.lms.mentoring.course.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {
    List<Course> findBySettings_StartDateBetween(LocalDateTime start, LocalDateTime end);
}
