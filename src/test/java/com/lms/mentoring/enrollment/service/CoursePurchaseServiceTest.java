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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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
    void purchaseCourse_WhenValidStudentAndCourse_ShouldDeductCoinsAndEnroll() {
        // given
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(studentRepository.save(any(Student.class))).thenReturn(student);

        // when
        PurchaseResult result = service.purchaseCourse(studentId, courseId);

        // then
        assertThat(result).isNotNull();
        assertThat(student.getCoins()).isEqualByComparingTo(new BigDecimal("50"));
        assertThat(student.getCourses()).contains(course);
        verify(studentRepository).save(student);
    }

    @Test
    void purchaseCourse_WhenStudentNotFound_ShouldThrowEntityNotFoundException() {
        // given
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.purchaseCourse(studentId, courseId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Student not found");
    }

    @Test
    void purchaseCourse_WhenCourseNotFound_ShouldThrowEntityNotFoundException() {
        // given
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.purchaseCourse(studentId, courseId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Course not found");
    }

    @Test
    void purchaseCourse_WhenAlreadyEnrolled_ShouldThrowIllegalStateException() {
        // given
        student.getCourses().add(course);
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        // when & then
        assertThatThrownBy(() -> service.purchaseCourse(studentId, courseId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Student already enrolled in this course");
    }

    @Test
    void purchaseCourse_WhenInsufficientCoins_ShouldThrowIllegalArgumentException() {
        // given
        student.setCoins(new BigDecimal("10"));
        course.setCoinsPaid(new BigDecimal("50"));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        // when & then
        assertThatThrownBy(() -> service.purchaseCourse(studentId, courseId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient coins");
    }
}