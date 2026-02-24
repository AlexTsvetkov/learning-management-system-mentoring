package com.lms.mentoring.course.repository;

import com.lms.mentoring.course.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {
    
    @EntityGraph(attributePaths = {"settings"})
    List<Course> findBySettings_StartDateBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find all courses with settings eagerly fetched to avoid N+1 problem.
     * Uses @EntityGraph for declarative fetch optimization.
     */
    @EntityGraph(attributePaths = {"settings"})
    @Query("SELECT c FROM Course c")
    List<Course> findAllWithSettings();

    /**
     * Find all courses with settings and lessons eagerly fetched.
     * Uses @EntityGraph to avoid N+1 queries.
     */
    @EntityGraph(attributePaths = {"settings", "lessons"})
    @Query("SELECT DISTINCT c FROM Course c")
    List<Course> findAllWithSettingsAndLessons();

    /**
     * Find course by ID with settings eagerly fetched.
     * Uses @EntityGraph for single entity fetch optimization.
     */
    @EntityGraph(attributePaths = {"settings"})
    @Query("SELECT c FROM Course c WHERE c.id = :id")
    Optional<Course> findByIdWithSettings(@Param("id") UUID id);

    /**
     * Find course by ID with settings and lessons eagerly fetched.
     * Uses @EntityGraph for comprehensive entity fetch.
     */
    @EntityGraph(attributePaths = {"settings", "lessons"})
    @Query("SELECT c FROM Course c WHERE c.id = :id")
    Optional<Course> findByIdWithSettingsAndLessons(@Param("id") UUID id);

    /**
     * Paginated query for courses with settings.
     * Uses @EntityGraph for efficient pagination with associated entities.
     */
    @EntityGraph(attributePaths = {"settings"})
    Page<Course> findAllBy(Pageable pageable);

    /**
     * Find courses starting between dates with settings eagerly fetched.
     * Uses @EntityGraph for declarative fetching.
     */
    @EntityGraph(attributePaths = {"settings"})
    @Query("SELECT c FROM Course c WHERE c.settings.startDate BETWEEN :start AND :end")
    List<Course> findByStartDateBetweenWithSettings(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
