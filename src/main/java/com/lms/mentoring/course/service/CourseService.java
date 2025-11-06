package com.lms.mentoring.course.service;

import com.lms.mentoring.course.entity.Course;
import com.lms.mentoring.course.repository.CourseRepository;
import com.lms.mentoring.student.entity.Student;
import com.lms.mentoring.student.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
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

    public CourseService(CourseRepository repo, StudentRepository studentRepo) {
        this.repo = repo;
        this.studentRepo = studentRepo;
    }

    public Course create(Course c) {
        if (c.getId() == null) c.setId(UUID.randomUUID());
        return repo.save(c);
    }

    public Optional<Course> findById(UUID id) {
        return repo.findById(id);
    }

    public List<Course> findAll() {
        return repo.findAllWithSettings();
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

    public List<Course> findStartingBetween(LocalDateTime start, LocalDateTime end) {
        return repo.findBySettings_StartDateBetween(start, end);
    }

    @Transactional
    public void enrollStudent(UUID courseId, UUID studentId) {
        Course c = repo.findById(courseId).orElseThrow();
        Student s = studentRepo.findById(studentId).orElseThrow();
        c.getStudents().add(s);
        repo.save(c);
    }
}
