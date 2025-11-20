package com.lms.mentoring.enrollment.service;

import com.lms.mentoring.course.entity.Course;
import com.lms.mentoring.course.repository.CourseRepository;
import com.lms.mentoring.enrollment.dto.PurchaseResult;
import com.lms.mentoring.student.entity.Student;
import com.lms.mentoring.student.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CoursePurchaseService {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public PurchaseResult<Student, Course> purchaseCourse(UUID studentId, UUID courseId) {
        // 1️⃣ Find entities
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student not found: " + studentId));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found: " + courseId));

        // 2️⃣ Prevent re-purchase
        if (student.getCourses().contains(course)) {
            throw new IllegalStateException("Student already enrolled in this course");
        }

        // 3️⃣ Validate balance
        BigDecimal studentCoins = student.getCoins() != null ? student.getCoins() : BigDecimal.ZERO;
        BigDecimal coursePrice = course.getCoinsPaid() != null ? course.getCoinsPaid() : BigDecimal.ZERO;

        if (studentCoins.compareTo(coursePrice) < 0) {
            throw new IllegalArgumentException("Insufficient coins. Balance: " + studentCoins + ", required: " + coursePrice);
        }

        // 4️⃣ Deduct and enroll
        student.setCoins(studentCoins.subtract(coursePrice));
        student.getCourses().add(course);

        // 5️⃣ Save (JPA will handle the join table)
        studentRepository.save(student);

        return new PurchaseResult<>(student, course);
    }
}
