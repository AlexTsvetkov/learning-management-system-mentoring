package com.lms.mentoring.enrollment.controller;

import com.lms.mentoring.course.dto.CourseDto;
import com.lms.mentoring.course.entity.Course;
import com.lms.mentoring.course.mapper.CourseMapper;
import com.lms.mentoring.enrollment.dto.PurchaseResult;
import com.lms.mentoring.enrollment.service.CoursePurchaseService;
import com.lms.mentoring.student.dto.StudentDto;
import com.lms.mentoring.student.entity.Student;
import com.lms.mentoring.student.mapper.StudentMapper;
import com.lms.mentoring.util.TestDataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

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

    private Student student;
    private Course course;
    private StudentDto studentDto;
    private CourseDto courseDto;

    @BeforeEach
    void setUp() {
        student = TestDataGenerator.createDefaultStudent();
        course = TestDataGenerator.createDefaultCourse();
        studentDto = TestDataGenerator.createDefaultStudentDto();
        studentDto.setId(student.getId());
        studentDto.setFirstName(student.getFirstName());
        studentDto.setLastName(student.getLastName());
        courseDto = TestDataGenerator.createDefaultCourseDto();
        courseDto.setId(course.getId());
        courseDto.setTitle(course.getTitle());
    }

    @Test
    void purchaseCourse_WhenValidStudentAndCourse_ShouldReturnSuccessMessage() {
        // given
        PurchaseResult<Student, Course> result = new PurchaseResult<>(student, course);
        when(purchaseService.purchaseCourse(student.getId(), course.getId())).thenReturn(result);
        when(studentMapper.toDto(student)).thenReturn(studentDto);
        when(courseMapper.toDto(course)).thenReturn(courseDto);

        // when
        ResponseEntity<String> response = controller.purchaseCourse(student.getId(), course.getId());

        // then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody())
                .contains(course.getTitle())
                .contains(student.getFirstName())
                .contains(student.getLastName());
        verify(purchaseService).purchaseCourse(student.getId(), course.getId());
        verify(studentMapper).toDto(student);
        verify(courseMapper).toDto(course);
    }

    @Test
    void purchaseCourse_WhenServiceFails_ShouldPropagateException() {
        // given
        when(purchaseService.purchaseCourse(student.getId(), course.getId()))
                .thenThrow(new IllegalArgumentException("Insufficient coins"));

        // when & then
        assertThatThrownBy(() -> controller.purchaseCourse(student.getId(), course.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient coins");
        verify(purchaseService).purchaseCourse(student.getId(), course.getId());
        verifyNoInteractions(studentMapper, courseMapper);
    }
}