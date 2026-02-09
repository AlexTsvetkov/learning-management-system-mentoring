package com.lms.mentoring.enrollment.controller;

import com.lms.mentoring.course.dto.CourseDto;
import com.lms.mentoring.course.entity.Course;
import com.lms.mentoring.course.mapper.CourseMapper;
import com.lms.mentoring.enrollment.dto.PurchaseResult;
import com.lms.mentoring.enrollment.service.CoursePurchaseService;
import com.lms.mentoring.student.dto.StudentDto;
import com.lms.mentoring.student.entity.Student;
import com.lms.mentoring.student.mapper.StudentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoursePurchaseControllerTest {

    @Mock
    private CoursePurchaseService purchaseService;

    @Mock
    private StudentMapper studentMapper;

    @Mock
    private CourseMapper courseMapper;

    @InjectMocks
    private CoursePurchaseController controller;

    private UUID studentId;
    private UUID courseId;
    private Student student;
    private Course course;
    private StudentDto studentDto;
    private CourseDto courseDto;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        courseId = UUID.randomUUID();

        student = Student.builder()
                .id(studentId)
                .firstName("John")
                .lastName("Doe")
                .coins(BigDecimal.valueOf(100))
                .build();

        course = Course.builder()
                .id(courseId)
                .title("Java Basics")
                .coinsPaid(BigDecimal.valueOf(50))
                .build();

        studentDto = StudentDto.builder()
                .id(studentId)
                .firstName("John")
                .lastName("Doe")
                .build();

        courseDto = CourseDto.builder()
                .id(courseId)
                .title("Java Basics")
                .build();
    }

    @Test
    void purchaseCourse_WhenValidStudentAndCourse_ShouldReturnSuccessMessage() {
        // given
        PurchaseResult<Student, Course> result = new PurchaseResult<>(student, course);
        when(purchaseService.purchaseCourse(studentId, courseId)).thenReturn(result);
        when(studentMapper.toDto(student)).thenReturn(studentDto);
        when(courseMapper.toDto(course)).thenReturn(courseDto);

        // when
        ResponseEntity<String> response = controller.purchaseCourse(studentId, courseId);

        // then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody())
                .isEqualTo("Course 'Java Basics' purchased successfully by student John Doe");
        verify(purchaseService).purchaseCourse(studentId, courseId);
        verify(studentMapper).toDto(student);
        verify(courseMapper).toDto(course);
    }

    @Test
    void purchaseCourse_WhenServiceFails_ShouldPropagateException() {
        // given
        when(purchaseService.purchaseCourse(studentId, courseId))
                .thenThrow(new IllegalArgumentException("Insufficient coins"));

        // when & then
        assertThatThrownBy(() -> controller.purchaseCourse(studentId, courseId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient coins");
        verify(purchaseService).purchaseCourse(studentId, courseId);
        verifyNoInteractions(studentMapper, courseMapper);
    }
}