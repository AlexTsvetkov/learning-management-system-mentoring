package com.lms.mentoring.student.controller;

import com.lms.mentoring.course.dto.CourseDto;
import com.lms.mentoring.course.entity.Course;
import com.lms.mentoring.course.mapper.CourseMapper;
import com.lms.mentoring.student.dto.StudentDto;
import com.lms.mentoring.student.entity.Student;
import com.lms.mentoring.student.mapper.StudentMapper;
import com.lms.mentoring.student.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentControllerTest {

    private StudentService service;
    private StudentMapper mapper;
    private CourseMapper courseMapper;
    private StudentController controller;

    @BeforeEach
    void setUp() {
        service = mock(StudentService.class);
        mapper = mock(StudentMapper.class);
        courseMapper = mock(CourseMapper.class);
        controller = new StudentController(service, mapper, courseMapper);
    }

    @Test
    void all_WhenStudentsExist_ShouldReturnStudentDtoList() {
        // given
        Student student = Student.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .coins(BigDecimal.valueOf(100))
                .build();
        StudentDto dto = StudentDto.builder()
                .id(student.getId())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .coins(student.getCoins())
                .build();
        when(service.findAll()).thenReturn(List.of(student));
        when(mapper.toDto(student)).thenReturn(dto);

        // when
        List<StudentDto> result = controller.all();

        // then
        assertEquals(1, result.size());
        assertEquals("John", result.getFirst().getFirstName());
        verify(service, times(1)).findAll();
        verify(mapper, times(1)).toDto(student);
    }

    @Test
    void get_WhenStudentExists_ShouldReturnOkWithStudentDto() {
        // given
        UUID id = UUID.randomUUID();
        Student student = Student.builder().id(id).firstName("Jane").build();
        StudentDto dto = StudentDto.builder().id(id).firstName("Jane").build();
        when(service.findById(id)).thenReturn(Optional.of(student));
        when(mapper.toDto(student)).thenReturn(dto);

        // when
        ResponseEntity<StudentDto> response = controller.get(id);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Jane", response.getBody().getFirstName());
    }

    @Test
    void get_WhenStudentNotFound_ShouldReturnNotFound() {
        // given
        UUID id = UUID.randomUUID();
        when(service.findById(id)).thenReturn(Optional.empty());

        // when
        ResponseEntity<StudentDto> response = controller.get(id);

        // then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void create_WhenValidDto_ShouldReturnCreatedWithStudentDto() {
        // given
        StudentDto dto = StudentDto.builder().firstName("Alice").build();
        Student student = Student.builder().firstName("Alice").build();
        Student created = Student.builder().id(UUID.randomUUID()).firstName("Alice").build();
        StudentDto createdDto = StudentDto.builder().id(created.getId()).firstName("Alice").build();
        when(mapper.toEntity(dto)).thenReturn(student);
        when(service.create(student)).thenReturn(created);
        when(mapper.toDto(created)).thenReturn(createdDto);

        // when
        ResponseEntity<StudentDto> response = controller.create(dto);

        // then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Alice", response.getBody().getFirstName());
        assertNotNull(response.getBody().getId());
    }

    @Test
    void update_WhenValidDto_ShouldReturnOkWithUpdatedStudentDto() {
        // given
        UUID id = UUID.randomUUID();
        StudentDto dto = StudentDto.builder().firstName("Bob").build();
        Student student = Student.builder().id(id).firstName("Bob").build();
        Student updated = Student.builder().id(id).firstName("Bobby").build();
        StudentDto updatedDto = StudentDto.builder().id(id).firstName("Bobby").build();
        when(mapper.toEntity(dto)).thenReturn(student);
        when(service.update(student)).thenReturn(updated);
        when(mapper.toDto(updated)).thenReturn(updatedDto);

        // when
        ResponseEntity<StudentDto> response = controller.update(id, dto);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Bobby", response.getBody().getFirstName());
        assertEquals(id, response.getBody().getId());
    }

    @Test
    void delete_WhenStudentExists_ShouldReturnNoContent() {
        // given
        UUID id = UUID.randomUUID();
        doNothing().when(service).delete(id);

        // when
        ResponseEntity<Void> response = controller.delete(id);

        // then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(service, times(1)).delete(id);
    }

    @Test
    void getCourses_WhenStudentHasCourses_ShouldReturnCourseDtoList() {
        // given
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        Course course = Course.builder()
                .id(courseId)
                .title("Spring Boot Fundamentals")
                .price(BigDecimal.valueOf(99.99))
                .build();
        CourseDto courseDto = CourseDto.builder()
                .id(courseId)
                .title("Spring Boot Fundamentals")
                .price(BigDecimal.valueOf(99.99))
                .build();
        when(service.findCoursesByStudentId(studentId)).thenReturn(List.of(course));
        when(courseMapper.toDto(course)).thenReturn(courseDto);

        // when
        List<CourseDto> result = controller.getCourses(studentId);

        // then
        assertEquals(1, result.size());
        assertEquals("Spring Boot Fundamentals", result.getFirst().getTitle());
        assertEquals(courseId, result.getFirst().getId());
        verify(service, times(1)).findCoursesByStudentId(studentId);
        verify(courseMapper, times(1)).toDto(course);
    }

    @Test
    void getCourses_WhenStudentHasNoCourses_ShouldReturnEmptyList() {
        // given
        UUID studentId = UUID.randomUUID();
        when(service.findCoursesByStudentId(studentId)).thenReturn(Collections.emptyList());

        // when
        List<CourseDto> result = controller.getCourses(studentId);

        // then
        assertTrue(result.isEmpty());
        verify(service, times(1)).findCoursesByStudentId(studentId);
    }
}