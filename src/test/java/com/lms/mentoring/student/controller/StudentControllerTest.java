package com.lms.mentoring.student.controller;

import com.lms.mentoring.course.dto.CourseDto;
import com.lms.mentoring.course.entity.Course;
import com.lms.mentoring.course.mapper.CourseMapper;
import com.lms.mentoring.student.dto.StudentDto;
import com.lms.mentoring.student.entity.Student;
import com.lms.mentoring.student.mapper.StudentMapper;
import com.lms.mentoring.student.service.StudentService;
import com.lms.mentoring.util.TestDataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
        Student student = TestDataGenerator.createDefaultStudent();
        StudentDto dto = TestDataGenerator.createDefaultStudentDto();
        dto.setId(student.getId());
        dto.setFirstName(student.getFirstName());
        when(service.findAll()).thenReturn(List.of(student));
        when(mapper.toDto(student)).thenReturn(dto);

        // when
        List<StudentDto> result = controller.all();

        // then
        assertEquals(1, result.size());
        assertEquals(student.getFirstName(), result.getFirst().getFirstName());
        verify(service, times(1)).findAll();
        verify(mapper, times(1)).toDto(student);
    }

    @Test
    void get_WhenStudentExists_ShouldReturnOkWithStudentDto() {
        // given
        Student student = TestDataGenerator.createDefaultStudent();
        StudentDto dto = TestDataGenerator.createDefaultStudentDto();
        dto.setId(student.getId());
        when(service.findById(student.getId())).thenReturn(Optional.of(student));
        when(mapper.toDto(student)).thenReturn(dto);

        // when
        ResponseEntity<StudentDto> response = controller.get(student.getId());

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dto.getFirstName(), response.getBody().getFirstName());
    }

    @Test
    void get_WhenStudentNotFound_ShouldReturnNotFound() {
        // given
        Student student = TestDataGenerator.createDefaultStudent();
        when(service.findById(student.getId())).thenReturn(Optional.empty());

        // when
        ResponseEntity<StudentDto> response = controller.get(student.getId());

        // then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void create_WhenValidDto_ShouldReturnCreatedWithStudentDto() {
        // given
        StudentDto inputDto = TestDataGenerator.createDefaultStudentDto();
        inputDto.setId(null);
        Student student = TestDataGenerator.createStudentWithoutId();
        Student created = TestDataGenerator.createDefaultStudent();
        StudentDto createdDto = TestDataGenerator.createDefaultStudentDto();
        createdDto.setId(created.getId());
        when(mapper.toEntity(inputDto)).thenReturn(student);
        when(service.create(student)).thenReturn(created);
        when(mapper.toDto(created)).thenReturn(createdDto);

        // when
        ResponseEntity<StudentDto> response = controller.create(inputDto);

        // then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(createdDto.getFirstName(), response.getBody().getFirstName());
        assertNotNull(response.getBody().getId());
    }

    @Test
    void update_WhenValidDto_ShouldReturnOkWithUpdatedStudentDto() {
        // given
        Student student = TestDataGenerator.createDefaultStudent();
        StudentDto inputDto = TestDataGenerator.createDefaultStudentDto();
        Student updated = TestDataGenerator.createDefaultStudent();
        updated.setId(student.getId());
        updated.setFirstName("UpdatedName");
        StudentDto updatedDto = TestDataGenerator.createDefaultStudentDto();
        updatedDto.setId(student.getId());
        updatedDto.setFirstName("UpdatedName");
        when(mapper.toEntity(inputDto)).thenReturn(student);
        when(service.update(student)).thenReturn(updated);
        when(mapper.toDto(updated)).thenReturn(updatedDto);

        // when
        ResponseEntity<StudentDto> response = controller.update(student.getId(), inputDto);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("UpdatedName", response.getBody().getFirstName());
        assertEquals(student.getId(), response.getBody().getId());
    }

    @Test
    void delete_WhenStudentExists_ShouldReturnNoContent() {
        // given
        Student student = TestDataGenerator.createDefaultStudent();
        doNothing().when(service).delete(student.getId());

        // when
        ResponseEntity<Void> response = controller.delete(student.getId());

        // then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(service, times(1)).delete(student.getId());
    }

    @Test
    void getCourses_WhenStudentHasCourses_ShouldReturnCourseDtoList() {
        // given
        Student student = TestDataGenerator.createDefaultStudent();
        Course course = TestDataGenerator.createDefaultCourse();
        CourseDto courseDto = TestDataGenerator.createDefaultCourseDto();
        courseDto.setId(course.getId());
        when(service.findCoursesByStudentId(student.getId())).thenReturn(List.of(course));
        when(courseMapper.toDto(course)).thenReturn(courseDto);

        // when
        List<CourseDto> result = controller.getCourses(student.getId());

        // then
        assertEquals(1, result.size());
        assertEquals(course.getTitle(), result.getFirst().getTitle());
        verify(service, times(1)).findCoursesByStudentId(student.getId());
        verify(courseMapper, times(1)).toDto(course);
    }

    @Test
    void getCourses_WhenStudentHasNoCourses_ShouldReturnEmptyList() {
        // given
        Student student = TestDataGenerator.createDefaultStudent();
        when(service.findCoursesByStudentId(student.getId())).thenReturn(Collections.emptyList());

        // when
        List<CourseDto> result = controller.getCourses(student.getId());

        // then
        assertTrue(result.isEmpty());
        verify(service, times(1)).findCoursesByStudentId(student.getId());
    }
}