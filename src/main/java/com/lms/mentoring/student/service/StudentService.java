package com.lms.mentoring.student.service;

import com.lms.mentoring.course.entity.Course;
import com.lms.mentoring.student.entity.Student;
import com.lms.mentoring.student.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class StudentService {
    private final StudentRepository repo;

    public StudentService(StudentRepository repo) {
        this.repo = repo;
    }

    public Student create(Student s) {
        if (s.getId() == null) s.setId(UUID.randomUUID());
        return repo.save(s);
    }

    @Transactional(readOnly = true)
    public Optional<Student> findById(UUID id) {
        return repo.findByIdWithCourses(id);
    }

    @Transactional(readOnly = true)
    public List<Student> findAll() {
        return repo.findAllWithCourses();
    }

    /**
     * Find all students with pagination support.
     * @param pageable pagination parameters
     * @return page of students
     */
    @Transactional(readOnly = true)
    public Page<Student> findAll(Pageable pageable) {
        return repo.findAllBy(pageable);
    }

    public void delete(UUID id) {
        repo.deleteById(id);
    }

    public Student update(Student s) {
        if (!repo.existsById(s.getId())) {
            throw new EntityNotFoundException("Student with id " + s.getId() + " not found");
        }
        return repo.save(s);
    }

    @Transactional
    public boolean chargeCoins(UUID studentId, BigDecimal amount) {
        Student st = repo.findById(studentId).orElseThrow();
        BigDecimal balance = st.getCoins() == null ? BigDecimal.ZERO : st.getCoins();
        if (balance.compareTo(amount) < 0) return false;
        st.setCoins(balance.subtract(amount));
        repo.save(st);
        return true;
    }

    @Transactional(readOnly = true)
    public List<Course> findCoursesByStudentId(UUID studentId) {
        return repo.findByIdWithCourses(studentId)
                .map(Student::getCourses)
                .orElse(Collections.emptyList());
    }

    /**
     * Find students enrolled in a specific course.
     * @param courseId the course ID
     * @return list of students
     */
    @Transactional(readOnly = true)
    public List<Student> findByCourseId(UUID courseId) {
        return repo.findByCourseId(courseId);
    }
}
