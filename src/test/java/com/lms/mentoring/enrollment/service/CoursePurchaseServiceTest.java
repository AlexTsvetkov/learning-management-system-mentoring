package com.lms.mentoring.enrollment.service;

import com.lms.mentoring.course.entity.Course;
import com.lms.mentoring.course.mapper.CourseMapper;
import com.lms.mentoring.course.repository.CourseRepository;
import com.lms.mentoring.enrollment.dto.PurchaseResult;
import com.lms.mentoring.student.dto.StudentDto;
import com.lms.mentoring.student.entity.Student;
import com.lms.mentoring.student.mapper.StudentMapper;
import com.lms.mentoring.student.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CoursePurchaseServiceTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private StudentMapper studentMapper;
    @Mock
    private CourseMapper courseMapper;

    @InjectMocks
    private CoursePurchaseService service;

    private UUID studentId;
    private UUID courseId;
    private Student student;
    private Course course;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        studentId = UUID.randomUUID();
        courseId = UUID.randomUUID();

        student = Student.builder()
                .id(studentId)
                .firstName("John")
                .lastName("Doe")
                .coins(new BigDecimal("100"))
                .courses(new ArrayList<>())
                .build();

        course = Course.builder()
                .id(courseId)
                .title("Spring Boot Masterclass")
                .coinsPaid(new BigDecimal("50"))
                .build();
    }

    @Test
    void shouldPurchaseCourseSuccessfully() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(studentMapper.toDto(any(Student.class))).thenReturn(new StudentDto());
        when(courseMapper.toDto(any(Course.class))).thenReturn(null);
        when(studentRepository.save(any(Student.class))).thenReturn(student);

        PurchaseResult result = service.purchaseCourse(studentId, courseId);

        assertNotNull(result);
        assertEquals(new BigDecimal("50"), student.getCoins());
        assertTrue(student.getCourses().contains(course));
        verify(studentRepository).save(student);
    }

    @Test
    void shouldThrowWhenStudentNotFound() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> service.purchaseCourse(studentId, courseId));

        assertTrue(ex.getMessage().contains("Student not found"));
    }

    @Test
    void shouldThrowWhenCourseNotFound() {
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> service.purchaseCourse(studentId, courseId));

        assertTrue(ex.getMessage().contains("Course not found"));
    }

    @Test
    void shouldThrowWhenAlreadyPurchased() {
        student.getCourses().add(course);

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.purchaseCourse(studentId, courseId));

        assertEquals("Student already enrolled in this course", ex.getMessage());
    }

    @Test
    void shouldThrowWhenInsufficientCoins() {
        student.setCoins(new BigDecimal("10"));
        course.setCoinsPaid(new BigDecimal("50"));

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.purchaseCourse(studentId, courseId));

        assertTrue(ex.getMessage().contains("Insufficient coins"));
    }
}
