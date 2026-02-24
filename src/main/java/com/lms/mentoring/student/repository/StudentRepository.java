package com.lms.mentoring.student.repository;

import com.lms.mentoring.student.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {

    /**
     * Find all students without fetching courses (basic list).
     * Use this when you don't need course information.
     */
    @Query("SELECT s FROM Student s")
    List<Student> findAllBasic();

    /**
     * Find all students with courses eagerly fetched to avoid N+1 problem.
     * Uses @EntityGraph for declarative fetch optimization.
     * Also fetches courses.settings to avoid LazyInitializationException.
     */
    @EntityGraph(attributePaths = {"courses", "courses.settings"})
    @Query("SELECT DISTINCT s FROM Student s")
    List<Student> findAllWithCourses();

    /**
     * Find student by ID with courses eagerly fetched.
     * Uses @EntityGraph for single entity fetch optimization.
     * Also fetches courses.settings to avoid LazyInitializationException.
     */
    @EntityGraph(attributePaths = {"courses", "courses.settings"})
    @Query("SELECT s FROM Student s WHERE s.id = :id")
    Optional<Student> findByIdWithCourses(@Param("id") UUID id);

    /**
     * Paginated query for students.
     * Uses @EntityGraph for efficient pagination.
     * Also fetches courses.settings to avoid LazyInitializationException.
     */
    @EntityGraph(attributePaths = {"courses", "courses.settings"})
    Page<Student> findAllBy(Pageable pageable);

    /**
     * Find students enrolled in a specific course.
     * Uses @EntityGraph for efficient fetching.
     * Also fetches courses.settings to avoid LazyInitializationException.
     */
    @EntityGraph(attributePaths = {"courses", "courses.settings"})
    @Query("SELECT DISTINCT s FROM Student s JOIN s.courses c WHERE c.id = :courseId")
    List<Student> findByCourseId(@Param("courseId") UUID courseId);

    /**
     * Find students by locale for notification purposes.
     */
    List<Student> findByLocale(String locale);
}
