package com.lms.mentoring.course.repository;

import com.lms.mentoring.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {
    List<Course> findBySettings_StartDateBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.settings")
    List<Course> findAllWithSettings();
}
