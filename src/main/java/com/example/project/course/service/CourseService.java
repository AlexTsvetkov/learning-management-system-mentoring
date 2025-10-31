package com.example.project.course.service;

import com.example.project.course.model.Course;
import com.example.project.course.repository.CourseRepository;
import com.example.project.student.model.Student;
import com.example.project.student.repository.StudentRepository;
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

    public Optional<Course> findById(UUID id) { return repo.findById(id); }
    public List<Course> findAll() { return repo.findAll(); }
    public void delete(UUID id) { repo.deleteById(id); }
    public Course update(Course c) { return repo.save(c); }

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
