package com.lms.mentoring.course.service;

import com.lms.mentoring.course.entity.Course;
import com.lms.mentoring.course.entity.Lesson;
import com.lms.mentoring.course.repository.CourseRepository;
import com.lms.mentoring.course.repository.LessonRepository;
import com.lms.mentoring.student.entity.Student;
import com.lms.mentoring.student.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CourseService {
    private final CourseRepository repo;
    private final StudentRepository studentRepo;
    private final LessonRepository lessonRepo;

    public CourseService(CourseRepository repo, StudentRepository studentRepo, LessonRepository lessonRepo) {
        this.repo = repo;
        this.studentRepo = studentRepo;
        this.lessonRepo = lessonRepo;
    }

    public Course create(Course c) {
        if (c.getId() == null) c.setId(UUID.randomUUID());
        return repo.save(c);
    }

    @Transactional(readOnly = true)
    public Optional<Course> findById(UUID id) {
        return repo.findByIdWithSettings(id);
    }

    @Transactional(readOnly = true)
    public Optional<Course> findByIdWithLessons(UUID id) {
        return repo.findByIdWithSettingsAndLessons(id);
    }

    @Transactional(readOnly = true)
    public List<Course> findAll() {
        return repo.findAllWithSettings();
    }

    /**
     * Find all courses with pagination support.
     * @param pageable pagination parameters
     * @return page of courses
     */
    @Transactional(readOnly = true)
    public Page<Course> findAll(Pageable pageable) {
        return repo.findAllBy(pageable);
    }

    public void delete(UUID id) {
        repo.deleteById(id);
    }

    public Course update(Course c) {
        if (!repo.existsById(c.getId())) {
            throw new EntityNotFoundException("Course with id " + c.getId() + " not found");
        }
        return repo.save(c);
    }

    @Transactional(readOnly = true)
    public List<Course> findStartingBetween(LocalDateTime start, LocalDateTime end) {
        return repo.findByStartDateBetweenWithSettings(start, end);
    }

    @Transactional
    public void enrollStudent(UUID courseId, UUID studentId) {
        Course c = repo.findById(courseId).orElseThrow();
        Student s = studentRepo.findById(studentId).orElseThrow();
        c.getStudents().add(s);
        repo.save(c);
    }

    // Lesson CRUD operations

    @Transactional
    public Lesson createLesson(UUID courseId, Lesson lesson) {
        Course course = repo.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course with id " + courseId + " not found"));
        
        if (lesson.getId() == null) {
            lesson.setId(UUID.randomUUID());
        }
        lesson.setCourse(course);
        return lessonRepo.save(lesson);
    }

    @Transactional
    public Lesson updateLesson(UUID courseId, UUID lessonId, Lesson updatedLesson) {
        Lesson existing = lessonRepo.findById(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("Lesson with id " + lessonId + " not found"));
        
        if (!existing.getCourse().getId().equals(courseId)) {
            throw new IllegalArgumentException("Lesson does not belong to course " + courseId);
        }
        
        return lessonRepo.save(updatedLesson);
    }

    @Transactional(readOnly = true)
    public Optional<Lesson> findLessonById(UUID lessonId) {
        return lessonRepo.findById(lessonId);
    }

    @Transactional
    public void deleteLesson(UUID courseId, UUID lessonId) {
        Lesson lesson = lessonRepo.findById(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("Lesson with id " + lessonId + " not found"));
        
        if (!lesson.getCourse().getId().equals(courseId)) {
            throw new IllegalArgumentException("Lesson does not belong to course " + courseId);
        }
        
        lessonRepo.delete(lesson);
    }
}
